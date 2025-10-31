package com.firebirdberlin.nightdream.services;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.work.Configuration;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.models.SimpleTime;

public class AlarmWifiService extends JobService {
    private static final String TAG = "AlarmWifiService";

    public AlarmWifiService() {
        Configuration.Builder builder = new Configuration.Builder();
        builder.setJobSchedulerJobIdRange(0, 1000);
    }

    public static void scheduleJob(Context context, SimpleTime nextAlarmTime) {

        Settings settings = new Settings(context);
        if (!settings.getShallRadioStreamActivateWiFi() || nextAlarmTime.radioStationIndex < 0) {
            return;
        }

        ComponentName serviceComponent = new ComponentName(
                context.getPackageName(), AlarmWifiService.class.getName()
        );
        JobInfo.Builder builder = new JobInfo.Builder(Config.JOB_ID_ALARM_WIFI, serviceComponent);
        builder.setPersisted(true);
        builder.setRequiresCharging(false);

        long nowInMillis = System.currentTimeMillis();
        long nextAlarmTimeMillis = nextAlarmTime.getMillis();

        long fiveMinutesInMillis = 5 * 60 * 1000;
        long timeUntilFiveMinutesBeforeAlarm = nextAlarmTimeMillis - fiveMinutesInMillis - nowInMillis;
        long minLatency = Math.max(0, timeUntilFiveMinutesBeforeAlarm);
        builder.setMinimumLatency(minLatency);
        builder.setOverrideDeadline(minLatency + 30000);

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            return;
        }

        int jobResult = jobScheduler.schedule(builder.build());

        if (jobResult == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "scheduled AlarmWifiService job successfully");
        } else {
            Log.e(TAG, "failed to schedule AlarmWifiService job. Result code: " + jobResult);
        }
    }

    public static void cancelJob(Context context) {
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
        if (settings.getShallRadioStreamActivateWiFi()) {
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
