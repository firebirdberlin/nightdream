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
    private static Handler handler = new Handler();
    private Context context = null;

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
            if (turnScreenOn) {
                Utility.turnScreenOn(context);
            }
            NightDreamActivity.start(context, "start standby mode");
        }
    }

    public static boolean shallActivateStandby(Context context, Settings settings) {
        if (Utility.isConfiguredAsDaydream(context)) return false;

        BatteryStats battery = new BatteryStats(context);
        if (battery.reference.isCharging && settings.handle_power && settings.standbyEnabledWhileConnected) {
            return PowerConnectionReceiver.shallAutostart(context, settings);
        }

        if ( !battery.reference.isCharging && settings.standbyEnabledWhileDisconnected &&
                settings.alwaysOnBatteryLevel <= battery.reference.level &&
                !settings.isBatteryTimeoutReached() &&
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
        final SensorManager sensorMan = (SensorManager) context.getSystemService(context.SENSOR_SERVICE);
        Sensor sensor = sensorMan.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if (sensor == null) {
            return;
        }
        sensorMan.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                Log.d(TAG, String.format("%3.2f %3.2f %3.2f",
                        sensorEvent.values[0],
                        sensorEvent.values[1],
                        sensorEvent.values[2]));
                float z = sensorEvent.values[2];
                isScreenUp = Math.abs(z) > 9.f;
                handler.post(checkAndActivateApp);
                sensorMan.unregisterListener(this);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        }, sensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            Log.i(TAG, "ACTION_SCREEN_OFF");
            this.context = context;
            isScreenUp = false;
            getGravity(context);
            handler.postDelayed(checkAndActivateApp, 1000);
        }
        if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            Settings settings = new Settings(context);
            settings.updateLastResumeTime();
        }
    }
}
