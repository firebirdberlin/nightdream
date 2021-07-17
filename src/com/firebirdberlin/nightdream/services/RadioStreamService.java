package com.firebirdberlin.nightdream.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.os.Build;
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
import com.firebirdberlin.radiostreamapi.PlaylistRequestTask;
import com.firebirdberlin.radiostreamapi.RadioStreamMetadata;
import com.firebirdberlin.radiostreamapi.RadioStreamMetadataRetriever;
import com.firebirdberlin.radiostreamapi.RadioStreamMetadataRetriever.RadioStreamMetadataListener;
import com.firebirdberlin.radiostreamapi.models.FavoriteRadioStations;
import com.firebirdberlin.radiostreamapi.models.RadioStation;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.icy.IcyHeaders;
import com.google.android.exoplayer2.metadata.icy.IcyInfo;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadRequestData;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;

import org.greenrobot.eventbus.Subscribe;

public class RadioStreamService extends Service {
    protected static final int NOTIFY_ID = 1337;
    private static final String NOTIFICATION_CHANNEL_ID = "nachtuhr";

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
    SimpleExoPlayer exoPlayer = null;
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
    private Intent intent;
    private static MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    private IcyInfo icyInfo;
    private Bitmap iconRadio;

    private final Runnable fadeIn = new Runnable() {
        @Override
        public void run() {
            //Log.i(TAG, "fadeIn Runnable");
            handler.removeCallbacks(fadeIn);
            if (exoPlayer == null) return;
            currentVolume += 0.01;
            if (currentVolume <= maxVolumePercent / 100.) {
                //Log.i(TAG, "volume: " + currentVolume);
                exoPlayer.setVolume(currentVolume);
                handler.postDelayed(fadeIn, fadeInDelay);
            }
        }
    };
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

    public static void updateMetaData(RadioStreamMetadataListener listener, Context context) {
        Log.d(TAG, "updateMetaData()");

        if (streamingMode != StreamingMode.RADIO) {
            return;
        }

        RadioStreamMetadataRetriever.getInstance().retrieveMetadata(streamURL, listener, context);
        if (RadioStreamMetadataRetriever.getInstance().getCachedMetadata() != null) {
            Log.d(TAG, "New Metadata: " + RadioStreamMetadataRetriever.getInstance().getCachedMetadata().streamTitle);
        }
    }

    public static boolean isSleepTimeSet() {
        long now = System.currentTimeMillis();
        return (sleepTimeInMillis > now);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() called.");
        vibrator = new VibrationHandler(this);

        enableMediaSession();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "RadioDroid2 Player", NotificationManager.IMPORTANCE_LOW);

            // Configure the notification channel.
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        //todo
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
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);

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
                        fadeInDelay = settings.alarmFadeInDurationSeconds * 1000 / maxVolumePercent;

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
                    Log.d(TAG, "stopself");
                    stopSelf();
