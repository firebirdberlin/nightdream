package com.firebirdberlin.openweathermapapi.apimodels;

import com.google.gson.annotations.SerializedName;

public class Snow {
    @SerializedName("3h")
    private double volume3h;

    public double getVolume3h() {
        return volume3h;
    }

    public void setVolume3h(double volume3h) {
        this.volume3h = volume3h;
    }
}