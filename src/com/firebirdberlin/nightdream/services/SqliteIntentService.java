package com.firebirdberlin.nightdream.services;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.JobIntentService;
import android.widget.Toast;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.DataSource;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.receivers.WakeUpReceiver;

public class SqliteIntentService extends JobIntentService {

    public static String ACTION_SAVE = "action_save";
    public static String ACTION_SNOOZE = "action_snooze";
    public static String ACTION_SKIP_ALARM = "action_skip_alarm";
    public static String ACTION_SCHEDULE_ALARM = "action_schedule_alarm";

    static void saveTime(Context context, SimpleTime time) {
        enqueueWork(context, time, ACTION_SAVE);
    }
    static void snooze(Context context, SimpleTime time) {
        enqueueWork(context, time, ACTION_SNOOZE);
    }
    static void skipAlarm(Context context, SimpleTime time) {
        enqueueWork(context, time, ACTION_SKIP_ALARM);
    }

    public static void scheduleAlarm(Context context) {
        enqueueWork(context, ACTION_SCHEDULE_ALARM);
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
        if (action == null || bundle == null) {
            return;
        }

        SimpleTime time = new SimpleTime(bundle);

        if (ACTION_SAVE.equals(action)) {
            save(time);
        } else if (ACTION_SNOOZE.equals(action)) {
            save(time);
            toast("S N O O Z E");
        } else if (ACTION_SKIP_ALARM.equals(action)) {
            skipAlarm(time);
        } else if (ACTION_SCHEDULE_ALARM.equals(action)) {
            schedule();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    final Handler mHandler = new Handler();

    // Helper for showing tests
    void toast(final CharSequence text) {
        mHandler.post(new Runnable() {
            @Override public void run() {
                Toast.makeText(SqliteIntentService.this, text, Toast.LENGTH_LONG).show();
            }
        });
    }
}
