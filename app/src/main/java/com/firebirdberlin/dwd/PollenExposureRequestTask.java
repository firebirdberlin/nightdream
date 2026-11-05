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

import com.firebirdberlin.nightdream.util.HttpReader;
import com.firebirdberlin.nightdream.PollenExposure;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.openweathermapapi.GeocoderApi;
import com.firebirdberlin.openweathermapapi.models.City;

public class PollenExposureRequestTask {

    private static final String TAG = PollenExposureRequestTask.class.getSimpleName();
    private static final String URL =
            "https://opendata.dwd.de/climate_environment/health/alerts/s31fg.json";

    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    // HttpReader is not thread safe and all requests share one cache file.
    private static final Object CACHE_LOCK = new Object();

    private final AsyncResponse delegate;
    private final Context context;

    public PollenExposureRequestTask(AsyncResponse listener, Context mContext) {
        this.delegate = listener;
        this.context = mContext.getApplicationContext();
    }

    public void execute(double lat, double lon) {
        try {
            executorService.execute(() -> {
                City city = GeocoderApi.findCityByCoordinates(context, lat, lon);
                if (city == null || city.countryCode == null || !city.isGermany()) {
                    Log.w(TAG, "no pollen data: geocoder could not resolve city or countryCode is unknown / not Germany");
                    mainThreadHandler.post(() -> delegate.onRequestFinished(null));
                    return;
                }

                String postalCode = city.postalCode;
                if (Utility.isEmpty(postalCode)) {
                    Log.w(TAG, "no postal code found for city: " + city.toJson());
                    mainThreadHandler.post(() -> delegate.onRequestFinished(null));
                    return;
                }

                Log.i(TAG, "requesting pollen data for postalCode=" + postalCode + " (lat=" + lat + ", lon=" + lon + ")");
                final PollenExposure pollen = new PollenExposure();
                pollen.setPostCode(postalCode);

                try {
                    load(pollen, postalCode);
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching or parsing pollen data", e);
                    mainThreadHandler.post(() -> delegate.onRequestError(e));
                    return;
                }

                Log.i(TAG, pollen.getDebugSummary());
                mainThreadHandler.post(() -> delegate.onRequestFinished(pollen));
            });
        } catch (RejectedExecutionException e) {
            Log.e(TAG, "Task rejected: Executor service may be shutting down or overloaded.", e);
            mainThreadHandler.post(() -> delegate.onRequestError(
                    new Exception("Search task rejected. The service may be unavailable.")));
        }
    }

    /**
     * Note on HttpReader semantics:
     * - readUrl(url, false) returns the cache if it was fetched less than 24h ago,
     *   otherwise it tries the network. It returns "" (not null) on any failure.
     * - readUrl(url, true) skips the cache, but is blocked for 10 minutes after ANY
     *   network attempt (HttpReader's lock file). That is our refresh throttle.
     * Strategy:
     * 1. use the cache; if it is unusable (expired while offline, network locked), fall back
     *    to the old cache file. PollenExposure rejects data that is more than 2 days old.
     * 2. refresh if the file is unusable or DWD should have published a newer one.
     * 3. if the refresh fails, keep what we have.
     */
    private void load(PollenExposure pollen, String postalCode) {
        HttpReader httpReader = new HttpReader(context, "pollen.json");

        synchronized (CACHE_LOCK) {
            String data = httpReader.readUrl(URL, false);
            if (data == null || data.isEmpty()) {
                data = httpReader.readCacheIgnoringExpiry(URL);
            }
            pollen.parse(data, postalCode);

            long nextUpdate = pollen.getNextUpdate();
            boolean refreshNeeded = nextUpdate == -1L || nextUpdate < System.currentTimeMillis();
            if (refreshNeeded) {
                String fresh = httpReader.readUrl(URL, true);
                if (fresh != null && !fresh.isEmpty()) {
                    // a broken response leaves the previous state untouched
                    pollen.parse(fresh, postalCode);
                } else {
                    Log.i(TAG, "no fresh data (offline, locked or failed), keeping cached data");
                }
            }
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
