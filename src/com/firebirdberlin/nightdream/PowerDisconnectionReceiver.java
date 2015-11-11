package com.firebirdberlin.nightdream;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.widget.Toast;

public class PowerDisconnectionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ( Utility.isDaydreamEnabled(context) ) return;

        SharedPreferences settings = context.getSharedPreferences(Settings.PREFS_KEY, 0);
        boolean handle_power = settings.getBoolean("handle_power", false);

        if (handle_power == true){
            Intent i = new Intent("com.firebirdberlin.nightdream.POWER_LISTENER");
            i.putExtra("charging","disconnected");
            context.sendBroadcast(i);
        }
    }

}
