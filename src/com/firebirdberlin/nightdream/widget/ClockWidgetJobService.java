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

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "onStartJob");
        if (Utility.isScreenOn(this)) {
            ClockWidgetProvider.updateAllWidgets(this);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
