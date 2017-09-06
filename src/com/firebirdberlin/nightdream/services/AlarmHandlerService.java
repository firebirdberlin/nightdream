package com.firebirdberlin.nightdream.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.receivers.WakeUpReceiver;

import java.util.Calendar;


public class AlarmHandlerService extends IntentService {
    private static String TAG = "AlarmHandlerService";
    private static String ACTION_CANCEL_ALARM = "com.firebirdberlin.nightdream.ACTION_CANCEL_ALARM";
    private static String ACTION_SET_ALARM = "com.firebirdberlin.nightdream.ACTION_SET_ALARM";
    private static String ACTION_STOP_ALARM = "com.firebirdberlin.nightdream.ACTION_STOP_ALARM";
    private static String ACTION_SNOOZE_ALARM = "com.firebirdberlin.nightdream.ACTION_SNOOZE_ALARM";
    private Context context = null;
    private Settings settings;

    public AlarmHandlerService() {
        super("AlarmHandlerService");
    }

    public AlarmHandlerService(String name) {
        super(name);
    }

    public static boolean alarmIsRunning() {
        return (AlarmService.isRunning
                || RadioStreamService.streamingMode == RadioStreamService.StreamingMode.ALARM);
    }

    public static void stop(Context context) {
        Intent i = new Intent(context, AlarmHandlerService.class);
        i.setAction(ACTION_STOP_ALARM);
        context.startService(i);
    }

    public static void snooze(Context context) {
        Intent i = new Intent(context, AlarmHandlerService.class);
        i.setAction(ACTION_SNOOZE_ALARM);
        context.startService(i);
    }

    public static void cancel(Context context) {
        Intent i = new Intent(context, AlarmHandlerService.class);
        i.setAction(ACTION_CANCEL_ALARM);
        context.startService(i);
    }

    public static void set(Context context, int alarmTimeMinutes) {
        Intent i = new Intent(context, AlarmHandlerService.class);
        i.setAction(ACTION_SET_ALARM);
        i.putExtra("alarmTimeMinutes", alarmTimeMinutes);
        context.startService(i);
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

    @Override
    protected void onHandleIntent(Intent intent) {
        context = this;
        settings = new Settings(this);
        Log.d(TAG, TAG + " started");
        String action = intent.getAction();
        if (ACTION_STOP_ALARM.equals(action) ) {
            stopAlarm();
        }
        else
        if (ACTION_SET_ALARM.equals(action) ) {
            int minutes = intent.getIntExtra("alarmTimeMinutes", 0);
            setNewAlarm(minutes);
        }
        else
        if (ACTION_CANCEL_ALARM.equals(action) ) {
            cancelAlarm();
        }
        else
        if (ACTION_SNOOZE_ALARM.equals(action) ) {
            snoozeAlarm();
        }
    }

    public void stopAlarm(){
        boolean isRunning = alarmIsRunning();
        if (AlarmService.isRunning) {
            AlarmService.stop(context);
        }

        if (RadioStreamService.streamingMode == RadioStreamService.StreamingMode.ALARM) {
            RadioStreamService.stop(context);
        }

        cancelAlarm();

        if (isRunning) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(Config.NOTIFICATION_ID_DISMISS_ALARMS);

            Intent intent = new Intent(Config.ACTION_ALARM_STOPPED);
            context.sendBroadcast(intent);
        } else {
            Intent intent = new Intent(Config.ACTION_ALARM_DELETED);
            context.sendBroadcast(intent);
        }

    }

    private void cancelAlarm() {
        settings.setAlarmTime(0);
        WakeUpReceiver.cancelAlarm(context);
    }

    public void snoozeAlarm() {
        stopAlarm();
        Calendar now = Calendar.getInstance();
        long nextAlarmTime = now.getTimeInMillis() + settings.snoozeTimeInMillis;

        int nextAlarmTimeMinutes = new SimpleTime(nextAlarmTime).toMinutes();
        setAlarm(nextAlarmTimeMinutes);
    }

    private void setNewAlarm(int alarmTimeMinutes) {
        cancelAlarm();
        setAlarm(alarmTimeMinutes);
    }

    private void setAlarm(int alarmTimeMinutes) {
        settings.setAlarmTime(alarmTimeMinutes);
        WakeUpReceiver.schedule(context);

        Intent intent = new Intent(Config.ACTION_ALARM_SET);
        intent.putExtra("alarmTime", alarmTimeMinutes);
        context.sendBroadcast(intent);
    }
}
