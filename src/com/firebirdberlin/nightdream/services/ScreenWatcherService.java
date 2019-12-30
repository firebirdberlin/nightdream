package com.firebirdberlin.nightdream.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.util.Log;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.receivers.ChargingStateChangeReceiver;
import com.firebirdberlin.nightdream.receivers.PowerConnectionReceiver;
import com.firebirdberlin.nightdream.receivers.ScreenReceiver;
import com.firebirdberlin.nightdream.receivers.StopServiceReceiver;
import com.firebirdberlin.nightdream.widget.ClockWidgetProvider;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;


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

        Notification note = buildDefaultNotification(this);

        note.flags |= Notification.FLAG_NO_CLEAR;
        note.flags |= Notification.FLAG_FOREGROUND_SERVICE;

        startForeground(Config.NOTIFICATION_ID_FOREGROUND_SERVICES, note);

        ChargingStateChangeReceiver.getAndSaveBatteryReference(this);

        mReceiver = ScreenReceiver.register(this);
        powerConnectionReceiver = PowerConnectionReceiver.register(this);
        chargingStateChangeReceiver = ChargingStateChangeReceiver.register(this);
    }

    public static void updateNotification(Context context, WeatherEntry weatherEntry, int temperatureUnit) {
        if (ScreenWatcherService.isRunning) {
            if (weatherEntry != null ) {
                String title = String.format(
                        "%s | %s",
                        weatherEntry.formatTemperatureText(temperatureUnit),
                        weatherEntry.cityName
                );
                String desc = weatherEntry.description;
                ScreenWatcherService.updateNotification(context, title, desc,
                        weatherEntry.timestamp * 1000, R.drawable.ic_cloud);
            } else {
                Notification note = buildDefaultNotification(context);
                updateNotification(context, note);
            }
        }
    }

    private static Notification buildDefaultNotification(Context context) {
        String title = context.getString(R.string.app_name);
        String text = context.getString(R.string.backgroundServiceNotificationText);
        long now = System.currentTimeMillis();
        return buildNotification(context, title, text, now, R.drawable.ic_expert);
    }

    public static void updateNotification(Context context, String title, String text,
                                          long when, int iconId) {
        Notification note = buildNotification(context, title, text, when, iconId);
        updateNotification(context, note);
    }

    public static void updateNotification(Context context, Notification notification) {
        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(context);
        mNotificationManager.notify(Config.NOTIFICATION_ID_FOREGROUND_SERVICES, notification);
    }

    private static Notification buildNotification(Context context, String title, String text,
                                                  long when, int iconID) {
        Intent stopIntent = new Intent(context, StopServiceReceiver.class);
        stopIntent.setAction("ACTION_STOP_SERVICE");
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(context, 0, stopIntent, 0);
        PendingIntent.getBroadcast(context, 0, stopIntent, 0);

        NotificationCompat.Builder noteBuilder =
                Utility.buildNotification(context, Config.NOTIFICATION_CHANNEL_ID_SERVICES)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setSmallIcon(iconID)
                        .setPriority(NotificationCompat.PRIORITY_MIN)
                        .setWhen(when)
                        .addAction(0, context.getString(R.string.action_stop), stopPendingIntent);

        return noteBuilder.build();
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
        if (settings.handle_power
                || settings.standbyEnabledWhileDisconnected
                || ClockWidgetProvider.hasWidgets(context)) {
            ScreenWatcherService.start(context);
        } else {
            ScreenWatcherService.stop(context);
        }
    }

    public static void conditionallyStart(Context context) {
        Settings settings = new Settings(context);
        conditionallyStart(context, settings);
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
