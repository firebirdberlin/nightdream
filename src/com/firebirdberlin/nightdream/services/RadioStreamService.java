package com.firebirdberlin.nightdream.services;

import android.app.Notification;
import android.app.PendingIntent;
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
import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.radiostreamapi.PlaylistParser;
import com.firebirdberlin.radiostreamapi.PlaylistRequestTask;
import com.firebirdberlin.radiostreamapi.IcecastMetadataRetriever;
import com.firebirdberlin.radiostreamapi.StreamMetadataTask;
import com.firebirdberlin.radiostreamapi.models.FavoriteRadioStations;
import com.firebirdberlin.radiostreamapi.models.PlaylistInfo;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class RadioStreamService extends Service implements MediaPlayer.OnErrorListener,
                                                           MediaPlayer.OnBufferingUpdateListener,
                                                           MediaPlayer.OnCompletionListener,
                                                           MediaPlayer.OnPreparedListener,
                                                           HttpStatusCheckTask.AsyncResponse,
                                                           PlaylistRequestTask.AsyncResponse {
    static public boolean isRunning = false;
    static public boolean alarmIsRunning = false;
    public static StreamingMode streamingMode = StreamingMode.INACTIVE;
    public static String EXTRA_RADIO_STATION_INDEX = "radioStationIndex";
    public static String EXTRA_DEBUG = "ExtraDebug";
    private static String TAG = "RadioStreamService";
    private static String ACTION_START = "start";
    private static String ACTION_START_STREAM = "start stream";
    private static String ACTION_STOP = "stop";
    private static String ACTION_NEXT_STATION = "next station";
    private static String ACTION_START_SLEEP_TIME = "start sleep time";
    static private int radioStationIndex;
    static private RadioStation radioStation;
    final private Handler handler = new Handler();
    private MediaPlayer mMediaPlayer = null;
    private boolean debug = false;
    private Settings settings = null;
    private SimpleTime alarmTime = null;
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

    public static void start(Context context, SimpleTime alarmTime) {
        start(context, alarmTime, false);
    }

    public static void start(Context context, SimpleTime alarmTime, boolean debug) {
        if (!Utility.hasNetworkConnection(context)) {
            Toast.makeText(context, R.string.message_no_data_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent i = new Intent(context, RadioStreamService.class);
        i.setAction(ACTION_START);
        i.putExtra(EXTRA_DEBUG, debug);
        if (alarmTime != null) {
            i.putExtras(alarmTime.toBundle());
        }
        context.startService(i);
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

    public static void startStream(Context context) {
        startStream(context, 0);
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

        String action = intent.getAction();

        Intent notificationIntent = new Intent(this, NightDreamActivity.class);
        if ( ACTION_START_STREAM.equals(action) ) {
            // uses action (using only extra params would cause android to treat this PI as identical with the PI of the widget, ignoring extra params)
            notificationIntent.setAction(Config.ACTION_SHOW_RADIO_PANEL);
        }
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder noteBuilder = new NotificationCompat.Builder(this)
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
        if (ACTION_START.equals(action)) {
            debug = intent.getBooleanExtra(EXTRA_DEBUG, false);
            if (!debug) {
                alarmIsRunning = true;
                streamingMode = StreamingMode.ALARM;
            }
            alarmTime = new SimpleTime(intent.getExtras());
            setAlarmVolume(settings.alarmVolume, settings.radioStreamMusicIsAllowedForAlarms);
            streamURL = settings.radioStreamURL;
            checkStreamAndStart(-1);
        } else
        if ( ACTION_START_STREAM.equals(action) ) {
            alarmTime = new SimpleTime(intent.getExtras());
            radioStationIndex = intent.getIntExtra(EXTRA_RADIO_STATION_INDEX, -1);

            Intent broadcastIndex = new Intent(Config.ACTION_RADIO_STREAM_STARTED);
            broadcastIndex.putExtra(EXTRA_RADIO_STATION_INDEX, radioStationIndex);
            sendBroadcast(broadcastIndex);
            streamingMode = StreamingMode.RADIO;
            currentStreamType = AudioManager.STREAM_MUSIC;

            checkStreamAndStart(radioStationIndex);
        } else
        if ( ACTION_STOP.equals(action) ) {
            stopSelf();
        } else if (ACTION_START_SLEEP_TIME.equals(action)) {
            handler.post(fadeOut);
        } else if (ACTION_NEXT_STATION.equals(action)) {
            switchToNextStation();
        }

        return Service.START_REDELIVER_INTENT;
    }

    private void checkStreamAndStart(int radioStationIndex) {

        Log.i(TAG, "checkStreamAndStart radioStationIndex=" + radioStationIndex);

        if (streamingMode == StreamingMode.RADIO) {
            streamURL = settings.radioStreamURLUI;
            FavoriteRadioStations stations = settings.getFavoriteRadioStations();
            if (stations != null) {
                radioStation = stations.get(radioStationIndex);
                if (radioStation != null) {
                    streamURL = radioStation.stream;
                }
            }
        }
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
        radioStationIndex = -1;
        radioStation = null;
        streamingMode = StreamingMode.INACTIVE;
        debug = false;

        Intent intent = new Intent(Config.ACTION_RADIO_STREAM_STOPPED);
        sendBroadcast(intent);
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

        Log.i(TAG, "max volume: " + String.valueOf(maxVolume));
        Log.i(TAG, "volume: " + String.valueOf(volume));
        audioManager.setStreamVolume(currentStreamType, volume, 0);
    }

    @Override
    public void onPlaylistRequestFinished(PlaylistInfo result) {
        if (result.valid) {
            statusCheckTask = new HttpStatusCheckTask(this);
            statusCheckTask.execute(result.streamUrl);
            return;
        } else if ( alarmIsRunning ) {
            AlarmService.startAlarm(this, alarmTime);
        }

        Toast.makeText(this, getString(R.string.radio_stream_failure), Toast.LENGTH_SHORT).show();
        stopSelf();
    }

    @Override
    public void onStatusCheckFinished(HttpStatusCheckTask.HttpStatusCheckResult checkResult) {
        if ( checkResult != null && checkResult.isSuccess() ) {
            streamURL = checkResult.url;
            playStream();
            return;
        } else if ( alarmIsRunning ) {
            AlarmService.startAlarm(this, alarmTime);
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
            AlarmService.startAlarm(this, alarmTime);
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
            // mute mediaplayer volume immediately, before it starts playing
            if (mMediaPlayer != null) {
                mMediaPlayer.setVolume(currentVolume, currentVolume);
            }
            handler.post(fadeIn);
        }
        try {
            mp.start();
            Intent intent = new Intent(Config.ACTION_RADIO_STREAM_READY_FOR_PLAYBACK);
            intent.putExtra(EXTRA_RADIO_STATION_INDEX, radioStationIndex);
            sendBroadcast( intent );

            // test metadata retriever

            URL url = null;
            try {
                url = new URL(streamURL);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            if (url != null) {
                StreamMetadataTask.AsyncResponse metadataCallback = new StreamMetadataTask.AsyncResponse() {

                    @Override
                    public void onMetadataRequestFinished(Map<String, String> metadata) {
                        Log.i(TAG, "meta data for url:" + streamURL);
                        if (metadata != null && !metadata.isEmpty() && metadata.containsKey(IcecastMetadataRetriever.META_KEY_STREAM_TITLE)) {

                            for (String key : metadata.keySet()) {
                                Log.i(TAG, key + ":" + metadata.get(key));
                            }

                            String title = metadata.get(IcecastMetadataRetriever.META_KEY_STREAM_TITLE);
                            if (!title.isEmpty()) {
                                Toast.makeText(getApplicationContext(), title, title.length()).show();
                            }

                        } else {
                            Log.i(TAG, "null/empty");
                        }
                    }

                };

                new StreamMetadataTask(metadataCallback, getApplicationContext()).execute(url);
            }


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

    /**
     * add stop button for normal radio, and for alarm radio preview (stream started in
     * preferences dialog), but not for alarm
     */
    private void addActionButtonsToNotificationBuilder(NotificationCompat.Builder noteBuilder,
                                                       Intent intent) {

        String action = intent.getAction();
        boolean hasExtraDebug = intent.getBooleanExtra(EXTRA_DEBUG, false);

        if ( ACTION_START_STREAM.equals(action) || (ACTION_START.equals(action) && hasExtraDebug) ) {
            noteBuilder.addAction(notificationStopAction());

            // show radio station name in notification
            noteBuilder.setContentText(currentRadioStationName(intent));
        }

        // if normal radio is playing and multiple stations are configured, also add button to
        // switch to next station
        if ( ACTION_START_STREAM.equals(action) ) {
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
        Log.d(TAG,"switchToNextStation() called.");
        if (streamingMode != StreamingMode.RADIO) {
            return;
        }

        int currentIndex = getCurrentRadioStationIndex();
        if (currentIndex < 0) {
            return;
        }

        FavoriteRadioStations stations = settings.getFavoriteRadioStations();
        int nextStationIndex = stations.nextAvailableIndex(currentIndex);
        Log.d(TAG,"nextStationIndex: " + nextStationIndex);

        // always stop and restart, so a new notification occurs
        stopSelf();
        if (nextStationIndex > -1) {
            startStream(this, nextStationIndex);
        }
    }

    public enum StreamingMode {INACTIVE, ALARM, RADIO}
}
