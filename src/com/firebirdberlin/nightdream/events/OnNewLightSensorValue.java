package com.firebirdberlin.nightdream.events;

public class OnNewLightSensorValue {

    public float value;
    public int n;

    public OnNewLightSensorValue(float value, int n) {
        this.value = value;
        this.n = n;
    }
}
