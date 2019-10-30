package com.firebirdberlin.nightdream.repositories;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;


@RequiresApi(api = Build.VERSION_CODES.M)
public class FlashlightProvider {

    private static final String TAG = FlashlightProvider.class.getSimpleName();
    private CameraManager camManager;
    private Context context;
    private boolean isOn = false;

    public FlashlightProvider(Context context) {
        this.context = context;
    }

    public boolean hasCameraFlash() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    private void turnFlashlightOn() {
        try {
            camManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String cameraId; // Usually front camera is at 0 position.
            if (camManager != null) {
                cameraId = camManager.getCameraIdList()[0];
                camManager.setTorchMode(cameraId, true);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, e.toString());
            return;
        }
        isOn = true;
    }

    private void turnFlashlightOff() {
        try {
            String cameraId;
            camManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (camManager != null) {
                cameraId = camManager.getCameraIdList()[0]; // Usually front camera is at 0 position.
                camManager.setTorchMode(cameraId, false);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
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
        return isOn;
     }
}


