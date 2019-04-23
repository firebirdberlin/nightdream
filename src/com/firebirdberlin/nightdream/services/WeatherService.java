package com.firebirdberlin.nightdream.services;

import android.content.Context;
import android.util.Log;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

public class WeatherService {
    private static String TAG = "NightDream.WeatherService";

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
        long age = entry.ageMillis();
        final int maxAge = 60 * 60 * 1000;
        final String cityID = String.valueOf(entry.cityID);
        Log.d(TAG, String.format("Weather: data age %d => %b", age, age > maxAge));
        Log.d(TAG, String.format("City ID changed => %b (%s =?= %s)",
                (!settings.weatherCityID.isEmpty() && !settings.weatherCityID.equals(cityID)),
                settings.weatherCityID, cityID));
        boolean result = (
                Utility.hasNetworkConnection(context) &&
                        (
                                age < 0L
                                        || (!settings.weatherCityID.isEmpty() && !settings.weatherCityID.equals(cityID))
                                        || (age > maxAge)
                        )
        );
        return result;
    }
}
