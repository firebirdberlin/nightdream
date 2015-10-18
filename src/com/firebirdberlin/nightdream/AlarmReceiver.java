package com.firebirdberlin.nightdream;
import android.util.Log;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.widget.Toast;
import android.provider.Settings;
import android.provider.Settings.System;

public class AlarmReceiver extends BroadcastReceiver {
	private static final String tag = "AlarmReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
		//int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
	    //boolean charging = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
	                            //status == BatteryManager.BATTERY_STATUS_FULL);

		//SharedPreferences settings = context.getSharedPreferences(NightDreamSettingsActivity.PREFS_KEY, 0);
		//boolean handle_power = settings.getBoolean("handle_power", false);

		//if (handle_power == true){
			////Toast.makeText(context, "Status : "+status+"\nDischarging !", Toast.LENGTH_SHORT).show();
			//Intent i = new  Intent("com.firebirdberlin.nightdream.POWER_LISTENER");
			//i.putExtra("charging","disconnected");
			//context.sendBroadcast(i);
		//}

		Log.d(tag, "intent=" + intent);
		Boolean message = intent.getBooleanExtra("alarmSet",false);
		Log.d(tag, "alarmSet: " + message);
		String nextAlarm = Settings.System.getString(context.getContentResolver(),android.provider.Settings.System.NEXT_ALARM_FORMATTED);
		Log.d(tag, "next alarm: " + nextAlarm);

		if (message == true){
			Intent i = new  Intent("com.firebirdberlin.nightdream.NOTIFICATION_LISTENER");
			i.putExtra("what","alarm");
			i.putExtra("action","changed");
			i.putExtra("when", nextAlarm);
			context.sendBroadcast(i);
		}
    }

}
