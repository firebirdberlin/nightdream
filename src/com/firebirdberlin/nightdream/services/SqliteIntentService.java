package com.firebirdberlin.nightdream.services;

import android.content.Context;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.firebirdberlin.nightdream.models.SimpleTime;
import com.google.gson.Gson;

public class SqliteIntentService {

    private static final String TAG = "SqliteIntentService";

    static void saveTime(Context context, SimpleTime time) {
        SqliteIntentServiceWorker.save(context, time);
        //enqueueWork(context, time, SqliteIntentServiceWorker.ACTION_SAVE);
    }

    static void snooze(Context context, SimpleTime time) {
        SqliteIntentServiceWorker.save(context, time);
        //enqueueWork(context, time, SqliteIntentServiceWorker.ACTION_SNOOZE);
    }

    public static void skipAlarm(Context context, SimpleTime time) {
        SqliteIntentServiceWorker.skipAlarm(context, time);
        //enqueueWork(context, time, SqliteIntentServiceWorker.ACTION_SKIP_ALARM);
    }

    public static void deleteAlarm(Context context, SimpleTime time) {
        SqliteIntentServiceWorker.delete(context, time);
        //enqueueWork(context, time, SqliteIntentServiceWorker.ACTION_DELETE_ALARM);
    }

    public static void scheduleAlarm(Context context) {
        enqueueWork(context, SqliteIntentServiceWorker.ACTION_SCHEDULE_ALARM);
    }

    public static void broadcastAlarm(Context context) {
        enqueueWork(context, SqliteIntentServiceWorker.ACTION_BROADCAST_ALARM);
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

        OneTimeWorkRequest sqliteWork =
                new OneTimeWorkRequest.Builder(SqliteIntentServiceWorker.class)
                        .setInputData(myData)
                        // TODO enable with target level 31
                        //.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build();
        WorkManager.getInstance(context).enqueue(sqliteWork);
    }

    static void enqueueWork(Context context, String action) {
        Log.d(TAG, "enqueueWork(Context, Action)");

        Data myData = new Data.Builder()
                .putString("action", action)
                .build();

        OneTimeWorkRequest mathWork =
                new OneTimeWorkRequest.Builder(SqliteIntentServiceWorker.class)
                        .setInputData(myData)
                        .build();
        WorkManager.getInstance(context).enqueue(mathWork);
    }

}
