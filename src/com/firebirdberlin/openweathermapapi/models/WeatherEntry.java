package com.firebirdberlin.openweathermapapi.models;

import java.util.Calendar;

public class WeatherEntry {
    public static final int CELSIUS = 1;
    public static final int FAHRENHEIT = 2;
    public static final int KELVIN = 3;
    public static final int METERS_PER_SECOND = 1;
    public static final int MILES_PER_HOUR = 2;
    public static final int KM_PER_HOUR = 3;
    public static final int BEAUFORT = 4;
    public static final long INVALID = -1L;

    public long timestamp = INVALID;
    public int cityID = 0;
    public String cityName = "";
    public int clouds = -1;
    public double rain3h = -1.f;
    public double temperature = 0.f;
    public long sunriseTime = 0L;
    public long sunsetTime = 0L;
    public String weatherIcon = "";
    public float lat = 0.0f;
    public float lon = 0.0f;
    public double windSpeed = 0.;
    public int windDirection = -1;

    public WeatherEntry() {

    }

    public void setFakeData() {
        timestamp = System.currentTimeMillis() / 1000;
        temperature = 284.15;
        weatherIcon = "03d";
    }

    public long ageMillis() {
        if (timestamp == INVALID) {
            return INVALID;
        }

        long now = System.currentTimeMillis();
        return now - 1000 * timestamp;
    }

    public String toString() {
        return String.format("%2.2fK %2.2fm/s %d° %d", temperature, windSpeed, windDirection, timestamp);
    }

    public static WeatherEntry fakeWeatherWithRandomTemperature() {
        WeatherEntry e = new WeatherEntry();
        e.setFakeData();
        e.temperature = Math.random() * 100;
        return e;
    }
}
