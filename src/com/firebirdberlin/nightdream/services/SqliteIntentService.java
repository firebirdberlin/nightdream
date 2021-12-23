package com.firebirdberlin.nightdream.services;

import android.content.Context;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.util.Log;

import com.firebirdberlin.nightdream.models.SimpleTime;
import com.google.gson.Gson;

public class SqliteIntentService {

    private static final String TAG = "SqliteIntentService";
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
        Log.d(TAG, "enqueueWork(Context,SimpleTime, Action)");
        if (time == null) {
            return;
        }

        Gson gson = new Gson();
        String jsonTime = gson.toJson(time);

        Data myData = new Data.Builder()
                .putString("action", action)
                .putString("time", jsonTime)
                .build();

        OneTimeWorkRequest sqliteWork = new OneTimeWorkRequest.Builder(SqliteIntentServiceWorker.class)
                .setInputData(myData)
                .build();
        WorkManager.getInstance(context).enqueue(sqliteWork);
    }

    static void enqueueWork(Context context, String action) {
        Log.d(TAG, "enqueueWork(Context, Action)");

        Data myData = new Data.Builder()
                .putString("action", action)
                .build();

        OneTimeWorkRequest mathWork = new OneTimeWorkRequest.Builder(SqliteIntentServiceWorker.class)
                .setInputData(myData)
                .build();
        WorkManager.getInstance(context).enqueue(mathWork);
    }

}
