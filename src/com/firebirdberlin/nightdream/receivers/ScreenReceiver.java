package com.firebirdberlin.nightdream.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.PowerConnectionReceiver;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.repositories.BatteryStats;

public class ScreenReceiver extends BroadcastReceiver {
    private static final String TAG = "ScreenReceiver";

    public static ScreenReceiver register(Context ctx) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        ScreenReceiver receiver = new ScreenReceiver();
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
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            Log.i(TAG, "ACTION_SCREEN_OFF");
            conditionallyActivateAlwaysOn(context);
        }
    }

    private void conditionallyActivateAlwaysOn(Context context) {
        Settings settings = new Settings(context);
        if ( shallActivateStandby(context, settings) ) {
            Bundle bundle = new Bundle();
            bundle.putString("action", "start standby mode");
            NightDreamActivity.start(context, bundle);
        }
    }

    public static boolean shallActivateStandby(Context context, Settings settings) {
        BatteryStats battery = new BatteryStats(context);
        if ( battery.reference.isCharging && settings.standbyEnabledWhileConnected ) {
            return PowerConnectionReceiver.shallAutostart(context, settings);
        }

        if ( !battery.reference.isCharging && settings.standbyEnabledWhileDisconnected ) {
            return true;
        }

        return false;
    }
}
