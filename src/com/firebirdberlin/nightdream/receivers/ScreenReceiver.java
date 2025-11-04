package com.firebirdberlin.nightdream.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.repositories.BatteryStats;

import java.util.Calendar;

public class ScreenReceiver extends BroadcastReceiver {
    private static final String TAG = "ScreenReceiver";
    private static boolean isScreenUp = false;
    private static boolean deviceIsCovered = false;
    static boolean gravitySensorChecked = false;
    static boolean proximitySensorChecked = false;
    private static Looper broadcastReceiverThreadLooper = null;
    private PowerManager.WakeLock wakeLock;

    public static ScreenReceiver register(Context ctx) {
        HandlerThread broadcastReceiverThread = new HandlerThread(TAG);
        broadcastReceiverThread.start();

        broadcastReceiverThreadLooper = broadcastReceiverThread.getLooper();
        Handler broadcastReceiverHandler = new Handler(broadcastReceiverThreadLooper);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        ScreenReceiver receiver = new ScreenReceiver();

        ctx.registerReceiver(receiver, filter, null, broadcastReceiverHandler);
        return receiver;
    }

    public static void unregister(Context ctx, BroadcastReceiver receiver) {
        if (receiver != null) {
            ctx.unregisterReceiver(receiver);
            broadcastReceiverThreadLooper.quit();
        }
    }

    private static void checkSensorDataAndActivate(Context context) {
        boolean waitingForSensors = getSensorData(context);
        long startTime = System.currentTimeMillis();
        while (waitingForSensors && (!proximitySensorChecked || !gravitySensorChecked)) {
            try {
                Log.d(TAG, "waiting for sensor data (" + proximitySensorChecked + "," + gravitySensorChecked + ")");
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }

            // timeout, sensors are not responding in time
            if ((System.currentTimeMillis() - startTime) > 4000) {
                break;
            }
        }
        Log.d(TAG, "finished waiting for sensor data (" + proximitySensorChecked + "," + gravitySensorChecked + ")");
        if (Utility.isScreenOn(context)) {
            return;
        }
        conditionallyActivateAlwaysOn(context, false);
    }

    private static void conditionallyActivateAlwaysOn(Context context, boolean turnScreenOn) {
        Settings settings = new Settings(context);
        if (shallActivateStandby(context, settings)) {
            Log.i(TAG, "conditionallyActivateAlwaysOn(): Activating standby mode");
            NightDreamActivity.start(context, "start standby mode");
            if (turnScreenOn) {
                Utility.turnScreenOn(context);
            }
        } else {
            Log.i(TAG, "conditionallyActivateAlwaysOn(): Not activating standby mode");
        }
        settings.deleteNextAlwaysOnTime();
    }

    public static boolean shallActivateStandby(final Context context, Settings settings) {
        Log.i(TAG, "shallActivateStandby()");
        if (Utility.isConfiguredAsDaydream(context)) return false;
        if (Build.VERSION.SDK_INT >= 29 && Utility.isLowRamDevice(context)) return false;

        BatteryStats battery = new BatteryStats(context);
        if (settings.handle_power && battery.reference.isCharging && settings.isAlwaysOnAllowed()) {
            Log.i(TAG, "shallActivateStandby() autostart allowed");
            return PowerConnectionReceiver.shallAutostart(context, settings);
        }

        if (settings.isStandbyEnabledWhileDisconnected()
                && (
                battery.reference.isCharging
                        || settings.alwaysOnBatteryLevel <= battery.reference.level
                )
                && settings.isAlwaysOnAllowed()
                && !deviceIsCovered
                && !Utility.isInCall(context)
                && (!settings.standbyEnabledWhileDisconnectedScreenUp || isScreenUp)
        ) {
            Log.i(TAG, "shallActivateStandby() standby allowed");

            Calendar now = Calendar.getInstance();
            if (!settings.alwaysOnWeekdays.contains(now.get(Calendar.DAY_OF_WEEK))) return false;

            Calendar start = new SimpleTime(settings.alwaysOnTimeRangeStartInMinutes).getCalendar();
            Calendar end = new SimpleTime(settings.alwaysOnTimeRangeEndInMinutes).getCalendar();
            boolean shall_auto_start = true;
            if (end.before(start)) {
                shall_auto_start = (now.after(start) || now.before(end));
            } else if (!start.equals(end)) {
                shall_auto_start = (now.after(start) && now.before(end));
            }
            return shall_auto_start;
        }

        Log.i(TAG, "shallActivateStandby() standby not allowed");
        return false;
    }

    private static boolean getSensorData(final Context context) {
        Log.i(TAG, "getSensorData()");
        gravitySensorChecked = false;
        proximitySensorChecked = false;

        final SensorManager sensorMan = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorMan == null) {
            return false;
        }

        Sensor sensor = sensorMan.getDefaultSensor(Sensor.TYPE_GRAVITY);
        Sensor proximitySensor = sensorMan.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if (sensor == null || proximitySensor == null ) {
            return false;
        }

        final HandlerThread broadcastReceiverThread = new HandlerThread(TAG);
        broadcastReceiverThread.start();
        Looper sensorListenerThreadLooper = broadcastReceiverThread.getLooper();
        Handler sensorListenerHandler = new Handler(sensorListenerThreadLooper);
        final long startTime = System.currentTimeMillis();
        SensorEventListener eventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                long now = System.currentTimeMillis();
                if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                    deviceIsCovered = (sensorEvent.values[0] == 0);
                    Log.i(TAG, "deviceIsCovered = " + deviceIsCovered);
                    proximitySensorChecked = true;
                } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {
                    float z = sensorEvent.values[2];
                    isScreenUp = Math.abs(z) > 9.f;
                    Log.i(TAG, "isScreenUp = " + isScreenUp);
                    gravitySensorChecked = true;
                }

                if (
                        (gravitySensorChecked && proximitySensorChecked)
                                || (now - startTime > 4000)
                ) {
                    Log.i(TAG, "unregisterListener");
                    sensorMan.unregisterListener(this);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        broadcastReceiverThread.quitSafely();
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        try {
            sensorMan.registerListener(eventListener, sensor, SensorManager.SENSOR_DELAY_GAME, sensorListenerHandler);
            sensorMan.registerListener(eventListener, proximitySensor, SensorManager.SENSOR_DELAY_GAME, sensorListenerHandler);
        } catch (IllegalStateException ignored) {
            return false;
        }
        return true;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive " + ((action != null) ? action : "null"));
        Settings settings = new Settings(context);
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            isScreenUp = false;
            deviceIsCovered = false;
            if (wakeLock != null) {
                if (wakeLock.isHeld()) wakeLock.release();
                wakeLock = null;
            }
            PowerManager pm = ((PowerManager) context.getSystemService(Context.POWER_SERVICE));
            wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "nightdream:SCREEN_OFF_WAKE_LOCK"
            );
            wakeLock.acquire(20000);
            checkSensorDataAndActivate(context);
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            deviceIsCovered = false;
            isScreenUp = false;
            settings.deleteNextAlwaysOnTime();
        }
    }
}
