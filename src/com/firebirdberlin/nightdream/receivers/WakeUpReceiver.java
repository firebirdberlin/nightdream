package com.firebirdberlin.nightdream.receivers;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.DataSource;
import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.SetAlarmClockActivity;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.events.OnAlarmStarted;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.services.AlarmHandlerService;
import com.firebirdberlin.nightdream.services.AlarmNotificationService;
import com.firebirdberlin.nightdream.services.AlarmWifiService;
import com.firebirdberlin.nightdream.services.SqliteIntentService;
import com.firebirdberlin.nightdream.viewmodels.AlarmClockViewModel;
import com.firebirdberlin.nightdream.widget.AlarmClockWidgetProvider;
import com.firebirdberlin.nightdream.widget.ClockWidgetProvider;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WakeUpReceiver extends BroadcastReceiver {
    private final static String TAG = "WakeUpReceiver";

    public static void schedule(Context context, DataSource db) {
        AlarmNotificationService.cancelNotification(context);
        SimpleTime next = db.getNextAlarmToSchedule();
        if (next != null) {
            setAlarm(context, next);

            AlarmNotificationService.scheduleJob(context, next);
            AlarmWifiService.scheduleJob(context, next);
        } else {
            PendingIntent pI = WakeUpReceiver.getPendingIntent(context, null, 0);
            AlarmManager am = (AlarmManager) (context.getSystemService(Context.ALARM_SERVICE));
            am.cancel(pI);
            AlarmNotificationService.cancelJob(context);
            AlarmWifiService.cancelJob(context);
        }

        Intent intent = new Intent(Config.ACTION_ALARM_SET);
        if (next != null) {
            intent.putExtras(next.toBundle());
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        AlarmClockViewModel.setNextAlarm(next);
        updateWidgets(context);
    }

    private static void updateWidgets(Context context) {
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);

        int[] clockWidgetIds = widgetManager.getAppWidgetIds(
                new ComponentName(context, ClockWidgetProvider.class)
        );
        Intent clockWidgetUpdateIntent = new Intent(
                AppWidgetManager.ACTION_APPWIDGET_UPDATE,
                null, context, ClockWidgetProvider.class
        );
        clockWidgetUpdateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, clockWidgetIds);
        context.sendBroadcast(clockWidgetUpdateIntent);

        int[] appWidgetIds = widgetManager.getAppWidgetIds(
                new ComponentName(context, AlarmClockWidgetProvider.class)
        );
        Intent updateIntent = new Intent(
                AppWidgetManager.ACTION_APPWIDGET_UPDATE,
                null, context, AlarmClockWidgetProvider.class
        );
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.sendBroadcast(updateIntent);
    }

    public static void cancelAlarm(Context context) {
        EventBus bus = EventBus.getDefault();
        bus.removeStickyEvent(OnAlarmStarted.class);

        PendingIntent pI = WakeUpReceiver.getPendingIntent(context, null, 0);
        AlarmManager am = (AlarmManager) (context.getSystemService(Context.ALARM_SERVICE));
        if (am != null) {
            am.cancel(pI);
        }
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
        return Utility.getImmutableBroadcast(context, 0, intent, flags);
    }

    private static PendingIntent getShowIntent(Context context) {
        Intent intent = new Intent(context, SetAlarmClockActivity.class);
        return Utility.getImmutableBroadcast(context, 0, intent);
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
        deleteCurrentlyActiveAlarm(context, intent);
        AlarmHandlerService.start(context);

        buildNotification(context);
    }

    private void deleteCurrentlyActiveAlarm(Context context, Intent intent) {
        Bundle extras = (intent != null) ? intent.getExtras() : null;
        SimpleTime next = (extras != null) ? new SimpleTime(extras) : null;
        EventBus bus = EventBus.getDefault();

        bus.postSticky(new OnAlarmStarted(next));
        SqliteIntentService.deleteAlarm(context, next);
    }

    private Notification buildNotification(Context context) {
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
        PendingIntent pStopIntent = Utility.getImmutableBroadcast(
                context, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        NotificationCompat.Action stopAction = new NotificationCompat.Action.Builder(
                0, textActionStop, pStopIntent
        ).build();

        Intent snoozeIntent = AlarmHandlerService.getSnoozeIntent(context);
        PendingIntent pSnoozeIntent = Utility.getImmutableBroadcast(
                context, 0, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        NotificationCompat.Action snoozeAction = new NotificationCompat.Action.Builder(
                0, textActionSnooze, pSnoozeIntent
        ).build();

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
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(date);
    }
}
