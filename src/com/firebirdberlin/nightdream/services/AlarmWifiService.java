package com.firebirdberlin.nightdream.services;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.models.SimpleTime;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class AlarmWifiService extends JobService {
    private static String TAG = "AlarmWifiService";

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void scheduleJob(Context context, SimpleTime nextAlarmTime) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        Settings settings = new Settings(context);
        if (!settings.radioStreamActivateWiFi || nextAlarmTime.radioStationIndex < 0) {
            return;
        }

        ComponentName serviceComponent = new ComponentName(
                context.getPackageName(), AlarmWifiService.class.getName());
        JobInfo.Builder builder = new JobInfo.Builder(Config.JOB_ID_ALARM_WIFI, serviceComponent);
        builder.setPersisted(true);
        builder.setRequiresCharging(false);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE);

        long nowInMillis = System.currentTimeMillis();
        long nextAlarmTimeMillis = nextAlarmTime.getMillis();

        long minutes_to_start = 5;
        long minLatency = 1000;
        if (nextAlarmTimeMillis - nowInMillis > minutes_to_start * 60000) {
            minLatency = (nextAlarmTimeMillis - minutes_to_start * 60000) - nowInMillis - 30000;
        }
        builder.setMinimumLatency(minLatency);
        builder.setOverrideDeadline(minLatency + 30000);


        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            return;
        }

        int jobResult = jobScheduler.schedule(builder.build());

        if (jobResult == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "scheduled AlarmWifiService job successfully");
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void cancelJob(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            return;
        }
        jobScheduler.cancel(Config.JOB_ID_ALARM_WIFI);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "onStartJob");
        Settings settings = new Settings(this);
        if (settings.radioStreamActivateWiFi) {
            WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
            try {
                boolean success = wifiManager.setWifiEnabled(true);
                Log.i(TAG, "Wifi is " + (success ? "" : "not ") + "activated.");
            } catch (SecurityException e) {
                // enabling WiFi is not allowed right now
                // Some devices throw a SecurityException if Airplane mode is active. Others just
                // return false.
                e.printStackTrace();
            }
        }

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

}
