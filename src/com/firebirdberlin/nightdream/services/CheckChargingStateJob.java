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