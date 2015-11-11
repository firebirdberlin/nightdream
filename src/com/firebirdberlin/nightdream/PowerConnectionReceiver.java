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
        if ( Utility.isDaydreamEnabled(context) ) return;

        SharedPreferences settings = context.getSharedPreferences(Settings.PREFS_KEY, 0);
        boolean handle_power = settings.getBoolean("handle_power", false);
        if (handle_power == true){
            Intent myIntent = new Intent();
            myIntent.setClassName("com.firebirdberlin.nightdream", "com.firebirdberlin.nightdream.NightDreamActivity");
            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(myIntent);
        }
    }
}
