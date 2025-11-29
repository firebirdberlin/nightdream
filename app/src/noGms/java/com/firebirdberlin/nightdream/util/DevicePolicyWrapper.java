package com.firebirdberlin.nightdream.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.firebirdberlin.nightdream.Settings;

/**
 * A no-op wrapper class for DevicePolicyManager for the noGms flavor.
 */
public class DevicePolicyWrapper {

    public DevicePolicyWrapper(Context context) {
        // No-op constructor for the noGms flavor
    }

    /**
     * No-op method for the noGms flavor.
     */
    public void lockDeviceIfNeeded(Settings settings) {
        // This method is intentionally left empty for the noGms flavor.
    }

    public boolean isAdminActive() {
        return false; // Always return false for the noGms flavor
    }

    public void setupDeviceAdministratorPermissions(boolean useDeviceLock) {
        // This method is intentionally left empty for the noGms flavor.
    }

    public void removeActiveAdmin() {
        // This method is intentionally left empty for the noGms flavor.
    }
}
