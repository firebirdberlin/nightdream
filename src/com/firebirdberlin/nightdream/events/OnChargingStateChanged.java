package com.firebirdberlin.nightdream.events;

import com.firebirdberlin.nightdream.BatteryValue;

public class OnChargingStateChanged {

    public BatteryValue referenceValue;

    public OnChargingStateChanged(BatteryValue value) {
        this.referenceValue = value;
    }

}
