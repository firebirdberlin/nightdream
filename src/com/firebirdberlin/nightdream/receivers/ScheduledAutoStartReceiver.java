package com.firebirdberlin.nightdream.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.BatteryValue;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.repositories.BatteryStats;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class ScheduledAutoStartReceiver extends BroadcastReceiver {
    private static final String TAG = "AutoStartReceiver";
    private static final int PENDING_INTENT_START_APP = 0;
    private static final String ACTION_START_SCHEDULED =
            "com.firebirdberlin.nightdream.ACTION_START_SCHEDULED";

    public static ScheduledAutoStartReceiver register(Context ctx) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MY_PACKAGE_REPLACED);
        ScheduledAutoStartReceiver receiver = new ScheduledAutoStartReceiver();
        ctx.registerReceiver(receiver, filter);
        return receiver;
    }

    public static void unregister(Context ctx, BroadcastReceiver receiver) {
        if (receiver != null) {
            ctx.unregisterReceiver(receiver);
        }
    }

    public static boolean shallAutostart(Context context, Settings settings) {
        if (!settings.scheduledAutoStartEnabled) return false;
        if (Utility.isConfiguredAsDaydream(context)) return false;
        if (Build.VERSION.SDK_INT >= 29 && Utility.isLowRamDevice(context)) return false;

        Calendar now = new GregorianCalendar();
        if (!settings.scheduledAutostartWeekdays.contains(now.get(Calendar.DAY_OF_WEEK))) {
            return false;
        }
        Calendar start = new SimpleTime(settings.scheduledAutoStartTimeRangeStartInMinutes).getCalendar();
        Calendar end = new SimpleTime(settings.scheduledAutoStartTimeRangeEndInMinutes).getCalendar();
        boolean shall_auto_start = true;
        if (end.before(start)) {
            shall_auto_start = (now.after(start) || now.before(end));
        } else if (!start.equals(end)) {
            shall_auto_start = (now.after(start) && now.before(end));
        }
        if (!shall_auto_start) return false;

        if (settings.scheduledAutoStartChargerRequired) {
            BatteryStats battery = new BatteryStats(context.getApplicationContext());
            BatteryValue batteryValue = battery.reference;
            return batteryValue.isCharging;
        }
        return true;

    }

    static public void schedule(Context context) {
        Log.d(TAG, "schedule()");
        Intent intent = new Intent(ACTION_START_SCHEDULED);
        intent.setClass(context, ScheduledAutoStartReceiver.class);
        PendingIntent pendingIntent = Utility.getImmutableBroadcast(
                context, PENDING_INTENT_START_APP, intent
        );

        Settings settings = new Settings(context);
        SimpleTime startTime = new SimpleTime(settings.scheduledAutoStartTimeRangeStartInMinutes);
        Calendar start = startTime.getCalendar();

        Log.d(TAG, startTime.toString());
        Log.d(TAG, start.toString());
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, start.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, start.getTimeInMillis(), pendingIntent);
        }
    }

    public static void conditionallyStartApp(final Context context) {
        Settings settings = new Settings(context);
        if (shallAutostart(context, settings)) {
            final SensorManager mSensorManager = (SensorManager)
                    context.getSystemService(Context.SENSOR_SERVICE);
            if (mSensorManager == null) {
                NightDreamActivity.start(context);
            }

            Sensor mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            if (mProximity == null) {
                NightDreamActivity.start(context);
            }

            mSensorManager.registerListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent sensorEvent) {
                    if (sensorEvent.values[0] > 0) {
                        NightDreamActivity.start(context);
                    }
                    mSensorManager.unregisterListener(this);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int i) {

                }
            }, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        action = (action == null) ? "none" : action;
        Log.d(TAG, action);
        if (ACTION_START_SCHEDULED.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakelock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK, "nightdream:ScheduledAutostartReceiver"
            );
            wakelock.acquire(10000);

            conditionallyStartApp(context);

            if (wakelock.isHeld()) {
                wakelock.release();
            }
        }
    }

}
