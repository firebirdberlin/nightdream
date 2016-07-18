package com.firebirdberlin.nightdream.events;

import com.firebirdberlin.nightdream.models.BatteryValue;

public class OnPowerDisconnected {

    public BatteryValue referenceValue;

    public OnPowerDisconnected(BatteryValue value) {
        this.referenceValue = value;
    }

}
