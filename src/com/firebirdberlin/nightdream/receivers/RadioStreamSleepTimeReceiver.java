package com.firebirdberlin.nightdream.receivers;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.services.RadioStreamService;

public class RadioStreamSleepTimeReceiver extends BroadcastReceiver {
    private final static String TAG = "RadioStreamSleepTimeReceiver";

    public static void cancelAlarm(Context context) {
        PendingIntent pI = RadioStreamSleepTimeReceiver.getPendingIntent(context);
        AlarmManager am = (AlarmManager) (context.getSystemService(Context.ALARM_SERVICE));
        am.cancel(pI);
    }

    public static PendingIntent getPendingIntent(Context context) {
        Intent intent = new Intent(Config.ACTION_START_SLEEP_TIME);
        //return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        // PendingIntent.FLAG_CANCEL_CURRENT seems to confuse AlarmManager.cancel() on certain
        // Android devices, e.g. HTC One m7, i.e. AlarmManager.getNextAlarmClock() still returns
        // already cancelled alarm times afterwards.
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    public static void schedule(Context context, long millis) {
        PendingIntent pI = RadioStreamSleepTimeReceiver.getPendingIntent(context);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pI);

        if (Build.VERSION.SDK_INT >= 19) {
            am.setExact(AlarmManager.RTC, millis, pI);
        } else {
            deprecatedSetAlarm(context, millis, pI);
        }
    }

    @SuppressWarnings("deprecation")
    private static void deprecatedSetAlarm(Context context, long millis, PendingIntent pendingIntent) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, millis, pendingIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Config.ACTION_START_SLEEP_TIME.equals(intent.getAction())) {
            RadioStreamService.startSleepTime(context);
        }
        NightDreamActivity.start(context);
    }
}
