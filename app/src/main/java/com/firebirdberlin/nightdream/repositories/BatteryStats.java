/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
        value.isCharging = isCharging(chargingMethod);
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

    private boolean isCharging(int plugged) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && plugged == BatteryManager.BATTERY_PLUGGED_DOCK
        ) {
            return true;
        }
        return (
            plugged == BatteryManager.BATTERY_PLUGGED_AC
                    || plugged == BatteryManager.BATTERY_PLUGGED_USB
                    || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
        );
    }

    private boolean isChargingAC(int chargingMethod) {
        return (chargingMethod == BatteryManager.BATTERY_PLUGGED_AC);
    }

    private boolean isChargingUSB(int chargingMethod) {
        return (chargingMethod == BatteryManager.BATTERY_PLUGGED_USB);
    }

    private boolean isChargingWireless(int chargingMethod) {
        return (chargingMethod == BatteryManager.BATTERY_PLUGGED_WIRELESS);
    }
}
