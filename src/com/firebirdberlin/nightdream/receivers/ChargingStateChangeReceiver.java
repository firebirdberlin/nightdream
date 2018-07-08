package com.firebirdberlin.nightdream.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.events.OnPowerConnected;
import com.firebirdberlin.nightdream.events.OnPowerDisconnected;
import com.firebirdberlin.nightdream.repositories.BatteryStats;
import com.firebirdberlin.nightdream.models.BatteryValue;

import org.greenrobot.eventbus.EventBus;

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
        String action = "";
        if ( intent != null ) {
            action = intent.getAction();
        }
        BatteryStats battery = new BatteryStats(context.getApplicationContext());
        BatteryValue referenceValue = battery.getBatteryValue();

        Settings settings = new Settings(context.getApplicationContext());
        settings.saveBatteryReference(referenceValue);

        if ( action.equals(Intent.ACTION_POWER_CONNECTED) ) {
            EventBus.getDefault().post(new OnPowerConnected(referenceValue));
        } else
        if ( action.equals(Intent.ACTION_POWER_DISCONNECTED) ) {
            EventBus.getDefault().post(new OnPowerDisconnected(referenceValue));
        }

    }
}
