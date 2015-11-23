package com.firebirdberlin.nightdream;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

public class PowerConnectionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences settings = context.getSharedPreferences(Settings.PREFS_KEY, 0);
        boolean handle_power = settings.getBoolean("handle_power", false);
        if (handle_power == true){
            if (shallAutostart(context)) {
                NightDreamActivity.start(context);
            }
        }
    }

    boolean shallAutostart(Context context) {
        SharedPreferences settings = context.getSharedPreferences(Settings.PREFS_KEY, 0);
        boolean handle_power = settings.getBoolean("handle_power", false);
        boolean handle_power_desk = settings.getBoolean("handle_power_desk", false);
        boolean handle_power_car = settings.getBoolean("handle_power_car", false);
        boolean handle_power_ac = settings.getBoolean("handle_power_ac", false);
        boolean handle_power_usb = settings.getBoolean("handle_power_usb", false);
        boolean handle_power_wireless = settings.getBoolean("handle_power_wireless", false);

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
