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
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.firebirdberlin.nightdream.Settings;


@RequiresApi(api = Build.VERSION_CODES.M)
public class FlashlightProvider {

    private static final String TAG = FlashlightProvider.class.getSimpleName();
    private final CameraManager camManager;
    private final Context context;
    private boolean isOn;
    private final String cameraId;

    public FlashlightProvider(Context context) {
        this.context = context;
        isOn = Settings.getFlashlightIsOn(context);
        camManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        cameraId = getCameraId();
    }

    public boolean hasCameraFlash() {
        return (
                context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
                        && cameraId != null
        );
    }

    private void turnFlashlightOn() {
        if (cameraId == null) {
            return;
        }
        try {
            camManager.setTorchMode(cameraId, true);
        } catch (CameraAccessException e) {
            Log.e(TAG, e.toString());
            return;
        }
        Settings.setFlashlightIsOn(context, true);
        isOn = true;
    }

    private void turnFlashlightOff() {
        if (cameraId == null) {
            return;
        }

        try {
            camManager.setTorchMode(cameraId, false);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Settings.setFlashlightIsOn(context, false);
        isOn = false;
    }

    public void toggleFlashlight() {
        if (isOn) {
            turnFlashlightOff();
        } else {
            turnFlashlightOn();
        }
    }

    public boolean isFlashlightOn() {
        //return isOn;
        return Settings.getFlashlightIsOn(context);
    }

    private String getCameraId() {
        try {
            String[] ids = camManager.getCameraIdList();
            for (String id : ids) {
                CameraCharacteristics c = camManager.getCameraCharacteristics(id);
                Boolean hasFlash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer flashDirection = c.get(CameraCharacteristics.LENS_FACING);
                if (hasFlash != null && hasFlash && flashDirection != null
                        && flashDirection == CameraCharacteristics.LENS_FACING_BACK) {
                    return id;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}


