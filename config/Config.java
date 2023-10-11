package @CONFIG.PACKAGENAME@;

public class Config {
    /** Whether or not to include logging statements in the application. */
    public final static boolean LOGGING = @CONFIG.LOGGING@;
    public final static boolean PRO = @CONFIG.PRO@;

    public final static double NOISE_AMPLITUDE_WAKE  = 250.;
    public final static double NOISE_AMPLITUDE_SLEEP = 2.f * NOISE_AMPLITUDE_WAKE;
    public final static String ACTION_RADIO_STREAM_STARTED = "com.firebirdberlin.nightdream.action_radio_stream_started";
    public final static String ACTION_RADIO_STREAM_STOPPED = "com.firebirdberlin.nightdream.action_radio_stream_stopped";
    public final static String ACTION_NOTIFICATION_LISTENER = "com.firebirdberlin.nightdream.NOTIFICATION_LISTENER";
    public final static String ACTION_ALARM_SET = "com.firebirdberlin.nightdream.ALARM_SET";
    public final static String ACTION_ALARM_DELETED = "com.firebirdberlin.nightdream.ALARM_DELETED";
    public final static String ACTION_SWITCH_NIGHT_MODE = "com.firebirdberlin.nightdream.ACTION_SWITCH_NIGHT_MODE";
    public final static int NOTIFICATION_ID_DISMISS_ALARMS = 1338;
}
