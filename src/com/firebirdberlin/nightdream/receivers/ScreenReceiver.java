package com.firebirdberlin.nightdream.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
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
    private boolean proximitySensorChecked = false;
    private boolean gravitySensorChecked = false;
    private static Handler handler = new Handler();
    private Context context = null;
    private PowerManager.WakeLock wakeLock;

    private Runnable checkAndActivateApp = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(checkAndActivateApp);
            conditionallyActivateAlwaysOn(context, false);
        }
    };

    public static ScreenReceiver register(Context ctx) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        ScreenReceiver receiver = new ScreenReceiver();
        ctx.registerReceiver(receiver, filter);
        return receiver;
    }

    public static void unregister(Context ctx, BroadcastReceiver receiver) {
        if (receiver != null) {
            ctx.unregisterReceiver(receiver);
        }
    }

    private static void conditionallyActivateAlwaysOn(Context context, boolean turnScreenOn) {
        Settings settings = new Settings(context);
        if ( shallActivateStandby(context, settings) ) {
            NightDreamActivity.start(context, "start standby mode");
            if (turnScreenOn) {
                Utility.turnScreenOn(context);
            }
        }
        settings.deleteNextAlwaysOnTime();
    }

    public static boolean shallActivateStandby(final Context context, Settings settings) {
        if (Utility.isConfiguredAsDaydream(context)) return false;

        BatteryStats battery = new BatteryStats(context);
        if (battery.reference.isCharging && settings.handle_power && settings.standbyEnabledWhileConnected) {
            return PowerConnectionReceiver.shallAutostart(context, settings);
        }

        if ( !battery.reference.isCharging && settings.standbyEnabledWhileDisconnected &&
                settings.alwaysOnBatteryLevel <= battery.reference.level &&
                settings.isAlwaysOnAllowed() &&
                !deviceIsCovered &&
                !Utility.isInCall(context) &&
                (!settings.standbyEnabledWhileDisconnectedScreenUp || isScreenUp)) {

            Calendar now = Calendar.getInstance();
            Calendar start = new SimpleTime(settings.alwaysOnTimeRangeStartInMinutes).getCalendar();
            Calendar end = new SimpleTime(settings.alwaysOnTimeRangeEndInMinutes).getCalendar();
            boolean shall_auto_start = true;
            if (end.before(start)){
                shall_auto_start = ( now.after(start) || now.before(end) );
            } else if (! start.equals(end)) {
                shall_auto_start = ( now.after(start) && now.before(end) );
            }
            return shall_auto_start;
        }

        return false;
    }

    private void getGravity(Context context) {
        gravitySensorChecked = false;
        proximitySensorChecked = false;
        final SensorManager sensorMan = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorMan == null) return;

        Sensor sensor = sensorMan.getDefaultSensor(Sensor.TYPE_GRAVITY);
        Sensor proximitySensor = sensorMan.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (sensor == null || proximitySensor == null) return;
        SensorEventListener eventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
               if( sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                   deviceIsCovered = (sensorEvent.values[0] == 0);
                   Log.d(TAG, String.format("proximity: %3.2f", sensorEvent.values[0]));
                   proximitySensorChecked = true;
               } else
               if( sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {
                   Log.d(TAG, String.format("%3.2f %3.2f %3.2f",
                           sensorEvent.values[0],
                           sensorEvent.values[1],
                           sensorEvent.values[2]));
                   float z = sensorEvent.values[2];
                   isScreenUp = Math.abs(z) > 9.f;
                   gravitySensorChecked = true;
               }
               if (gravitySensorChecked && proximitySensorChecked ) {
                   handler.post(checkAndActivateApp);
                   sensorMan.unregisterListener(this);
               }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        sensorMan.registerListener(eventListener, sensor, SensorManager.SENSOR_DELAY_GAME);
        sensorMan.registerListener(eventListener, proximitySensor , SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            Log.i(TAG, "ACTION_SCREEN_OFF");
            this.context = context;

            isScreenUp = false;
            deviceIsCovered = false;
            if ( wakeLock != null ) {
                if (wakeLock.isHeld()) wakeLock.release();
                wakeLock = null;
            }
            wakeLock = ((PowerManager) context.getSystemService(Context.POWER_SERVICE))
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP ,
                            "SCREEN_OFF_WAKE_LOCK");
            wakeLock.acquire(20000);
            getGravity(context);
            handler.postDelayed(checkAndActivateApp, 1000);
        }

        if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            deviceIsCovered = false;
            isScreenUp = false;
            Settings settings = new Settings(context);
            settings.deleteNextAlwaysOnTime();
        }
    }

}
