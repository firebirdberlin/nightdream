package com.firebirdberlin.nightdream.services;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

public class WeatherService {
    private static String TAG = "WeatherService";

    public static void start(Context context) {
        DownloadWeatherService.start(context);
    }

    public static boolean shallUpdateWeatherData(Context context, Settings settings) {
        if (!settings.showWeather || !Utility.isScreenOn(context)) return false;
        WeatherEntry entry = settings.weatherEntry;
        long age = entry.ageMillis();
        final int maxAge = 60 * 60 * 1000;
        final String cityID = String.valueOf(entry.cityID);
        Location weatherLocation = entry.getLocation();
        Location gpsLocation = settings.getLocation();
        float gpsDistance = (weatherLocation != null && gpsLocation != null)
                ? weatherLocation.distanceTo(gpsLocation) : -1.f;

        Log.d(TAG, String.format("Weather: data age %d => %b", age, age > maxAge));
        Log.d(TAG, String.format("City ID changed => %b ('%s' =?= '%s')",
                (!settings.weatherCityID.isEmpty() && !settings.weatherCityID.equals(cityID)),
                settings.weatherCityID, cityID));
        if (settings.weatherCityID.isEmpty() ) {
            Log.d(TAG, "GPS distance " + gpsDistance + " m ");
        }

        boolean result = (
                Utility.hasNetworkConnection(context) &&
                        (
                                age < 0L
                                        || (!settings.weatherCityID.isEmpty() && !settings.weatherCityID.equals(cityID))
                                        || age > maxAge
                                        || (settings.weatherCityID.isEmpty() && gpsDistance > 10000.f)
                        )
        );

        Log.d(TAG, "shallUpdateWeatherData = " + result);
        return result;
    }
}
