package com.firebirdberlin.nightdream;

import java.util.Calendar;

import de.greenrobot.event.EventBus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

import com.firebirdberlin.nightdream.events.OnClockClicked;
import com.firebirdberlin.nightdream.events.OnLightSensorValueTimeout;
import com.firebirdberlin.nightdream.events.OnNewAmbientNoiseValue;
import com.firebirdberlin.nightdream.events.OnNewLightSensorValue;
import com.firebirdberlin.nightdream.events.OnPowerConnected;
import com.firebirdberlin.nightdream.events.OnPowerDisconnected;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.models.BatteryValue;
import com.firebirdberlin.nightdream.ui.NightDreamUI;
import com.firebirdberlin.nightdream.repositories.BatteryStats;


public class NightDreamActivity extends Activity implements View.OnTouchListener {
    private static String TAG ="NightDreamActivity";
    private static int PENDING_INTENT_STOP_APP = 1;
    private static String ACTION_SHUT_DOWN = "com.firebirdberlin.nightdream.SHUTDOWN";
    private static String ACTION_POWER_DISCONNECTED = "android.intent.action.ACTION_POWER_DISCONNECTED";
    private static String ACTION_NOTIFICATION_LISTENER = "com.firebirdberlin.nightdream.NOTIFICATION_LISTENER";
    final private Handler handler = new Handler();
    TextView current;
    AlarmClock alarmClock;
    ImageView background_image;

    Sensor lightSensor = null;
    int mode = 2;
    int currentRingerMode;
    mAudioManager AudioManage = null;
    boolean isDebuggable = false;

    private float last_ambient = 4.0f;
    private double last_ambient_noise = 32000; // something loud

    private NightDreamUI nightDreamUI = null;
    private Utility utility;
    private NotificationReceiver nReceiver = null;
    private ReceiverShutDown shutDownReceiver = null;
    private PowerManager pm;

    private double NOISE_AMPLITUDE_WAKE  = Config.NOISE_AMPLITUDE_WAKE;
    private double NOISE_AMPLITUDE_SLEEP = Config.NOISE_AMPLITUDE_SLEEP;

    private Settings mySettings = null;
    private boolean isChargingWireless = false;


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

        lightSensor = Utility.getLightSensor(this);
        if (lightSensor == null){
            Toast.makeText(this, "No Light Sensor!", Toast.LENGTH_SHORT).show();
            last_ambient = 400.0f;
        }

