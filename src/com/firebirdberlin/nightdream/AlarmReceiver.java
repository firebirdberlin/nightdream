package com.firebirdberlin.nightdream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.provider.Settings.System;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String tag = "AlarmReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(tag, "intent=" + intent);
        Boolean message = intent.getBooleanExtra("alarmSet",false);
        Log.d(tag, "alarmSet: " + message);
        String nextAlarm = Settings.System.getString(context.getContentResolver(),android.provider.Settings.System.NEXT_ALARM_FORMATTED);
        Log.d(tag, "next alarm: " + nextAlarm);

        if (message == true){
            Intent i = new  Intent("com.firebirdberlin.nightdream.NOTIFICATION_LISTENER");
            i.putExtra("what","alarm");
            i.putExtra("action","changed");
            i.putExtra("when", nextAlarm);
            context.sendBroadcast(i);
        }
    }

}
