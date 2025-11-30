/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.firebirdberlin.nightdream.services;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.firebirdberlin.nightdream.DataSource;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.receivers.WakeUpReceiver;
import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.events.OnAlarmStarted;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;

public class SqliteIntentService {

    private static final String TAG = "SqliteIntentService";

    static void saveTime(Context context, SimpleTime time) {
        Log.d(TAG, "save(time)");
        DataSource db = new DataSource(context);
        db.open();
        db.save(time, false);

        WakeUpReceiver.schedule(context, db);
        db.close();
    }

    static void snooze(Context context, SimpleTime time) {
        saveTime(context, time);
    }

    public static void skipAlarm(Context context, SimpleTime time) {
        Log.d(TAG, "skipAlarm(time)");
        if (time == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmNotificationService.cancelNotification(context);
        }

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

    public static void deleteAlarm(Context context, SimpleTime time) {
        Log.d(TAG, "delete(time)");
        if (time != null && !time.isRecurring()) {
            // no alarm is currently active
            DataSource db = new DataSource(context);
            db.open();
            db.deleteOneTimeAlarm(time.id);
            db.close();
        }
    }

    public static void scheduleAlarm(Context context) {
        Log.d(TAG, "schedule()");
        DataSource db = new DataSource(context);
        db.open();
        WakeUpReceiver.schedule(context, db);
        db.close();
    }

    public static void broadcastAlarm(Context context) {
        Log.d(TAG, "broadcastNextAlarm()");
        SimpleTime next = getLastActivatedAlarmTime();
        Intent intent = new Intent(Config.ACTION_ALARM_SET);
        if (next != null) {
            intent.putExtras(next.toBundle());
        } else {
            DataSource db = new DataSource(context);
            db.open();
            SimpleTime nextAlarm = db.getNextAlarmToSchedule();
            if (nextAlarm != null) {
                intent.putExtras(nextAlarm.toBundle());
            }
            db.close();
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    static SimpleTime getLastActivatedAlarmTime() {
        EventBus bus = EventBus.getDefault();
        OnAlarmStarted event = bus.getStickyEvent(OnAlarmStarted.class);
        return (event != null) ? event.entry : null;
    }
}
