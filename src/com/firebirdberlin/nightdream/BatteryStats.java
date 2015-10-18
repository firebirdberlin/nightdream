package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.os.BatteryManager;


public class BatteryStats{
    Context mContext;
	public int reference_level;
	public long reference_time;

    // constructor
    public BatteryStats(Context context){
        this.mContext = context;
		reference_level = getLevel();
		reference_time = System.currentTimeMillis();
		// if the battery level should be tracked more exactly
		//this.registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

    }

	 public int getLevel() {
        Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    }

     public int getScale() {
        Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
    }

     public float getPercentage() {
        Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        return (((float)level / (float)scale) * 100.0f);
    }

	public long getEstimateMillis() {
        Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		int dL = level - reference_level;
		long dt = System.currentTimeMillis() - reference_time;

		if ((dL == 0) || (dt == 0.)) return 0;
		//double scale = dL/dt * te + L0
		// te = (scale - L0) * dt /dL
		return (long) ( (scale - level) * ((double) dt / (double) dL));
	}

	public long getDischargingEstimateMillis() {
        Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		int dL = level - reference_level;
		long dt = System.currentTimeMillis() - reference_time;
		if ((dL == 0) || (dt == 0.)) return 0;
		//double scale = dL/dt * te + L0
		// te = (scale - L0) * dt /dL
		return (long) (( level) * ((double) dt / (double) dL));
	}


    // Are we charging / charged?
	public boolean isCharging(){
        Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		return (status == BatteryManager.BATTERY_STATUS_CHARGING ||
		                     status == BatteryManager.BATTERY_STATUS_FULL);
	}
	// How are we charging?
    public int getChargingMethod(){
        Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int chargePlug = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		//boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
		//boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
		//boolean acWireless = chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS;
		return chargePlug;
	}

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver(){
    @Override
    public void onReceive(Context arg0, Intent intent) {
      //int level = intent.getIntExtra("level", 0);
      int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
      int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

       //histogram.setBatteryPercentage(((float)level / (float)scale) * 100.0f);
      //contentTxt.setText(String.valueOf(level) + "%");
    }
  };
}
