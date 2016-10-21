package com.firebirdberlin.nightdream.models;

public class WeatherEntry {
    public long timestamp = 0L;
    public int cityID = 0;
    public String cityName = "";
    public double temperature = 0.f;
    public long sunriseTime = 0L;
    public long sunsetTime = 0L;
    public String weatherIcon = "";
    public float lat = 0.0f;
    public float lon = 0.0f;

    public WeatherEntry() {

    }
}
