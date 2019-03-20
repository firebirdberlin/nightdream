package com.firebirdberlin.nightdream.services;

import android.content.Context;
import android.util.Log;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

public class WeatherService {
    private static String TAG = "NightDream.WeatherService";
    private static long lastLocationRequest = 0L;

    public static void start(Context context, String cityID) {
        if (!cityID.isEmpty()) {
            DownloadWeatherService.start(context, cityID);
            return;
        }

        LocationService.start(context);
    }

    public static boolean shallUpdateWeatherData(Context context, Settings settings) {
        if (!settings.showWeather) return false;

        WeatherEntry entry = settings.weatherEntry;
        long requestAge = System.currentTimeMillis() - lastLocationRequest;
        long diff = entry.ageMillis();
        final int maxDiff = 90 * 60 * 1000;
        final int maxRequestAge = 15 * 60 * 1000;
        final String cityID = String.valueOf(entry.cityID);
        Log.d(TAG, String.format("Weather: data age %d => %b", diff, diff > maxDiff));
        Log.d(TAG, String.format("Time since last request %d => %b", requestAge, requestAge > maxRequestAge));
        Log.d(TAG, String.format("City ID changed => %b (%s =?= %s)",
                (!settings.weatherCityID.isEmpty() && !settings.weatherCityID.equals(cityID)),
                settings.weatherCityID, cityID));
        boolean result = (
                Utility.hasNetworkConnection(context) &&
                        (
                                diff < 0L
                                        || (!settings.weatherCityID.isEmpty() && !settings.weatherCityID.equals(cityID))
                                        || (diff > maxDiff && requestAge > maxRequestAge)
                        )
        );
        if (result) {
            lastLocationRequest = System.currentTimeMillis();
        }
        return result;
    }
}
