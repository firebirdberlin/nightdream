package com.firebirdberlin.nightdream.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.Build;
import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.services.AlarmService;
import com.firebirdberlin.nightdream.services.RadioStreamService;

public class WakeUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Settings settings = new Settings(context);
        if (settings.useInternalAlarm ) {
            if ( settings.useRadioAlarmClock && Utility.hasFastNetworkConnection(context) ) {
                RadioStreamService.start(context);
            } else {
                AlarmService.startAlarm(context);
            }

            NightDreamActivity.start(context);
        }
    }

    public static void schedule(Context context) {
        Settings settings = new Settings(context);
        if (settings.nextAlarmTime == 0L) return;
        PendingIntent pI = WakeUpReceiver.getPendingIntent(context);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pI);
        if (Build.VERSION.SDK_INT >= 21) {
            AlarmManager.AlarmClockInfo info =
                new AlarmManager.AlarmClockInfo(settings.nextAlarmTime, pI);
            am.setAlarmClock(info, pI);
        } else
        if (Build.VERSION.SDK_INT >= 19) {
            am.setExact(AlarmManager.RTC_WAKEUP, settings.nextAlarmTime, pI );
        } else {
            am.set(AlarmManager.RTC_WAKEUP, settings.nextAlarmTime, pI );
        }
    }

    public static void cancelAlarm(Context context) {
        PendingIntent pI = WakeUpReceiver.getPendingIntent(context);
        AlarmManager am = (AlarmManager) (context.getSystemService( Context.ALARM_SERVICE ));
        am.cancel(pI);
    }

    public static PendingIntent getPendingIntent(Context context) {
        Intent intent = new Intent("com.firebirdberlin.nightdream.WAKEUP");
        intent.putExtra("action", "start alarm");
        //return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        // PendingIntent.FLAG_CANCEL_CURRENT seems to confuse AlarmManager.cancel() on certain
        // Android devices, e.g. HTC One m7, i.e. AlarmManager.getNextAlarmClock() still returns
        // already cancelled alarm times afterwards.
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }
}
