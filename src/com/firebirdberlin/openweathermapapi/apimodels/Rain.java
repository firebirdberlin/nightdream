package com.firebirdberlin.openweathermapapi.apimodels;

import com.google.gson.annotations.SerializedName;

public class Rain {
    @SerializedName("1h")
    private Double volume1h; // Use Double (object type) to allow null if not present
    @SerializedName("3h")
    private Double volume3h; // Use Double (object type) to allow null if not present

    public Double getVolume1h() {
        return volume1h;
    }

    public void setVolume1h(Double volume1h) {
        this.volume1h = volume1h;
    }

    public Double getVolume3h() {
        return volume3h;
    }

    public void setVolume3h(Double volume3h) {
        this.volume3h = volume3h;
    }
}