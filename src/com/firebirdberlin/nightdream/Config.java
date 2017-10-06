package com.firebirdberlin.nightdream;

public class Config {
    /** Whether or not to include logging statements in the application. */
    public final static boolean LOGGING = false;
    public final static boolean PRO = true;

    public final static double NOISE_AMPLITUDE_WAKE  = 250.;
    public final static double NOISE_AMPLITUDE_SLEEP = 2.f * NOISE_AMPLITUDE_WAKE;
    public final static String ACTION_RADIO_STREAM_STARTED = "com.firebirdberlin.nightdream.action_radio_stream_started";
    public final static String ACTION_RADIO_STREAM_STOPPED = "com.firebirdberlin.nightdream.action_radio_stream_stopped";
    public final static String ACTION_SHUT_DOWN = "com.firebirdberlin.nightdream.SHUTDOWN";
    public final static String ACTION_NOTIFICATION_LISTENER = "com.firebirdberlin.nightdream.NOTIFICATION_LISTENER";
    public final static String ACTION_ALARM_SET = "com.firebirdberlin.nightdream.ALARM_SET";
    public final static String ACTION_ALARM_STOPPED = "com.firebirdberlin.nightdream.ALARM_STOPPED";
    public final static String ACTION_ALARM_DELETED = "com.firebirdberlin.nightdream.ALARM_DELETED";
    public final static String ACTION_SWITCH_NIGHT_MODE = "com.firebirdberlin.nightdream.ACTION_SWITCH_NIGHT_MODE";
    public final static String ACTION_START_SLEEP_TIME = "com.firebirdberlin.nightdream.ACTION_START_SLEEP_TIME";
    public final static int NOTIFICATION_ID_DISMISS_ALARMS = 1338;
}
