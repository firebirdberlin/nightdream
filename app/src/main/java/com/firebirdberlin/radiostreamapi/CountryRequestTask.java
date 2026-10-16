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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.firebirdberlin.radiostreamapi.models.Country;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CountryRequestTask {

    public interface AsyncResponse {
        void onCountryRequestFinished(List<Country> countries);
    }

    private final AsyncResponse delegate;
    private final Context context;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean cancelled = false;

    public CountryRequestTask(AsyncResponse listener, Context context) {
        this.delegate = listener;
        this.context = context;
    }

    public void execute(Void... params) {
        executor.execute(() -> {
            if (cancelled) return;
            List<Country> countries = RadioBrowserApi.fetchCountries(context.getCacheDir());
            mainHandler.post(() -> {
                if (!cancelled && delegate != null) {
                    delegate.onCountryRequestFinished(countries);
                }
            });
        });
    }

    public void cancel(boolean mayInterruptIfRunning) {
        this.cancelled = true;
    }
}
