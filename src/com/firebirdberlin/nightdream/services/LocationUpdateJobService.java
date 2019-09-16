package com.firebirdberlin.nightdream.services;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.receivers.LocationUpdateReceiver;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LocationUpdateJobService extends JobService {

    private final static String TAG = "LocationUpdateJob";
    private NightDreamBroadcastReceiver broadcastReceiver = null;
    private JobParameters params;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void schedule(Context context) {

        Settings settings = new Settings(context);
        if (! settings.showWeather ) return;

        ComponentName serviceComponent = new ComponentName(
                context.getPackageName(), LocationUpdateJobService.class.getName()
        );
        JobInfo.Builder builder = new JobInfo.Builder(Config.JOB_ID_UPDATE_LOCATION, serviceComponent);
        builder = builder
                .setPersisted(true)
                .setRequiresCharging(false)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(15 * 60000);

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            return;
        }

        try {
            int jobResult = jobScheduler.schedule(builder.build());

            if (jobResult == JobScheduler.RESULT_SUCCESS) {
                Log.d(TAG, "scheduled LocationUpdateJob successfully");
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "onStartJob");

        this.params = params;
        broadcastReceiver = registerBroadcastReceiver();
        LocationService.start(this);
        // continue running until jobFinished is called
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "onStopJob()");
        unregisterLocalReceiver(broadcastReceiver);
        return false;
    }

    private NightDreamBroadcastReceiver registerBroadcastReceiver() {
        Log.d(TAG, "registerReceiver()");
        NightDreamBroadcastReceiver receiver = new NightDreamBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(LocationUpdateReceiver.ACTION_LOCATION_UPDATED);
        filter.addAction(LocationUpdateReceiver.ACTION_LOCATION_FAILURE);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
        return receiver;
    }

    private void unregisterLocalReceiver(BroadcastReceiver receiver) {
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        } catch ( IllegalArgumentException ignored) {

        }
    }

    public class NightDreamBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive() -> ");
            if (intent == null) return;
            String action = intent.getAction();
            Log.i(TAG, "action -> " + action);
            if (LocationUpdateReceiver.ACTION_LOCATION_UPDATED.equals(action)
                    || LocationUpdateReceiver.ACTION_LOCATION_FAILURE.equals(action)) {
                unregisterLocalReceiver(broadcastReceiver);
                jobFinished(params, false);
            }
        }
    }
}
