package com.firebirdberlin.nightdream;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class PowerConnectionReceiver extends BroadcastReceiver {
    Settings settings = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        settings = new Settings(context);
        if (settings.handle_power == true){
            if (shallAutostart(context)) {
                NightDreamActivity.start(context);
            }
        }
    }

    boolean shallAutostart(Context context) {
        Calendar now = new GregorianCalendar();
        SimpleTime startMillis = new SimpleTime(settings.autostartTimeRangeStart);
        SimpleTime endMillis = new SimpleTime(settings.autostartTimeRangeEnd);

        Calendar start = startMillis.getCalendar();
        Calendar end = endMillis.getCalendar();
        boolean is_within_range = false;
        if (end.before(start)){
            is_within_range = ( now.after(start) || now.before(end) );
        } else if (! start.equals(end)) {
            is_within_range = ( now.after(start) && now.before(end) );
        }
        if (! is_within_range) return false;

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
}
