package com.firebirdberlin.nightdream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.firebirdberlin.nightdream.events.OnChargingStateChanged;
import com.firebirdberlin.nightdream.BatteryStats;
import com.firebirdberlin.nightdream.models.BatteryValue;

import de.greenrobot.event.EventBus;

public class ChargingStateChangeReceiver extends BroadcastReceiver {
    private static int PENDING_INTENT_START_APP = 0;

    private Settings settings = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        BatteryStats battery = new BatteryStats(context.getApplicationContext());
        BatteryValue referenceValue = battery.getBatteryValue();
        EventBus.getDefault().post(new OnChargingStateChanged(referenceValue));
        Settings settings = new Settings(context.getApplicationContext());
        settings.saveBatteryReference(referenceValue);
    }
}
