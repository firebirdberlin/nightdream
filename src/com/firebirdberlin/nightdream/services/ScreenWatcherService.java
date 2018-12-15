package com.firebirdberlin.nightdream.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.receivers.ChargingStateChangeReceiver;
import com.firebirdberlin.nightdream.receivers.PowerConnectionReceiver;
import com.firebirdberlin.nightdream.receivers.ScreenReceiver;
import com.firebirdberlin.nightdream.receivers.StopServiceReceiver;


public class ScreenWatcherService extends Service {
    private static String TAG = "ScreenWatcherService";

    public static boolean isRunning = false;
    private ScreenReceiver mReceiver;
    private PowerConnectionReceiver powerConnectionReceiver;
    private ChargingStateChangeReceiver chargingStateChangeReceiver;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()");
        isRunning = true;
        Intent stopIntent = new Intent(getApplicationContext(), StopServiceReceiver.class);
        stopIntent.setAction("ACTION_STOP_SERVICE");
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, 0);
                PendingIntent.getBroadcast(this, 0, stopIntent, 0);

        NotificationCompat.Builder noteBuilder =
                Utility.buildNotification(this, Config.NOTIFICATION_CHANNEL_ID_SERVICES)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.backgroundServiceNotificationText))
                        .setSmallIcon(R.drawable.ic_expert)
                        .setPriority(NotificationCompat.PRIORITY_MIN)
                        .addAction(0, getString(R.string.action_stop), stopPendingIntent);

        Notification note = noteBuilder.build();

        note.flags |= Notification.FLAG_NO_CLEAR;
        note.flags |= Notification.FLAG_FOREGROUND_SERVICE;

        startForeground(Config.NOTIFICATION_ID_FOREGROUND_SERVICES, note);

        ChargingStateChangeReceiver.getAndSaveBatteryReference(this);

        mReceiver = ScreenReceiver.register(this);
        powerConnectionReceiver = PowerConnectionReceiver.register(this);
        chargingStateChangeReceiver = ChargingStateChangeReceiver.register(this);
    }

    private void removeBatteryReference() {
        Settings settings = new Settings(this);
        settings.removeBatteryReference();
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
        ChargingStateChangeReceiver.unregister(this, chargingStateChangeReceiver);
        removeBatteryReference();
    }


    public static void conditionallyStart(Context context, Settings settings) {
        if (settings.handle_power || settings.standbyEnabledWhileDisconnected) {
            ScreenWatcherService.start(context);
        }
    }

    public static void start(Context context) {
        if (ScreenWatcherService.isRunning) return;
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
