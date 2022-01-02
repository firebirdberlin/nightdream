package com.firebirdberlin.nightdream.services;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.openweathermapapi.BrightSkyApi;
import com.firebirdberlin.openweathermapapi.DarkSkyApi;
import com.firebirdberlin.openweathermapapi.MetNoApi;
import com.firebirdberlin.openweathermapapi.OpenWeatherMapApi;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

public class DownloadWeatherService extends Worker {
    private static final String TAG = "DownloadWeatherService";

    public DownloadWeatherService(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void start(Context context, Settings settings) {
        if (!shallUpdateWeatherData(context, settings)) {
            return;
        }
        OneTimeWorkRequest downloadWeatherWork = new OneTimeWorkRequest.Builder(
                DownloadWeatherService.class
        ).build();
        WorkManager.getInstance(context).enqueue(downloadWeatherWork);
    }

    public static boolean shallUpdateWeatherData(Context context, Settings settings) {
        if (!settings.showWeather || !Utility.isScreenOn(context)) return false;
        WeatherEntry entry = settings.weatherEntry;
        long age = entry.ageMillis();
        final int maxAge = 60 * 60 * 1000;
        Location weatherLocation = entry.getLocation();
        Location gpsLocation = settings.getLocation();
        float gpsDistance =
                (weatherLocation != null && gpsLocation != null)
                        ? weatherLocation.distanceTo(gpsLocation) : -1.f;

        Log.d(TAG, String.format("Weather: data age %d => %b", age, age > maxAge));
        if (settings.weatherCityID.isEmpty()) {
            Log.d(TAG, "GPS distance " + gpsDistance + " m ");
        }

        boolean result = (
                Utility.hasNetworkConnection(context) && (
                        age < 0L || age > maxAge
                                || (settings.weatherCityID.isEmpty() && gpsDistance > 10000.f)
                )
        );

        Log.d(TAG, "shallUpdateWeatherData = " + result);
        return result;
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork()");

        Settings settings = new Settings(getApplicationContext());
        if (!Utility.hasNetworkConnection(getApplicationContext())) {
            Log.d(TAG, "!hasNetworkConnection");
            return Result.failure();
        }
        City city = settings.getCityForWeather();
        Location location = settings.getLocation();
        String cityID = settings.weatherCityID;
        Settings.WeatherProvider weatherProvider = settings.getWeatherProvider();
        WeatherEntry entry;

        Log.d(TAG, "fetchWeatherData");
        switch (weatherProvider) {
            case OPEN_WEATHER_MAP:
            default:
                entry = OpenWeatherMapApi.fetchWeatherData(
                        getApplicationContext(),
                        cityID,
                        (float) location.getLatitude(),
                        (float) location.getLongitude()
                );
                break;
            case DARK_SKY:
                entry = DarkSkyApi.fetchCurrentWeatherData(
                        getApplicationContext(),
                        city,
                        (float) location.getLatitude(),
                        (float) location.getLongitude()
                );
                break;
            case BRIGHT_SKY:
                if (city != null) {
                    entry = BrightSkyApi.fetchCurrentWeatherData(
                            getApplicationContext(),
                            (float) city.lat,
                            (float) city.lon
                    );
                } else {
                    entry = BrightSkyApi.fetchCurrentWeatherData(
                            getApplicationContext(),
                            (float) location.getLatitude(),
                            (float) location.getLongitude()
                    );
                }
                break;
            case MET_NO:
                if (city != null) {
                    entry = MetNoApi.fetchCurrentWeatherData(
                            getApplicationContext(),
                            (float) city.lat,
                            (float) city.lon
                    );
                } else {
                    entry = MetNoApi.fetchCurrentWeatherData(
                            getApplicationContext(),
                            (float) location.getLatitude(),
                            (float) location.getLongitude()
                    );
                }
                break;
        }

        onPostExecute(entry);

        return Result.success();
    }

    protected void onPostExecute(WeatherEntry entry) {
        Log.d(TAG, "onPostExecute(WeatherEntry)");

        if (entry == null) {
            return;
        }

        if (entry.timestamp > WeatherEntry.INVALID) {
            Settings settings = new Settings(getApplicationContext());
            settings.setWeatherEntry(entry);
            broadcastResult(settings, entry);
            Log.d(TAG, "Download finished.");
        } else {
            Log.w(TAG, "entry.timestamp is INVALID!");
        }

    }

    private void broadcastResult(Settings settings, WeatherEntry entry) {
        Log.d(TAG, "broadcastResult(Settings, WeatherEntry)");
        Intent intent = new Intent(OpenWeatherMapApi.ACTION_WEATHER_DATA_UPDATED);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        ScreenWatcherService.updateNotification(getApplicationContext(), entry, settings.temperatureUnit);
    }
}