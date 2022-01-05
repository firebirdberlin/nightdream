package com.firebirdberlin.nightdream.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.repositories.BatteryStats;
import com.firebirdberlin.nightdream.models.BatteryValue;

public class ChargingStateChangeReceiver extends BroadcastReceiver {

    public static ChargingStateChangeReceiver register(Context ctx) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        ChargingStateChangeReceiver receiver = new ChargingStateChangeReceiver();
        ctx.registerReceiver(receiver, filter);
        return receiver;
    }

    public static void unregister(Context ctx, BroadcastReceiver receiver) {
        if (receiver != null) {
            ctx.unregisterReceiver(receiver);
        }
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
                getAndSaveBatteryReference(context.getApplicationContext());
        }
    }

    public static BatteryValue getAndSaveBatteryReference(Context context) {
        BatteryStats battery = new BatteryStats(context.getApplicationContext());
        BatteryValue referenceValue = battery.getBatteryValue();

        Settings settings = new Settings(context.getApplicationContext());
        settings.saveBatteryReference(referenceValue);
        return referenceValue;
    }

}
