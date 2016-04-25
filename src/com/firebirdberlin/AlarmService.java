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

public class AlarmService extends Service {
    private static String TAG = "NightDream.AlarmService";
    final private Handler handler = new Handler();

    static public boolean isRunning = false;
    private boolean error_on_microphone = false;
    PowerManager.WakeLock wakelock;
    private PowerManager pm;
    private Utility utility = null;

    private boolean debug = true;

    @Override
    public void onCreate(){
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakelock.acquire();

        if (debug){
            Log.d(TAG,"onCreate() called.");
        }

        utility = new Utility(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (debug){
            Log.d(TAG,"onStartCommand() called.");
        }

        Notification note = new Notification(R.drawable.ic_nightdream,
                                             "Alarm",
                                             System.currentTimeMillis());

        Intent i = new Intent(this, NightDreamActivity.class);
        i.putExtra("action", "stop alarm");
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
        String msg = this.getResources().getString(R.string.notification_alarm);
        note.setLatestEventInfo(this, "NightDream", msg, pi);
        note.flags |= Notification.FLAG_NO_CLEAR;
        note.flags |= Notification.FLAG_FOREGROUND_SERVICE;
        startForeground(1337, note);


        Bundle extras = intent.getExtras();
        if (extras != null) {
            if ( intent.hasExtra("start alarm") ){
                isRunning = true;

                try {
                    utility.AlarmPlay();
                } catch (Exception e) {}
                handler.postDelayed(timeout, 120000);
            } else
            if ( intent.hasExtra("stop alarm") ){
                handler.post(timeout);
            }
        }

        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy(){
        if (debug){
            Log.d(TAG,"onDestroy() called.");
        }

        isRunning = false;

        if (wakelock.isHeld()){
            wakelock.release();
        }
    }

    private Runnable timeout = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(timeout);
            utility.AlarmStop();
            stopForeground(false); // bool: true = remove Notification
            stopSelf();
        }
    };
}
