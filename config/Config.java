package @CONFIG.PACKAGENAME@;

public class Config
{
    /** Whether or not to include logging statements in the application. */
    public final static boolean LOGGING = @CONFIG.LOGGING@;
    public final static boolean PRO = @CONFIG.PRO@;

    public final static double NOISE_AMPLITUDE_WAKE  = 250.;
    public final static double NOISE_AMPLITUDE_SLEEP = 2.f * NOISE_AMPLITUDE_WAKE;
    public final static String ACTION_RADIO_STREAM_STOPPED = "com.firebirdberlin.nightdream.action_radio_stream_stopped";
}
