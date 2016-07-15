package com.firebirdberlin.nightdream;

public class BatteryValue {
    public int level = 0;
    public long time = 0L;
    public int chargingMethod = -1;
    public int status = -1;

    public BatteryValue(int level) {
        this.level = level;
        this.time = System.currentTimeMillis();
    }

    public BatteryValue(int level, int status, int chargingMethod) {
        this.level = level;
        this.time = System.currentTimeMillis();
        this.chargingMethod = chargingMethod;
        this.status = status;
    }
}
