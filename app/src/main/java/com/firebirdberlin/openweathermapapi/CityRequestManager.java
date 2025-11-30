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

package com.firebirdberlin.openweathermapapi; // Or your preferred package

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import com.firebirdberlin.openweathermapapi.OpenWeatherMapApi; // Assuming your API class is here
import com.firebirdberlin.openweathermapapi.models.City; // Your City model

public class CityRequestManager {

    private static final String TAG = "CityRequestManager";
    // Use a fixed thread pool for network operations. The size can be adjusted based on expected load.
    // A pool size of 4 is generally a good starting point for I/O bound tasks.
    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);

    // Handler to post results back to the main thread
    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    /**
     * Interface for receiving the result of the city search.
     */
    public interface AsyncResponse {
        /**
         * Called when the city search is successful.
         * @param cities A list of City objects found. Can be empty if no cities match the query.
         */
        void onRequestFinished(List<City> cities);

        /**
         * Called when an error occurs during the city search.
         * @param exception The exception that caused the error.
         */
        void onRequestError(Exception exception);
    }

    /**
     * Executes the city search asynchronously and invokes the provided callback
     * on the main thread when the operation is complete or an error occurs.
     *
     * @param query            The search query string (e.g., city name).
     * @param responseListener The callback interface to receive the results or errors.
     */
    public static void findCities(Context context, String query, AsyncResponse responseListener) {
        if (query == null || query.trim().isEmpty()) {
            // Immediately call error on main thread for invalid input
            mainThreadHandler.post(() -> responseListener.onRequestError(new IllegalArgumentException("Search query cannot be empty.")));
            return;
        }

        if (responseListener == null) {
            Log.w(TAG, "findCities called with a null responseListener. No callback will be invoked.");
            return;
        }

        try {
            // Submit the task to the thread pool
            executorService.execute(() -> {
                // This code runs on a background thread managed by the executorService
                Log.d(TAG, "Executing findCityApi in background thread: " + Thread.currentThread().getName());
                List<City> cities = null;
                Exception error = null;

                try {
                    //cities = OpenWeatherMapApi.findCityApi(query);
                    cities = GeocoderApi.findCitiesByName(context, query);
                    if (cities == null) {
                        error = new Exception("API returned null for city search.");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error during city search for query: '" + query + "'", e);
                    error = e; // Capture the exception
                }

                final List<City> finalCities = cities;
                final Exception finalError = error;

                mainThreadHandler.post(() -> {
                    if (finalError != null) {
                        responseListener.onRequestError(finalError);
                    } else if (finalCities != null) {
                        responseListener.onRequestFinished(finalCities);
                    } else {
                        responseListener.onRequestError(new Exception("Unknown error: City search resulted in a null list without an exception."));
                    }
                });
            });
        } catch (RejectedExecutionException e) {
            // This exception is thrown if the executor service is shutting down or has rejected the task.
            Log.e(TAG, "Task rejected: Executor service may be shutting down or overloaded.", e);
            // Inform the listener about this specific failure
            mainThreadHandler.post(() -> responseListener.onRequestError(new Exception("Search task rejected. The service may be unavailable.")));
        }
    }

    /**
     * Shuts down the thread pool. This should be called when your application is
     * no longer running or the manager is no longer needed to release resources.
     * Typically called in your Application's `onTerminate()` or `onDestroy()`.
     */
    public static void shutdownExecutor() {
        Log.i(TAG, "Shutting down ExecutorService...");
        executorService.shutdown(); // Initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted.
        // Consider adding executorService.awaitTermination(...) if you need to ensure
        // all tasks complete before exiting.
    }


    // Remember to call shutdownExecutor() when your application exits:
    // In your Application class:
    // @Override
    // public void onTerminate() {
    //     super.onTerminate();
    //     CityRequestManager.shutdownExecutor();
    // }
}
