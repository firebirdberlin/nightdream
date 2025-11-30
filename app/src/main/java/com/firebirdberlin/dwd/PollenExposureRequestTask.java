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

package com.firebirdberlin.dwd;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import com.firebirdberlin.HttpReader;
import com.firebirdberlin.nightdream.PollenExposure;

public class PollenExposureRequestTask {

    private static final String TAG = "PollenExposureRequestTask";
    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private final AsyncResponse delegate;
    private final Context context;

    public PollenExposureRequestTask(AsyncResponse listener, Context mContext) {
        this.delegate = listener;
        this.context = mContext;
    }

    public void execute(String postalCode) {
        if (postalCode == null || postalCode.isEmpty()) {
            mainThreadHandler.post(() -> delegate.onRequestError(new IllegalArgumentException("Postal code cannot be null or empty.")));
            return;
        }

        try {
            executorService.execute(() -> {
                HttpReader httpReader = new HttpReader(context, "pollen.json");
                String url = "https://opendata.dwd.de/climate_environment/health/alerts/s31fg.json";
                PollenExposure pollen = new PollenExposure();
                pollen.setPostCode(postalCode);

                String result = null;
                try {
                    // read data (either from the url or from the cache)
                    result = httpReader.readUrl(url, false);
                    if (result != null && !result.isEmpty() && postalCode.length() > 2) {
                        pollen.parse(result, postalCode);

                        // check if there's an update pending
                        long nextUpdate = pollen.getNextUpdate();
                        long now = System.currentTimeMillis();
                        if (nextUpdate > -1L && nextUpdate < now) {
                            result = httpReader.readUrl(url, true);
                            if (result != null && !result.isEmpty() && postalCode.length() > 2) {
                                pollen.parse(result, postalCode);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching or parsing pollen data", e);
                    mainThreadHandler.post(() -> delegate.onRequestError(e));
                    return; // Ensure no further execution after an error
                }

                final PollenExposure finalPollen = pollen;
                mainThreadHandler.post(() -> delegate.onRequestFinished(finalPollen));
            });
        } catch (RejectedExecutionException e) {
            Log.e(TAG, "Task rejected: Executor service may be shutting down or overloaded.", e);
            mainThreadHandler.post(() -> delegate.onRequestError(new Exception("Search task rejected. The service may be unavailable.")));
        }
    }

    public interface AsyncResponse {
        void onRequestFinished(PollenExposure result);
        void onRequestError(Exception exception);
    }

    public static void shutdownExecutor() {
        Log.i(TAG, "Shutting down ExecutorService...");
        executorService.shutdown();
    }
}
