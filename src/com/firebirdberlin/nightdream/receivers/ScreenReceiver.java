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
import android.os.PowerManager;
import android.util.Log;

import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.NightModeListener;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.repositories.BatteryStats;
import com.firebirdberlin.nightdream.services.ScreenWatcherService;
import com.firebirdberlin.nightdream.widget.ClockWidgetProvider;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

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
            if (Utility.isScreenOn(context)) {
                return;
            }
            conditionallyActivateAlwaysOn(context, false);
        }
    };

    public static ScreenReceiver register(Context ctx) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
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
        if (Build.VERSION.SDK_INT >= 29 && Utility.isLowRamDevice(context)) return false;
        if (NightModeListener.running) return false;

        BatteryStats battery = new BatteryStats(context);
        if (battery.reference.isCharging && settings.handle_power && settings.isAlwaysOnAllowed()) {
            return PowerConnectionReceiver.shallAutostart(context, settings);
        }

        if ( !battery.reference.isCharging && settings.standbyEnabledWhileDisconnected &&
                settings.alwaysOnBatteryLevel <= battery.reference.level &&
                settings.isAlwaysOnAllowed() &&
                !deviceIsCovered &&
                !Utility.isInCall(context) &&
                (!settings.standbyEnabledWhileDisconnectedScreenUp || isScreenUp)) {

            Calendar now = Calendar.getInstance();
            if (!settings.alwaysOnWeekdays.contains(now.get(Calendar.DAY_OF_WEEK))) return false;

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

    private void getGravity(final Context context) {
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
                   proximitySensorChecked = true;
               } else
               if( sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {
                   float z = sensorEvent.values[2];
                   isScreenUp = Math.abs(z) > 9.f;
                   gravitySensorChecked = true;
               }
               if (gravitySensorChecked && proximitySensorChecked ) {
                   handler.removeCallbacks(checkAndActivateApp);
                   long delay = (Utility.isScreenLocked(context) ? 1000 : 5000);
                   handler.postDelayed(checkAndActivateApp, delay);
                   sensorMan.unregisterListener(this);
               }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        try {
            sensorMan.registerListener(eventListener, sensor, SensorManager.SENSOR_DELAY_GAME);
            sensorMan.registerListener(eventListener, proximitySensor, SensorManager.SENSOR_DELAY_GAME);
        } catch (IllegalStateException ignored) {
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        handler.removeCallbacks(checkAndActivateApp);
        Settings settings = new Settings(context);
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            this.context = context;

            isScreenUp = false;
            deviceIsCovered = false;
            if ( wakeLock != null ) {
                if (wakeLock.isHeld()) wakeLock.release();
                wakeLock = null;
            }
            PowerManager pm = ((PowerManager) context.getSystemService(Context.POWER_SERVICE));
            wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "nightdream:SCREEN_OFF_WAKE_LOCK"
            );
            wakeLock.acquire(20000);
            getGravity(context);
            long delay = (Utility.isScreenLocked(context) ? 6000 : 6000);
            handler.postDelayed(checkAndActivateApp, delay);
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            deviceIsCovered = false;
            isScreenUp = false;
            settings.deleteNextAlwaysOnTime();
        }

        switch (intent.getAction()) {
            case Intent.ACTION_SCREEN_ON:
            case Intent.ACTION_TIMEZONE_CHANGED:
            case Intent.ACTION_TIME_CHANGED:
            case Intent.ACTION_TIME_TICK:
                ClockWidgetProvider.updateAllWidgets(context);
                ScreenWatcherService.updateNotification(context, settings);
                break;
        }
    }

}
