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

package com.firebirdberlin.nightdream.util;

import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.firebirdberlin.nightdream.AdminReceiver;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;

/**
 * A wrapper class to encapsulate all DevicePolicyManager behavior.
 */
public class DevicePolicyWrapper {
    private static final String TAG = "DevicePolicyWrapper";
    private final DevicePolicyManager devicePolicyManager;
    private final KeyguardManager keyguardManager;
    private final ComponentName adminComponentName;
    private final Context context;

    public DevicePolicyWrapper(Context context) {
        this.context = context;
        this.devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        this.adminComponentName = new ComponentName(context, AdminReceiver.class);
    }

    /**
     * Locks the device if device admin is active and the feature is enabled in settings.
     * @param settings The app settings.
     */
    public void lockDeviceIfNeeded(Settings settings) {
        if (settings.shallUseDeviceLock() && !isLocked() && isAdminActive()) {
            Log.d(TAG, "Locking device now.");
            devicePolicyManager.lockNow();
            Utility.turnScreenOn(context);
        }
    }

    public boolean isAdminActive() {
        if (devicePolicyManager == null) return false;
        return devicePolicyManager.isAdminActive(adminComponentName);
    }

    private boolean isLocked() {
        if (keyguardManager == null) {
            return false;
        }
        return keyguardManager.inKeyguardRestrictedInputMode();
    }

    public void setupDeviceAdministratorPermissions(boolean useDeviceLock) {
        if (useDeviceLock) {
            if (!isAdminActive()) {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        context.getString(R.string.useDeviceLockExplanation));
                context.startActivity(intent);
            }
        } else {
            removeActiveAdmin();
        }
    }

    public void removeActiveAdmin() {
        if (isAdminActive()) {
            devicePolicyManager.removeActiveAdmin(adminComponentName);
        }
    }
}
