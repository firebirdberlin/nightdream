package com.firebirdberlin.openweathermapapi.models;

import android.location.Location;
import android.location.LocationManager;
import android.os.Build;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.WindSpeedConversion;

import org.shredzone.commons.suncalc.SunTimes;

import java.util.Date;

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
    public int humidity = 0;
    public double rain1h = -1.f;
    public double rain3h = -1.f;
    public double temperature = 0.f;
    public double apparentTemperature = 0.f;
    public long sunriseTime = 0L;
    public long sunsetTime = 0L;
    public String weatherIcon = "";
    public String weatherIconMeteoconsSymbol = "";
    public String description = "";
    public float lat = 0.0f;
    public float lon = 0.0f;
    public double windSpeed = 0.;
    public int windDirection = -1;
    public WeatherEntry() {

    }

    public long getSunriseTime() {
        if (sunriseTime > 0L) {
            return sunriseTime * 1000;
        }
        SunTimes sunTime = SunTimes.compute().at(lat, lon).execute();
        Date rise = sunTime.getRise();
        if (rise != null) {
            return rise.getTime();
        }
        return 0L;
    }

    public long getSunsetTime() {
        if (sunsetTime > 0L) {
            return sunsetTime * 1000;
        }
        SunTimes sunTime = SunTimes.compute().at(lat, lon).execute();
        Date set = sunTime.getSet();
        if (set != null) {
            return set.getTime();
        }
        return 0L;
    }

    public void setFakeData() {
        request_timestamp = System.currentTimeMillis();
        timestamp = System.currentTimeMillis() / 1000;
        temperature = 273.15 + randomFromRange(0, 40); // in K
        apparentTemperature = 273.15 + randomFromRange(0, 40); // in K
        weatherIcon = "03d";
    }

    public long ageMillis() {
        if (request_timestamp == INVALID) {
            return INVALID;
        }

        return System.currentTimeMillis() - request_timestamp;
    }

    public Location getLocation() {
        Location l = new Location(LocationManager.NETWORK_PROVIDER);
        l.setLongitude(lon);
        l.setLatitude(lat);
        l.setTime(timestamp);
        return l;
    }

    public String toString() {
        return String.format(java.util.Locale.getDefault(),
                "%2.2fK %2.2fm/s %d° %d %s",
                temperature, windSpeed, windDirection, timestamp, description
        );
    }


    private int randomFromRange(int min, int max) {
        int range = (max - min) + 1;
        return (int) (Math.random() * range) + min;
    }

    public String formatTemperatureText(int temperatureUnit) {
        return formatTemperatureText(temperatureUnit, true);
    }

    public String formatTemperatureText(int temperatureUnit, boolean showApparentTemperature) {
        if (showApparentTemperature && apparentTemperature > 0.f) {
            return String.format("%s (%s)", formatTemperatureText(temperatureUnit, temperature), formatTemperatureText(temperatureUnit, apparentTemperature));
        }

        return formatTemperatureText(temperatureUnit, temperature);
    }

    public String formatTemperatureText(int temperatureUnit, double temp) {
        switch (temperatureUnit) {
            case WeatherEntry.CELSIUS:
                return String.format(java.util.Locale.getDefault(), "%d°C", Math.round(toDegreesCelcius(temp)));
            case WeatherEntry.FAHRENHEIT:
                return String.format(java.util.Locale.getDefault(), "%d°F", Math.round(toDegreesFahrenheit(temp)));
            default:
                return String.format(java.util.Locale.getDefault(), "%d K", Math.round(temp));
        }
    }

    public String formatHumidityText() {
        if (humidity < 0) return "";
        return String.format(java.util.Locale.getDefault(), "%d %%", humidity);
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
                return String.format(java.util.Locale.getDefault(), "%d Bft", WindSpeedConversion.metersPerSecondToBeaufort(this.windSpeed));
            case WeatherEntry.MILES_PER_HOUR:
                double mph = WindSpeedConversion.metersPerSecondToMilesPerHour(this.windSpeed);
                return String.format(java.util.Locale.getDefault(), "%.1f mi/h", mph);
            case WeatherEntry.KM_PER_HOUR:
                double kmph = WindSpeedConversion.metersPerSecondToKilometersPerHour(this.windSpeed);
                return String.format(java.util.Locale.getDefault(), "%.1f km/h", kmph);
            case WeatherEntry.KNOT:
                double kn = WindSpeedConversion.metersPerSecondToKnot(this.windSpeed);
                return String.format(java.util.Locale.getDefault(), "%.1f kn", kn);
            case WeatherEntry.METERS_PER_SECOND:
            default:
                return String.format(java.util.Locale.getDefault(), "%.1f m/s", this.windSpeed);
        }
    }

    public boolean isValid() {
        long age = ageMillis();
        return (timestamp > -1L && age < 4 * 60 * 60 * 1000);
    }

    public String getWeatherIconIdentifier(int weatherIconMode, boolean widget) {
        if (weatherIconMode == Settings.WEATHER_ICON_MODE_DEFAULT) {
            return weatherIconMeteoconsSymbol;
        }

        String identifier;
        switch (weatherIconMeteoconsSymbol) {
            case "A":
            case "B":
                identifier = "weather_day";
                break;
            case "C":
                identifier = "weather_night";
                break;
            case "G":
                identifier = "weather_snow_1";
                break;
            case "H":
                identifier = "weather_cloudy_day";
                break;
            case "I":
                identifier = "weather_cloudy_night";
                break;
            case "J":
            case "K":
            case "L":
            case "M":
                identifier = "weather_fog";
                break;
            case "N":
                identifier = "weather_cloudy";
                break;
            case "O":
            case "P":
                identifier = "weather_thunder";
                break;
            case "Q":
                identifier = "weather_rain_1";
                break;
            case "R":
                identifier = "weather_rain_3";
                break;
            case "S":
                identifier = "weather_cloudy";
                break;
            case "T":
                identifier = "weather_rain_2";
                break;
            case "U":
            case "V":
                identifier = "weather_snow_1";
                break;
            case "W":
                identifier = "weather_snow_3";
                break;
            case "X":
                identifier = "weather_rain_2";
                break;
            case "Y":
                identifier = "weather_cloudy";
                break;
            case "Z":
            case "0":
                identifier = "weather_thunder";
                break;
            default:
                identifier = "weather_cloudy";
                break;
        }
        if (weatherIconMode == Settings.WEATHER_ICON_MODE_ANIMATED
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !widget
        ) {
            identifier += "_avd";
        }

        return identifier;
    }

}
