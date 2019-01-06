package com.firebirdberlin.nightdream.receivers;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.DataSource;
import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.SetAlarmClockActivity;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.services.AlarmHandlerService;
import com.firebirdberlin.nightdream.services.AlarmNotificationService;
import com.firebirdberlin.nightdream.services.AlarmWifiService;

import java.text.SimpleDateFormat;
import java.util.Date;

public class WakeUpReceiver extends BroadcastReceiver {
    private final static String TAG = "WakeUpReceiver";

    public static void schedule(Context context) {
        DataSource db = new DataSource(context);
        db.open();
        schedule(context, db);
        db.close();
    }

    public static void schedule(Context context, DataSource db) {
        SimpleTime next = db.getNextAlarmToSchedule();
        if (next != null) {
            setAlarm(context, next);
            next = db.setNextAlarm(next);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AlarmNotificationService.scheduleJob(context, next);
                AlarmWifiService.scheduleJob(context, next);
            }
        } else {
            PendingIntent pI = WakeUpReceiver.getPendingIntent(context, null, 0);
            AlarmManager am = (AlarmManager) (context.getSystemService(Context.ALARM_SERVICE));
            am.cancel(pI);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AlarmNotificationService.cancelJob(context);
                AlarmWifiService.cancelJob(context);
            }
        }

        Intent intent = new Intent(Config.ACTION_ALARM_SET);
        if (next != null) {
            intent.putExtras(next.toBundle());
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

    }

    public static void broadcastNextAlarm(Context context) {
        DataSource db = new DataSource(context);
        db.open();
        SimpleTime next = db.getNextAlarmEntry();
        db.close();

        Intent intent = new Intent(Config.ACTION_ALARM_SET);
        if (next != null) {
            Log.w(TAG, next.toString());
            intent.putExtras(next.toBundle());
        } else {
            Log.w(TAG, "no next alarm");
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void cancelAlarm(Context context) {
        PendingIntent pI = WakeUpReceiver.getPendingIntent(context, null, 0);
        AlarmManager am = (AlarmManager) (context.getSystemService(Context.ALARM_SERVICE));
        am.cancel(pI);


        DataSource db = new DataSource(context);
        db.open();
        db.cancelPendingAlarms();
        db.close();
    }

    public static PendingIntent getPendingIntent(Context context, SimpleTime alarmTime, int flags) {
        Intent intent = new Intent("com.firebirdberlin.nightdream.WAKEUP");
        intent.setClass(context, WakeUpReceiver.class);
        if (alarmTime != null) {
            intent.putExtras(alarmTime.toBundle());
        }
        Utility.logIntent(TAG, "getPendingIntent", intent);
        //return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        // PendingIntent.FLAG_CANCEL_CURRENT seems to confuse AlarmManager.cancel() on certain
        // Android devices, e.g. HTC One m7, i.e. AlarmManager.getNextAlarmClock() still returns
        // already cancelled alarm times afterwards.
        return PendingIntent.getBroadcast(context, 0, intent, flags);
    }

    private static PendingIntent getShowIntent(Context context) {
        Intent intent = new Intent(context, SetAlarmClockActivity.class);
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    private static void setAlarm(Context context, SimpleTime nextAlarmEntry) {
        PendingIntent pI = WakeUpReceiver.getPendingIntent(context, nextAlarmEntry, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pI);

        pI = WakeUpReceiver.getPendingIntent(context, nextAlarmEntry, PendingIntent.FLAG_CANCEL_CURRENT);
        long nextAlarmTime = nextAlarmEntry.getMillis();
        if (Build.VERSION.SDK_INT >= 21) {
            PendingIntent pi = getShowIntent(context);
            AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(nextAlarmTime, pi);
            am.setAlarmClock(info, pI);
        } else if (Build.VERSION.SDK_INT >= 19) {
            am.setExact(AlarmManager.RTC_WAKEUP, nextAlarmTime, pI);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, nextAlarmTime, pI);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Utility.logIntent(TAG, "onReceive()", intent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmNotificationService.cancelNotification(context);
            AlarmNotificationService.cancelJob(context);
            AlarmWifiService.cancelJob(context);
        }
        AlarmHandlerService.start(context, intent);

        buildNotification(context);
        NightDreamActivity.start(context);
    }

    private Notification buildNotification(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH ) {
            return null;
        }
        Settings settings = new Settings(context);
        String text = dateAsString(settings.getTimeFormat());
        String textActionSnooze = context.getString(R.string.action_snooze);
        String textActionStop = context.getString(R.string.action_stop);

        NotificationCompat.Builder note =
            Utility.buildNotification(context, Config.NOTIFICATION_CHANNEL_ID_ALARMS)
                .setAutoCancel(true)
                .setContentTitle(context.getString(R.string.alarm))
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_audio)
                .setPriority(NotificationCompat.PRIORITY_MAX);

        NotificationCompat.WearableExtender wearableExtender =
            new NotificationCompat.WearableExtender().setHintHideIcon(true);

        Intent stopIntent = AlarmHandlerService.getStopIntent(context);
        PendingIntent pStopIntent = PendingIntent.getService(
                context, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action stopAction =
            new NotificationCompat.Action.Builder(0, textActionStop, pStopIntent).build();

        Intent snoozeIntent = AlarmHandlerService.getSnoozeIntent(context);
        PendingIntent pSnoozeIntent = PendingIntent.getService(
                context, 0, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action snoozeAction =
            new NotificationCompat.Action.Builder(0, textActionSnooze, pSnoozeIntent).build();

        NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
        bigStyle.bigText(text);

        note.setStyle(bigStyle);
        note.addAction(stopAction);
        note.addAction(snoozeAction);
        wearableExtender.addAction(stopAction);
        wearableExtender.addAction(snoozeAction);


        note.extend(wearableExtender);

        Notification notification = note.build();
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Config.NOTIFICATION_ID_DISMISS_ALARMS, notification);

        return notification;
    }

    private String dateAsString(String format) {
        Date date = new Date();
        return dateAsString(format, date);
    }

    private String dateAsString(String format, Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }
}
