package com.firebirdberlin.nightdream.models;

public class WeatherEntry {
    public static final int CELSIUS = 1;
    public static final int FAHRENHEIT = 2;
    public static final int KELVIN = 3;
    public static final long INVALID = -1L;

    public long timestamp = INVALID;
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

    public void setFakeData() {
        timestamp = System.currentTimeMillis();
        temperature = 284.15;
        weatherIcon = "03d";
    }
}
