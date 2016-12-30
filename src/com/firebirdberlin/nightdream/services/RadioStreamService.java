package com.firebirdberlin.nightdream.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import java.io.IOException;
import android.support.v4.app.NotificationCompat;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;

public class RadioStreamService extends Service implements MediaPlayer.OnErrorListener,
                                                           MediaPlayer.OnBufferingUpdateListener,
                                                           MediaPlayer.OnCompletionListener {
    private static String TAG = "NightDream.RadioStreamService";
    final private Handler handler = new Handler();

    static public boolean isRunning = false;
    private MediaPlayer mMediaPlayer = null;
    private Settings settings = null;


    @Override
    public void onCreate(){
        Log.d(TAG,"onCreate() called.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand() called.");

        Intent i = getStopIntent(this);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);

        Notification note = new NotificationCompat.Builder(this)
            .setContentTitle("Radio")
            .setContentText(getString(R.string.notification_alarm))
            .setSmallIcon(R.drawable.ic_radio)
            .setContentIntent(pi)
            .build();

        note.flags |= Notification.FLAG_NO_CLEAR;
        note.flags |= Notification.FLAG_FOREGROUND_SERVICE;

        startForeground(1337, note);

        Bundle extras = intent.getExtras();
        if (extras != null) {
            if ( intent.hasExtra("stop") ){
                handler.post(timeout);
            } else
            if ( intent.hasExtra("start") ){
                isRunning = true;
                settings = new Settings(this);
                playStream();
            }
        }

        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy(){
        Log.d(TAG,"onDestroy() called.");

        isRunning = false;
    }

    private Runnable timeout = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(timeout);
            stopPlaying();
            stopForeground(false); // bool: true = remove Notification
            stopSelf();
        }
    };

    private void playStream() {
        Log.i(TAG, "playStream()");
        stopPlaying();
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        try {
            mMediaPlayer.setDataSource(settings.radioStreamURL);
        } catch (IllegalStateException e) {
            Log.e(TAG, "MediaPlayer.setDataSource() failed", e);
        } catch (IOException e) {
            Log.e(TAG, "MediaPlayer.setDataSource() failed", e);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "MediaPlayer.setDataSource() failed", e);
        } catch (SecurityException e) {
            Log.e(TAG, "MediaPlayer.setDataSource() failed", e);
        }

        try {
            mMediaPlayer.prepare();
        } catch (IOException e) {
            Log.e(TAG, "MediaPlayer.prepare() failed", e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "MediaPlayer.prepare() failed", e);
        }

        try {
            mMediaPlayer.start();
        } catch (IllegalStateException e) {
            Log.e(TAG, "MediaPlayer.prepare() failed", e);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer.error: " + String.valueOf(what) + " " + String.valueOf(extra));
        return false;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.e(TAG, "onBufferingUpdate " + String.valueOf(percent));
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.e(TAG, "onCompletion");
        playStream();
    }

    public void stopPlaying(){
        if (mMediaPlayer != null){
            if(mMediaPlayer.isPlaying()) {
                Log.i(TAG, "stopPlaying()");
                mMediaPlayer.stop();
                mMediaPlayer.release();
            }
            mMediaPlayer = null;
        }
    }

    public static void start(Context context) {
        Intent i = new Intent(context, RadioStreamService.class);
        i.putExtra("start", true);
        context.startService(i);
    }

    public static void stop(Context context) {
        Intent i = getStopIntent(context);
        context.startService(i);
    }

    private static Intent getStopIntent(Context context) {
        Intent i = new Intent(context, RadioStreamService.class);
        i.putExtra("stop", true);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return i;
    }
}
