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
		//int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
	    //boolean charging = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
	                            //status == BatteryManager.BATTERY_STATUS_FULL);

		SharedPreferences settings = context.getSharedPreferences(NightDreamSettingsActivity.PREFS_KEY, 0);
		boolean handle_power = settings.getBoolean("handle_power", false);

		if (handle_power == true){
			//Toast.makeText(context, "Status : "+status+"\nDischarging !", Toast.LENGTH_SHORT).show();
			Intent i = new  Intent("com.firebirdberlin.nightdream.POWER_LISTENER");
			i.putExtra("charging","disconnected");
			context.sendBroadcast(i);
		}

    }

}
