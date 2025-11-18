package com.firebirdberlin.openweathermapapi;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

public class ForecastRequestTask {

    private static final String TAG = "ForecastRequestTask";
    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private final AsyncResponse delegate;
    private final Settings.WeatherProvider weatherProvider;
    private final Context context;

    public ForecastRequestTask(AsyncResponse listener, Settings.WeatherProvider weatherProvider, Context mContext) {
        this.delegate = listener;
        this.weatherProvider = weatherProvider;
        this.context = mContext;
    }

    public void execute(String cityJson) {
        if (cityJson == null || cityJson.isEmpty()) {
            mainThreadHandler.post(() -> delegate.onRequestFinished(null)); // Or handle error appropriately
            return;
        }

        try {
            executorService.execute(() -> {
                City city = City.fromJson(cityJson);
                if (city == null) {
                    mainThreadHandler.post(() -> delegate.onRequestFinished(null)); // Or handle error appropriately
                    return;
                }

                List<WeatherEntry> weatherEntries = null;
                try {
                    switch (weatherProvider) {
                        case DARK_SKY:
                            weatherEntries = DarkSkyApi.fetchHourlyWeatherData(context, city);
                            break;
                        case BRIGHT_SKY:
                            weatherEntries = BrightSkyApi.fetchHourlyWeatherData(context, (float) city.lat, (float) city.lon);
                            break;
                        case MET_NO:
                            weatherEntries = MetNoApi.fetchHourlyWeatherData(context, (float) city.lat, (float) city.lon);
                            break;
                        case OPEN_WEATHER_MAP:
                        default:
                            weatherEntries = OpenWeatherMapApi.fetchWeatherForecastApi(context, city);
                            break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching weather data", e);
                    // Handle exception appropriately, perhaps by posting an error to the delegate
                    mainThreadHandler.post(() -> delegate.onRequestError(e));
                }

                final List<WeatherEntry> finalWeatherEntries = weatherEntries;
                mainThreadHandler.post(() -> delegate.onRequestFinished(finalWeatherEntries));
            });
        } catch (RejectedExecutionException e) {
            Log.e(TAG, "Task rejected: Executor service may be shutting down or overloaded.", e);
            // Handle rejection, maybe by posting an error to the delegate
            mainThreadHandler.post(() -> delegate.onRequestError(new Exception("Search task rejected. The service may be unavailable.")));
        }
    }

    public interface AsyncResponse {
        void onRequestFinished(List<WeatherEntry> entries);
        void onRequestError(Exception exception);
    }

    public static void shutdownExecutor() {
        Log.i(TAG, "Shutting down ExecutorService...");
        executorService.shutdown();
    }
}
