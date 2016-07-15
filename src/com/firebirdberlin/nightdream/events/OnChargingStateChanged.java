package com.firebirdberlin.nightdream.events;

import com.firebirdberlin.nightdream.models.BatteryValue;

public class OnChargingStateChanged {

    public BatteryValue referenceValue;

    public OnChargingStateChanged(BatteryValue value) {
        this.referenceValue = value;
    }

}
