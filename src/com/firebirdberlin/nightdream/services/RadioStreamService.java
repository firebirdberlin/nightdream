package com.firebirdberlin.nightdream.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.HttpStatusCheckTask;
import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.events.OnSleepTimeChanged;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.repositories.VibrationHandler;
import com.firebirdberlin.radiostreamapi.PlaylistParser;
import com.firebirdberlin.radiostreamapi.PlaylistRequestTask;
import com.firebirdberlin.radiostreamapi.RadioStreamMetadata;
import com.firebirdberlin.radiostreamapi.RadioStreamMetadataRetriever;
import com.firebirdberlin.radiostreamapi.RadioStreamMetadataRetriever.RadioStreamMetadataListener;
import com.firebirdberlin.radiostreamapi.models.FavoriteRadioStations;
import com.firebirdberlin.radiostreamapi.models.PlaylistInfo;
import com.firebirdberlin.radiostreamapi.models.RadioStation;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadRequestData;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;

public class RadioStreamService extends Service implements MediaPlayer.OnErrorListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener,
        HttpStatusCheckTask.AsyncResponse,
        PlaylistRequestTask.AsyncResponse {
    static public boolean isRunning = false;
    static RadioStreamService mRadioStreamService = null;
    static public boolean alarmIsRunning = false;
    public static StreamingMode streamingMode = StreamingMode.INACTIVE;
    public static int currentStreamType = AudioManager.USE_DEFAULT_STREAM_TYPE;
    public static String EXTRA_RADIO_STATION_INDEX = "radioStationIndex";
    private static boolean readyForPlayback = false;
    private static final String TAG = "RadioStreamService";
    private static final String ACTION_START = "start";
    private static final String ACTION_START_STREAM = "start stream";
    private static final String ACTION_STOP = "stop";
    private static final String ACTION_NEXT_STATION = "next station";
    static private int radioStationIndex;
    static private RadioStation radioStation;
    private static long sleepTimeInMillis = 0L;
    private static String streamURL = "";
    private static long muteDelayInMillis = 0;
    final private Handler handler = new Handler();
    long fadeInDelay = 50;
    int maxVolumePercent = 100;
    private long readyForPlaybackSince = 0L;
    MediaPlayer mMediaPlayer = null;
    private Settings settings = null;
    private SimpleTime alarmTime = null;
    private float currentVolume = 0.f;
    private int currentStreamVolume = -1;
    private HttpStatusCheckTask statusCheckTask = null;
    private PlaylistRequestTask resolveStreamUrlTask = null;
    private final IntentFilter myNoisyAudioStreamIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private BecomingNoisyReceiver myNoisyAudioStreamReceiver;
    private VibrationHandler vibrator = null;
    CastSession castSession;
    private final Runnable fadeIn = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(fadeIn);
            if (mMediaPlayer == null) return;
            currentVolume += 0.01;
            if (currentVolume <= maxVolumePercent / 100.) {
                Log.i(TAG, "volume: " + currentVolume);
                mMediaPlayer.setVolume(currentVolume, currentVolume);
                handler.postDelayed(fadeIn, fadeInDelay);
            }
        }
    };
    private final Runnable fadeOut = new Runnable() {
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
    private final Runnable startSleep = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(startSleep);
            if (sleepTimeInMillis <= 0L || alarmIsRunning) {
                return;
            }
            sleepTimeInMillis = 0L;
            Settings settings = new Settings(getApplicationContext());
            settings.setSleepTimeInMillis(0L);
            handler.post(fadeOut);
        }
    };
    private final Runnable timeout = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(timeout);
            handler.removeCallbacks(fadeIn);
            handler.removeCallbacks(fadeOut);
            handler.removeCallbacks(startSleep);
            handler.post(fadeOut);
        }
    };

    public static void start(Context context, SimpleTime alarmTime) {
        Log.d(TAG, "start()");
        if (!Utility.hasNetworkConnection(context)) {
            Toast.makeText(context, R.string.message_no_data_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent i = new Intent(context, RadioStreamService.class);
        i.setAction(ACTION_START);
        if (alarmTime != null) {
            i.putExtras(alarmTime.toBundle());
        }
        Utility.startForegroundService(context, i);
    }

    public static boolean isReadyForPlayback() {
        return readyForPlayback;
    }

    public static int getCurrentRadioStationIndex() {
        if (streamingMode != StreamingMode.RADIO) {
            return -1;
        }

        return radioStationIndex;
    }

    public static RadioStation getCurrentRadioStation() {
        if (streamingMode != StreamingMode.RADIO) {
            return null;
        }

        return radioStation;
    }

    public static RadioStreamMetadata getCurrentIcecastMetadata() {
        return RadioStreamMetadataRetriever.getInstance().getCachedMetadata();
    }

    public static void startStream(Context context, int radioStationIndex) {
        if (!Utility.hasNetworkConnection(context)) {
            Toast.makeText(context, R.string.message_no_data_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent i = new Intent(context, RadioStreamService.class);
        i.setAction(ACTION_START_STREAM);
        i.putExtra(EXTRA_RADIO_STATION_INDEX, radioStationIndex);
        Log.i(TAG, "put extra " + radioStationIndex);
        Utility.startForegroundService(context, i);
    }

    public static void stop(Context context) {
        Intent i = getStopIntent(context);
        context.stopService(i);
    }

    private static Intent getStopIntent(Context context) {
        Intent i = new Intent(context, RadioStreamService.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return i;
    }

    public static void updateMetaData(RadioStreamMetadataListener listener, Context context) {
        if (streamingMode != StreamingMode.RADIO) {
            return;
        }

        RadioStreamMetadataRetriever.getInstance().retrieveMetadata(streamURL, listener, context);
    }

    public static boolean isSleepTimeSet() {
        long now = System.currentTimeMillis();
        return (sleepTimeInMillis > now);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() called.");
        vibrator = new VibrationHandler(this);
        startForeground();
        Utility.registerEventBus(this);
    }

    private void startForeground() {
        NotificationCompat.Builder noteBuilder =
                Utility.buildNotification(this, Config.NOTIFICATION_CHANNEL_ID_RADIO)
                        .setContentTitle(getString(R.string.radio))
                        .setSmallIcon(R.drawable.ic_radio)
                        .setPriority(NotificationCompat.PRIORITY_MAX);

        Notification note = noteBuilder.build();

        note.flags |= Notification.FLAG_NO_CLEAR;
        note.flags |= Notification.FLAG_FOREGROUND_SERVICE;

        startForeground(1337, note);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called.");
        settings = new Settings(this);
        isRunning = true;
        mRadioStreamService = this;

        String action = intent.getAction();

        Intent notificationIntent = new Intent(this, NightDreamActivity.class);
        if (ACTION_START_STREAM.equals(action)) {
            // uses action (using only extra params would cause android to treat this PI as identical with the PI of the widget, ignoring extra params)
            notificationIntent.setAction(Config.ACTION_SHOW_RADIO_PANEL);
        }
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder noteBuilder =
                Utility.buildNotification(this, Config.NOTIFICATION_CHANNEL_ID_RADIO)
                        .setContentTitle(getString(R.string.radio))
                        .setSmallIcon(R.drawable.ic_radio)
                        .setContentIntent(contentIntent)
                        .setPriority(NotificationCompat.PRIORITY_MAX);

        addActionButtonsToNotificationBuilder(noteBuilder, intent);

        Notification note = noteBuilder.build();

        note.flags |= Notification.FLAG_NO_CLEAR;
        note.flags |= Notification.FLAG_FOREGROUND_SERVICE;

        startForeground(1337, note);

        alarmTime = null;
        Bundle extras = intent.getExtras();
        if (action != null) {
            switch (action) {
                case ACTION_START:
                    if (extras != null) {
                        alarmIsRunning = true;
                        streamingMode = StreamingMode.ALARM;
                        alarmTime = new SimpleTime(extras);
                        setAlarmVolume(settings.alarmVolume, settings.radioStreamMusicIsAllowedForAlarms);

                        maxVolumePercent = (100 - settings.alarmVolumeReductionPercent);
                        fadeInDelay = settings.alarmFadeInDurationSeconds * 1000L / maxVolumePercent;

                        radioStationIndex = alarmTime.radioStationIndex;
                        checkStreamAndStart(radioStationIndex);
                        // stop the alarm automatically after playing for two hours
                        handler.postDelayed(timeout, 60000 * 120);
                    }
                    break;
                case ACTION_START_STREAM:
                    if (extras != null) {
                        alarmTime = new SimpleTime(extras);
                        radioStationIndex = intent.getIntExtra(EXTRA_RADIO_STATION_INDEX, -1);

                        Intent broadcastIndex = new Intent(Config.ACTION_RADIO_STREAM_STARTED);
                        broadcastIndex.putExtra(EXTRA_RADIO_STATION_INDEX, radioStationIndex);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIndex);
                        streamingMode = StreamingMode.RADIO;
                        currentStreamType = AudioManager.STREAM_MUSIC;
                        fadeInDelay = 50;
                        if (myNoisyAudioStreamReceiver == null) {
                            myNoisyAudioStreamReceiver = new BecomingNoisyReceiver();
                            registerReceiver(myNoisyAudioStreamReceiver, myNoisyAudioStreamIntentFilter);
                        }

                        RadioStreamMetadataRetriever.getInstance().clearCache();
                        readyForPlayback = false;
                        checkStreamAndStart(radioStationIndex);
                    }
                    break;
                case ACTION_STOP:
                    RadioStreamMetadataRetriever.getInstance().clearCache();
                    readyForPlayback = false;
                    stopSelf();
                    break;
                case ACTION_NEXT_STATION:
                    switchToNextStation();
                    break;
            }
        }

        if (!alarmIsRunning) {
            // re-init the sleep timer
            sleepTimeInMillis = settings.sleepTimeInMillis;
            initSleepTime();
        } else {
            sleepTimeInMillis = 0L;
        }

        return Service.START_REDELIVER_INTENT;
    }

    private void initSleepTime() {
        handler.removeCallbacks(startSleep);
        long now = System.currentTimeMillis();
        if (sleepTimeInMillis > now) {
            handler.postDelayed(startSleep, sleepTimeInMillis - now);
        }
    }

    private void checkStreamAndStart(int radioStationIndex) {

        Log.i(TAG, "checkStreamAndStart radioStationIndex=" + radioStationIndex);

        streamURL = "";
        if (radioStationIndex > -1) {
            FavoriteRadioStations stations = settings.getFavoriteRadioStations();
            if (stations != null) {
                radioStation = stations.get(radioStationIndex);
                if (radioStation != null) {
                    streamURL = radioStation.stream;
                    muteDelayInMillis = radioStation.muteDelayInMillis;
                }
            }
        }
        if (PlaylistParser.isPlaylistUrl(streamURL)) {
            resolveStreamUrlTask = new PlaylistRequestTask(this);
            resolveStreamUrlTask.execute(streamURL);
        } else {
            statusCheckTask = new HttpStatusCheckTask(this);
            statusCheckTask.execute(streamURL);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called.");
        Utility.unregisterEventBus(this);
        sleepTimeInMillis = 0L;

        if (statusCheckTask != null) {
            statusCheckTask.cancel(true);
        }

        if (resolveStreamUrlTask != null) {
            resolveStreamUrlTask.cancel(true);
        }

        if (streamingMode == StreamingMode.ALARM) {
            Intent intent = new Intent(Config.ACTION_ALARM_STOPPED);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

            restoreAlarmVolume();
        }
        stopRemoteMedia();

        handler.removeCallbacks(fadeIn);
        handler.removeCallbacks(fadeOut);
        handler.removeCallbacks(timeout);
        handler.removeCallbacks(startSleep);
        stopPlaying();
        if (alarmIsRunning) {
            AlarmHandlerService.stop(getApplicationContext());
        }
        if (myNoisyAudioStreamReceiver != null) {
            unregisterReceiver(myNoisyAudioStreamReceiver);
        }
        isRunning = false;
        mRadioStreamService = null;
        alarmIsRunning = false;
        radioStationIndex = -1;
        radioStation = null;
        streamingMode = StreamingMode.INACTIVE;

        Intent intent = new Intent(Config.ACTION_RADIO_STREAM_STOPPED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        stopForeground(false); // bool: true = remove Notification
    }

    public void setAlarmVolume(int volume, boolean useMusicStream) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            return;
        }
        currentStreamType =
                (useMusicStream) ? AudioManager.STREAM_MUSIC : AudioManager.STREAM_ALARM;

        int maxVolume = audioManager.getStreamMaxVolume(currentStreamType);
        volume = (int) (volume / 7. * maxVolume);

        Log.i(TAG, "max volume: " + maxVolume);
        Log.i(TAG, "volume: " + volume);
        currentStreamVolume = audioManager.getStreamVolume(currentStreamType);
        audioManager.setStreamVolume(currentStreamType, volume, 0);
    }

    private void restoreAlarmVolume() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            return;
        }

        audioManager.setStreamVolume(currentStreamType, currentStreamVolume, 0);
    }

    @Override
    public void onPlaylistRequestFinished(PlaylistInfo result) {
        if (result != null && result.valid) {
            statusCheckTask = new HttpStatusCheckTask(this);
            statusCheckTask.execute(result.streamUrl);
            return;
        }

        if (alarmIsRunning) {
            startFallbackAlarm();
        }

        Toast.makeText(this, getString(R.string.radio_stream_failure), Toast.LENGTH_SHORT).show();
        stopSelf();
    }

    @Override
    public void onStatusCheckFinished(HttpStatusCheckTask.HttpStatusCheckResult checkResult) {
        if (checkResult != null && checkResult.isSuccess()) {
            streamURL = checkResult.url;
            playStream();
            return;
        }

        if (alarmIsRunning) {
            startFallbackAlarm();
        }

        Toast.makeText(this, getString(R.string.radio_stream_failure), Toast.LENGTH_SHORT).show();
        stopSelf();
    }

    void startFallbackAlarm() {
        AlarmService.startAlarm(this, alarmTime);
        alarmIsRunning = false;
    }

    private void playStream() {
        Log.i(TAG, "playStream() " + streamURL);

        stopPlaying();
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int usage = (currentStreamType == AudioManager.STREAM_ALARM)
                    ? AudioAttributes.USAGE_ALARM
                    : AudioAttributes.USAGE_MEDIA;
            mMediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(usage)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
            );
        } else {
            mMediaPlayer.setAudioStreamType(currentStreamType);
        }
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

        //todo
        Log.d(TAG, "start chromecast music");
        castSession = CastContext.getSharedInstance(getApplicationContext()).getSessionManager()
                .getCurrentCastSession();

        loadRemoteMedia();
    }

    private MediaInfo getRemoteMediaData() {
        Log.d(TAG, "getRemoteMediadata()");
        MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, radioStation.name);
        //mediaMetadata.putString(MediaMetadata.KEY_ALBUM_ARTIST, "Test Artist");
        return new MediaInfo.Builder(
                streamURL)
                .setContentType("audio/mp3")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .build();
    }

    public static void loadRemoteMediaListener(CastSession castSession) {
        Log.d(TAG, "loadRemoteMediaStatic()");
        mRadioStreamService.castSession = castSession;
        mRadioStreamService.loadRemoteMedia();
    }

    public void loadRemoteMedia() {
        Log.d(TAG, "loadRemoteMedia()");
        if (castSession == null) {
            Log.d(TAG, "castSession == null");
            return;
        }
        RemoteMediaClient mRemoteMediaPlayer = castSession.getRemoteMediaClient();
        if (mRemoteMediaPlayer == null) {
            Log.d(TAG, "mRemoteMediaPlayer == null");
            return;
        }

        mRemoteMediaPlayer.load(
                new MediaLoadRequestData.Builder().setMediaInfo(getRemoteMediaData()).build()
        );

        stopPlaying();
    }

    void stopRemoteMedia() {
        Log.d(TAG, "stopRemoteMedia()");
        if (castSession == null) {
            Log.d(TAG, "castSession == null");
            return;
        }
        RemoteMediaClient mRemoteMediaPlayer = castSession.getRemoteMediaClient();
        if (mRemoteMediaPlayer != null) {
            mRemoteMediaPlayer.stop();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer.error: " + what + " " + extra);
        long now = System.currentTimeMillis();
        if (alarmIsRunning && now - readyForPlaybackSince < 120000) {
            // if the stream stops during the first two minutes there are probably issues connecting
            // to the stream
            stopSelf();
            startFallbackAlarm();
            return true;
        }
        return false;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.e(TAG, "onBufferingUpdate " + percent);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        currentVolume = 0.f;
        if (mMediaPlayer != null) {
            // mute mediaplayer volume immediately, before it starts playing
            mMediaPlayer.setVolume(currentVolume, currentVolume);
        }
        handler.postDelayed(fadeIn, muteDelayInMillis);
        try {
            mp.start();
            readyForPlayback = true;
            readyForPlaybackSince = System.currentTimeMillis();
            Intent intent = new Intent(Config.ACTION_RADIO_STREAM_READY_FOR_PLAYBACK);
            intent.putExtra(EXTRA_RADIO_STATION_INDEX, radioStationIndex);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } catch (IllegalStateException e) {
            Log.e(TAG, "MediaPlayer.start() failed", e);
        }

        if (currentStreamType == AudioManager.STREAM_ALARM && alarmTime.vibrate && vibrator != null) {
            vibrator.startVibration();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.i(TAG, "onCompletion");
        playStream();
    }

    public void stopPlaying() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                Log.i(TAG, "stopPlaying()");
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (vibrator != null) {
            vibrator.stopVibration();
        }
    }

    /**
     * add stop button for normal radio, and for alarm radio preview (stream started in
     * preferences dialog), but not for alarm
     */
    private void addActionButtonsToNotificationBuilder(NotificationCompat.Builder noteBuilder,
                                                       Intent intent) {

        String action = intent.getAction();

        if (ACTION_START_STREAM.equals(action) || (ACTION_START.equals(action))) {
            noteBuilder.addAction(notificationStopAction());

            // show radio station name in notification
            noteBuilder.setContentText(currentRadioStationName(intent));
        }

        // if normal radio is playing and multiple stations are configured, also add button to
        // switch to next station
        if (ACTION_START_STREAM.equals(action)) {
            FavoriteRadioStations stations = settings.getFavoriteRadioStations();
            if (stations != null && stations.numAvailableStations() > 1)
                noteBuilder.addAction(notificationNextStationAction());
        }
    }

    private NotificationCompat.Action notificationStopAction() {
        return notificationAction(ACTION_STOP, getString(R.string.action_stop));
    }

    private NotificationCompat.Action notificationNextStationAction() {
        return notificationAction(ACTION_NEXT_STATION, getString(R.string.next));
    }

    private NotificationCompat.Action notificationAction(String intentAction, String text) {
        Intent intent = new Intent(this, RadioStreamService.class);
        intent.setAction(intentAction);

        PendingIntent pi = PendingIntent.getService(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Action.Builder(0, text, pi).build();
    }

    private String currentRadioStationName(Intent intent) {
        int currentIndex = intent.getIntExtra(EXTRA_RADIO_STATION_INDEX, -1);
        RadioStation station = settings.getFavoriteRadioStation(currentIndex);
        if (station != null && station.name != null && !station.name.isEmpty()) {
            return station.name;
        }
        return "";
    }

    private void switchToNextStation() {
        Log.d(TAG, "switchToNextStation() called.");
        if (streamingMode != StreamingMode.RADIO) {
            return;
        }

        int currentIndex = getCurrentRadioStationIndex();
        if (currentIndex < 0) {
            return;
        }

        FavoriteRadioStations stations = settings.getFavoriteRadioStations();
        int nextStationIndex = stations.nextAvailableIndex(currentIndex);
        Log.d(TAG, "nextStationIndex: " + nextStationIndex);

        // always stop and restart, so a new notification occurs
        stopSelf();
        if (nextStationIndex > -1) {
            startStream(this, nextStationIndex);
        }
    }

    @Subscribe
    public void onEvent(OnSleepTimeChanged event) {
        sleepTimeInMillis = event.sleepTimeInMillis;
        initSleepTime();
    }

    public enum StreamingMode {INACTIVE, ALARM, RADIO}

    private class BecomingNoisyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                stopSelf();
            }
        }
    }
}
