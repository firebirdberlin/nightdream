package com.firebirdberlin.nightdream.models;

public class BatteryValue {
    public static int BATTERY_PLUGGED_AC = 1;
    public static int BATTERY_PLUGGED_USB = 2;
    public static int BATTERY_PLUGGED_WIRELESS = 3;

    public int level = 0;
    public int scale = -1;
    public float levelNormalized;
    public long time = 0L;
    public int chargingMethod = -1;
    public int status = -1;
    public boolean isCharging = false;
    public boolean isChargingAC = false;
    public boolean isChargingUSB = false;
    public boolean isChargingWireless = false;

    public BatteryValue(int level) {
        this.level = level;
        this.time = System.currentTimeMillis();
    }

    public BatteryValue(int level, int scale, int status, int chargingMethod) {
        this.level = level;
        this.scale = scale;
        this.time = System.currentTimeMillis();
        this.chargingMethod = chargingMethod;
        this.status = status;
        this.levelNormalized = level / (float) scale;
    }

    public float getPercentage() {
        return (((float) level / (float) scale) * 100.0f);
    }

    public long getEstimateMillis(BatteryValue reference) {
        if ( reference.level == -1 || reference.level == level ) return -1L;
        int dL = level - reference.level;
        long dt = System.currentTimeMillis() - reference.time;

        if ((dL == 0) || (dt == 0.)) return 0;
        //double scale = dL/dt * te + L0
        // te = (scale - L0) * dt /dL
        return (long) ( (scale - level) * ((double) dt / (double) dL));
    }

    public long getDischargingEstimateMillis(BatteryValue reference) {
        if ( reference.level == -1 || reference.level == level ) return -1L;

        int dL = level - reference.level;
        long dt = System.currentTimeMillis() - reference.time;
        if ((dL == 0) || (dt == 0.)) return 0;
        //double scale = dL/dt * te + L0
        // te = (scale - L0) * dt /dL
        return (long) ((-level) * ((double) dt / (double) dL));
    }
}
