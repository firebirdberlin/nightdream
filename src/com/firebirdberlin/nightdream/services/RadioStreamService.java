package com.firebirdberlin.nightdream.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.util.LruCache;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media.session.MediaButtonReceiver;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
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
import com.firebirdberlin.radiostreamapi.models.FavoriteRadioStations;
import com.firebirdberlin.radiostreamapi.models.PlaylistInfo;
import com.firebirdberlin.radiostreamapi.models.RadioStation;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadRequestData;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;

import org.greenrobot.eventbus.Subscribe;

public class RadioStreamService extends Service implements HttpStatusCheckTask.AsyncResponse,
        PlaylistRequestTask.AsyncResponse {

    protected static final int NOTIFY_ID = 1337;
    private static final String TAG = "RadioStreamService";
    private static final String ACTION_START = "start";
    private static final String ACTION_START_STREAM = "start stream";
    private static final String ACTION_STOP = "stop";
    static public boolean isRunning = false;
    static public boolean alarmIsRunning = false;
    public static StreamingMode streamingMode = StreamingMode.INACTIVE;
    public static int currentStreamType = AudioManager.USE_DEFAULT_STREAM_TYPE;
    public static String EXTRA_RADIO_STATION_INDEX = "radioStationIndex";
    public static String EXTRA_TITLE = "title";
    static RadioStreamService mRadioStreamService = null;
    private static boolean readyForPlayback = false;
    static private int radioStationIndex;
    static private RadioStation radioStation;
    private static long sleepTimeInMillis = 0L;
    private static String streamURL = "";
    private static long muteDelayInMillis = 0;
    private static MediaSessionCompat mediaSession;
    final private Handler handler = new Handler();
    private final IntentFilter myNoisyAudioStreamIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    long fadeInDelay = 50;
    int maxVolumePercent = 100;
    CastSession castSession;
    private ExoPlayer exoPlayer = null;
    private Settings settings = null;
    private SimpleTime alarmTime = null;
    private float currentVolume = 0.f;
    private final Runnable fadeOut = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(fadeOut);
            if (exoPlayer == null) return;
            if (RadioStreamService.streamingMode == StreamingMode.INACTIVE) {
                stop(getApplicationContext());
            }
            currentVolume -= 0.01;
            if (currentVolume > 0.) {
                exoPlayer.setVolume(currentVolume);
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
    private int currentStreamVolume = -1;
    private HttpStatusCheckTask statusCheckTask = null;
    private PlaylistRequestTask resolveStreamUrlTask = null;
    private BecomingNoisyReceiver myNoisyAudioStreamReceiver;
    private VibrationHandler vibrator = null;
    private Intent intent;
    private PlaybackStateCompat.Builder stateBuilder;
    private com.google.android.exoplayer2.MediaMetadata mediaMetaData = null;
    private Bitmap iconRadio;
    private final Runnable fadeIn = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(fadeIn);
            if (exoPlayer == null) return;
            if (currentVolume == 0.0) {
                updateNotification(getResources().getString(R.string.radio_playing));
            }
            currentVolume += 0.01;
            if (currentVolume <= maxVolumePercent / 100.) {
                //Log.i(TAG, "volume: " + currentVolume);
                exoPlayer.setVolume(currentVolume);
                handler.postDelayed(fadeIn, fadeInDelay);
            } else {
                if (mediaMetaData != null && mediaMetaData.title != null) {
                    updateNotification(mediaMetaData.title.toString());
                }
            }
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

    public static void startStream(Context context, int radioStationIndex) {
        Log.d(TAG, "startStream()");
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

    public static boolean isSleepTimeSet() {
        long now = System.currentTimeMillis();
        return (sleepTimeInMillis > now);
    }

    public static void loadRemoteMediaListener(CastSession castSession) {
        Log.d(TAG, "loadRemoteMediaListener()");

        mRadioStreamService.castSession = castSession;
        mRadioStreamService.loadRemoteMedia();
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() called.");
        vibrator = new VibrationHandler(this);

        //todo changing Radio Titles
        Log.d(TAG, "Init chromecast");

        castSession = CastContext.getSharedInstance(getApplicationContext()).getSessionManager()
                .getCurrentCastSession();

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

        startForeground(NOTIFY_ID, note);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void enableMediaSession() {
        Log.d(TAG, "enableMediaSession()");

        mediaSession = new MediaSessionCompat(getBaseContext(), getBaseContext().getPackageName());
        mediaSession.setCallback(new sessionCallback());
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setMediaButtonReceiver(null);
        mediaSession.setActive(true);

        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY
                                | PlaybackStateCompat.ACTION_PAUSE
                                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                );

        mediaSession.setPlaybackState(stateBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called. Action: " + intent.getAction());
        settings = new Settings(this);
        isRunning = true;
        mRadioStreamService = this;

        MediaButtonReceiver.handleIntent(mediaSession, intent);

        this.intent = intent;
        String action = intent.getAction();

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

                        readyForPlayback = false;
                        checkStreamAndStart(radioStationIndex);
                    }
                    break;
                case ACTION_STOP:
                    readyForPlayback = false;
                    Log.d(TAG, "stopself");
                    stopSelf();
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

    private void loadRadioFavIcon() {
        RequestQueue requestQueue;
        ImageLoader imageLoader;

        requestQueue = Volley.newRequestQueue(this);
        imageLoader = new ImageLoader(requestQueue, new ImageLoader.ImageCache() {

            private final LruCache<String, Bitmap> lruCache = new LruCache<>(10);

            public Bitmap getBitmap(String url) {
                return lruCache.get(url);
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                lruCache.put(url, bitmap);
            }
        });

        imageLoader.get(radioStation.favIcon, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                iconRadio = imageContainer.getBitmap();
            }

            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if (volleyError.getMessage() != null) {
                    Log.e(TAG, volleyError.getMessage());
                } else {
                    Log.e(TAG, "volleyError.getMessage() = null");
                }
            }
        }).getBitmap();
    }

    private void checkStreamAndStart(int radioStationIndex) {
        Log.i(TAG, "checkStreamAndStart radioStationIndex=" + radioStationIndex);

        streamURL = "";
        iconRadio = BitmapFactory.decodeResource(getResources(), R.drawable.ic_audiotrack_dark);

        if (radioStationIndex > -1) {
            FavoriteRadioStations stations = settings.getFavoriteRadioStations();
            if (stations != null) {
                radioStation = stations.get(radioStationIndex);
                if (radioStation != null) {
                    streamURL = radioStation.stream;
                    muteDelayInMillis = radioStation.muteDelayInMillis;
                    loadRadioFavIcon();
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

    void startFallbackAlarm() {
        AlarmService.startAlarm(this, alarmTime);
        alarmIsRunning = false;
    }

    private void playStream() {
        Log.i(TAG, "playStream() " + streamURL);

        stopPlaying();

        if (exoPlayer == null) {
            Log.d(TAG, "init exoPlayer");

            exoPlayer = new ExoPlayer.Builder(getApplicationContext()).build();
            exoPlayer.setMediaItem(MediaItem.fromUri(streamURL));
            exoPlayer.prepare();

            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(@Player.State int state) {
                    Log.d(TAG, "onPlaybackStateChanged() state: " + state);

                    switch (state) {
                        case ExoPlayer.STATE_READY:
                            Log.d(TAG, "The player is able to immediately play from its current position.");

                            Intent intent = new Intent(Config.ACTION_RADIO_STREAM_READY_FOR_PLAYBACK);
                            intent.putExtra(EXTRA_RADIO_STATION_INDEX, radioStationIndex);
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                            currentVolume = 0.f;
                            handler.postDelayed(fadeIn, muteDelayInMillis);
                            updateNotification(getResources().getString(R.string.radio_muting));

                            if (currentStreamType == AudioManager.STREAM_ALARM && alarmTime.vibrate && vibrator != null) {
                                vibrator.startVibration();
                            }
                            break;
                        case ExoPlayer.STATE_ENDED:
                            Log.d(TAG, "The player finished playing all media");
                            stateBuilder.setState(
                                    PlaybackStateCompat.STATE_STOPPED,
                                    exoPlayer.getCurrentPosition(),
                                    1f
                            );
                            handler.removeCallbacks(fadeIn);
                            break;
                        case Player.STATE_BUFFERING:
                            updateNotification(getResources().getString(R.string.radio_connecting));
                            break;
                        case Player.STATE_IDLE:
                            break;
                    }

                    mediaSession.setPlaybackState(stateBuilder.build());
                }

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    Log.d(TAG, "onIsPlayingChanged() isPlaying: " + isPlaying);

                    if (isPlaying) {
                        Log.d(TAG, "onIsPlayingChanged: Play");
                        stateBuilder.setState(
                                PlaybackStateCompat.STATE_PLAYING,
                                exoPlayer.getCurrentPosition(),
                                1f
                        );

                        if (mediaMetaData != null && mediaMetaData.title != null) {
                            updateNotification(mediaMetaData.title.toString());
                        } else {
                            updateNotification(getResources().getString(R.string.radio_playing));
                        }

                    } else if (!exoPlayer.getPlayWhenReady()) {
                        Log.d(TAG, "onIsPlayingChanged: PAUSED");
                        stateBuilder.setState(
                                PlaybackStateCompat.STATE_PAUSED,
                                exoPlayer.getCurrentPosition(), 1f
                        );
                        handler.removeCallbacks(fadeIn);
                        updateNotification(getResources().getString(R.string.radio_paused));
                    }
                }

                @Override
                public void onPlayerError(PlaybackException error) {
                    Log.e(TAG, "Exoplayer Error: " + error.getMessage());
                    updateNotification(error.getMessage());
                    if (alarmIsRunning) {
                        Log.d(TAG, "stopself");
                        stopSelf();
                        startFallbackAlarm();
                        Toast.makeText(getApplicationContext(), getString(R.string.radio_stream_failure), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onMetadata(Metadata metadata) {
                    Log.i(TAG, "onMetaData:" + metadata.toString());
                }

                @Override
                public void onMediaMetadataChanged(com.google.android.exoplayer2.MediaMetadata metadata) {
                    Log.i(TAG, "onMediaMetadataChanged:" + metadata.title);
                    mediaMetaData = metadata;
                    if (metadata.title != null) {
                        updateNotification(metadata.title.toString());
                        Intent intent = new Intent(Config.ACTION_RADIO_STREAM_METADATA_UPDATED);
                        intent.putExtra(EXTRA_RADIO_STATION_INDEX, radioStationIndex);
                        intent.putExtra(EXTRA_TITLE, metadata.title.toString());
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                    }
                }
            });

            Log.d(TAG, "exoPlayer.play()");
            exoPlayer.setVolume(0);
            exoPlayer.setPlayWhenReady(true);
            enableMediaSession();
            exoPlayer.play();
        }
    }

    private MediaInfo getRemoteMediaData() {
        Log.d(TAG, "getRemoteMediadata()");
        MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, radioStation.name);

        WebImage image = new WebImage(new Uri.Builder().encodedPath(radioStation.favIcon).build());
        // First image for showing the audio album art
        mediaMetadata.addImage(image);
        // Second image on the full screen
        mediaMetadata.addImage(image);

        //todo show icy metadata title
        //mediaMetadata.putString(MediaMetadata.KEY_ALBUM_ARTIST, "Test Artist");

        return new MediaInfo.Builder(streamURL)
                .setContentType("audio/mp3")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .build();
    }

    public void loadRemoteMedia() {
        Log.d(TAG, "loadRemoteMedia()");

        if (castSession == null) {
            Log.d(TAG, "castSession == null");
            return;
        }

        RemoteMediaClient mRemoteMediaPlayer = castSession.getRemoteMediaClient();
        if (mRemoteMediaPlayer == null) {
            Log.d(TAG, "loadRemoteMedia() mRemoteMediaPlayer == null");
            return;
        }

        mRemoteMediaPlayer.load(
                new MediaLoadRequestData.Builder().setMediaInfo(getRemoteMediaData()).build()
        );

        mRemoteMediaPlayer.getMediaInfo();

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

    public void stopPlaying() {
        Log.d(TAG, "stopPlaying()");

        handler.removeCallbacks(timeout);
        handler.removeCallbacks(fadeIn);
        handler.removeCallbacks(fadeOut);
        handler.removeCallbacks(startSleep);

        if (exoPlayer != null) {
            if (exoPlayer.isPlaying()) {
                Log.i(TAG, "exoPlayer.stop()");
                exoPlayer.stop();
            }
            exoPlayer.release();
            exoPlayer = null;
            Log.i(TAG, "notificationManager.cancel");
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.cancel(NOTIFY_ID);
        }

        if (vibrator != null) {
            vibrator.stopVibration();
        }

        if (mediaSession != null) {
            mediaSession.setActive(false);
        }
        stopForeground(true);
    }

    private void updateNotification(String title) {
        Log.i(TAG, "UpdateNotification() Title: " + title);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(NOTIFY_ID);

        String action = this.intent.getAction();
        if (ACTION_START.equals(action) || exoPlayer == null) {
            Log.d(TAG, "UpdateNotification() return. action: " + action + " - exoPlayer: " + exoPlayer);
            return;
        }

        Intent notificationIntent = new Intent(this, NightDreamActivity.class);
        notificationIntent.setAction(Config.ACTION_SHOW_RADIO_PANEL);

        PendingIntent contentIntent = Utility.getImmutableActivity(this, 0, notificationIntent);

        if (mediaSession == null) {
            enableMediaSession();
        }

        NotificationChannel channelRadio = notificationManager.getNotificationChannel(Config.NOTIFICATION_CHANNEL_ID_RADIO);
        if (channelRadio == null) {
            Utility.createNotificationChannels(this);
        }

        NotificationCompat.Builder noteBuilder = Utility.buildNotification(this, Config.NOTIFICATION_CHANNEL_ID_RADIO)
                .setContentIntent(contentIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_radio)
                .setLargeIcon(iconRadio)
                .setTicker(title)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(1, 2, 3)
                        .setMediaSession(mediaSession.getSessionToken()))
                .setContentText(title)
                .setWhen(System.currentTimeMillis())
                .setUsesChronometer(true);

        if (radioStation != null) {
            noteBuilder.setContentTitle(radioStation.name);
        } else {
            noteBuilder.setContentTitle(currentRadioStationName(intent));
        }

        addActionButtonsToNotificationBuilder(noteBuilder);

        Notification note = noteBuilder.build();

        note.flags |= Notification.FLAG_NO_CLEAR;
        note.flags |= Notification.FLAG_FOREGROUND_SERVICE;

        startForeground(NOTIFY_ID, note);
    }

    /*
     * add stop button for normal radio, and for alarm radio preview (stream started in
     * preferences dialog), but not for alarm
     */
    private void addActionButtonsToNotificationBuilder(NotificationCompat.Builder noteBuilder) {
        Log.d(TAG, "addActionButton");

        noteBuilder.addAction(notificationStopAction());
        noteBuilder.addAction(notificationPreviousStationAction());
        noteBuilder.addAction(notificationPlayPauseAction());
        noteBuilder.addAction(notificationNextStationAction());
    }

    private NotificationCompat.Action notificationStopAction() {
        return notificationAction(R.drawable.exo_icon_stop, ACTION_STOP, getString(R.string.action_stop));
    }

    private NotificationCompat.Action notificationPlayPauseAction() {
        Log.d(TAG, "notificationPlayPauseAction()");

        Log.d(TAG, "stateBuilder: " + stateBuilder.build().getState());
        Log.d(TAG, "STATE_PLAYING: " + PlaybackStateCompat.STATE_PLAYING);

        if (stateBuilder.build().getState() == PlaybackStateCompat.STATE_PLAYING) {
            return new NotificationCompat.Action(
                    R.drawable.exo_controls_pause, getString(R.string.radio_pause),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                            this, PlaybackStateCompat.ACTION_PAUSE
                    )
            );
        } else {
            return new NotificationCompat.Action(
                    R.drawable.exo_controls_play, getString(R.string.radio_play),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                            this, PlaybackStateCompat.ACTION_PLAY
                    )
            );
        }
    }

    private NotificationCompat.Action notificationNextStationAction() {
        return new NotificationCompat.Action(
                R.drawable.exo_controls_next, getString(R.string.radio_next),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                )
        );
    }

    private NotificationCompat.Action notificationPreviousStationAction() {
        return new NotificationCompat.Action(
                R.drawable.exo_controls_previous, getString(R.string.radio_previous),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
        );
    }

    private NotificationCompat.Action notificationAction(int res, String intentAction, String text) {
        Intent intent = new Intent(this, RadioStreamService.class);
        intent.setAction(intentAction);

        PendingIntent pi = Utility.getImmutableService(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Action.Builder(res, text, pi).build();
    }

    private String currentRadioStationName(Intent intent) {
        if (intent != null) {
            int currentIndex = intent.getIntExtra(EXTRA_RADIO_STATION_INDEX, -1);
            RadioStation station = settings.getFavoriteRadioStation(currentIndex);
            if (station != null && station.name != null && !station.name.isEmpty()) {
                return station.name;
            }
        }
        return "";
    }

    private void skipToNextStation() {
        Log.d(TAG, "skipToNextStation() called.");

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

        if (nextStationIndex > -1) {
            mediaMetaData = null;
            startStream(this, nextStationIndex);
        }
    }

    private void skipToPreviousStation() {
        Log.d(TAG, "skipToPreviousStation() called.");

        if (streamingMode != StreamingMode.RADIO) {
            return;
        }

        int currentIndex = getCurrentRadioStationIndex();
        if (currentIndex < 0) {
            return;
        }

        FavoriteRadioStations stations = settings.getFavoriteRadioStations();
        int previousStationIndex = stations.previousAvailableIndex(currentIndex);
        Log.d(TAG, "previousStationIndex: " + previousStationIndex);

        if (previousStationIndex > -1) {
            mediaMetaData = null;
            startStream(this, previousStationIndex);
        }
    }

    @Subscribe
    public void onEvent(OnSleepTimeChanged event) {
        sleepTimeInMillis = event.sleepTimeInMillis;
        initSleepTime();
    }

    public enum StreamingMode {INACTIVE, ALARM, RADIO}

    public static class MediaReceiver extends BroadcastReceiver {

        public MediaReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "MediaReceiver");
            MediaButtonReceiver.handleIntent(mediaSession, intent);
        }
    }

    private class BecomingNoisyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "BecomingNoisyReceiver");
            MediaButtonReceiver.handleIntent(mediaSession, intent);
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                Log.d(TAG, "stopself");
                stopSelf();
            }
        }
    }

    private class sessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            Log.d(TAG, "onPlay");
            if (exoPlayer != null) {
                exoPlayer.setPlayWhenReady(true);
            }
        }

        @Override
        public void onPause() {
            Log.d(TAG, "onPause: ");
            if (exoPlayer != null) {
                exoPlayer.setPlayWhenReady(false);
            }
        }

        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "onSkipToPrevious");
            skipToPreviousStation();
        }

        @Override
        public void onSkipToNext() {
            Log.d(TAG, "skipToNext");
            skipToNextStation();
        }
    }
}