package com.firebirdberlin.nightdream.services;

import android.app.Notification;
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
import android.os.PowerManager.WakeLock;
import android.util.Log;
import java.io.IOException;
import android.support.v4.app.NotificationCompat;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;

public class AlarmService extends Service implements MediaPlayer.OnErrorListener,
                                                     MediaPlayer.OnBufferingUpdateListener,
                                                     MediaPlayer.OnCompletionListener {
    private static String TAG = "NightDream.AlarmService";
    final private Handler handler = new Handler();

    static public boolean isRunning = false;
    PowerManager.WakeLock wakelock;
    private PowerManager pm;
    private MediaPlayer mMediaPlayer = null;
    private Settings settings = null;
    private float currentVolume = 0.f;
    private Context context;

    @Override
    public void onCreate(){
        context = this;
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

        Notification note = new NotificationCompat.Builder(this)
            .setContentTitle(getString(R.string.alarm))
            .setSmallIcon(R.drawable.ic_audio)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build();

        note.flags |= Notification.FLAG_NO_CLEAR;
        note.flags |= Notification.FLAG_FOREGROUND_SERVICE;
        startForeground(1337, note);


        Bundle extras = intent.getExtras();
        if (extras != null) {
            if ( intent.hasExtra("stop alarm") ){
                stopAlarm();
            } else
            if ( intent.hasExtra("start alarm") ){
                settings = new Settings(this);
                setVolume(settings.alarmVolume);
                AlarmPlay();
                handler.postDelayed(timeout, 120000);
            }
        }

        return Service.START_REDELIVER_INTENT;
    }


    @Override
    public void onDestroy(){
        Log.d(TAG, "onDestroy() called.");

        handler.removeCallbacks(fadeIn);
        isRunning = false;

        if (wakelock.isHeld()){
            wakelock.release();
        }
    }


    public void stopAlarm() {
        handler.removeCallbacks(timeout);
        handler.removeCallbacks(fadeIn);
        AlarmStop();
        sendBroadcast(new Intent(Config.ACTION_ALARM_STOPPED));
        stopForeground(false); // bool: true = remove Notification
        stopSelf();
    }

    private Runnable timeout = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(timeout);
            handler.removeCallbacks(fadeIn);
            AlarmHandlerService.snooze(context);
        }
    };

    private Runnable retry = new Runnable() {
        @Override
        public void run() {
            AlarmPlay();
            handler.postDelayed(timeout, 120000);
        }
    };

    private Runnable fadeIn = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(fadeIn);
            if ( mMediaPlayer == null ) return;
            currentVolume += 0.01;
            if ( currentVolume < 1. ) {
                mMediaPlayer.setVolume(currentVolume, currentVolume);
                handler.postDelayed(fadeIn, 150);
            }
        }
    };

    public void setVolume(int volume) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0);
    }

    public void AlarmPlay() {
        AlarmStop();
        Log.i(TAG, "AlarmPlay()");
        isRunning = true;
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setLooping(true);

        boolean result = false;
        Uri soundUri = getAlarmToneUri();
        result = setDataSource(soundUri);
        if (! result ) {
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            result = setDataSource(soundUri);
        }

        if (! result ) {
            Log.e(TAG, "Could not set the data source !");
            handler.removeCallbacks(timeout);
            handler.removeCallbacks(fadeIn);
            AlarmStop();
            handler.postDelayed(retry, 10000);
            return;
        }

        try {
            mMediaPlayer.prepare();
        } catch (IOException e) {
            Log.e(TAG, "MediaPlayer.prepare() failed", e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "MediaPlayer.prepare() failed", e);
        }

        if ( settings.alarmFadeIn ) {
            currentVolume = 0.f;
            // mute mediaplayer volume immediately, before it starts playing
            if (mMediaPlayer != null) {
                mMediaPlayer.setVolume(currentVolume, currentVolume);
            }
            handler.post(fadeIn);
        };

        mMediaPlayer.start();
    }

    private boolean setDataSource(Uri soundUri) {
        if (soundUri == null) return false;
        try {
            mMediaPlayer.setDataSource(this, soundUri);
        } catch (IOException e) {
            Log.e(TAG, String.format("Setting the Uri %s failed !", soundUri.toString()));
            return false;
        } catch (IllegalStateException e) {
            Log.e(TAG, "MediaPlayer.setDataSource() failed", e);
            return false;
        }
        return true;

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
        Log.e(TAG, "onCompletion ");
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
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public static void startAlarm(Context context) {
        if ( AlarmService.isRunning ) return;
        Intent i = new Intent(context, AlarmService.class);
        i.putExtra("start alarm", true);
        context.startService(i);
    }

    public static void stop(Context context) {
        if ( ! AlarmService.isRunning ) return;
        Intent i = getStopIntent(context);
        context.startService(i);
    }

    private static Intent getStopIntent(Context context) {
        Intent i = new Intent(context, AlarmService.class);
        i.putExtra("stop alarm", true);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return i;
    }
}
