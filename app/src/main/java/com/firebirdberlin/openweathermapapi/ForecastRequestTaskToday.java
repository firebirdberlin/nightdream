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

package com.firebirdberlin.openweathermapapi;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

public class ForecastRequestTaskToday {

    private static final String TAG = "ForecastRequestTask";
    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private final AsyncResponse delegate;
    private final Settings.WeatherProvider weatherProvider;
    private final Context context;

    public ForecastRequestTaskToday(AsyncResponse listener, Settings.WeatherProvider weatherProvider, Context mContext) {
        this.delegate = listener;
        this.weatherProvider = weatherProvider;
        this.context = mContext;
    }

    public void execute(String cityJson) {
        if (cityJson == null || cityJson.isEmpty()) {
            mainThreadHandler.post(() -> delegate.onRequestError(new IllegalArgumentException("City JSON cannot be null or empty.")));
            return;
        }

        try {
            executorService.execute(() -> {
                City city = City.fromJson(cityJson);
                if (city == null) {
                    mainThreadHandler.post(() -> delegate.onRequestError(new Exception("Failed to parse City JSON.")));
                    return;
                }

                WeatherEntry weatherEntry;
                try {
                    switch (weatherProvider) {
                        case BRIGHT_SKY:
                            weatherEntry = BrightSkyApi.fetchCurrentWeatherData(context, (float) city.lat, (float) city.lon);
                            break;
                        case MET_NO:
                            weatherEntry = MetNoApi.fetchCurrentWeatherData(context, (float) city.lat, (float) city.lon);
                            break;
                        case OPEN_WEATHER_MAP:
                        default:
                            weatherEntry = OpenWeatherMapApi.fetchWeatherDataApi(context, String.valueOf(city.id), (float) city.lat, (float) city.lon);
                            break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching weather data", e);
                    mainThreadHandler.post(() -> delegate.onRequestError(e));
                    return; // Ensure no further execution after an error
                }

                final WeatherEntry finalWeatherEntry = weatherEntry;
                mainThreadHandler.post(() -> delegate.onRequestFinished(finalWeatherEntry));
            });
        } catch (RejectedExecutionException e) {
            Log.e(TAG, "Task rejected: Executor service may be shutting down or overloaded.", e);
            mainThreadHandler.post(() -> delegate.onRequestError(new Exception("Search task rejected. The service may be unavailable.")));
        }
    }

    public interface AsyncResponse {
        void onRequestFinished(WeatherEntry entries);
        void onRequestError(Exception exception);
    }

    public static void shutdownExecutor() {
        Log.i(TAG, "Shutting down ExecutorService...");
        executorService.shutdown();
    }
}
