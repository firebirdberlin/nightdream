package com.firebirdberlin.nightdream.services;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.HttpStatusCheckTask;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.radiostreamapi.PlaylistParser;
import com.firebirdberlin.radiostreamapi.PlaylistRequestTask;
import com.firebirdberlin.radiostreamapi.models.PlaylistInfo;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

import java.io.IOException;
import java.net.URL;

public class RadioStreamService extends Service implements MediaPlayer.OnErrorListener,
                                                           MediaPlayer.OnBufferingUpdateListener,
                                                           MediaPlayer.OnCompletionListener,
                                                           MediaPlayer.OnPreparedListener,
                                                           HttpStatusCheckTask.AsyncResponse,
                                                           PlaylistRequestTask.AsyncResponse {
    static public boolean isRunning = false;
    static public boolean alarmIsRunning = false;
    public static StreamingMode streamingMode = StreamingMode.INACTIVE;
    private static String TAG = "RadioStreamService";
    private static String ACTION_START = "start";
    private static String ACTION_START_STREAM = "start stream";
    private static String ACTION_STOP = "stop";
    private static String ACTION_START_SLEEP_TIME = "start sleep time";
    final private Handler handler = new Handler();
    private MediaPlayer mMediaPlayer = null;
    private Settings settings = null;
    private float currentVolume = 0.f;
    private int currentStreamType = AudioManager.STREAM_ALARM;
    private String streamURL = "";
    private HttpStatusCheckTask statusCheckTask = null;
    private PlaylistRequestTask resolveStreamUrlTask = null;
    private Runnable fadeIn = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(fadeIn);
            if (mMediaPlayer == null) return;
            currentVolume += 0.01;
            if (currentVolume < 1.) {
                mMediaPlayer.setVolume(currentVolume, currentVolume);
                handler.postDelayed(fadeIn, 50);
            }
        }
    };
    private Runnable fadeOut = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(fadeOut);
            if (mMediaPlayer == null) return;
            if (RadioStreamService.streamingMode == StreamingMode.INACTIVE) {
                stop(getApplicationContext());
            }
            currentVolume -= 0.01;
            if (currentVolume > 0.) {
                mMediaPlayer.setVolume(currentVolume, currentVolume);
                handler.postDelayed(fadeOut, 50);
            } else {
                stop(getApplicationContext());
            }
        }
    };

    public static void start(Context context) {
        if (!Utility.hasNetworkConnection(context)) {
            Toast.makeText(context, R.string.message_no_data_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent i = new Intent(context, RadioStreamService.class);
        i.setAction(ACTION_START);
        context.startService(i);
    }

    public static void startStream(Context context) {
        if (!Utility.hasNetworkConnection(context)) {
            Toast.makeText(context, R.string.message_no_data_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent i = new Intent(context, RadioStreamService.class);
        i.setAction(ACTION_START_STREAM);
        context.startService(i);
    }

    public static void stop(Context context) {
        Intent i = getStopIntent(context);
        context.stopService(i);
    }

    public static void startSleepTime(Context context) {
        Log.i(TAG, "startSleepTime");
        Intent i = new Intent(context, RadioStreamService.class);
        i.setAction(ACTION_START_SLEEP_TIME);
        context.startService(i);
    }

    private static Intent getStopIntent(Context context) {
        Intent i = new Intent(context, RadioStreamService.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return i;
    }

    public static RadioStation getCurrentRadioStation(Context context) {
        if (streamingMode != StreamingMode.INACTIVE) {
            Settings settings = new Settings(context);
            return settings.getCurrentRadioStation();
        }
        return null;
    }

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
        settings = new Settings(this);
        isRunning = true;

        Notification note = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.radio))
                .setSmallIcon(R.drawable.ic_radio)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();

        note.flags |= Notification.FLAG_NO_CLEAR;
        note.flags |= Notification.FLAG_FOREGROUND_SERVICE;

        startForeground(1337, note);

        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            alarmIsRunning = true;
            streamingMode = StreamingMode.ALARM;
            currentStreamType = AudioManager.STREAM_ALARM;
            setAlarmVolume(settings.alarmVolume);

            checkStreamAndStart();
        } else
        if ( ACTION_START_STREAM.equals(action) ) {
            sendBroadcast( new Intent(Config.ACTION_RADIO_STREAM_STARTED) );
            streamingMode = StreamingMode.RADIO;
            currentStreamType = AudioManager.STREAM_MUSIC;

            checkStreamAndStart();
        } else
        if ( ACTION_STOP.equals(action) ) {
            stopSelf();
        } else if (ACTION_START_SLEEP_TIME.equals(action)) {
            handler.post(fadeOut);
        }

        return Service.START_REDELIVER_INTENT;
    }

    private void checkStreamAndStart() {
        streamURL = settings.radioStreamURLUI;

        if ( PlaylistParser.isPlaylistUrl(streamURL) ) {
            resolveStreamUrlTask = new PlaylistRequestTask(this);
            resolveStreamUrlTask.execute(streamURL);
        } else {
            statusCheckTask = new HttpStatusCheckTask(this);
            statusCheckTask.execute(streamURL);
        }
    }

    @Override
    public void onDestroy(){
        Log.d(TAG,"onDestroy() called.");

        if (statusCheckTask != null) {
            statusCheckTask.cancel(true);
        }

        if (resolveStreamUrlTask != null) {
            resolveStreamUrlTask.cancel(true);
        }

        if (streamingMode == StreamingMode.ALARM) {
            Intent intent = new Intent(Config.ACTION_ALARM_STOPPED);
            sendBroadcast(intent);
        }

        handler.removeCallbacks(fadeIn);
        stopPlaying();
        stopForeground(false); // bool: true = remove Notification
        isRunning = false;
        alarmIsRunning = false;
        streamingMode = StreamingMode.INACTIVE;

        Intent intent = new Intent(Config.ACTION_RADIO_STREAM_STOPPED);
        sendBroadcast(intent);
    }

    public void setAlarmVolume(int volume) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0);
    }

    @Override
    public void onPlaylistRequestFinished(PlaylistInfo result) {
        if (result.valid) {
            statusCheckTask = new HttpStatusCheckTask(this);
            statusCheckTask.execute(result.streamUrl);
            return;
        } else if ( alarmIsRunning ) {
            AlarmService.startAlarm(this);
        }

        Toast.makeText(this, getString(R.string.radio_stream_failure), Toast.LENGTH_SHORT).show();
        stopSelf();
    }

    public void onStatusCheckFinished(Boolean success, String url, int numRedirects) {
        if ( success ) {
            streamURL = url;
            playStream();
            return;
        } else if ( alarmIsRunning ) {
            AlarmService.startAlarm(this);
        }

        Toast.makeText(this, getString(R.string.radio_stream_failure), Toast.LENGTH_SHORT).show();
        stopSelf();
    }

    private void playStream() {
        Log.i(TAG, "playStream()");

        stopPlaying();
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setAudioStreamType(currentStreamType);
        mMediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);

        try {
            mMediaPlayer.setDataSource(streamURL);
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
            mMediaPlayer.prepareAsync();
        } catch (IllegalStateException e) {
            Log.e(TAG, "MediaPlayer.prepare() failed", e);
        }

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer.error: " + String.valueOf(what) + " " + String.valueOf(extra));
        if ( alarmIsRunning ) {
            AlarmService.startAlarm(this);
            stopSelf();
            return true;
        }
        return false;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.e(TAG, "onBufferingUpdate " + String.valueOf(percent));
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if ( settings.alarmFadeIn ) {
            currentVolume = 0.f;
            handler.post(fadeIn);
        }
        try {
            mp.start();
        } catch (IllegalStateException e) {
            Log.e(TAG, "MediaPlayer.start() failed", e);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.i(TAG, "onCompletion");
        playStream();
    }

    public void stopPlaying(){
        if (mMediaPlayer != null){
            if(mMediaPlayer.isPlaying()) {
                Log.i(TAG, "stopPlaying()");
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public enum StreamingMode {INACTIVE, ALARM, RADIO}
}
