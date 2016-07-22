package com.firebirdberlin.nightdream.events;

import com.firebirdberlin.nightdream.models.BatteryValue;

public class OnPowerConnected {

    public BatteryValue referenceValue;

    public OnPowerConnected(BatteryValue value) {
        this.referenceValue = value;
    }

}
