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
