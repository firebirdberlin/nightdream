package com.firebirdberlin.nightdream;

import com.firebirdberlin.nightdream.events.OnLightSensorValueTimeout;
import com.firebirdberlin.nightdream.events.OnNewLightSensorValue;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import de.greenrobot.event.EventBus;

public class LightSensorEventListener implements SensorEventListener {

    final private Handler handler = new Handler();
    private boolean pending = false;
    private EventBus bus;
    private float ambient_mean = 0.f;
    private float last_value = -1.f;
    private Sensor lightSensor = null;
    private SensorManager mSensorManager = null;
    public int count = 0;


    public LightSensorEventListener(Context context){
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        lightSensor = Utility.getLightSensor(context);
        bus = EventBus.getDefault();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if( event.sensor.getType() == Sensor.TYPE_LIGHT ) {
            if (! pending) {
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
        if (lightSensor == null) return;
        mSensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        handler.postDelayed(calculateMeanValue, 1000);
        handler.postDelayed(sensorTimeout, 15000);// start timer
    }

    public void unregister(){
        removeCallbacks(sensorTimeout); // stop other instances
        removeCallbacks(calculateMeanValue);
        mSensorManager.unregisterListener(this);
    }

    private void removeCallbacks(Runnable runnable) {
        if (handler == null) return;
        if (runnable == null) return;

        handler.removeCallbacks(runnable);
    }

    private Runnable calculateMeanValue = new Runnable() {
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

    private Runnable sensorTimeout = new Runnable() {
        @Override
        public void run() {
            bus.post(new OnLightSensorValueTimeout(last_value));
        }
    };
}