        BatteryValue batteryValue = new BatteryStats(this).reference;
        this.isChargingWireless = batteryValue.isChargingWireless;
    }

    @Override
    protected void onResume() {
        super.onResume();

        setKeepScreenOn(true);
        Log.i(TAG, "onResume()");
        mySettings = new Settings(this);
        alarmClock.setSettings(mySettings);

        scheduleShutdown();
        nightDreamUI.onResume();
        nReceiver = registerNotificationReceiver();
        shutDownReceiver = registerPowerDisconnectionReceiver();

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
                    alarmClock.startAlarm();
                    nightDreamUI.showAlarmClock();
                }

                if (action.equals("stop alarm")) {
                    alarmClock.stopAlarm();
                }

                if (action.equals("start night mode")) {
                    last_ambient = mySettings.minIlluminance;
                    last_ambient_noise = 32000;
                    nightDreamUI.dimScreen(0, last_ambient, mySettings.dim_offset);
                    if (lightSensor == null) {
                        handler.postDelayed(setScreenOff, 20000);
                    }
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
        Log.i(TAG ,"onPause()");

        nightDreamUI.onPause();

        handler.removeCallbacks(finishApp);
        PowerConnectionReceiver.schedule(this);
        cancelShutdown();
        unregisterReceiver(nReceiver);
        unregisterReceiver(shutDownReceiver);

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");

        // do not restore ringer mode, otherwise calls will ring
        // after the dream has ended
        //audiomanage.setRingerMode(currentRingerMode);

        utility  = null;
        shutDownReceiver = null;
        nReceiver  = null;
    }

    private NotificationReceiver registerNotificationReceiver(){
        NotificationReceiver receiver = new NotificationReceiver(getWindow());
        IntentFilter filter = new IntentFilter(ACTION_NOTIFICATION_LISTENER);
        registerReceiver(receiver, filter);
        return receiver;
    }

    private ReceiverShutDown registerPowerDisconnectionReceiver(){
        ReceiverShutDown shutDownReceiver = new ReceiverShutDown();
        IntentFilter pwrFilter = new IntentFilter(ACTION_SHUT_DOWN);
        registerReceiver(shutDownReceiver, pwrFilter);
        return shutDownReceiver;
    }

    public boolean onTouch(View view, MotionEvent e) {
        refreshScreenTimeout();
        return nightDreamUI.onTouch(view, e, last_ambient);
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
            boolean on = shallKeepScreenOn(mode);
            setKeepScreenOn(on); // allow the screen to go off
            nightDreamUI.setAlpha(current, .0f, 3000);
        } else if ((mode > 0) && (current_mode != mode)) {
            setKeepScreenOn(true);
            nightDreamUI.setAlpha(current, 1.0f, 3000);
        }

    }

    private boolean shallKeepScreenOn(int mode) {
        if (mode > 0
                || ! mySettings.allow_screen_off) return true;

        long now = Calendar.getInstance().getTimeInMillis();
        if ( (0 < mySettings.nextAlarmTime - now
                && mySettings.nextAlarmTime - now < 600000)
                || AlarmService.isRunning ) {
            Log.d(TAG, "shallKeepScreenOn() true");
            return true;
        }
        Log.d(TAG, "shallKeepScreenOn() false");
        return false;
    }

    private Runnable setScreenOff = new Runnable() {
       @Override
       public void run() {
            handler.removeCallbacks(setScreenOff);
            SwitchModes(last_ambient, last_ambient_noise);
       }
    };

    private Runnable finishApp = new Runnable() {
       @Override
       public void run() {
           finish();
       }
    };

    private void startBackgroundListener() {
        Intent i = new Intent(this, NightModeListener.class);
        if (AudioManage != null) {
            i.putExtra("SYSTEM_RINGER_MODE", AudioManage.getSystemRingerMode());
        }
        startService(i);

        finish();
    }

    public void onEvent(OnClockClicked event){
        refreshScreenTimeout();
        if (AlarmService.isRunning) alarmClock.stopAlarm();

        if (lightSensor == null){
            last_ambient = ( mode == 0 ) ? 400.f : mySettings.minIlluminance;
            SwitchModes(last_ambient, 0);
            String msg = (mode == 0) ? "night mode enabled" : "day mode enabled";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    public void onEvent(OnNewLightSensorValue event){
        Log.i(TAG, String.valueOf(event.value) + " lux, n=" + String.valueOf(event.n));
        if (isDebuggable) {
            current.setText(String.valueOf(event.value) + " lux, n=" +
                            String.valueOf(event.n));
        }
        last_ambient = event.value;
        SwitchModes(last_ambient, last_ambient_noise);
    }

    public void onEvent(OnLightSensorValueTimeout event){
        last_ambient = (event.value >= 0.f) ? event.value : mySettings.minIlluminance;
        Log.i(TAG, "Static for 15s: " + String.valueOf(last_ambient) + " lux.");
        if (isDebuggable) {
            current.setText("Static for 15s: " + String.valueOf(last_ambient) + " lux.");
        }
        SwitchModes(last_ambient, last_ambient_noise);
    }

    public void onEvent(OnNewAmbientNoiseValue event) {
        last_ambient_noise = event.value;
        if (isDebuggable){
            current.setText("Ambient noise " + String.valueOf(last_ambient_noise));
        }
        SwitchModes(last_ambient, last_ambient_noise);
    }

    public void onEvent(OnPowerDisconnected event) {
        handler.removeCallbacks(finishApp);
        if ( isChargingWireless ) {
            handler.postDelayed(finishApp, 5000);
        } else {
            finish();
        }
    }

    public void onEvent(OnPowerConnected event) {
        handler.removeCallbacks(finishApp);
        if ( event != null ) {
            isChargingWireless = event.referenceValue.isChargingWireless;
        }
    }

    class ReceiverShutDown extends BroadcastReceiver{
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
        refreshScreenTimeout();
    }

    private void refreshScreenTimeout() {
        handler.removeCallbacks(finishApp);
        if ( ! isKeepScreenOn() ) {
            int screenOffTimeout = utility.getScreenOffTimeout();
            handler.postDelayed(finishApp, screenOffTimeout + 1000);
        }
    }

    private boolean isKeepScreenOn() {
        int flags = getWindow().getAttributes().flags;

        return ((flags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0);
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
        Intent intent = new Intent(context, NightDreamActivity.class);
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

    @SuppressLint("NewApi")
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
