package com.firebirdberlin.nightdream.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.events.OnPowerConnected;
import com.firebirdberlin.nightdream.events.OnPowerDisconnected;
import com.firebirdberlin.nightdream.repositories.BatteryStats;
import com.firebirdberlin.nightdream.models.BatteryValue;

import org.greenrobot.eventbus.EventBus;

public class ChargingStateChangeReceiver extends BroadcastReceiver {

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
