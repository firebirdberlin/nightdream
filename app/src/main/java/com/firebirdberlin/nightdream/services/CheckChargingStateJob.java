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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.firebirdberlin.nightdream.models.BatteryValue;
import com.firebirdberlin.nightdream.repositories.BatteryStats;
import com.firebirdberlin.nightdream.viewmodels.BatteryReferenceViewModel;

import java.util.concurrent.TimeUnit;

public class CheckChargingStateJob extends Worker {
    private static final String TAG = "CheckChargingStateJob";

    public static void schedule(Context context) {
        Log.d(TAG, "Scheduling " + TAG);
        cancel(context);
        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(
                        CheckChargingStateJob.class,
                        15, TimeUnit.MINUTES,
                        30, TimeUnit.MINUTES
                ).addTag(TAG).build();

        WorkManager.getInstance(context).enqueue(request);
    }

   public static void cancel(Context context) {
       WorkManager.getInstance(context).cancelAllWorkByTag(TAG);
   }

    public CheckChargingStateJob(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork()");
        Context context = getApplicationContext();
        BatteryValue current = new BatteryStats(context).reference;
        BatteryReferenceViewModel.updateIfNecessary(context, current);
        return Result.success();
    }

}