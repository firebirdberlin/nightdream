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

package com.firebirdberlin.nightdream;

import com.firebirdberlin.nightdream.events.OnLightSensorValueTimeout;
import com.firebirdberlin.nightdream.events.OnNewLightSensorValue;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;
import org.greenrobot.eventbus.EventBus;

public class LightSensorEventListener implements SensorEventListener {

    private static final String TAG = "LightSensorEventListener";
    final private Handler handler = new Handler();
    private boolean isRegistered = false;
    private boolean pending = false;
    private final EventBus bus;
    private float ambient_mean = 0.f;
    private float last_value = -1.f;
    private Sensor lightSensor = null;
    private final SensorManager mSensorManager;
    public int count = 0;


    public LightSensorEventListener(Context context){
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            lightSensor = Utility.getLightSensor(context);
        } else {
            Log.e(TAG, "SensorManager is null");
        }
        bus = EventBus.getDefault();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            if (!pending) {
                handler.postDelayed(calculateMeanValue, 10000);
            }

            // triggers handler if no change occurs within the next 15s
            removeCallbacks(sensorTimeout); // stop other instances
            handler.postDelayed(sensorTimeout, 15000);// start timer

            pending = true;
            last_value = event.values[0];
            ambient_mean += event.values[0];
            count += 1;
        }
    }

    public void register(){
        if (mSensorManager == null) {
            Log.e(TAG, "Cannot register listener: SensorManager is null.");
            return;
        }
        if (lightSensor == null) {
            Log.e(TAG, "Cannot register listener: Light sensor not available.");
            return;
        }
        if (isRegistered) return; // Prevent multiple registration

        Log.d(TAG, "Brightness Sensor Max Range: " + lightSensor.getMaximumRange());
        Log.d(TAG, "Brightness Sensor Name: " + lightSensor.getName());
        Log.d(TAG, "Brightness Sensor Vendor: " + lightSensor.getVendor());
        boolean success = mSensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        if (success) {
            isRegistered = true;
            // Remove any pending callbacks before posting new ones to avoid duplicate events
            removeCallbacks(calculateMeanValue);
            removeCallbacks(sensorTimeout);
            pending = false; // Reset pending state on re-registration
            // The calculateMeanValue runnable will be posted by onSensorChanged when the first event arrives.
            handler.postDelayed(sensorTimeout, 15000);// start timer
        }
    }

    public void unregister(){
        if (mSensorManager != null) {
            removeCallbacks(sensorTimeout); // stop other instances
            removeCallbacks(calculateMeanValue);
            mSensorManager.unregisterListener(this);
        } else {
            Log.e(TAG, "Cannot unregister listener: SensorManager is null.");
        }
    }

    private void removeCallbacks(Runnable runnable) {
        if (runnable == null) return;

        handler.removeCallbacks(runnable);
    }

    private final Runnable calculateMeanValue = new Runnable() {
        @Override
        public void run() {
            pending = false;
            if (count == 0) return;
            ambient_mean /= (float) count;
            bus.post(new OnNewLightSensorValue(ambient_mean, count));
            count = 0;
            ambient_mean = 0.f;
        }
    };

    private final Runnable sensorTimeout = new Runnable() {
        @Override
        public void run() {
            bus.post(new OnLightSensorValueTimeout(last_value));
        }
    };
}