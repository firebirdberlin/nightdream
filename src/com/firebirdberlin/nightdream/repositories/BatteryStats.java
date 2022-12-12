package com.firebirdberlin.nightdream.repositories;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.BatteryValue;
import com.firebirdberlin.nightdream.models.DockState;

public class BatteryStats {
    private final Context mContext;
    private final Intent receivedBatteryIntent;
    public BatteryValue reference;

    /**
     * use this to retrieve battery values using a sticky intent ACTION_BATTERY_CHANGED
     */
    public BatteryStats(Context context) {
        this.mContext = context;
        this.receivedBatteryIntent = null;
        reference = getBatteryValue();
    }

    /**
     * use this to retrieve battery values from a real ACTION_BATTERY_CHANGED receiver
     */
    public BatteryStats(Context context, Intent receivedBatteryIntent) {
        this.mContext = context;
        this.receivedBatteryIntent = receivedBatteryIntent;
        reference = getBatteryValue();
    }

    private BatteryValue getBatteryValue() {
        Intent batteryIntent = receivedBatteryIntent;
        if (batteryIntent == null) {
            try {
                batteryIntent = mContext.registerReceiver(
                        null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                );
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (batteryIntent == null) {
            return null;
        }
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int chargingMethod = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        BatteryValue value = new BatteryValue(level, scale, status, chargingMethod);
        value.isCharging = isCharging(status);
        value.isChargingAC = isChargingAC(chargingMethod);
        value.isChargingUSB = isChargingUSB(chargingMethod);
        value.isChargingWireless = isChargingWireless(chargingMethod);
        value.isAirplaneModeOn = Utility.isAirplaneModeOn(mContext);
        return value;
    }

    public DockState getDockState() {
        Intent dockStatus = null;
        try {
            dockStatus = mContext.registerReceiver(
                    null, new IntentFilter(Intent.ACTION_DOCK_EVENT)
            );
        } catch (IllegalArgumentException ignored) {
        }

        if (dockStatus == null) {
            return new DockState(true, false, false);
        }

        int dockState = dockStatus.getIntExtra(Intent.EXTRA_DOCK_STATE, -1);
        boolean undocked = isUndocked(dockState);
        boolean car = isDockedCar(dockState);
        boolean desk = isDockedDesk(dockState);
        return new DockState(undocked, car, desk);
    }


    private boolean isUndocked(int dockState) {
        return (dockState == Intent.EXTRA_DOCK_STATE_UNDOCKED);
    }

    private boolean isDockedCar(int dockState) {
        return (dockState == Intent.EXTRA_DOCK_STATE_CAR);
    }

    private boolean isDockedDesk(int dockState) {
        return (dockState == Intent.EXTRA_DOCK_STATE_DESK ||
                dockState == Intent.EXTRA_DOCK_STATE_LE_DESK ||
                dockState == Intent.EXTRA_DOCK_STATE_HE_DESK);
    }

    private boolean isCharging(int status) {
        return (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL);
    }

    private boolean isChargingAC(int chargingMethod) {
        return (chargingMethod == BatteryManager.BATTERY_PLUGGED_AC);
    }

    private boolean isChargingUSB(int chargingMethod) {
        return (chargingMethod == BatteryManager.BATTERY_PLUGGED_USB);
    }

    private boolean isChargingWireless(int chargingMethod) {
        if (Build.VERSION.SDK_INT >= 17) {
            return (chargingMethod == BatteryManager.BATTERY_PLUGGED_WIRELESS);
        }
        return false;
    }
}
