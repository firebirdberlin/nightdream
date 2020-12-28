package com.firebirdberlin.nightdream.services;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.repositories.VibrationHandler;

import java.io.IOException;

public class AlarmService extends Service implements MediaPlayer.OnErrorListener,
                                                     MediaPlayer.OnBufferingUpdateListener,
                                                     MediaPlayer.OnCompletionListener {
    private static String TAG = "NightDream.AlarmService";
    final private Handler handler = new Handler();

    static public boolean isRunning = false;
    PowerManager.WakeLock wakelock;
    private MediaPlayer mMediaPlayer = null;
    private Settings settings = null;
    private float currentVolume = 0.f;
    private int currentAlarmVolume = -1;
    private Context context;
    private SimpleTime alarmTime = null;
    private static int FADEOUT_TIME_MILLIS = 10000;
    VibrationHandler vibrator = null;
    long startTime = 0;
    long fadeInDelay = 150;
    long fadeOutDelay = 150;
    int maxVolumePercent = 100;

    @Override
    public void onCreate(){
        startForeground();
        context = this;
        vibrator = new VibrationHandler(this);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakelock.acquire();

        Log.d(TAG, "onCreate() called.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void startAlarm(Context context, SimpleTime alarmTime) {
        if (AlarmService.isRunning) return;
        Intent i = new Intent(context, AlarmService.class);
        i.putExtra("start alarm", true);
        if (alarmTime != null) {
            i.putExtras(alarmTime.toBundle());
        }

        Utility.startForegroundService(context, i);
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
        handler.removeCallbacks(fadeOutStartDelay);
        handler.removeCallbacks(fadeOut);
        handler.removeCallbacks(fadeIn);
        AlarmStop();
        restoreVolume();
        Intent intent = new Intent(Config.ACTION_ALARM_STOPPED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        stopForeground(false); // bool: true = remove Notification
        stopSelf();
    }

    private Runnable retry = new Runnable() {
        @Override
        public void run() {
            AlarmPlay();
            setTimeoutOrFadeOut();
        }
    };

    private Runnable fadeIn = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(fadeIn);
            if ( mMediaPlayer == null ) return;
            currentVolume += 0.01;
            if ( currentVolume < maxVolumePercent / 100.) {
                Log.i(TAG, String.format("fadeIn: currentVolume = %3.2f", currentVolume));
                mMediaPlayer.setVolume(currentVolume, currentVolume);
                handler.postDelayed(fadeIn, fadeInDelay);
            }
        }
    };

    private Runnable timeout = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(timeout);
            handler.removeCallbacks(fadeOutStartDelay);
            handler.removeCallbacks(fadeOut);
            handler.removeCallbacks(fadeIn);
            AlarmHandlerService.autoSnooze(context);
        }
    };

    private Runnable fadeOut = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(fadeOut);
            if ( mMediaPlayer == null ) return;
            currentVolume -= 0.01;
            if ( currentVolume > 0.) {
                Log.i(TAG, String.format("fadeOut: currentVolume = %3.2f", currentVolume));
                mMediaPlayer.setVolume(currentVolume, currentVolume);
                handler.postDelayed(fadeOut, fadeOutDelay);
            } else {
                handler.post(timeout);
            }
        }
    };

    private Runnable fadeOutStartDelay = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(fadeOutStartDelay);
            fadeOutDelay = FADEOUT_TIME_MILLIS / maxVolumePercent;
            handler.post(fadeOut);
        }
    };

    private void setTimeoutOrFadeOut() {
        long duration = mMediaPlayer.getDuration();
        long adjustedAutoSnoozeTimeInMillis = duration * (long)Math.ceil((float)settings.autoSnoozeTimeInMillis / (float)duration);
        if (adjustedAutoSnoozeTimeInMillis > 1.5 * settings.autoSnoozeTimeInMillis) {
            handler.postDelayed(fadeOutStartDelay, settings.autoSnoozeTimeInMillis);
        }
    };

    public void setVolume(int volume) {
        Log.i(TAG, String.format("setVolume(%d)", volume));
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        currentAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        Log.i(TAG, String.format("volume = %d/%d", currentAlarmVolume, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)));
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0);
    }

    private void restoreVolume() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, currentAlarmVolume, 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called.");

        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (intent.hasExtra("stop alarm")) {
                stopAlarm();
            } else if (intent.hasExtra("start alarm")) {
                settings = new Settings(this);
                alarmTime = new SimpleTime(intent.getExtras());
                setVolume(settings.alarmVolume);
                maxVolumePercent = (100 - settings.alarmVolumeReductionPercent);
                fadeInDelay = settings.alarmFadeInDurationSeconds * 1000 / maxVolumePercent;

                AlarmPlay();
                setTimeoutOrFadeOut();
            }
        }

        return Service.START_REDELIVER_INTENT;
    }

    private void startForeground() {
        NotificationCompat.Builder noteBuilder =
                Utility.buildNotification(this, Config.NOTIFICATION_CHANNEL_ID_SERVICES)
                        .setContentTitle(getString(R.string.alarm))
                        .setSmallIcon(R.drawable.ic_audio)
                        .setPriority(NotificationCompat.PRIORITY_MIN);

        Notification note = noteBuilder.build();

        note.flags |= Notification.FLAG_NO_CLEAR;
        note.flags |= Notification.FLAG_FOREGROUND_SERVICE;
        startForeground(1337, note);
    }

    private boolean setDataSource(Uri soundUri) {
        if (soundUri == null) return false;
        try {
            mMediaPlayer.setDataSource(this, soundUri);
        } catch (IOException | IllegalStateException | SecurityException e) {
            e.printStackTrace();
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

        long now = System.currentTimeMillis();
        if (now - startTime > settings.autoSnoozeTimeInMillis) {
            handler.post(timeout);
        } else {
            mMediaPlayer.stop();
            try {
                mMediaPlayer.prepare();
            } catch (IOException e) {
                Log.e(TAG, "MediaPlayer.prepare() failed", e);
            } catch (IllegalStateException e) {
                Log.e(TAG, "MediaPlayer.prepare() failed", e);
            }
            mMediaPlayer.start();
        }
    }

    public void AlarmPlay() {
        AlarmStop();
        Log.i(TAG, "AlarmPlay()");
        isRunning = true;
        startTime = System.currentTimeMillis();
        mMediaPlayer = new MediaPlayer();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
            );
        } else {
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        }
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setLooping(false);

        boolean result;
        Uri soundUri = getAlarmToneUri();
        result = setDataSource(soundUri);
        if (! result ) {
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            result = setDataSource(soundUri);
        }

        if (! result ) {
            Log.e(TAG, "Could not set the data source !");
            handler.removeCallbacks(timeout);
            handler.removeCallbacks(fadeOutStartDelay);
            handler.removeCallbacks(fadeOut);
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
        }

        mMediaPlayer.start();
        Log.i(TAG, "vibrate = " + alarmTime.vibrate);
        if (alarmTime.vibrate) {
            vibrator.startVibration();
        }
    }

    private void AlarmStop(){
        vibrator.stopVibration();
        if (mMediaPlayer != null){
            if(mMediaPlayer.isPlaying()) {
                Log.i(TAG, "AlarmStop()");
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }


    public Uri getAlarmToneUri() {
        if (alarmTime != null && alarmTime.soundUri != null) {
            Log.d(TAG, "soundUri = " + alarmTime.soundUri);
            try {
                return Uri.parse(alarmTime.soundUri);
            } catch (Exception e) {
                Log.e(TAG, "Exception", e);
            }
        }

        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
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
