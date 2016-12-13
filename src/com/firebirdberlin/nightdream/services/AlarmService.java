package com.firebirdberlin.nightdream.services;

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

import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;

public class AlarmService extends Service {
    private static String TAG = "NightDream.AlarmService";
    final private Handler handler = new Handler();

    static public boolean isRunning = false;
    PowerManager.WakeLock wakelock;
    private PowerManager pm;
    private MediaPlayer mMediaPlayer = null;
    private Settings settings = null;

    @Override
    public void onCreate(){
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakelock.acquire();

        Log.d(TAG, "onCreate() called.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called.");

        Intent i = getStopIntent(this);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);

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
            if ( intent.hasExtra("stop alarm") ){
                handler.post(timeout);
            } else
            if ( intent.hasExtra("start alarm") ){
                isRunning = true;
                settings = new Settings(this);
                AlarmPlay();
                handler.postDelayed(timeout, 120000);
            } else
            if ( intent.hasExtra("start stream") ){
                isRunning = true;
                settings = new Settings(this);
                playStream();
            }
        }

        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy(){
        Log.d(TAG, "onDestroy() called.");

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

    private void playStream() {
        Log.i(TAG, "playStream()");
        AlarmStop();
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mMediaPlayer.setDataSource("http://rbb-mp3-radioeins-m.akacast.akamaistream.net/7/854/292097/v1/gnl.akacast.akamaistream.net/rbb_mp3_radioeins_m");
        } catch (IOException e) {
            Log.e(TAG, "MediaPlayer.setDataSource() failed", e);
        }

        try {
            mMediaPlayer.prepare();
        } catch (IOException e) {
            Log.e(TAG, "DatMediaPlayer.prepare() failed", e);
        }

        mMediaPlayer.start();
    }

    public void AlarmPlay() {
        AlarmStop();
        Log.i(TAG, "AlarmPlay()");
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        mMediaPlayer.setLooping(true);

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

    public static void startAlarm(Context context) {
        if ( AlarmService.isRunning ) return;
        Intent i = new Intent(context, AlarmService.class);
        i.putExtra("start alarm", true);
        context.startService(i);
    }

    public static void startStream(Context context) {
        Intent i = new Intent(context, AlarmService.class);
        i.putExtra("start stream", true);
        context.startService(i);
    }

    public static void stopAlarm(Context context) {
        if ( ! AlarmService.isRunning ) return;
        Intent i = getStopIntent(context);
        context.startService(i);
    }

    private Intent getStopIntent(Context context) {
        Intent i = new Intent(context, AlarmService.class);
        i.putExtra("stop alarm", true);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return i;
    }
}
