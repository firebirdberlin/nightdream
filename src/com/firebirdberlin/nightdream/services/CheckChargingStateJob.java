package com.firebirdberlin.nightdream.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Configuration;
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

        if(!isInitialized()){
            WorkManager.initialize(context, new Configuration.Builder().build());
        }

        WorkManager.getInstance(context).enqueue(request);
    }

    /**
     * Provides a way to check if {@link WorkManager} is initialized in this process.
     * https://android-review.googlesource.com/c/platform/frameworks/support/+/1941186
     * ToDo use Workmanager.isInitialized with Ver 2.8.0
     */
    @SuppressWarnings("deprecation")
    private static boolean isInitialized(){
        return WorkManager.getInstance() != null;
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