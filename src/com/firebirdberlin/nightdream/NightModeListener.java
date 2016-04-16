package com.firebirdberlin.nightdream;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class NightModeListener extends Service {
    private static String TAG = "NightDream.NightModeListener";
    final private Handler handler = new Handler();
    private SoundMeter soundmeter;

    private boolean running = false;
    private boolean error_on_microphone = false;
    PowerManager.WakeLock wakelock;
    private PowerManager pm;


    private double maxAmplitude = Config.NOISE_AMPLITUDE_WAKE;
    private static int measurementMillis = 5000;

    private int SYSTEM_RINGER_MODE = -1;

    private boolean debug = true;

    @Override
    public void onCreate(){
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakelock.acquire();

        SharedPreferences settings = getSharedPreferences(Settings.PREFS_KEY, 0);
        int sensitivity = 10 - settings.getInt("NoiseSensitivity", 4);
        maxAmplitude *= sensitivity;

        if (debug){
            Log.d(TAG,"onCreate() called.");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (debug){
            Log.d(TAG,"onStartCommand() called.");
        }

        running = true;

        Notification note = new Notification(R.drawable.ic_nightdream,
                                             "NightDream night mode listener active.",
                                             System.currentTimeMillis());

        Intent i = new Intent(this, NightDreamActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP| Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

        note.setLatestEventInfo(this, "NightDream", "microphone is open ...", pi);
        note.flags|=Notification.FLAG_NO_CLEAR;
        note.flags|=Notification.FLAG_FOREGROUND_SERVICE;
        startForeground(1337, note);


        Bundle extras = intent.getExtras();
        if (extras != null) {
            if ( intent.hasExtra("SYSTEM_RINGER_MODE") ){
                SYSTEM_RINGER_MODE = extras.getInt("SYSTEM_RINGER_MODE", -1);
            }
        }

        handler.postDelayed(startRecording, 1000);

        return Service.START_REDELIVER_INTENT;
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
            if (pm.isScreenOn()) { // screen was turned on
                Log.i(TAG,"screen turned ON: stopSelf()");
                stopSelf();
                return;
            }

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
        soundmeter.release();
        soundmeter = null;

        stopForeground(false); // bool: true = remove Notification
        startApp();
        stopSelf();
    }

    private void startApp(){
        Bundle bundle = new Bundle();
        bundle.putString("action", "start night mode");
        bundle.putInt("SYSTEM_RINGER_MODE", SYSTEM_RINGER_MODE);
        NightDreamActivity.start(this, bundle);
    }
}
