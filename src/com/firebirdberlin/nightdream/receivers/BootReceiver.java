package com.firebirdberlin.nightdream.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.services.ScreenWatcherService;
import com.firebirdberlin.nightdream.services.SqliteIntentService;
import com.firebirdberlin.nightdream.services.WeatherUpdateJobService;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "BootReceiver: " + intent.getAction());
        Utility.createNotificationChannels(context);
        PowerConnectionReceiver.schedule(context);
        Settings settings = new Settings(context);
        if (settings.useInternalAlarm) {
            SqliteIntentService.scheduleAlarm(context);
        }

        ScreenWatcherService.conditionallyStart(context, settings);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            WeatherUpdateJobService.schedule(context);
        }
    }
}
