package com.firebirdberlin.nightdream;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.firebirdberlin.nightdream.events.OnNewLightSensorValue;

import org.greenrobot.eventbus.Subscribe;


public class NightModeListener extends Service {
    private static String TAG = "NightModeListener";
    private static String ACTION_POWER_DISCONNECTED = "android.intent.action.ACTION_POWER_DISCONNECTED";
    final private Handler handler = new Handler();
    private SoundMeter soundmeter;
    private ReceiverPowerDisconnected pwrReceiver = null;
    private LightSensorEventListener lightSensorEventListener = null;
    private boolean activateDnD = false;
    private boolean reactivate_on_noise = false;
    private int reactivate_on_ambient_light_value = 30;

    private boolean error_on_microphone = false;
    PowerManager.WakeLock wakelock;


    private double maxAmplitude = Config.NOISE_AMPLITUDE_WAKE;
    private static int measurementMillis = 5000;

    private boolean debug = true;
    private ScreenOnBroadcastReceiver broadcastReceiver = null;

    class ScreenOnBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                Log.d(TAG, "Screen turned on ... stopSelf();");
                stopService();
            }
        }
    }

    @Override
    public void onCreate(){
        startForeground();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakelock.acquire();
        Settings settings = new Settings(this);
        reactivate_on_noise = settings.reactivateScreenOnNoise();
        maxAmplitude *= settings.sensitivity;
        reactivate_on_ambient_light_value = settings.reactivate_on_ambient_light_value;
        activateDnD = settings.activateDoNotDisturb;

        conditionallyActivateDoNotDisturb(true);

        if (debug){
            Log.d(TAG,"onCreate() called.");
        }
    }

    private void conditionallyActivateDoNotDisturb(boolean on) {
        if ( ! activateDnD ) return;
        mAudioManager audioManage = new mAudioManager(this);
        audioManage.activateDnDMode(on);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (debug){
            Log.d(TAG,"onStartCommand() called.");
        }

        pwrReceiver = registerPowerDisconnectionReceiver();
        broadcastReceiver = registerScreenOnBroadcastReceiver();

        if (reactivate_on_noise ) {
            handler.postDelayed(startRecording, 1000);
        }

        Utility.registerEventBus(this);

        lightSensorEventListener = new LightSensorEventListener(this);
        lightSensorEventListener.register();

        return Service.START_REDELIVER_INTENT;
    }

    private void startForeground() {
        Notification note = Utility.getForegroundServiceNotification(
                this, R.string.backgroundServiceNotificationTextNightMode);
        startForeground(Config.NOTIFICATION_ID_FOREGROUND_SERVICES_NIGHT_MODE, note);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy(){
        if (debug){
            Log.d(TAG,"onDestroy() called.");
        }

        if (soundmeter != null){
            soundmeter.release();
            soundmeter = null;
        }

        Utility.unregisterEventBus(this);
        unregisterReceiver(pwrReceiver);
        unregister(broadcastReceiver);
        lightSensorEventListener.unregister();

        if (wakelock.isHeld()){
            wakelock.release();
        }
    }

    private Runnable startRecording = new Runnable() {
        @Override
        public void run() {
            soundmeter = null;
            soundmeter = new SoundMeter(true);
            boolean result = soundmeter.start();
            error_on_microphone = !result;
            handler.postDelayed(getAmplitude, measurementMillis);
        }
    };

    private Runnable getAmplitude = new Runnable() {
        @Override
        public void run() {
            if (error_on_microphone || soundmeter == null) {
                Log.w(TAG,"mic reported error!");
                stopService();
                return;
            }

            double last_amplitude = soundmeter.getAmplitude();
            Log.i(TAG,"last amplitude :" + String.valueOf(last_amplitude));
            if (last_amplitude > maxAmplitude){
                stopService();
                return;
            }

            handler.postDelayed(getAmplitude, measurementMillis);
        }
    };

    private void stopService() {
        Log.i(TAG, "stopService()");
        stopForeground(true); // bool: true = remove Notification
        startApp();
        stopSelf();
    }

    private void startApp(){
        conditionallyActivateDoNotDisturb(false);
        NightDreamActivity.start(this, "start night mode");
    }

    private void restoreRingerMode() {
        mAudioManager audioManager = new mAudioManager(this);
        audioManager.restoreRingerMode();
    }

    class ReceiverPowerDisconnected extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            Log.d(TAG,"Power was disconnected ... stopSelf();");
            restoreRingerMode();
            stopSelf();
        }
    }

    private ReceiverPowerDisconnected registerPowerDisconnectionReceiver(){
        ReceiverPowerDisconnected pwrReceiver = new ReceiverPowerDisconnected();
        IntentFilter pwrFilter = new IntentFilter(ACTION_POWER_DISCONNECTED);
        registerReceiver(pwrReceiver, pwrFilter);
        return pwrReceiver;
    }

    private ScreenOnBroadcastReceiver registerScreenOnBroadcastReceiver(){
        Log.d(TAG, "registerScreenOnBroadcastReceiver()");
        ScreenOnBroadcastReceiver receiver = new ScreenOnBroadcastReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        registerReceiver(receiver, filter);
        return receiver;
    }

    private void unregister(BroadcastReceiver receiver) {
        try {
            unregisterReceiver(receiver);
        } catch ( IllegalArgumentException e ) {

        }
    }

    @Subscribe
    public void onEvent(OnNewLightSensorValue event){
        if (event.value > (float) reactivate_on_ambient_light_value) {
            Log.d(TAG,"It's getting bright ... stopSelf();");
            stopService();
        }
    }
}
