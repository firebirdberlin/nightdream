package com.firebirdberlin.nightdream;

public class Config {
    /** Whether or not to include logging statements in the application. */
    public final static boolean LOGGING = false;
    public final static boolean PRO = true;

    public final static boolean USE_RECORD_AUDIO = false;
    public final static double NOISE_AMPLITUDE_WAKE  = 250.;
    public final static double NOISE_AMPLITUDE_SLEEP = 2.f * NOISE_AMPLITUDE_WAKE;
    public final static String ACTION_RADIO_STREAM_STARTED = "com.firebirdberlin.nightdream.action_radio_stream_started";
    public final static String ACTION_RADIO_STREAM_STOPPED = "com.firebirdberlin.nightdream.action_radio_stream_stopped";
    public final static String ACTION_RADIO_STREAM_READY_FOR_PLAYBACK = "com.firebirdberlin.nightdream.action_radio_stream_ready_for_playback";
    public final static String ACTION_RADIO_STREAM_METADATA_UPDATED = "com.firebirdberlin.nightdream.action_radio_stream_metadata_updated";
    public final static String ACTION_NOTIFICATION_LISTENER = "com.firebirdberlin.nightdream.NOTIFICATION_LISTENER";
    public final static String ACTION_NOTIFICATION_APPS_LISTENER = "com.firebirdberlin.nightdream.NOTIFICATION_APPS_LISTENER";
    public final static String ACTION_ALARM_SET = "com.firebirdberlin.nightdream.ALARM_SET";
    public final static String ACTION_ALARM_DELETED = "com.firebirdberlin.nightdream.ALARM_DELETED";
    public final static String ACTION_SWITCH_NIGHT_MODE = "com.firebirdberlin.nightdream.ACTION_SWITCH_NIGHT_MODE";
    public final static String ACTION_STOP_BACKGROUND_SERVICE = "com.firebirdberlin.nightdream.ACTION_STOP_BACKGROUND_SERVICE";

    public final static int NOTIFICATION_ID_DISMISS_ALARMS = 1338;
    public final static int NOTIFICATION_ID_FOREGROUND_SERVICES = 1339;
    public final static int NOTIFICATION_ID_FOREGROUND_SERVICES_LOCATION = 1340;
    public final static int NOTIFICATION_ID_FOREGROUND_SERVICES_NIGHT_MODE = 1341;
    public final static String NOTIFICATION_CHANNEL_ID_ALARMS = "com.firebirdberlin.nightdream.notification_channel_alarms";
    public final static String NOTIFICATION_CHANNEL_ID_RADIO = "com.firebirdberlin.nightdream.notification_channel_radio";
    public final static String NOTIFICATION_CHANNEL_ID_SERVICES = "com.firebirdberlin.nightdream.notification_channel_services";
    public final static String NOTIFICATION_CHANNEL_ID_DEVMSG = "com.firebirdberlin.nightdream.notification_channel_devmsg";

    public final static int JOB_ID_ALARM_NOTIFICATION = 2000;
    public final static int JOB_ID_FETCH_WEATHER_DATA = 2001;
    public final static int JOB_ID_ALARM_WIFI = 2002;
    public final static int JOB_ID_SQLITE_SERVICE= 2003;
    public final static int JOB_ID_UPDATE_LOCATION = 2004;
    public final static int JOB_ID_UPDATE_WEATHER = 2005;

    public final static String backgroundImageCacheFilename = "bgimage.png";
}
