
package com.firebirdberlin.nightdream.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.BatteryValue;
import com.firebirdberlin.nightdream.models.DockState;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.repositories.BatteryStats;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class PowerConnectionReceiver extends BroadcastReceiver {
    private static String TAG = "NightDream.PowerConnectionReceiver";
    private static int PENDING_INTENT_START_APP = 0;

    private Settings settings = null;

    public static boolean shallAutostart(Context context, Settings settings) {
        if (!settings.handle_power) return false;
        if (Utility.isConfiguredAsDaydream(context)) return false;

        Calendar now = new GregorianCalendar();
        Calendar start;
        Calendar end;
        if (settings.autostartTimeRangeStartInMinutes > -1) {
            start = new SimpleTime(settings.autostartTimeRangeStartInMinutes).getCalendar();
        } else { // deprecated
            start = new SimpleTime(settings.autostartTimeRangeStart).getCalendar();
        }
        if (settings.autostartTimeRangeEndInMinutes > -1) {
            end = new SimpleTime(settings.autostartTimeRangeEndInMinutes).getCalendar();
        } else { // deprecated
            end = new SimpleTime(settings.autostartTimeRangeEnd).getCalendar();
        }

        boolean shall_auto_start = true;
        if (end.before(start)){
            shall_auto_start = ( now.after(start) || now.before(end) );
        } else if (! start.equals(end)) {
            shall_auto_start = ( now.after(start) && now.before(end) );
        }
        if (! shall_auto_start) return false;

        BatteryStats battery = new BatteryStats(context.getApplicationContext());
        BatteryValue batteryValue = battery.reference;
        DockState dockState = battery.getDockState();

        return (settings.handle_power_ac && batteryValue.isChargingAC) ||
                (settings.handle_power_usb && batteryValue.isChargingUSB) ||
                (settings.handle_power_wireless && batteryValue.isChargingWireless) ||
                (settings.handle_power_desk && dockState.isDockedDesk) ||
                (settings.handle_power_car && dockState.isDockedCar);

    }

    static public void schedule(Context context) {
        Intent alarmIntent = new Intent(context, PowerConnectionReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, PENDING_INTENT_START_APP, alarmIntent, 0);

        Settings settings = new Settings(context);
        Calendar start = new SimpleTime(settings.autostartTimeRangeStart).getCalendar();

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, start.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, start.getTimeInMillis(), pendingIntent);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakelock.acquire();

        settings = new Settings(context);
        if (!settings.standbyEnabledWhileConnected // postpone autostart until screen turns off
                && shallAutostart(context, settings)) {
            NightDreamActivity.start(context);
        }

        if (wakelock.isHeld()) {
            wakelock.release();
        }
    }

}
