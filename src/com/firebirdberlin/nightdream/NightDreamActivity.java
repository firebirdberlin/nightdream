package com.firebirdberlin.nightdream;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
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
    private static int PENDING_INTENT_STOP_APP = 1;
    private static String ACTION_SHUT_DOWN = "com.firebirdberlin.nightdream.SHUTDOWN";
    private static String ACTION_POWER_DISCONNECTED = "android.intent.action.ACTION_POWER_DISCONNECTED";
    private static String ACTION_NOTIFICATION_LISTENER = "com.firebirdberlin.nightdream.NOTIFICATION_LISTENER";
    TextView current;
    AlarmClock alarmClock;
    ImageView background_image;

    Sensor lightSensor;
    int mode = 2;
    int currentRingerMode;
    mAudioManager AudioManage = null;
    boolean isDebuggable = false;
    private Thread exitOp ;

    private float last_ambient = 4.0f;
    private double last_ambient_noise = 32000; // something loud

    private NightDreamUI nightDreamUI = null;
    private Utility utility;
    private NotificationReceiver nReceiver = null;
    private ReceiverPowerDisconnected pwrReceiver = null;
    private PowerManager pm;

    private double NOISE_AMPLITUDE_WAKE  = Config.NOISE_AMPLITUDE_WAKE;
    private double NOISE_AMPLITUDE_SLEEP = Config.NOISE_AMPLITUDE_SLEEP;

    private Settings mySettings = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Log.i(TAG, "onCreate()");
        Window window = getWindow();

        nightDreamUI = new NightDreamUI(this, window);
        utility = new Utility(this);
        AudioManage = new mAudioManager(this);
        mySettings = new Settings(this);

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        isDebuggable = utility.isDebuggable();

        // allow the app to be displayed above the keyguard
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setKeepScreenOn(true);

        background_image = (ImageView) findViewById(R.id.background_view);
        background_image.setOnTouchListener(this);

        current = (TextView) findViewById(R.id.current);
        alarmClock = (AlarmClock) findViewById(R.id.AlarmClock);
        alarmClock.setUtility(utility);
        alarmClock.setSettings(mySettings);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setKeepScreenOn(true);
        Log.i(TAG, "onStart()");
        EventBus.getDefault().register(this);

        nightDreamUI.onStart();

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

        setKeepScreenOn(true);
        Log.i(TAG, "onResume()");
        mySettings = new Settings(this);
        scheduleShutdown();
        nightDreamUI.onResume();
        nReceiver = registerNotificationReceiver();
        pwrReceiver = registerPowerDisconnectionReceiver();

        if (Build.VERSION.SDK_INT >= 18){
            // ask for active notifications
            Intent i = new Intent(ACTION_NOTIFICATION_LISTENER);
            i.putExtra("command", "list");
            sendBroadcast(i);
        }

        NOISE_AMPLITUDE_SLEEP *= mySettings.sensitivity;
        NOISE_AMPLITUDE_WAKE  *= mySettings.sensitivity;

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            String action = extras.getString("action");

            if (action != null) {
                Log.i(TAG, action);
                if (action.equals("start alarm")) {
                    Log.i(TAG, "alarm goes off");
                    alarmClock.startAlarm();
                    nightDreamUI.showAlarmClock(last_ambient);
                }

                if (action.equals("power connected")) {
                    nightDreamUI.powerConnected();
                }

                if (action.equals("start night mode")) {
                    last_ambient = mySettings.minIlluminance;
                    last_ambient_noise = 32000; // something loud
                    nightDreamUI.dimScreen(0, last_ambient, mySettings.dim_offset);
                }
            }

            if (intent.hasExtra("SYSTEM_RINGER_MODE") ){
                int mode = extras.getInt("SYSTEM_RINGER_MODE",-1);
                if (AudioManage != null) {
                    AudioManage.restoreRingerMode(mode);
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG ,"onPause()");

        nightDreamUI.onPause();

        PowerConnectionReceiver.schedule(this);
        cancelShutdown();
        unregisterReceiver(nReceiver);
        unregisterReceiver(pwrReceiver);

        if (mySettings.allow_screen_off && mode == 0 && pm.isScreenOn() == false){ // screen off in night mode
            startBackgroundListener();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop()");

        nightDreamUI.onStop();
        EventBus.getDefault().unregister(this);

        if ( utility.AlarmRunning() ) alarmClock.stopAlarm();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");

        // do not restore ringer mode, otherwise calls will ring
        // after the dream has ended
        //audiomanage.setRingerMode(currentRingerMode);

        utility  = null;
        pwrReceiver = null;
        nReceiver  = null;
    }

    private NotificationReceiver registerNotificationReceiver(){
        NotificationReceiver receiver = new NotificationReceiver(getWindow());
        IntentFilter filter = new IntentFilter(ACTION_NOTIFICATION_LISTENER);
        registerReceiver(receiver, filter);
        return receiver;
    }

    private ReceiverPowerDisconnected registerPowerDisconnectionReceiver(){
        ReceiverPowerDisconnected pwrReceiver = new ReceiverPowerDisconnected();
        IntentFilter pwrFilter = new IntentFilter(ACTION_POWER_DISCONNECTED);
        pwrFilter.addAction(ACTION_SHUT_DOWN);
        registerReceiver(pwrReceiver, pwrFilter);
        return pwrReceiver;
    }

    public boolean onTouch(View view, MotionEvent e) {
        return nightDreamUI.onTouch(view, e, last_ambient);
    }

    public void onClick(View v) {
        Log.i(TAG, "onClick()");
        if ( utility.AlarmRunning() ) alarmClock.stopAlarm();

        if (v instanceof TextView){
            nightDreamUI.onClockClicked(last_ambient);
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
                    SwitchModes(mySettings.minIlluminance - 0.2f, last_ambient_noise);
                    break;
            }
        }
    }

    // click on the settings icon
    public void onSettingsClick(View v) {
        PreferencesActivity.start(this);
    }

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

    class ReceiverPowerDisconnected extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            finish();
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

    static public void start(Context context) {
        NightDreamActivity.start(context, null);
    }

    static public void start(Context context, Bundle extras) {
        Intent intent = new Intent();
        intent.setClassName("com.firebirdberlin.nightdream",
                              "com.firebirdberlin.nightdream.NightDreamActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (extras != null) {
            intent.putExtras(extras);
        }
        context.startActivity(intent);
    }

    private PendingIntent getShutdownIntent() {
        Intent alarmIntent = new Intent(ACTION_SHUT_DOWN);
        return PendingIntent.getBroadcast(this,
                                          PENDING_INTENT_STOP_APP,
                                          alarmIntent,
                                          PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void scheduleShutdown() {
        if (mySettings == null) return;

        if (PowerConnectionReceiver.shallAutostart(this, mySettings)) {
            PendingIntent pendingIntent = getShutdownIntent();
            Calendar calendar = new SimpleTime(mySettings.autostartTimeRangeEnd).getCalendar();
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
              if (Build.VERSION.SDK_INT >= 19){
                  alarmManager.setExact(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
              } else {
                  alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
              }
        } else {
            cancelShutdown();
        }

    }

    private void cancelShutdown() {
        PendingIntent pendingIntent = getShutdownIntent();
        pendingIntent.cancel();
    }
}
