package com.firebirdberlin.nightdream.receivers;

import java.util.Calendar;
import java.lang.IllegalArgumentException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.models.TimeRange;

public class NightModeReceiver extends BroadcastReceiver {
    private static int PENDING_INTENT_SWITCH_MODES = 2;

    public interface Event {
        public void onSwitchNightMode();
    }

    private Event delegate = null;

    public NightModeReceiver(Event listener) {
        this.delegate = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Config.ACTION_SWITCH_NIGHT_MODE.equals(intent.getAction())) {
            delegate.onSwitchNightMode();
        }
    }

    public static NightModeReceiver register(Context ctx, Event event) {
        NightModeReceiver receiver = new NightModeReceiver(event);
        IntentFilter filter = new IntentFilter(Config.ACTION_SWITCH_NIGHT_MODE);
        ctx.registerReceiver(receiver, filter);
        return receiver;
    }

    public static void unregister(Context ctx, BroadcastReceiver receiver) {
        try {
            ctx.unregisterReceiver(receiver);
        } catch ( IllegalArgumentException e ) {

        }
    }

    public static void schedule(Context context, TimeRange timerange) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = getSwitchNightModeIntent(context);
        alarmManager.cancel(pendingIntent);

        Calendar time = timerange.getNextEvent();
        if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC, time.getTimeInMillis(), pendingIntent);
        } else {
            deprecatedSetAlarm(context, time, pendingIntent);
        }
    }

    public static void cancel(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = getSwitchNightModeIntent(context);
        alarmManager.cancel(pendingIntent);
    }

    private static PendingIntent getSwitchNightModeIntent(Context context) {
        Intent intent = new Intent(Config.ACTION_SWITCH_NIGHT_MODE);
        return PendingIntent.getBroadcast(context, PENDING_INTENT_SWITCH_MODES, intent, 0);
    }


    @SuppressWarnings("deprecation")
    private static void deprecatedSetAlarm(Context context, Calendar calendar, PendingIntent pendingIntent) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
    }
}
