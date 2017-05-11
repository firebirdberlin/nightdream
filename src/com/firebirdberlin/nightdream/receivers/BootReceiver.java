package com.firebirdberlin.nightdream.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.firebirdberlin.nightdream.PowerConnectionReceiver;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.services.ScreenWatcherService;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "BootReceiver: " + intent.getAction());
        PowerConnectionReceiver.schedule(context);
        WakeUpReceiver.schedule(context);

        Settings settings = new Settings(context);
        if (settings.standbyEnabledWhileConnected || settings.standbyEnabledWhileDisconnected) {
            ScreenWatcherService.start(context);
        }
    }
}
