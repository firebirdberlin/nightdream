package com.firebirdberlin.nightdream.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.firebirdberlin.nightdream.NightModeListener;
import com.firebirdberlin.nightdream.receivers.PowerConnectionReceiver;

import java.util.concurrent.TimeUnit;

public class CheckPowerConnectionJob extends Worker {
    private static final String TAG = "CheckPowerConnectionJob";

    public static void schedule(Context context) {
        Log.d(TAG, "Scheduling " + TAG);
        cancel(context);
        Constraints constraints = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            constraints = new Constraints.Builder()
                    .setRequiresCharging(true)
                    .setRequiresDeviceIdle(true)
                    .build();
        } else {
            constraints = new Constraints.Builder()
                    .setRequiresCharging(true)
                    .build();
        }
        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(
                        CheckPowerConnectionJob.class,
                        15, TimeUnit.MINUTES,
                        5, TimeUnit.MINUTES
                )
                        .addTag(TAG)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context).enqueue(request);
    }

   public static void cancel(Context context) {
       WorkManager.getInstance(context).cancelAllWorkByTag(TAG);
   }

    public CheckPowerConnectionJob(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork()");
        if (NightModeListener.running) {
            return Result.success();
        }
        PowerConnectionReceiver.conditionallyStartApp(getApplicationContext(), null);
        return Result.success();
    }

}