package com.firebirdberlin.nightdream;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class PowerConnectionReceiver extends BroadcastReceiver {
    private static int PENDING_INTENT_START_APP = 0;

    private Settings settings = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        settings = new Settings(context);
        if (shallAutostart(context, settings)) {
            Bundle extras = new Bundle();
            extras.putString("action", "power connected");
            NightDreamActivity.start(context, extras);
        }
    }

    public static boolean shallAutostart(Context context, Settings settings) {
        if (settings.handle_power == false) return false;
        Calendar now = new GregorianCalendar();
        Calendar start = new SimpleTime(settings.autostartTimeRangeStart).getCalendar();
        Calendar end = new SimpleTime(settings.autostartTimeRangeEnd).getCalendar();

        boolean shall_auto_start = true;
        if (end.before(start)){
            shall_auto_start = ( now.after(start) || now.before(end) );
        } else if (! start.equals(end)) {
            shall_auto_start = ( now.after(start) && now.before(end) );
        }
        if (! shall_auto_start) return false;

        boolean handle_power_desk = settings.handle_power_desk;
        boolean handle_power_car = settings.handle_power_car;
        boolean handle_power_ac = settings.handle_power_ac;
        boolean handle_power_usb = settings.handle_power_usb;
        boolean handle_power_wireless = settings.handle_power_wireless;

        BatteryStats battery = new BatteryStats(context.getApplicationContext());
        if ( battery.isUndocked() ) {

            if ((handle_power_ac && battery.isChargingAC()) ||
                (handle_power_usb && battery.isChargingUSB()) ||
                (handle_power_wireless && battery.isChargingWireless())) {

                return true;
            }
        }

        if ( (handle_power_desk && battery.isDockedDesk()) ||
             (handle_power_car && battery.isDockedCar())) {
            return true;
        }

        return false;
    }

    static public void schedule(Context context) {
        Intent alarmIntent = new Intent(context, PowerConnectionReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, PENDING_INTENT_START_APP,
                                                                 alarmIntent,
                                                                 PendingIntent.FLAG_UPDATE_CURRENT);

        Settings settings = new Settings(context);
        Calendar calendar = new SimpleTime(settings.autostartTimeRangeStart).getCalendar();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

}
