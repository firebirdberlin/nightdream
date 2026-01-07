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

import androidx.annotation.NonNull;

import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.BatteryValue;
import com.firebirdberlin.nightdream.models.DockState;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.repositories.BatteryStats;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class PowerConnectionReceiver extends BroadcastReceiver {
    private static String TAG = "NightDream.PowerConnectionReceiver";
    private static int PENDING_INTENT_START_APP = 0;

    public static PowerConnectionReceiver register(Context ctx) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_MY_PACKAGE_REPLACED);
        PowerConnectionReceiver receiver = new PowerConnectionReceiver();
        ctx.registerReceiver(receiver, filter);
        return receiver;
    }

    public static void unregister(Context ctx, BroadcastReceiver receiver) {
        if (receiver != null) {
            ctx.unregisterReceiver(receiver);
        }
    }

    public static boolean shallAutostart(@NonNull Context context, @NonNull Settings settings) {
        if (!settings.handle_power) return false;
        if (Utility.isConfiguredAsDaydream(context)) return false;
        if (Build.VERSION.SDK_INT >= 29 && Utility.isLowRamDevice(context)) return false;
        if (settings.isNightModeActive()) return false;

        Calendar now = new GregorianCalendar();
        Log.d(TAG, "autostart ?");
        if (!settings.autostartWeekdays.contains(now.get(Calendar.DAY_OF_WEEK))) return false;
        Log.d(TAG, "autostart : YES");
        Calendar start = new SimpleTime(settings.autostartTimeRangeStartInMinutes).getCalendar();
        Calendar end = new SimpleTime(settings.autostartTimeRangeEndInMinutes).getCalendar();
        boolean shall_auto_start = true;
        if (end.before(start)){
            shall_auto_start = ( now.after(start) || now.before(end) );
        } else if (! start.equals(end)) {
            shall_auto_start = ( now.after(start) && now.before(end) );
        }
        if (! shall_auto_start) return false;

        BatteryStats battery = new BatteryStats(context.getApplicationContext());
        BatteryValue batteryValue = battery.reference;
        DockState dockState = battery.getDockState();

        return (settings.handle_power_ac && batteryValue.isChargingAC) ||
                (settings.handle_power_usb && batteryValue.isChargingUSB) ||
                (settings.handle_power_wireless && batteryValue.isChargingWireless) ||
                (settings.handle_power_desk && dockState.isDockedDesk) ||
                (settings.handle_power_car && dockState.isDockedCar);

    }

    static public void schedule(Context context) {
        Intent alarmIntent = new Intent(context, PowerConnectionReceiver.class);
        PendingIntent pendingIntent = Utility.getImmutableBroadcast(
                context, PENDING_INTENT_START_APP, alarmIntent
        );

        Settings settings = new Settings(context);
        if (settings.isScheduledAutoStartEnabled()) {
            // The autostart feature is replaced by a new version which has a separate setting
            // in the preferences. Thus, the old autostart is deactivated when the new one is
            // active.
            return;
        }
        Calendar start = new SimpleTime(settings.autostartTimeRangeStartInMinutes).getCalendar();

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, start.getTimeInMillis(), pendingIntent);
    }

    public static void conditionallyStartApp(final Context context, final String action) {
        Settings settings = new Settings(context);

        if (settings.isScheduledAutoStartEnabled() && Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            return;
        }

        if (shallAutostart(context, settings)) {
            final SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            Sensor mProximity = null;
            if (mSensorManager != null) {
                mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            }

            if (mProximity == null) {
                NightDreamActivity.start(context);
                return;
            }

            SensorEventListener listener =
                    new SensorEventListener() {
                        @Override
                        public void onSensorChanged(SensorEvent sensorEvent) {
                            if (sensorEvent.values[0] > 0 ) {
                                NightDreamActivity.start(context);
                            }
                            mSensorManager.unregisterListener(this);
                        }

                        @Override
                        public void onAccuracyChanged(Sensor sensor, int i) {

                        }
                    };
            try {
                mSensorManager.registerListener(listener, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
            } catch (IllegalStateException e) {
                NightDreamActivity.start(context);
            }
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

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakelock.acquire();

        conditionallyStartApp(context, action);

        if (wakelock.isHeld()) {
           wakelock.release();
        }
    }

}
