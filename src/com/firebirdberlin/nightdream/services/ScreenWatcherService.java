package com.firebirdberlin.nightdream.services;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.receivers.PowerConnectionReceiver;
import com.firebirdberlin.nightdream.receivers.ScreenReceiver;


public class ScreenWatcherService extends Service {
    private static String TAG = "ScreenWatcherService";

    public static boolean isRunning = false;
    private ScreenReceiver mReceiver;
    private PowerConnectionReceiver powerConnectionReceiver;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()");
        isRunning = true;
        Notification note = Utility.getForegroundServiceNotification(
                this, R.string.backgroundServiceNotificationText);
        startForeground(Config.NOTIFICATION_ID_FOREGROUND_SERVICES, note);

        mReceiver = ScreenReceiver.register(this);
        powerConnectionReceiver = PowerConnectionReceiver.register(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void onDestroy() {
        Log.i(TAG, "ScreenWatcherService destroyed.");
        isRunning = false;
        ScreenReceiver.unregister(this, mReceiver);
        PowerConnectionReceiver.unregister(this, powerConnectionReceiver);
    }


    public static void conditionallyStart(Context context, Settings settings) {
        if ((settings.handle_power && settings.standbyEnabledWhileConnected) ||
                settings.standbyEnabledWhileDisconnected) {
            ScreenWatcherService.start(context);
        }
    }

    public static void start(Context context) {
        Intent i = new Intent(context, ScreenWatcherService.class);
        Utility.startForegroundService(context, i);
    }

    public static void stop(Context context) {
        if ( !ScreenWatcherService.isRunning) {
            return;
        }
        Intent i = new Intent(context, ScreenWatcherService.class);
        context.stopService(i);
    }
}