/*
                    Intent stopIntent = new Intent(Config.ACTION_RADIO_STREAM_STOPPED);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(stopIntent);

                    stopIntent.setAction(Config.ACTION_NOTIFICATION_LISTENER);
                    stopIntent.putExtra("action", "remove_media");
                    LocalBroadcastManager.getInstance(this).sendBroadcast(stopIntent);
                    break;

 */
            }
        }

        if (!alarmIsRunning) {
            // re-init the sleep timer
            sleepTimeInMillis = settings.sleepTimeInMillis;
            initSleepTime();
        } else {
            sleepTimeInMillis = 0L;
        }

        if (!action.equals(ACTION_STOP)) {
            playStream();
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

    private void loadRadioFavIcon(){
        RequestQueue requestQueue;
        ImageLoader imageLoader;

        requestQueue = Volley.newRequestQueue(this);
        imageLoader = new ImageLoader(requestQueue, new ImageLoader.ImageCache() {

            private final LruCache<String, Bitmap> lruCache = new LruCache<String, Bitmap>(10);

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
                if (icyInfo != null) {
                    updateNotification(icyInfo.title);
                } else {
                    updateNotification(getResources().getString(R.string.radio_connecting));
                }
            }

            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(TAG, volleyError.getMessage());
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

    void startFallbackAlarm() {
        AlarmService.startAlarm(this, alarmTime);
        alarmIsRunning = false;
    }

    private void playStream() {
        Log.i(TAG, "playStream() " + streamURL);

        stopPlaying();

        if (exoPlayer == null) {
            Log.d(TAG, "init exoPlayer");

            exoPlayer = new SimpleExoPlayer.Builder(getApplicationContext()).build();
            exoPlayer.setMediaItem(MediaItem.fromUri(streamURL));
            exoPlayer.prepare();

            exoPlayer.addMetadataOutput(metadata -> {
                if ((metadata != null)) {
                    final int length = metadata.length();
                    if (length > 0) {
                        for (int i = 0; i < length; i++) {
                            final Metadata.Entry entry = metadata.get(i);
                            if (entry instanceof IcyInfo) {
                                icyInfo = ((IcyInfo) entry);
                                Log.d(TAG, "IcyInfo:" + icyInfo.title);
                                mediaSession.setPlaybackState(stateBuilder.build());
                                updateNotification(icyInfo.title);
                            } else if (entry instanceof IcyHeaders) {
                                final IcyHeaders icyHeaders = ((IcyHeaders) entry);
                                Log.d(TAG, "IcyHeaders: " + icyHeaders.name);
                                Log.d(TAG, "IcyHeaders: " + icyHeaders.genre);
                            }
                        }
                    }
                }
            });

            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(@Player.State int state) {
                    Log.d(TAG, "onPlaybackStateChanged() state: " + state);

                    if ((state == ExoPlayer.STATE_READY) && exoPlayer.getPlayWhenReady()) {
                        Log.d(TAG, "onPlayerStateChanged PLAYING");

                        stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,
                                exoPlayer.getCurrentPosition(), 1f);

                        Intent intent = new Intent(Config.ACTION_RADIO_STREAM_READY_FOR_PLAYBACK);
                        intent.putExtra(EXTRA_RADIO_STATION_INDEX, radioStationIndex);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                        fadeInDelay = 50;
                        currentVolume = 0.f;
                        handler.postDelayed(fadeIn, muteDelayInMillis);

                        if (icyInfo != null) {
                            updateNotification(icyInfo.title);
                        } else {
                            updateNotification(getResources().getString(R.string.radio_connecting));
                        }

                        if (currentStreamType == AudioManager.STREAM_ALARM && alarmTime.vibrate && vibrator != null) {
                            vibrator.startVibration();
                        }

                    } else if ((state == ExoPlayer.STATE_READY)) {
                        Log.d(TAG, "onPlayerStateChanged: PAUSED");
                        stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED,
                                exoPlayer.getCurrentPosition(), 1f);
                        handler.removeCallbacks(fadeIn);
                        updateNotification(getResources().getString(R.string.radio_paused));
                        Log.d(TAG, "onPlayerStateChanged stateBuilder: " + stateBuilder.build().getState());
                        Log.d(TAG, "onPlayerStateChanged STATE_PLAYING: " + PlaybackStateCompat.STATE_PAUSED);
                    } else if ((state == ExoPlayer.STATE_IDLE)) {
                        Log.d(TAG, "onPlayerStateChanged: The player does not have any media to play");
                        stateBuilder.setState(PlaybackStateCompat.STATE_STOPPED,
                                exoPlayer.getCurrentPosition(), 1f);
                    }

                    mediaSession.setPlaybackState(stateBuilder.build());
                }

                @Override
                public void onPlayerError(ExoPlaybackException error) {
                    switch (error.type) {
                        case ExoPlaybackException.TYPE_SOURCE:
                            Log.e(TAG, "TYPE_SOURCE: " + error.getSourceException().getMessage());
                            updateNotification(error.getMessage());
                            break;
                        case ExoPlaybackException.TYPE_RENDERER:
                            Log.e(TAG, "TYPE_RENDERER: " + error.getRendererException().getMessage());
                            updateNotification(error.getMessage());
                            break;
                        case ExoPlaybackException.TYPE_UNEXPECTED:
                            Log.e(TAG, "TYPE_UNEXPECTED: " + error.getUnexpectedException().getMessage());
                            updateNotification(error.getMessage());
                            break;
                    }
                    long now = System.currentTimeMillis();
                    if (alarmIsRunning && now - readyForPlaybackSince < 120000) {
                        // if the stream stops during the first two minutes there are probably issues connecting
                        // to the stream
                        Log.d(TAG, "stopself");
                        stopSelf();
                        startFallbackAlarm();
                        Toast.makeText(getApplicationContext(), getString(R.string.radio_stream_failure), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            Log.d(TAG, "exoPlayer.play()");
            exoPlayer.setVolume(0);
            exoPlayer.setPlayWhenReady(true);
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

        return new MediaInfo.Builder(
                streamURL)
                .setContentType("audio/mp3")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .build();
    }

    public static void loadRemoteMediaListener(CastSession castSession) {
        Log.d(TAG, "loadRemoteMediaListener()");

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
    }

    private void updateNotification(String title) {
        Log.i(TAG, "UpdateNotification()");

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(NOTIFY_ID);

        String action = this.intent.getAction();
        if (ACTION_START.equals(action) || exoPlayer == null) {
            return;
        }

        Intent notificationIntent = new Intent(this, NightDreamActivity.class);
        notificationIntent.setAction(Config.ACTION_SHOW_RADIO_PANEL);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        MediaSessionCompat mediaSession = new MediaSessionCompat(getBaseContext(), getBaseContext().getPackageName());

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

        notificationManager.notify(NOTIFY_ID, note);
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
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                            PlaybackStateCompat.ACTION_PAUSE));
        } else {
            return new NotificationCompat.Action(
                    R.drawable.exo_controls_play, getString(R.string.radio_play),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                            PlaybackStateCompat.ACTION_PLAY));
        }
    }

    private NotificationCompat.Action notificationNextStationAction() {
        return new NotificationCompat.Action(
                R.drawable.exo_controls_next, getString(R.string.radio_next),
                MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT));
    }

    private NotificationCompat.Action notificationPreviousStationAction() {
        return new NotificationCompat.Action(
                R.drawable.exo_controls_previous, getString(R.string.radio_previous),
                MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));
    }

    private NotificationCompat.Action notificationAction(int res, String intentAction, String text) {
        Intent intent = new Intent(this, RadioStreamService.class);
        intent.setAction(intentAction);

        PendingIntent pi = PendingIntent.getService(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

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
            icyInfo = null;
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
            icyInfo = null;
            startStream(this, previousStationIndex);
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
            Log.d(TAG, "BecomingNoisyReceiver");
            MediaButtonReceiver.handleIntent(mediaSession, intent);
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                Log.d(TAG, "stopself");
                stopSelf();
            }
        }
    }

    public static class MediaReceiver extends BroadcastReceiver {

        public MediaReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "MediaReceiver");
            MediaButtonReceiver.handleIntent(mediaSession, intent);
        }
    }

    private class sessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            Log.d(TAG, "onplay Callback");
            exoPlayer.setPlayWhenReady(true);
        }

        @Override
        public void onPause() {
            Log.d(TAG, "onpause Callback");
            exoPlayer.setPlayWhenReady(false);
        }

        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "skiptoprevious Callback");
            skipToPreviousStation();
        }

        @Override
        public void onSkipToNext() {
            Log.d(TAG, "skiptoNext Callback");
            skipToNextStation();
        }
    }
}