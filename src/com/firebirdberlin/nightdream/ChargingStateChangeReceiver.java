package com.firebirdberlin.nightdream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.firebirdberlin.nightdream.events.OnPowerConnected;
import com.firebirdberlin.nightdream.events.OnPowerDisconnected;
import com.firebirdberlin.nightdream.repositories.BatteryStats;
import com.firebirdberlin.nightdream.models.BatteryValue;

import de.greenrobot.event.EventBus;

public class ChargingStateChangeReceiver extends BroadcastReceiver {
    private static int PENDING_INTENT_START_APP = 0;

    private Settings settings = null;

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
