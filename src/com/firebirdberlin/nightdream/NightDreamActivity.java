package com.firebirdberlin.nightdream;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.AlarmClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import de.greenrobot.event.EventBus;
import java.util.Calendar;


public class NightDreamActivity extends Activity implements View.OnTouchListener {
    private static String TAG ="NightDreamActivity";
    TextView current;
    Histogram histogram;
    ImageView background_image;
    ImageView SettingsIcon;

    Sensor lightSensor;
    int mode = 2;
    int currentRingerMode;
    mAudioManager AudioManage;
    boolean isDebuggable;
    private Thread exitOp ;

    private float last_ambient = 4.0f;
    private double last_ambient_noise = 32000; // something loud
    private boolean stock_alarm_present;

    private Handler handler;
    private NightDreamUI nightDreamUI = null;
    private Utility utility;
    private NotificationReceiver nReceiver;
    private NotificationReceiverPower pwrReceiver;
    private PowerManager pm;

    private double NOISE_AMPLITUDE_WAKE  = Config.NOISE_AMPLITUDE_WAKE;
    private double NOISE_AMPLITUDE_SLEEP = Config.NOISE_AMPLITUDE_SLEEP;

    private Settings mySettings = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Window window = getWindow();

        nightDreamUI = new NightDreamUI(this, window);
        utility = new Utility(this);
        handler = new Handler();
        AudioManage  = new mAudioManager(this);
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        isDebuggable = utility.isDebuggable();

        // allow the app to be displayed above the keyguard
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setKeepScreenOn(true);

        background_image = (ImageView) findViewById(R.id.background_view);
        background_image.setOnTouchListener(this);

        current = (TextView) findViewById(R.id.current);
        SettingsIcon = (ImageView)findViewById(R.id.settings_icon);
        histogram = (Histogram)findViewById(R.id.Histogram);
        histogram.setUtility(utility);
        //histogram.restoreData();

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (intent.hasExtra("mode") ){
                String mode = extras.getString("mode");
                if (mode.equals("night")) {
                    mySettings = new Settings(this);
                    last_ambient = 4.2f;
                    last_ambient_noise = 32000; // something loud
                    nightDreamUI.dimScreen(0, last_ambient, mySettings.dim_offset);
                }
            }

