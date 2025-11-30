/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.firebirdberlin.nightdream.services;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
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
import com.firebirdberlin.openweathermapapi.MetNoApi;
import com.firebirdberlin.openweathermapapi.OpenWeatherMapApi;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;
import com.google.gson.Gson;

public class DownloadWeatherService extends Worker {
    private static final String TAG = "DownloadWeatherService";
    public static MutableLiveData<WeatherEntry> outputObservable = new MutableLiveData<>();

    public DownloadWeatherService(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void start(Context context, Settings settings) {
        Log.d(TAG, "start");
        if (!shallUpdateWeatherData(context, settings)) {
            return;
        }

        Constraints constraints =
                new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

        OneTimeWorkRequest downloadWeatherWork =
                new OneTimeWorkRequest.Builder(DownloadWeatherService.class)
                        .addTag(TAG)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context).enqueue(downloadWeatherWork);
    }

    public static boolean shallUpdateWeatherData(Context context, Settings settings) {
        if (!settings.shallShowWeather() || !Utility.isScreenOn(context)) return false;
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

    public void stopWorker(Context context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG);
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
                entry = OpenWeatherMapApi.fetchWeatherDataApi(
                        getApplicationContext(),
                        cityID,
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

        if ((entry != null) &&
                (entry.timestamp > WeatherEntry.INVALID)) {
            settings.setWeatherEntry(entry);
            ScreenWatcherService.updateNotification(getApplicationContext(), entry, settings.temperatureUnit);
            Log.d(TAG, "Download finished.");

            outputObservable.postValue(entry);

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
}