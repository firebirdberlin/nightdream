package com.firebirdberlin.nightdream.services;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.JobIntentService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.DataSource;
import com.firebirdberlin.nightdream.events.OnAlarmStarted;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.receivers.WakeUpReceiver;

import org.greenrobot.eventbus.EventBus;

public class SqliteIntentService extends JobIntentService {

    public static String ACTION_SAVE = "action_save";
    public static String ACTION_SNOOZE = "action_snooze";
    public static String ACTION_SKIP_ALARM = "action_skip_alarm";
    public static String ACTION_SCHEDULE_ALARM = "action_schedule_alarm";
    public static String ACTION_DELETE_ALARM = "action_delete_alarm";
    public static String ACTION_BROADCAST_ALARM = "action_broadcast_alarm";

    static void saveTime(Context context, SimpleTime time) {
        enqueueWork(context, time, ACTION_SAVE);
    }
    static void snooze(Context context, SimpleTime time) {
        enqueueWork(context, time, ACTION_SNOOZE);
    }
    static void skipAlarm(Context context, SimpleTime time) {
        enqueueWork(context, time, ACTION_SKIP_ALARM);
    }

    public static void deleteAlarm(Context context, SimpleTime time) {
        enqueueWork(context, time, ACTION_DELETE_ALARM);
    }

    public static void scheduleAlarm(Context context) {
        enqueueWork(context, ACTION_SCHEDULE_ALARM);
    }

    public static void broadcastAlarm(Context context) {
        enqueueWork(context, ACTION_BROADCAST_ALARM);
    }

    static void enqueueWork(Context context, SimpleTime time, String action) {
        if (time == null) {
            return;
        }
        Intent i = new Intent(context, SqliteIntentService.class);
        i.setAction(action);
        i.putExtras(time.toBundle());
        enqueueWork(context, SqliteIntentService.class, Config.JOB_ID_SQLITE_SERVICE, i);
    }

    static void enqueueWork(Context context, String action) {
        Intent i = new Intent(context, SqliteIntentService.class);
        i.setAction(action);
        enqueueWork(context, SqliteIntentService.class, Config.JOB_ID_SQLITE_SERVICE, i);
    }

    public static Intent getSkipIntent(Context context, SimpleTime next) {
        Intent i = new Intent(context, SqliteIntentService.class);
        i.putExtras(next.toBundle());
        i.setAction(SqliteIntentService.ACTION_SKIP_ALARM);
        return i;
    }


    @Override
    protected void onHandleWork(Intent intent) {
        String action = intent.getAction();
        Bundle bundle = intent.getExtras();
        if (action == null) {
            return;
        }

        Log.i("SqliteIntentService", "onHandleWork action = " + action);
        SimpleTime time = null;
        if (bundle != null) {
            time = new SimpleTime(bundle);
        }

        if (ACTION_SAVE.equals(action)) {
            save(time);
        } else if (ACTION_SNOOZE.equals(action)) {
            save(time);
        } else if (ACTION_SKIP_ALARM.equals(action)) {
            skipAlarm(time);
        } else if (ACTION_DELETE_ALARM.equals(action)) {
            delete(time);
        } else if (ACTION_SCHEDULE_ALARM.equals(action)) {
            schedule();
        } else if (ACTION_BROADCAST_ALARM.equals(action)) {
            broadcastNextAlarm();
        }
    }

    private void save(SimpleTime time) {
        DataSource db = new DataSource(this);
        db.open();
        db.save(time);
        WakeUpReceiver.schedule(this, db);
        db.close();
    }

    private void skipAlarm(SimpleTime time) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                || time == null) {
            return;
        }
        AlarmNotificationService.cancelNotification(this);
        DataSource db = new DataSource(this);
        db.open();

        if (time != null) {
            if (time.isRecurring()) {
                // the next allowed alarm time is after the next alarm.
                db.updateNextEventAfter(time.id, time.getMillis());
            } else {
                db.delete(time);
            }
        }
        WakeUpReceiver.schedule(this, db);
        db.close();
    }

    private void schedule() {
        DataSource db = new DataSource(this);
        db.open();
        WakeUpReceiver.schedule(this, db);
        db.close();
    }

    private void delete(SimpleTime time) {
        if (time != null && !time.isRecurring()) {
            // no alarm is currently active
            DataSource db = new DataSource(this);
            db.open();
            db.deleteOneTimeAlarm(time.id);
            db.close();
        }
    }

    void broadcastNextAlarm() {
        SimpleTime next = getLastActivatedAlarmTime();
        Intent intent = new Intent(Config.ACTION_ALARM_SET);
        if (next != null) {
            intent.putExtras(next.toBundle());
        } else {
            DataSource db = new DataSource(this);
            db.open();
            SimpleTime nextAlarm = db.getNextAlarmToSchedule();
            if (nextAlarm != null) {
                intent.putExtras(nextAlarm.toBundle());
            }
            db.close();
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    SimpleTime getLastActivatedAlarmTime() {
        EventBus bus = EventBus.getDefault();
        OnAlarmStarted event = bus.getStickyEvent(OnAlarmStarted.class);
        return (event != null) ? event.entry : null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
