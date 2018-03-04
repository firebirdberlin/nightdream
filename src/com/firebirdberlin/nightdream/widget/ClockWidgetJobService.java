package com.firebirdberlin.nightdream.widget;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.util.Log;

import com.firebirdberlin.nightdream.Utility;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ClockWidgetJobService extends JobService {

    private final static String TAG = "ClockWidgetJobService";
    private static long lastExecutionTime = 0;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "onStartJob");
        if ( Utility.isScreenOn(this)) {
            long now = System.currentTimeMillis();
            Log.d(TAG, String.valueOf(now - lastExecutionTime));
//            if (now - lastExecutionTime > 59000) {
                // on Android M the job executes multiple times when triggered
                // don't do update too often.
                lastExecutionTime = System.currentTimeMillis();
                Log.d(TAG, " ... screen is on :)");

                ClockWidgetProvider.updateAllWidgets(this);
//            }
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
