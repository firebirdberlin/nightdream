package com.firebirdberlin.nightdream.services;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.DataSource;
import com.firebirdberlin.nightdream.events.OnAlarmStarted;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.receivers.WakeUpReceiver;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;

public class SqliteIntentServiceWorker extends Worker {
    private static final String TAG = "SqliteServiceWorker";
    public static String ACTION_SAVE = "action_save";
    public static String ACTION_SNOOZE = "action_snooze";
    public static String ACTION_SKIP_ALARM = "action_skip_alarm";
    public static String ACTION_SCHEDULE_ALARM = "action_schedule_alarm";
    public static String ACTION_DELETE_ALARM = "action_delete_alarm";
    public static String ACTION_BROADCAST_ALARM = "action_broadcast_alarm";

    public SqliteIntentServiceWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork()");

        //Fetch arguments
        String action = getInputData().getString("action");
        Log.d(TAG, "doWork() action: " + action);
        Gson gson = new Gson();
        SimpleTime time = gson.fromJson(
                getInputData().getString("time"), SimpleTime.class
        );
        Log.d(TAG, "doWork() time: " + time);

        if (action == null) {
            return Result.failure();
        }

        if (ACTION_SAVE.equals(action)) {
            save(getApplicationContext(), time);
        } else if (ACTION_SNOOZE.equals(action)) {
            save(getApplicationContext(), time);
        } else if (ACTION_SKIP_ALARM.equals(action)) {
            skipAlarm(getApplicationContext(), time);
        } else if (ACTION_DELETE_ALARM.equals(action)) {
            delete(getApplicationContext(), time);
        } else if (ACTION_SCHEDULE_ALARM.equals(action)) {
            schedule();
        } else if (ACTION_BROADCAST_ALARM.equals(action)) {
            broadcastNextAlarm();
        }

        // Indicate whether the work finished successfully with the Result
        return Result.success();
    }

    protected static void save(Context context, SimpleTime time) {
        Log.d(TAG, "save(time)");
        DataSource db = new DataSource(context);
        db.open();
        db.save(time);
        WakeUpReceiver.schedule(context, db);
        db.close();
    }

    protected static void skipAlarm(Context context, SimpleTime time) {
        Log.d(TAG, "skipAlarm(time)");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                || time == null) {
            return;
        }
        AlarmNotificationService.cancelNotification(context);
        DataSource db = new DataSource(context);
        db.open();

        if (time.isRecurring()) {
            // the next allowed alarm time is after the next alarm.
            db.updateNextEventAfter(time.id, time.getMillis());
        } else {
            db.delete(time);
        }
        WakeUpReceiver.schedule(context, db);
        db.close();
    }

    private void schedule() {
        Log.d(TAG, "schedule()");
        DataSource db = new DataSource(getApplicationContext());
        db.open();
        WakeUpReceiver.schedule(getApplicationContext(), db);
        db.close();
    }

    protected static void delete(Context context, SimpleTime time) {
        Log.d(TAG, "delete(time)");
        if (time != null && !time.isRecurring()) {
            // no alarm is currently active
            DataSource db = new DataSource(context);
            db.open();
            db.deleteOneTimeAlarm(time.id);
            db.close();
        }
    }

    void broadcastNextAlarm() {
        Log.d(TAG, "broadcastNextAlarm()");
        SimpleTime next = getLastActivatedAlarmTime();
        Intent intent = new Intent(Config.ACTION_ALARM_SET);
        if (next != null) {
            intent.putExtras(next.toBundle());
        } else {
            DataSource db = new DataSource(getApplicationContext());
            db.open();
            SimpleTime nextAlarm = db.getNextAlarmToSchedule();
            if (nextAlarm != null) {
                intent.putExtras(nextAlarm.toBundle());
            }
            db.close();
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    SimpleTime getLastActivatedAlarmTime() {
        EventBus bus = EventBus.getDefault();
        OnAlarmStarted event = bus.getStickyEvent(OnAlarmStarted.class);
        return (event != null) ? event.entry : null;
    }
}
