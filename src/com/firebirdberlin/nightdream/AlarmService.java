package com.firebirdberlin.nightdream;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import java.io.IOException;
import android.support.v4.app.NotificationCompat;

public class AlarmService extends Service {
    private static String TAG = "NightDream.AlarmService";
    final private Handler handler = new Handler();

    static public boolean isRunning = false;
    private boolean error_on_microphone = false;
    PowerManager.WakeLock wakelock;
    private PowerManager pm;
    private MediaPlayer mMediaPlayer = null;
    private Settings settings = null;

    private boolean debug = true;

    @Override
    public void onCreate(){
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakelock.acquire();

        if (debug){
            Log.d(TAG,"onCreate() called.");
        }
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

        Intent i = new Intent(this, NightDreamActivity.class);
        i.putExtra("action", "stop alarm");
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

        Notification note = new NotificationCompat.Builder(this)
            .setContentTitle("Alarm")
            .setContentText(getString(R.string.notification_alarm))
            .setSmallIcon(R.drawable.ic_audio)
            .setContentIntent(pi)
            .build();

        note.flags |= Notification.FLAG_NO_CLEAR;
        note.flags |= Notification.FLAG_FOREGROUND_SERVICE;

        startForeground(1337, note);

        Bundle extras = intent.getExtras();
        if (extras != null) {
            if ( intent.hasExtra("start alarm") ){
                isRunning = true;
                settings = new Settings(this);
                AlarmPlay();
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
            AlarmStop();
            stopForeground(false); // bool: true = remove Notification
            stopSelf();
        }
    };

    public void AlarmPlay() {
        AlarmStop();
        Log.i(TAG, "AlarmPlay()");
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mMediaPlayer.setLooping(true);
        }


        try {
            Uri soundUri = getAlarmToneUri();
            mMediaPlayer.setDataSource(this, soundUri);
        } catch (IOException e1) {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            try {
                mMediaPlayer.setDataSource(this, soundUri);
            } catch (IOException e2) {
                Log.e(TAG, "Playing the default alarm tone failed", e2);
            }
        }

        try {
            mMediaPlayer.prepare();
        } catch (IOException e) {
            Log.e(TAG, "MediaPlayer.prepare() failed", e);
        }

        mMediaPlayer.start();
    }

    public Uri getAlarmToneUri() {
        Log.i(TAG, settings.AlarmToneUri);
        try {
            return Uri.parse(settings.AlarmToneUri);
        } catch (Exception e) {
            Log.e(TAG, "Exception", e);
        }

        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    }

    public void AlarmStop(){
        if (mMediaPlayer != null){
            if(mMediaPlayer.isPlaying()) {
                Log.i(TAG, "AlarmStop()");
                mMediaPlayer.stop();
                mMediaPlayer.release();
            }
            mMediaPlayer = null;
        }
    }
}
