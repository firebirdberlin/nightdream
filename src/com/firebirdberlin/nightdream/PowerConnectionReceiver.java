package com.firebirdberlin.nightdream;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.widget.Toast;

public class PowerConnectionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
		// don't get it why but I cant read the charging state here
		//int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
	    //boolean charging = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
	                            //status == BatteryManager.BATTERY_STATUS_FULL);

	    //int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
	    //boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
		//boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

		//switch (status){
			//case BatteryManager.BATTERY_STATUS_CHARGING:
				//Toast.makeText(context, "Status : "+status+"\nCharging : "+isCharging, Toast.LENGTH_SHORT).show();break;
			//case BatteryManager.BATTERY_STATUS_FULL:
				//Toast.makeText(context, "Status : "+status+"\nCharging : FULL", Toast.LENGTH_SHORT).show();break;

		//}

        //Toast.makeText(context, "Status : "+status+"\nPlugged : "+chargePlug, Toast.LENGTH_SHORT).show();

		SharedPreferences settings = context.getSharedPreferences(NightDreamSettingsActivity.PREFS_KEY, 0);
		boolean handle_power = settings.getBoolean("handle_power", false);


		if (handle_power == true){
			//Toast.makeText(context, "Status : "+status+"\nCharging ", Toast.LENGTH_SHORT).show();

			Intent myIntent = new Intent();
			myIntent.setClassName("com.firebirdberlin.nightdream", "com.firebirdberlin.nightdream.NightDreamActivity");
			myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(myIntent);

			//if (charging == true){
				//Intent myIntent = new Intent();
				//myIntent.setClassName("com.firebirdberlin.nightdreamlite", "com.firebirdberlin.nightdreamlite.NightDreamActivity");
				//myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				//context.startActivity(myIntent);
			//} else { // not charging
				//Intent i = new  Intent("com.firebirdberlin.nightdreamlite.POWER_LISTENER");
	            //i.putExtra("charging","disconnected");
	            //context.sendBroadcast(i);
			//}
		}

    }

}