            if (intent.hasExtra("SYSTEM_RINGER_MODE") ){
                int mode = extras.getInt("SYSTEM_RINGER_MODE",-1);
                if (AudioManage != null)
                    AudioManage.restoreRingerMode(mode);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        setKeepScreenOn(true);
        EventBus.getDefault().register(this);
        String nextAlarm = utility.getNextAlarmFormatted();
        if (nextAlarm == null){ // no stock Alarm present
            stock_alarm_present = false;
        } else {
            stock_alarm_present = true;
            if (nextAlarm.isEmpty())
                histogram.setNextAlarmString("");
            else
                histogram.setNextAlarmString(nextAlarm);
        }

        nightDreamUI.setAlpha(SettingsIcon, .2f, 3000);
        nightDreamUI.onStart();
        handler.postDelayed(fadeOutSettings, 20000);

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (lightSensor == null){
            Toast.makeText(this, "No Light Sensor!", Toast.LENGTH_LONG).show();
            mode = 2;
            last_ambient = 30.0f;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        nightDreamUI.onResume();
        pwrReceiver = registerPowerReceiver();
        nReceiver = registerNotificationReceiver();

        if (Build.VERSION.SDK_INT >= 18){
            // ask for active notifications
            Intent i = new Intent("com.firebirdberlin.nightdream.NOTIFICATION_LISTENER");
            i.putExtra("command","list");
            sendBroadcast(i);
        }
        mySettings = new Settings(this);

        NOISE_AMPLITUDE_SLEEP *= mySettings.sensitivity;
        NOISE_AMPLITUDE_WAKE  *= mySettings.sensitivity;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Another activity is taking focus (this activity is about to be "paused").
        nightDreamUI.onPause();
        if (isDebuggable)
            Log.d("NightDreamActivity","onPause() called.");

        unregisterReceiver(pwrReceiver);
        unregisterReceiver(nReceiver);

        // use this to start and trigger a service
        if (histogram.isAlarmSet()){
            long now = Calendar.getInstance().getTimeInMillis();
            if (isDebuggable){
                Log.w(TAG, String.valueOf(now) + " <? "
                        + String.valueOf(histogram.getAlarmTimeMillis()) + " "  +
                        String.valueOf(now < histogram.getAlarmTimeMillis()));
            }
            if ( (stock_alarm_present==true) && (now < histogram.getAlarmTimeMillis() ) ){
                Intent NewAlarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
                NewAlarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                NewAlarmIntent.putExtra(AlarmClock.EXTRA_HOUR, histogram.getAlarmHour());
                NewAlarmIntent.putExtra(AlarmClock.EXTRA_MINUTES, histogram.getAlarmMinutes());
                NewAlarmIntent.putExtra(AlarmClock.EXTRA_SKIP_UI,true);
                startActivity(NewAlarmIntent);
            }
            histogram.removeAlarm();
        }

        if (mySettings.allow_screen_off && mode == 0 && pm.isScreenOn() == false){ // screen off in night mode
            startBackgroundListener();
            finish();
        }

        //finish();
        //Release();
    }

    @Override
    protected void onStop() {
        super.onStop();

        nightDreamUI.onStop();
        EventBus.getDefault().unregister(this);

        if (handler != null){
            handler.removeCallbacks(fadeOutSettings);
        }

        if (utility.AlarmRunning() == true) histogram.stopAlarm();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // do not restore ringer mode, otherwise calls will ring
        // after the dream has ended
        //audiomanage.setRingerMode(currentRingerMode);

        utility     = null;
        pwrReceiver = null;
        nReceiver   = null;
        handler     = null;
    }

    private NotificationReceiver registerNotificationReceiver(){
        NotificationReceiver receiver = new NotificationReceiver(getWindow());
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.firebirdberlin.nightdream.NOTIFICATION_LISTENER");
        registerReceiver(receiver,filter);
        return receiver;
    }

    private NotificationReceiverPower registerPowerReceiver(){
        NotificationReceiverPower pwrReceiver = new NotificationReceiverPower();
        IntentFilter pwrFilter = new IntentFilter();
        pwrFilter.addAction("com.firebirdberlin.nightdream.POWER_LISTENER");
        registerReceiver(pwrReceiver,pwrFilter);
        return pwrReceiver;
    }

    public boolean onTouch(View view, MotionEvent e) {
        return nightDreamUI.onTouch(view, e, last_ambient);
    }

    public void onClick(View v) {
        if (utility.AlarmRunning() == true) histogram.stopAlarm();

        nightDreamUI.setAlpha(SettingsIcon, 1.f,3000);
        if (handler != null){
            handler.removeCallbacks(fadeOutSettings);
            handler.postDelayed(fadeOutSettings, 20000);
        }

        if (v instanceof TextView){
            nightDreamUI.onClockClicked();
        }

        if (lightSensor == null){
            switch (mode){
                case 0:
                    SwitchModes(18.f, last_ambient_noise);
                    break;
                case 1:
                    SwitchModes(39.f, last_ambient_noise);
                    break;
                case 2:
                    SwitchModes(50.f, last_ambient_noise);
                    break;
                case 3:
                    SwitchModes(4.0f, last_ambient_noise);
                    break;
            }
        }
    }

    // click on the settings icon
    public void onSettingsClick(View v) {
        if (Build.VERSION.SDK_INT < 14){
            NightDreamSettingsActivity.start(this);
        } else {
            PreferencesActivity.start(this);
        }
    }

    private Runnable fadeOutSettings = new Runnable() {
        @Override
        public void run() {
            nightDreamUI.setAlpha(SettingsIcon, 0.f, 3000);
        }
    };


    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        nightDreamUI.onConfigurationChanged();
    }


    private void SwitchModes(float light_value, double last_ambient_noise){
        int current_mode = mode;
        mode = nightDreamUI.determineScreenMode(current_mode, light_value, last_ambient_noise);

        nightDreamUI.switchModes(light_value, last_ambient_noise);

        if ((mode == 0) && (current_mode != 0)){
            if (mySettings.allow_screen_off){
                setKeepScreenOn(false); // allow the screen to go off
            } else {
                setKeepScreenOn(true);
            }
            nightDreamUI.setAlpha(current, .0f, 3000);
        } else if ((mode > 0) && (current_mode != mode)) {
            setKeepScreenOn(true);
            nightDreamUI.setAlpha(current, 1.0f, 3000);
        }

    }

    private void startBackgroundListener() {
        Intent i = new Intent(this, NightModeListener.class);
        if (AudioManage != null) {
            i.putExtra("SYSTEM_RINGER_MODE", AudioManage.getSystemRingerMode());
        }
        startService(i);

        finish();
    }

    public void onEvent(OnNewLightSensorValue event){
        if (isDebuggable)
            current.setText(String.valueOf(event.value) + " lux, n=" +
                            String.valueOf(event.n));
        last_ambient = event.value;
        SwitchModes(event.value, last_ambient_noise);
    }

    public void onEvent(OnLightSensorValueTimeout event){
        if (isDebuggable)
            current.setText("Static for 15s: " + String.valueOf(event.value) + " lux.");
        last_ambient = event.value;
        SwitchModes(event.value, last_ambient_noise);
    }

    public void onEvent(OnNewAmbientNoiseValue event) {
        last_ambient_noise = event.value;
        if (isDebuggable){
            current.setText("Ambient noise " + String.valueOf(last_ambient_noise));
        }
        SwitchModes(last_ambient, last_ambient_noise);
    }

    class NotificationReceiverPower extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            try{
                if (intent.getStringExtra("charging").equals("disconnected")){
                    finish();
                }
            } catch (Exception e) {};
        }
    }

    public void setKeepScreenOn(boolean keepScreenOn) {
        if( keepScreenOn ) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}
