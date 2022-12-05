package com.firebirdberlin.nightdream.services;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.events.OnAlarmStarted;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.receivers.WakeUpReceiver;

import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;


public class AlarmHandlerService extends BroadcastReceiver {
    private static final String TAG = "AlarmHandlerService";
    private static final String ACTION_SKIP_ALARM = "com.firebirdberlin.nightdream.ACTION_SKIP_ALARM";
    private static final String ACTION_STOP_ALARM = "com.firebirdberlin.nightdream.ACTION_STOP_ALARM";
    private static final String ACTION_SNOOZE_ALARM = "com.firebirdberlin.nightdream.ACTION_SNOOZE_ALARM";


    public static boolean alarmIsRunning() {
        return (AlarmService.isRunning
                || RadioStreamService.streamingMode == RadioStreamService.StreamingMode.ALARM);
    }

    public static void start(Context context) {
        Settings settings = new Settings(context);
        boolean hasNetworkConnection = settings.radioStreamRequireWiFi ?
                Utility.hasFastNetworkConnection(context) : Utility.hasNetworkConnection(context);

        SimpleTime alarmTime = getCurrentlyActiveAlarm();
        int radioStationIndex = (alarmTime != null) ? alarmTime.radioStationIndex : -1;
        Log.d(TAG, "settings.radioStreamRequireWiFi: " + settings.radioStreamRequireWiFi);
        Log.d(TAG, "hasNetworkConnection: " + hasNetworkConnection);
        Log.d(TAG, "radioStationIndex: " + radioStationIndex);
        if (radioStationIndex > -1 && hasNetworkConnection) {

            RadioStreamService.start(context, alarmTime);
        } else {
            if (RadioStreamService.streamingMode != RadioStreamService.StreamingMode.INACTIVE) {
                RadioStreamService.stop(context);
            }
            AlarmService.startAlarm(context, alarmTime);
        }

    }

    public static void stop(Context context) {
        AlarmHandlerService.stopAlarm(context);
    }

    public static void snooze(final Context context) {
        AlarmHandlerService.snoozeAlarm(context);
    }

    public static void autoSnooze(final Context context) {
        SimpleTime currentAlarm = getCurrentlyActiveAlarm();
        if (currentAlarm != null
                && currentAlarm.numAutoSnoozeCycles == Settings.getAutoSnoozeCycles(context)) {
            stopAlarm(context);
        } else {
            snoozeAlarm(context, true);
        }
    }

    public static void cancel(Context context) {
        WakeUpReceiver.cancelAlarm(context);
    }

    public static void set(Context context, SimpleTime time) {
        SqliteIntentService.saveTime(context, time);
    }

    public static Intent getStopIntent(Context context) {
        Intent i = new Intent(context, AlarmHandlerService.class);
        i.setAction(ACTION_STOP_ALARM);
        return i;
    }

    public static Intent getSnoozeIntent(Context context) {
        Intent i = new Intent(context, AlarmHandlerService.class);
        i.setAction(ACTION_SNOOZE_ALARM);
        return i;
    }

    public static Intent getSkipIntent(Context context, SimpleTime next) {
        Intent i = new Intent(context, AlarmHandlerService.class);
        i.putExtras(next.toBundle());
        i.setAction(AlarmHandlerService.ACTION_SKIP_ALARM);
        return i;
    }

    public static SimpleTime getCurrentlyActiveAlarm() {
        EventBus bus = EventBus.getDefault();
        OnAlarmStarted event = bus.getStickyEvent(OnAlarmStarted.class);
        return (event != null) ? event.entry : null;
    }

    private static void stopAlarm(Context context) {
        stopAlarm(context, true);
    }

    private static void stopAlarm(Context context, boolean reschedule) {
        boolean isRunning = alarmIsRunning();
        if (AlarmService.isRunning) {
            AlarmService.stop(context);
        }

        if (RadioStreamService.streamingMode == RadioStreamService.StreamingMode.ALARM) {
            RadioStreamService.stop(context);
        }

        AlarmHandlerService.cancel(context);

        if (isRunning) {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(Config.NOTIFICATION_ID_DISMISS_ALARMS);
        } else {
            Intent intent = new Intent(Config.ACTION_ALARM_DELETED);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmNotificationService.cancelNotification(context);
        }

        if (reschedule) {
            SqliteIntentService.scheduleAlarm(context);
        }
    }

    public static void skipAlarm(Context context, SimpleTime time) {
        SqliteIntentService.skipAlarm(context, time);
    }

    public static void snoozeAlarm(Context context) {
        snoozeAlarm(context, false);
    }

    public static void snoozeAlarm(Context context, boolean isAutoSnooze) {
        SimpleTime currentAlarm = getCurrentlyActiveAlarm();
        stopAlarm(context, false);

        Calendar now = Calendar.getInstance();
        SimpleTime time = new SimpleTime(
                now.getTimeInMillis() + Settings.getSnoozeTimeMillis(context)
        );
        time.isActive = true;
        time.soundUri = (currentAlarm != null) ? currentAlarm.soundUri : null;
        time.radioStationIndex = (currentAlarm != null) ? currentAlarm.radioStationIndex : -1;
        time.vibrate = currentAlarm != null && currentAlarm.vibrate;
        time.numAutoSnoozeCycles = (currentAlarm != null && isAutoSnooze)
                ? currentAlarm.numAutoSnoozeCycles + 1
                : 0;
        SqliteIntentService.snooze(context, time);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, TAG + " started");
        String action = intent.getAction();

        if (ACTION_STOP_ALARM.equals(action)) {
            stopAlarm(context);
        } else if (ACTION_SKIP_ALARM.equals(action)) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                SimpleTime time = new SimpleTime(bundle);
                SqliteIntentService.skipAlarm(context, time);
            }
        } else if (ACTION_SNOOZE_ALARM.equals(action)) {
            snoozeAlarm(context);
        }
    }
}
