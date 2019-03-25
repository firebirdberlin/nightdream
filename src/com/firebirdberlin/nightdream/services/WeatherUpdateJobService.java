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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.receivers.LocationUpdateReceiver;
import com.firebirdberlin.openweathermapapi.OpenWeatherMapApi;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class WeatherUpdateJobService extends JobService {

    private final static String TAG = "WeatherUpdateJobService";
    private NightDreamBroadcastReceiver broadcastReceiver = null;
    private JobParameters params;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void schedule(Context context) {

        Settings settings = new Settings(context);
        if (! settings.showWeather ) return;

        ComponentName serviceComponent = new ComponentName(
                context.getPackageName(), WeatherUpdateJobService.class.getName()
        );
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        builder = builder.setPersisted(true);
        builder = builder.setRequiresCharging(false);
        builder = builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        builder = builder.setPeriodic(30 * 60000);

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            return;
        }

        try {
            int jobResult = jobScheduler.schedule(builder.build());

            if (jobResult == JobScheduler.RESULT_SUCCESS) {
                Log.d(TAG, "scheduled WeatherUpdateJobService job successfully");
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "onStartJob");

        Settings settings = new Settings(this);
        this.params = params;
        Utility.logToFile(
                getApplicationContext(), "weatherUpdates.txt",
               "checking for weather update " + String.valueOf(settings.weatherCityID)
        );
        if (WeatherService.shallUpdateWeatherData(this, settings)) {
            broadcastReceiver = registerBroadcastReceiver();
            settings.setLastWeatherRequestTime(System.currentTimeMillis());
            WeatherService.start(this, settings.weatherCityID);
            return true;
        }

        Log.d(TAG, "nothing to do ...");
        return false;
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
        filter.addAction(OpenWeatherMapApi.ACTION_WEATHER_DATA_UPDATED);
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
            if (LocationUpdateReceiver.ACTION_LOCATION_UPDATED.equals(action)) {
                // we need to reload the location
                Settings settings = new Settings(context);
                DownloadWeatherService.start(context, settings.getLocation());
            } else if (LocationUpdateReceiver.ACTION_LOCATION_FAILURE.equals(action)) {
                unregisterLocalReceiver(broadcastReceiver);
                jobFinished(params, false);
            } else if (OpenWeatherMapApi.ACTION_WEATHER_DATA_UPDATED.equals(action)) {
                unregisterLocalReceiver(broadcastReceiver);
                jobFinished(params, false);
                Log.d(TAG, "weather data updated");
            }
        }
    }

}
