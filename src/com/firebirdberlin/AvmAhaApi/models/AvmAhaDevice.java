package com.firebirdberlin.AvmAhaApi.models;

import android.util.Log;

public class AvmAhaDevice {

    public static final String STATE_OFF = "0";
    public static final String STATE_ON = "1";
    public static final String STATE_TOGGLE = "2";

    public String ain;
    public String name;
    public String manufacturer;
    public String productname;
    public String state;
    public String present;
    public String level;
    public int functionbitmask;

    public void setState(String state) {
        this.state = state;
    }
    public boolean isOn() {
        return "1".equals(state);
    }

    public boolean isPresent() {
        return "1".equals(present);
    }

    public boolean isSwitchable() {
        /*
        Log.d("Smart", "isSwitchable 1 " + bitmask());
        Log.d("Smart", "isSwitchable 2 " + Integer.toBinaryString((1 << 15)));
        Log.d("Smart", "isSwitchable 3 " + Integer.toBinaryString(functionbitmask & (1 << 15)));
        */
        return (( (functionbitmask) & (1 << 15) ) != 0);
    }

    public boolean hasLevel() {
        return ((functionbitmask & (1 << 16)) != 0);
    }

    public boolean hasColor() {
        return ((functionbitmask & (1 << 17)) != 0);
    }

    public boolean isSwitch() {
        return ((functionbitmask & (1 << 9)) != 0);
    }

    public boolean isLightBulb() {
        return ((functionbitmask & (1 << 2)) != 0);
    }

    public String bitmask() {
        return Integer.toBinaryString(functionbitmask);
    }

    public String toString() {
        return String.format("%s %s %s %s %s", manufacturer, productname, name, state, bitmask());
    }
}
