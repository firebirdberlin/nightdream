package com.firebirdberlin.openweathermapapi.models;

import com.firebirdberlin.nightdream.WindSpeedConversion;

public class WeatherEntry {
    public static final int CELSIUS = 1;
    public static final int FAHRENHEIT = 2;
    public static final int KELVIN = 3;
    public static final int METERS_PER_SECOND = 1;
    public static final int MILES_PER_HOUR = 2;
    public static final int KM_PER_HOUR = 3;
    public static final int BEAUFORT = 4;
    public static final int KNOT = 5;
    public static final long INVALID = -1L;

    public long timestamp = INVALID;
    public long request_timestamp = INVALID;
    public int cityID = 0;
    public String cityName = "";
    public int clouds = -1;
    public double rain3h = -1.f;
    public double temperature = 0.f;
    public long sunriseTime = 0L;
    public long sunsetTime = 0L;
    public String weatherIcon = "";
    public String description = "";
    public float lat = 0.0f;
    public float lon = 0.0f;
    public double windSpeed = 0.;
    public int windDirection = -1;

    public WeatherEntry() {

    }

    public void setFakeData() {
        request_timestamp = System.currentTimeMillis();
        timestamp = System.currentTimeMillis() / 1000;
        temperature = 273.15 + randomFromRange(0, 40); // in K
        weatherIcon = "03d";
    }

    public long ageMillis() {
        if (request_timestamp == INVALID) {
            return INVALID;
        }

        return System.currentTimeMillis() - request_timestamp;
    }

    public String toString() {
        return String.format("%2.2fK %2.2fm/s %d° %d %s",
                temperature, windSpeed, windDirection, timestamp,
                description);
    }


    private int randomFromRange(int min, int max) {
        int range = (max - min) + 1;
        return (int) (Math.random() * range) + min;
    }

    public String formatTemperatureText(int temperatureUnit) {
        switch (temperatureUnit) {
            case WeatherEntry.CELSIUS:
                return String.format("%d°C", Math.round(toDegreesCelcius(this.temperature)));
            case WeatherEntry.FAHRENHEIT:
                return String.format("%d°F", Math.round(toDegreesFahrenheit(this.temperature)));
            default:
                return String.format("%d K", Math.round(this.temperature));
        }
    }
    private double toDegreesCelcius(double kelvin) {
        return kelvin - 273.15;
    }

    private double toDegreesFahrenheit(double kelvin) {
        return kelvin * 1.8 - 459.67;
    }

    public String formatWindText(int speedUnit) {
        switch (speedUnit) {
            case WeatherEntry.BEAUFORT:
                return String.format("%d Bft", WindSpeedConversion.metersPerSecondToBeaufort(this.windSpeed));
            case WeatherEntry.MILES_PER_HOUR:
                double mph = WindSpeedConversion.metersPerSecondToMilesPerHour(this.windSpeed);
                return String.format("%.1f mi/h", mph);
            case WeatherEntry.KM_PER_HOUR:
                double kmph = WindSpeedConversion.metersPerSecondToKilometersPerHour(this.windSpeed);
                return String.format("%.1f km/h", kmph);
            case WeatherEntry.KNOT:
                double kn = WindSpeedConversion.metersPerSecondToKnot(this.windSpeed);
                return String.format("%.1f kn", kn);
            case WeatherEntry.METERS_PER_SECOND:
            default:
                return String.format("%.1f m/s", this.windSpeed);
        }
    }

}
