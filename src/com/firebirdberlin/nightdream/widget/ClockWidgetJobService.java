package com.firebirdberlin.nightdream.widget;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.firebirdberlin.nightdream.Utility;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ClockWidgetJobService extends JobService {

    private final static String TAG = "ClockWidgetJobService";
    private static long lastExecutionTime = 0;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void schedule(Context context) {

        ComponentName serviceComponent = new ComponentName(
                context.getPackageName(), ClockWidgetJobService.class.getName());
        JobInfo.Builder builder =
                new JobInfo.Builder(0, serviceComponent)
                        .setPersisted(true)
                        .setRequiresCharging(false)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setPeriodic(60000);

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            return;
        }

        int jobResult = jobScheduler.schedule(builder.build());

        if (jobResult == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "scheduled ClockWidgetJobService job successfully");
        }
    }


    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "onStartJob");
        if ( Utility.isScreenOn(this)) {
            long now = System.currentTimeMillis();
            Log.d(TAG, String.valueOf(now - lastExecutionTime));
            if (now - lastExecutionTime > 59000) {
                // on Android M the job executes multiple times when triggered
                // don't do update too often.
                lastExecutionTime = System.currentTimeMillis();
                Log.d(TAG, " ... screen is on :)");

                ClockWidgetProvider.updateAllWidgets(this);
            }
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
