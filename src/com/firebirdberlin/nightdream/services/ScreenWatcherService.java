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
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.receivers.ChargingStateChangeReceiver;
import com.firebirdberlin.nightdream.receivers.PowerConnectionReceiver;
import com.firebirdberlin.nightdream.receivers.ScreenReceiver;


public class ScreenWatcherService extends Service {
    private static String TAG = "ScreenWatcherService";

    private ScreenReceiver mReceiver;
    private PowerConnectionReceiver powerConnectionReceiver;
    private ChargingStateChangeReceiver chargingStateChangeReceiver;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()");
        Notification note = Utility.getForegroundServiceNotification(
                this, R.string.backgroundServiceNotificationText);
        startForeground(Config.NOTIFICATION_ID_FOREGROUND_SERVICES, note);

        mReceiver = ScreenReceiver.register(this);
        powerConnectionReceiver = PowerConnectionReceiver.register(this);
        chargingStateChangeReceiver = ChargingStateChangeReceiver.register(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void onDestroy() {
        Log.i(TAG, "ScreenWatcherService destroyed.");
        ScreenReceiver.unregister(this, mReceiver);
        PowerConnectionReceiver.unregister(this, powerConnectionReceiver);
        ChargingStateChangeReceiver.unregister(this, chargingStateChangeReceiver);
    }


    public static void start(Context context) {
        Intent i = new Intent(context, ScreenWatcherService.class);
        Utility.startForegroundService(context, i);
    }

    public static void stop(Context context) {
        Intent i = new Intent(context, ScreenWatcherService.class);
        context.stopService(i);
    }
}
