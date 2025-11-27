package com.firebirdberlin.nightdream.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.services.CheckChargingStateJob;
import com.firebirdberlin.nightdream.services.ScreenWatcherService;
import com.firebirdberlin.nightdream.services.SqliteIntentService;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "BootReceiver: " + intent.getAction());
        Utility.createNotificationChannels(context);
        PowerConnectionReceiver.schedule(context);
        ScheduledAutoStartReceiver.schedule(context);
        CheckChargingStateJob.schedule(context);

        SqliteIntentService.scheduleAlarm(context);

        Settings settings = new Settings(context);
        ScreenWatcherService.conditionallyStart(context, settings);
    }
}
