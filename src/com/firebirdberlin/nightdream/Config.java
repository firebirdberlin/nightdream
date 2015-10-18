package com.firebirdberlin.nightdream;

public class Config
{
    /** Whether or not to include logging statements in the application. */
    public final static boolean LOGGING = false;
    public final static boolean PRO = true;

    public final static double NOISE_AMPLITUDE_WAKE  = 250.;
	public final static double NOISE_AMPLITUDE_SLEEP = 2.f * NOISE_AMPLITUDE_WAKE;
}
