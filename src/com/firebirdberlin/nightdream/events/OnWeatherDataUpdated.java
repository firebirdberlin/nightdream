package com.firebirdberlin.nightdream.events;

import com.firebirdberlin.nightdream.models.WeatherEntry;

public class OnWeatherDataUpdated {

    public WeatherEntry entry;
    public OnWeatherDataUpdated(WeatherEntry entry) {
        this.entry = entry;
    }

}
