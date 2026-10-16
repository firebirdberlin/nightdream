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

package com.firebirdberlin.radiostreamapi;

import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.firebirdberlin.radiostreamapi.models.RadioStation;

public class StationRequestTask {

    public interface AsyncResponse {
        void onRequestFinished(List<RadioStation> stations);
    }

    private final AsyncResponse delegate;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean cancelled = false;

    public StationRequestTask(AsyncResponse listener) {
        this.delegate = listener;
    }

    public void execute(String... query) {
        if (query == null || query.length == 0) {
            return;
        }
        String q = query[0];
        String countryCode = query.length > 1 ? query[1] : null;

        executor.execute(() -> {
            if (cancelled) return;
            List<RadioStation> stations = RadioBrowserApi.fetchStations(q, countryCode);
            mainHandler.post(() -> {
                if (!cancelled && delegate != null) {
                    delegate.onRequestFinished(stations);
                }
            });
        });
    }

    public void cancel(boolean mayInterruptIfRunning) {
        this.cancelled = true;
    }
}
