package com.firebirdberlin.nightdream;

import java.util.Calendar;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.service.dreams.DreamService;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import de.greenrobot.event.EventBus;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.events.OnLightSensorValueTimeout;
import com.firebirdberlin.nightdream.events.OnNewAmbientNoiseValue;
import com.firebirdberlin.nightdream.events.OnNewLightSensorValue;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.models.TimeRange;
import com.firebirdberlin.nightdream.services.AlarmHandlerService;
import com.firebirdberlin.nightdream.services.RadioStreamService;
import com.firebirdberlin.nightdream.receivers.NightModeReceiver;
import com.firebirdberlin.nightdream.ui.NightDreamUI;


public class NightDreamService extends DreamService implements View.OnTouchListener,
                                                               NightModeReceiver.Event {

    AlarmClock alarmClock;
    ImageView background_image;

    SensorManager sensorManager;
    Sensor lightSensor;
    int mode;

    GestureDetector.SimpleOnGestureListener mSimpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (mySettings.doubleTapToFinish) {
                finish();
                return true;
            }
            return false;
        }
    };

    private float last_ambient;
    private double last_ambient_noise = 32000.;
    private NightDreamUI nightDreamUI = null;
    private NotificationReceiver nReceiver;
    private NightModeReceiver nightModeReceiver = null;
    private ReceiverRadioStream receiverRadioStream = null;
    private TextView alarmTime = null;

    private double NOISE_AMPLITUDE_WAKE  = Config.NOISE_AMPLITUDE_WAKE;
    private double NOISE_AMPLITUDE_SLEEP = Config.NOISE_AMPLITUDE_SLEEP;
    private Settings mySettings = null;
    private GestureDetector mGestureDetector = null;

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setContentView(R.layout.main);

        nightDreamUI = new NightDreamUI(this, getWindow(), true);
        mySettings = new Settings(this);

        if (mySettings.force_auto_rotation ) {
            NightDreamActivity.start(this);
            finish();
        }

        setInteractive(true);
        setFullscreen(true);
        setScreenBright(true);

        NOISE_AMPLITUDE_SLEEP *= mySettings.sensitivity;
        NOISE_AMPLITUDE_WAKE  *= mySettings.sensitivity;

        alarmClock = (AlarmClock) findViewById(R.id.AlarmClock);
        alarmTime = (TextView) findViewById(R.id.textview_alarm_time);
        alarmClock.setSettings(mySettings);

        mGestureDetector = new GestureDetector(this, mSimpleOnGestureListener);
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (lightSensor == null){
            Toast.makeText(NightDreamService.this, "No Light Sensor!", Toast.LENGTH_LONG).show();
            mode = 2;
            last_ambient = 30.0f;
        }

        nightModeReceiver = NightModeReceiver.register(this, this);
        nReceiver = registerNotificationReceiver();
        receiverRadioStream = registerReceiverRadioStream();

        nReceiver.setColor(mySettings.secondaryColor);
        background_image = (ImageView)findViewById(R.id.background_view);
        background_image.setOnTouchListener(this);

        alarmTime.setClickable(false);
        if ( !mySettings.useInternalAlarm && Build.VERSION.SDK_INT >= 19 ) {
            alarmTime.setOnClickListener(onStockAlarmTimeClickListener);
            alarmTime.setClickable(true);
        }
    }

    View.OnClickListener onStockAlarmTimeClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (Build.VERSION.SDK_INT < 19) return;

            Intent mClockIntent = new Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS);
            mClockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mClockIntent);
            finish();
        }
    };

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();

        nightDreamUI.onStart();
        nightDreamUI.onResume();
        setupNightMode();
        setupRadioStreamUI();
        EventBus.getDefault().register(this);

        // ask for active notifications
        if (Build.VERSION.SDK_INT >= 18){
            Intent i = new Intent(Config.ACTION_NOTIFICATION_LISTENER);
            i.putExtra("command","list");
            sendBroadcast(i);
        }
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        nightDreamUI.restoreRingerMode();
        nightDreamUI.onPause();
        nightDreamUI.onStop();
        EventBus.getDefault().unregister(this);
        unregister(nightModeReceiver);
        unregisterLocalReceiver(nReceiver);
        unregister(receiverRadioStream);

        //stop notification listener service
        if (Build.VERSION.SDK_INT >= 18){
            Intent i = new Intent(Config.ACTION_NOTIFICATION_LISTENER);
            i.putExtra("command","release");
            sendBroadcast(i);
        }
    }

    private void unregister(BroadcastReceiver receiver) {
        try {
            unregisterReceiver(receiver);
        } catch ( IllegalArgumentException e ) {

        }
    }

    private void unregisterLocalReceiver(BroadcastReceiver receiver) {
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        } catch ( IllegalArgumentException ignored) {

        }
    }

    @Override
    public void onDestroy(){
        if (nightDreamUI != null) {
            nightDreamUI.onDestroy();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d("NightDreamService","onDetachedFromWindow() called.");
    }

    public void onSwitchNightMode() {
        setupNightMode();
    }

    void setupNightMode() {
        if (mySettings.nightModeActivationMode != Settings.NIGHT_MODE_ACTIVATION_SCHEDULED) return;
        Calendar start = new SimpleTime(mySettings.nightModeTimeRangeStart).getCalendar();
        Calendar end = new SimpleTime(mySettings.nightModeTimeRangeEnd).getCalendar();

        TimeRange timerange = new TimeRange(start, end);
        int new_mode = ( timerange.inRange() ) ? 0 : 2;
        if ( lightSensor == null ) {
            last_ambient = ( new_mode == 2 ) ? 400.f : mySettings.minIlluminance;
        }
        setMode(new_mode);
        NightModeReceiver.schedule(this, timerange);
    }

    class ReceiverRadioStream extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            setupRadioStreamUI();
        }
    }

    void setupRadioStreamUI() {
        switch (RadioStreamService.streamingMode) {
            case RADIO:
                nightDreamUI.setRadioIconActive();
                break;
            default:
                nightDreamUI.setRadioIconInactive();
                break;
        }
    }

    private ReceiverRadioStream registerReceiverRadioStream() {
        ReceiverRadioStream receiver = new ReceiverRadioStream();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.ACTION_RADIO_STREAM_STARTED);
        filter.addAction(Config.ACTION_RADIO_STREAM_STOPPED);
        registerReceiver(receiver, filter);
        return receiver;
    }

    private NotificationReceiver registerNotificationReceiver(){
        NotificationReceiver receiver = new NotificationReceiver(getWindow());
        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.ACTION_NOTIFICATION_LISTENER);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
        return receiver;
    }

    public boolean onTouch(View view, MotionEvent e) {
        return mGestureDetector.onTouchEvent(e) || nightDreamUI.onTouch(view, e, last_ambient);
    }

    // click on the settings icon
    public void onSettingsClick(View v) {
        Intent intent = new Intent(this, PreferencesActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    public void onRadioClick(View v) {
        if ( AlarmHandlerService.alarmIsRunning() ) {
            AlarmHandlerService.stop(this);
        }

        if ( mySettings.radioStreamURLUI.isEmpty() ) {
            PreferencesActivity.start(this, PreferencesActivity.PREFERENCES_SCREEN_WEB_RADIO_INDEX);
            return;
        }

        toggleRadioStreamState();
    }

    private void toggleRadioStreamState() {
        final Context context = this;
        if ( RadioStreamService.streamingMode == RadioStreamService.StreamingMode.RADIO ) {
            RadioStreamService.stop(this);
            return;
        }

        if (Utility.hasNetworkConnection(this)) {
            RadioStreamService.startStream(this);
        } else { // no network connection
            Toast.makeText(context, R.string.message_no_data_connection, Toast.LENGTH_LONG).show();
        }
    }
    public void onNightModeClick(View v) {
        // toggle the night mode manually
        if ( lightSensor == null
                || mySettings.nightModeActivationMode == Settings.NIGHT_MODE_ACTIVATION_MANUAL ) {
            if ( lightSensor == null ) {
                last_ambient = ( mode == 0 ) ? 400.f : mySettings.minIlluminance;
            }
            int new_mode = ( mode == 0) ? 2 : 0;
            setMode(new_mode);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        if (nightDreamUI != null) {
            nightDreamUI.onConfigurationChanged(newConfig);
        }
    }

    private void setMode(int new_mode) {
        nightDreamUI.setMode(new_mode, last_ambient);

        setScreenBright(new_mode >= 2);
        mode = new_mode;
    }

    public void onEvent(OnNewLightSensorValue event){
        last_ambient = event.value;
        int new_mode = nightDreamUI.determineScreenMode(mode, last_ambient, last_ambient_noise);
        setMode(new_mode);
    }

    public void onEvent(OnLightSensorValueTimeout event){
        last_ambient = (event.value >= 0.f) ? event.value : mySettings.minIlluminance;
        int new_mode = nightDreamUI.determineScreenMode(mode, last_ambient, last_ambient_noise);
        setMode(new_mode);
    }

    public void onEvent(OnNewAmbientNoiseValue event) {
        last_ambient_noise = event.value;
        int new_mode = nightDreamUI.determineScreenMode(mode, last_ambient, last_ambient_noise);
        setMode(new_mode);
    }
}
