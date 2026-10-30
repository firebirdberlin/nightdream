/*
 * NightDream
 * Copyright (C) 2026 Stefan Fruhner
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

package com.firebirdberlin.nightdream.airquality;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class AirQualityRequestTask {

    private static final String TAG = AirQualityRequestTask.class.getSimpleName();
    private static final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private final AsyncResponse delegate;
    private final Context context;

    public interface AsyncResponse {
        void onRequestFinished(@Nullable AirQualityData result);
        void onRequestError(@NonNull Exception exception);
    }

    public AirQualityRequestTask(@NonNull AsyncResponse listener, @NonNull Context mContext) {
        this.delegate = listener;
        this.context = mContext.getApplicationContext();
    }

    public void execute(final double latitude, final double longitude) {
        try {
            executorService.execute(() -> {
                try {
                    AirQualityRepository repo = new AirQualityRepository(context);
                    final AirQualityData result = repo.getAirQualityForLocation(latitude, longitude);
                    mainThreadHandler.post(() -> delegate.onRequestFinished(result));
                } catch (final Exception e) {
                    Log.e(TAG, "Error fetching air quality data", e);
                    mainThreadHandler.post(() -> delegate.onRequestError(e));
                }
            });
        } catch (RejectedExecutionException e) {
            Log.e(TAG, "Task rejected", e);
            final Exception exc = new Exception("Air quality task rejected.");
            mainThreadHandler.post(() -> delegate.onRequestError(exc));
        }
    }
}
