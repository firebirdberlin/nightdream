package com.firebirdberlin.nightdream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;


public class BatteryStats{
    Context mContext;
    public int reference_level;
    public long reference_time;

    // constructor
    public BatteryStats(Context context){
        this.mContext = context;
        reference_level = getLevel();
        reference_time = System.currentTimeMillis();

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
        return (long) ((-level) * ((double) dt / (double) dL));
    }


    // Are we charging / charged?
    public boolean isCharging(){
        Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL);
    }

    public boolean isChargingAC() {
        return (getChargingMethod() == BatteryManager.BATTERY_PLUGGED_AC);
    }

    public boolean isChargingUSB() {
        return (getChargingMethod() == BatteryManager.BATTERY_PLUGGED_USB);
    }

    public boolean isChargingWireless() {
        if (Build.VERSION.SDK_INT >= 17){
            return (getChargingMethod() == BatteryManager.BATTERY_PLUGGED_WIRELESS);
        }
        return false;
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

    public boolean isUndocked() {
        int dockState = getDockState();
        return (dockState == Intent.EXTRA_DOCK_STATE_UNDOCKED);
    }

    public boolean isDockedCar() {
        int dockState = getDockState();
        return (dockState == Intent.EXTRA_DOCK_STATE_CAR);
    }

    public boolean isDockedDesk() {
        int dockState = getDockState();
        if (Build.VERSION.SDK_INT >= 11){
            return (dockState == Intent.EXTRA_DOCK_STATE_DESK ||
                    dockState == Intent.EXTRA_DOCK_STATE_LE_DESK ||
                    dockState == Intent.EXTRA_DOCK_STATE_HE_DESK);
        }
        return (dockState == Intent.EXTRA_DOCK_STATE_DESK);
    }

    private int getDockState() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
        Intent dockStatus = mContext.registerReceiver(null, ifilter);

        if (dockStatus == null) {
            return Intent.EXTRA_DOCK_STATE_UNDOCKED;
        }

        return dockStatus.getIntExtra(Intent.EXTRA_DOCK_STATE, -1);
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
