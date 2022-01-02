package com.firebirdberlin.nightdream.services;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
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
import com.google.gson.Gson;

public class DownloadWeatherService extends Worker {
    private static final String TAG = "DownloadWeatherService";

    public DownloadWeatherService(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void start(Context context, Settings settings) {
        start(context, settings, false);
    }

    public static void start(Context context, Settings settings, Boolean widget) {
        if (!shallUpdateWeatherData(context, settings)) {
            return;
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest downloadWeatherWork = new OneTimeWorkRequest.Builder(
                DownloadWeatherService.class)
                .addTag(TAG)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueue(downloadWeatherWork);

        if(!widget) {
            //Cannot invoke observe on a background thread -> handler.post
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> WorkManager.getInstance(context).getWorkInfoByIdLiveData(downloadWeatherWork.getId())
                    .observe((LifecycleOwner) context, info -> {
                        if (info != null && info.getState().isFinished()) {
                            Log.d(TAG, "onPostExecute");
                            Gson gson = new Gson();
                            WeatherEntry entry = gson.fromJson(
                                    info.getOutputData().getString("entry"), WeatherEntry.class);

                            broadcastResult(context);
                        }
                    }));
        }
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


        if ((entry != null)
                && (entry.timestamp > WeatherEntry.INVALID)) {
            settings.setWeatherEntry(entry);
            ScreenWatcherService.updateNotification(getApplicationContext(), entry, settings.temperatureUnit);
            Log.d(TAG, "Download finished.");

            Gson gson = new Gson();
            String jsonEntry = gson.toJson(entry);

            Data myData = new Data.Builder()
                    .putString("entry", jsonEntry)
                    .build();

            return Result.success(myData);
        } else {
            Log.w(TAG, "entry.timestamp is INVALID!");
            return Result.failure();
        }
    }

    private static void broadcastResult(Context context) {
        Log.d(TAG, "broadcastResult()");
        Intent intent = new Intent(OpenWeatherMapApi.ACTION_WEATHER_DATA_UPDATED);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}