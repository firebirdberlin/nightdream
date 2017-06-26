package com.firebirdberlin.nightdream;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebirdberlin.nightdream.events.OnLightSensorValueTimeout;
import com.firebirdberlin.nightdream.events.OnNewAmbientNoiseValue;
import com.firebirdberlin.nightdream.events.OnNewLightSensorValue;
import com.firebirdberlin.nightdream.events.OnPowerConnected;
import com.firebirdberlin.nightdream.events.OnPowerDisconnected;
import com.firebirdberlin.nightdream.models.BatteryValue;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.models.TimeRange;
import com.firebirdberlin.nightdream.receivers.NightModeReceiver;
import com.firebirdberlin.nightdream.receivers.ScreenReceiver;
import com.firebirdberlin.nightdream.repositories.BatteryStats;
import com.firebirdberlin.nightdream.services.AlarmHandlerService;
import com.firebirdberlin.nightdream.services.AlarmService;
import com.firebirdberlin.nightdream.services.RadioStreamService;
import com.firebirdberlin.nightdream.services.ScreenWatcherService;
import com.firebirdberlin.nightdream.ui.NightDreamUI;

import java.util.Calendar;

import de.greenrobot.event.EventBus;


public class NightDreamActivity extends Activity implements View.OnTouchListener,
                                                            NightModeReceiver.Event {
    public static String TAG ="NightDreamActivity";
    private static int PENDING_INTENT_STOP_APP = 1;
    final private Handler handler = new Handler();
    protected PowerManager.WakeLock wakelock;
    ImageView background_image;
    Sensor lightSensor = null;
    int mode = 2;
    private boolean screenWasOn = false;
    mAudioManager AudioManage = null;
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
    private Context context = null;
    private float last_ambient = 4.0f;
    private double last_ambient_noise = 32000; // something loud
    private NightDreamUI nightDreamUI = null;
    private NotificationReceiver nReceiver = null;
    private NightModeReceiver nightModeReceiver = null;
    private ReceiverShutDown shutDownReceiver = null;
    private ReceiverRadioStream receiverRadioStream = null;
    private PowerManager pm;
    private Settings mySettings = null;
    private boolean isChargingWireless = false;
    private DevicePolicyManager mgr = null;
    private ComponentName cn = null;
    private GestureDetector mGestureDetector = null;
    private Runnable setScreenOff = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(setScreenOff);
            int new_mode = nightDreamUI.determineScreenMode(mode, last_ambient, last_ambient_noise);
            setMode(new_mode);
        }
    };
    private Runnable finishApp = new Runnable() {
        @Override
        public void run() {
            finish();
        }
    };
    private Runnable lockDevice = new Runnable() {
        @Override
        public void run() {
           if (mySettings.useDeviceLock && mgr.isAdminActive(cn) && !isLocked()) {
                   mgr.lockNow();
               Utility.turnScreenOn(context);
           }
        }
    };

    static public void start(Context context) {
        NightDreamActivity.start(context, null);
    }

    static public void start(Context context, Bundle extras) {
        Intent intent = getDefaultIntent(context);
        if (extras != null) {
            intent.putExtras(extras);
        }
        context.startActivity(intent);
    }

    static private Intent getDefaultIntent(Context context) {
        Intent intent = new Intent(context, NightDreamActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.main);

        Log.i(TAG, "onCreate()");
        Window window = getWindow();

        nightDreamUI = new NightDreamUI(this, window);
        AudioManage = new mAudioManager(this);
        mySettings = new Settings(this);

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        // allow the app to be displayed above the keyguard
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        setKeepScreenOn(true);

        background_image = (ImageView) findViewById(R.id.background_view);
        background_image.setOnTouchListener(this);
        mgr = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        cn = new ComponentName(this, AdminReceiver.class);
        mGestureDetector = new GestureDetector(this, mSimpleOnGestureListener);

        if (mySettings.standbyEnabledWhileConnected || mySettings.standbyEnabledWhileDisconnected) {
            ScreenWatcherService.start(context);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        setKeepScreenOn(true);
        Log.i(TAG, "onStart()");
        EventBus.getDefault().register(this);

        nightDreamUI.onStart();

        lightSensor = Utility.getLightSensor(this);
        if ( lightSensor == null ){
            last_ambient = 400.0f;
        }

        BatteryValue batteryValue = new BatteryStats(this).reference;
        this.isChargingWireless = batteryValue.isChargingWireless;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");

        screenWasOn = false;
        setKeepScreenOn(true);
        mySettings = new Settings(this);
        handler.postDelayed(lockDevice, Utility.getScreenOffTimeout(this));

        scheduleShutdown();
        nightDreamUI.onResume();
        nReceiver = registerNotificationReceiver();
        nightModeReceiver = NightModeReceiver.register(this, this);
        shutDownReceiver = registerPowerDisconnectionReceiver();
        receiverRadioStream = registerReceiverRadioStream();

        nReceiver.setColor(mySettings.secondaryColor);
        // ask for active notifications
        if (Build.VERSION.SDK_INT >= 18){
            Intent i = new Intent(Config.ACTION_NOTIFICATION_LISTENER);
            i.putExtra("command", "list");
            sendBroadcast(i);
        }

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Log.i(TAG, "Intent has extras");
            String action = extras.getString("action");

            if (action != null) {
                Log.i(TAG, "action: " + action);

                if ("start standby mode".equals(action)) {
                    nightDreamUI.setLocked(true);
                }

                if (action.equals("start night mode")) {
                    last_ambient = mySettings.minIlluminance;
                    last_ambient_noise = 32000;
                    setMode(0);
                    if ( lightSensor == null ) {
                        handler.postDelayed(setScreenOff, 20000);
                    }
                }
            }
        }

        if ( AlarmHandlerService.alarmIsRunning() ) {
            nightDreamUI.showAlarmClock();
        }

        setupNightMode();
        setupRadioStreamUI();
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
        toggleNightMode(new_mode);
        NightModeReceiver.schedule(this, timerange);
    }

    void setupRadioStreamUI() {
        switch (RadioStreamService.streamingMode) {
            case ALARM:
                setVolumeControlStream(AudioManager.STREAM_ALARM);
                nightDreamUI.showAlarmClock();
                nightDreamUI.setRadioIconInactive();
                break;
            case RADIO:
                setVolumeControlStream(AudioManager.STREAM_MUSIC);
                nightDreamUI.setRadioIconActive();
                break;
            default:
                setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
                nightDreamUI.setRadioIconInactive();
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG ,"onPause()");
        setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

        nightDreamUI.onPause();

        handler.removeCallbacks(finishApp);
        handler.removeCallbacks(lockDevice);
        PowerConnectionReceiver.schedule(this);
        cancelShutdown();
        NightModeReceiver.cancel(this);
        unregisterLocalReceiver(nReceiver);
        unregister(nightModeReceiver);
        unregister(shutDownReceiver);
        unregister(receiverRadioStream);

        if (mySettings.allow_screen_off && mode == 0 && screenWasOn && !isScreenOn() ){ // screen off in night mode
            startBackgroundListener();
        } else {
            nightDreamUI.restoreRingerMode();
        }
    }

    private void unregister(BroadcastReceiver receiver) {
        try {
            unregisterReceiver(receiver);
        } catch ( IllegalArgumentException ignored) {

        }
    }

    private void unregisterLocalReceiver(BroadcastReceiver receiver) {
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        } catch ( IllegalArgumentException ignored) {

        }
    }

    protected boolean isScreenOn() {
        if (Build.VERSION.SDK_INT <= 19){
            return deprecatedIsScreenOn();
        }
        return pm.isInteractive();
    }

    @SuppressWarnings("deprecation")
    protected boolean deprecatedIsScreenOn() {
        return pm.isScreenOn();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop()");

        nightDreamUI.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");

        // do not restore ringer mode, otherwise calls will ring
        // after the dream has ended
        //audiomanage.setRingerMode(currentRingerMode);

        nightModeReceiver = null;
        shutDownReceiver = null;
        nReceiver  = null;
    }

    private NotificationReceiver registerNotificationReceiver() {
        NotificationReceiver receiver = new NotificationReceiver(getWindow());
        IntentFilter filter = new IntentFilter(Config.ACTION_NOTIFICATION_LISTENER);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
        return receiver;
    }

    private ReceiverShutDown registerPowerDisconnectionReceiver() {
        ReceiverShutDown shutDownReceiver = new ReceiverShutDown();
        IntentFilter pwrFilter = new IntentFilter(Config.ACTION_SHUT_DOWN);
        registerReceiver(shutDownReceiver, pwrFilter);
        return shutDownReceiver;
    }

    private ReceiverRadioStream registerReceiverRadioStream() {
        ReceiverRadioStream receiver = new ReceiverRadioStream();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.ACTION_RADIO_STREAM_STARTED);
        filter.addAction(Config.ACTION_RADIO_STREAM_STOPPED);
        registerReceiver(receiver, filter);
        return receiver;
    }

    public boolean onTouch(View view, MotionEvent e) {
        return mGestureDetector.onTouchEvent(e) || nightDreamUI.onTouch(view, e, last_ambient);
    }

    // click on the settings icon
    @SuppressWarnings("UnusedParameters")
    public void onSettingsClick(View v) {
        PreferencesActivity.start(this);
    }

    @SuppressWarnings("UnusedParameters")
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

    @SuppressWarnings("UnusedParameters")
    public void onNightModeClick(View v) {
        if ( lightSensor == null
                || mySettings.nightModeActivationMode == Settings.NIGHT_MODE_ACTIVATION_MANUAL ) {
            int new_mode = ( mode == 0) ? 2 : 0;
            toggleNightMode(new_mode);
        }
    }

    private void toggleNightMode(int new_mode) {
        if ( lightSensor == null ) {
            last_ambient = ( new_mode == 2 ) ? 400.f : mySettings.minIlluminance;
        }
        setMode(new_mode);
    }

    private void toggleRadioStreamState() {
        if ( RadioStreamService.streamingMode == RadioStreamService.StreamingMode.RADIO ) {
            RadioStreamService.stop(this);
            return;
        }

        if (Utility.hasNetworkConnection(this)) {
            if (Utility.hasFastNetworkConnection(this)) {
                RadioStreamService.startStream(this);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                new AlertDialog.Builder(this, R.style.DialogTheme)
                        .setTitle(R.string.message_mobile_data_connection)
                        .setMessage(R.string.message_mobile_data_connection_confirmation)
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(R.drawable.ic_attention)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                RadioStreamService.startStream(context);
                            }
                        })
                        .show();
            }

        } else { // no network connection
            Toast.makeText(context, R.string.message_no_data_connection, Toast.LENGTH_LONG).show();
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

        setKeepScreenOn(shallKeepScreenOn(new_mode)); // allow the screen to go off
        mode = new_mode;
    }

    private boolean shallKeepScreenOn(int mode) {
        screenWasOn = screenWasOn || isScreenOn();

        Log.d(TAG, "screenWasOn = " + String.valueOf(screenWasOn));
        if (mode > 0
                || ! mySettings.allow_screen_off
                || ScreenReceiver.shallActivateStandby(context, mySettings)) {
            Log.d(TAG, "shallKeepScreenOn() true");
            return true;
        }

        long now = Calendar.getInstance().getTimeInMillis();
        if ( (0 < mySettings.nextAlarmTime - now
                && mySettings.nextAlarmTime - now < 600000)
                || AlarmService.isRunning
                || RadioStreamService.isRunning ) {
            Log.d(TAG, "shallKeepScreenOn() true");
            return true;
        }
        Log.d(TAG, "shallKeepScreenOn() false");
        return false;
    }

    private boolean isLocked() {
        KeyguardManager myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        return myKM.inKeyguardRestrictedInputMode();
    }

    private void startBackgroundListener() {
        Intent i = new Intent(this, NightModeListener.class);
        startService(i);

        finish();
    }

    public void onEvent(OnNewLightSensorValue event){
        Log.i(TAG, String.valueOf(event.value) + " lux, n=" + String.valueOf(event.n));
        last_ambient = event.value;
        handleBrightnessChange();
    }

    public void onEvent(OnLightSensorValueTimeout event){
        last_ambient = (event.value >= 0.f) ? event.value : mySettings.minIlluminance;
        Log.i(TAG, "Static for 15s: " + String.valueOf(last_ambient) + " lux.");
        handleBrightnessChange();
    }

    public void onEvent(OnNewAmbientNoiseValue event) {
        last_ambient_noise = event.value;
        handleBrightnessChange();
    }

    private void handleBrightnessChange() {
        if (mySettings.nightModeActivationMode == Settings.NIGHT_MODE_ACTIVATION_AUTOMATIC) {
            int new_mode = nightDreamUI.determineScreenMode(mode, last_ambient, last_ambient_noise);
            setMode(new_mode);
        }
    }

    public void onEvent(OnPowerDisconnected event) {
        if ( mySettings.handle_power_disconnection ) {
            handler.removeCallbacks(finishApp);
            if ( isChargingWireless ) {
                handler.postDelayed(finishApp, 5000);
            } else {
                finish();
            }
        }
    }

    public void onEvent(OnPowerConnected event) {
        handler.removeCallbacks(finishApp);
        if ( event != null ) {
            isChargingWireless = event.referenceValue.isChargingWireless;
        }
    }

    public void setKeepScreenOn(boolean keepScreenOn) {
        if( keepScreenOn ) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Log.i(TAG, "new intent received");
        //now getIntent() should always return the last received intent
    }

    private PendingIntent getShutdownIntent() {
        Intent alarmIntent = new Intent(Config.ACTION_SHUT_DOWN);
        return PendingIntent.getBroadcast(this,
                                          PENDING_INTENT_STOP_APP,
                                          alarmIntent,
                                          PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void scheduleShutdown() {
        if (mySettings == null) return;

        Calendar start = new SimpleTime(mySettings.autostartTimeRangeStart).getCalendar();
        Calendar end = new SimpleTime(mySettings.autostartTimeRangeEnd).getCalendar();
        if( start.equals(end)) {
            cancelShutdown();
            return;
        }

        if ( ! mySettings.standbyEnabledWhileConnected
                && PowerConnectionReceiver.shallAutostart(this, mySettings)) {
            PendingIntent pendingIntent = getShutdownIntent();
            Calendar calendar = new SimpleTime(mySettings.autostartTimeRangeEnd).getCalendar();
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= 19){
                alarmManager.setExact(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
            } else {
                deprecatedSetAlarm(calendar, pendingIntent);
            }
        } else {
            cancelShutdown();
        }

    }

    @SuppressWarnings("deprecation")
    private void deprecatedSetAlarm(Calendar calendar, PendingIntent pendingIntent) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
    }

    private void cancelShutdown() {
        PendingIntent pendingIntent = getShutdownIntent();
        pendingIntent.cancel();
    }

    class ReceiverShutDown extends BroadcastReceiver {
        // this receiver is needed to shutdown the app at the end of the autostart time range
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            if (mySettings.handle_power_disconnection) {
                finish();
            }
        }
    }

    class ReceiverRadioStream extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            setupRadioStreamUI();
        }
    }

    }
