package com.firebirdberlin.nightdream.events;

public class OnLightSensorValueTimeout {

    public float value;

    public OnLightSensorValueTimeout(float last_value) {
        this.value = last_value;
    }
}
