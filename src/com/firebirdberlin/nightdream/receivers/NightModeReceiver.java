package com.firebirdberlin.nightdream.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.TimeRange;

import java.util.Calendar;

public class NightModeReceiver extends BroadcastReceiver {
    private static int PENDING_INTENT_SWITCH_MODES = 2;
    private Event delegate;

    public NightModeReceiver(Event listener) {
        this.delegate = listener;
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
        } catch (IllegalArgumentException ignored) {
        }
    }

    public static void schedule(Context context, TimeRange timerange) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = getSwitchNightModeIntent(context);

        Calendar time = timerange.getNextEvent();
        if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC, time.getTimeInMillis(), pendingIntent);
        } else {
            deprecatedSetAlarm(context, time, pendingIntent);
        }
    }

    public static void cancel(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = getSwitchNightModeIntentForCancel(context);
        alarmManager.cancel(pendingIntent);
    }

    private static PendingIntent getSwitchNightModeIntent(Context context) {
        Intent intent = new Intent(Config.ACTION_SWITCH_NIGHT_MODE);
        return Utility.getImmutableBroadcast(context, PENDING_INTENT_SWITCH_MODES, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent getSwitchNightModeIntentForCancel(Context context) {
        Intent intent = new Intent(Config.ACTION_SWITCH_NIGHT_MODE);
        return Utility.getImmutableBroadcast(context, PENDING_INTENT_SWITCH_MODES, intent);
    }

    @SuppressWarnings("deprecation")
    private static void deprecatedSetAlarm(Context context, Calendar calendar, PendingIntent pendingIntent) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Config.ACTION_SWITCH_NIGHT_MODE.equals(intent.getAction())) {
            delegate.onSwitchNightMode();
        }
    }


    public interface Event {
        void onSwitchNightMode();
    }
}
