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

import static android.content.Context.LOCATION_SERVICE;
import static android.content.Context.POWER_SERVICE;
import static android.text.format.DateFormat.getBestDateTimePattern;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings.System;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityManagerCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.palette.graphics.Palette;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

public class Utility {
    private static final String SCREENSAVER_ENABLED = "screensaver_enabled";
    private static final String SCREENSAVER_COMPONENTS = "screensaver_components";
    private static final SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static String TAG = "NightDreamUtility";
    int system_brightness_mode = System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
    private Context mContext;

    public Utility(Context context) {
        this.mContext = context;
        getSystemBrightnessMode();
    }

    public static void prepareDirectory(File directory) {
        deleteDirectory(directory);
        directory.mkdirs();
    }

    public static void deleteDirectory(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles()) {
                Utility.deleteDirectory(child);
            }
        fileOrDirectory.delete();
    }

    public static boolean copyToDirectory(Context context, Uri srcUri, File directory, String name) {
        InputStream inputStream;
        try {
            inputStream = context.getContentResolver().openInputStream(srcUri);
            File file = new File(directory, name);
            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            return true;
        } catch (IOException e) {
            //throw new RuntimeException(e);
            return false;
        }
    }

    static public PendingIntent getImmutableBroadcast(Context context, int requestCode, Intent intent, int flags) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                flags
        );
    }

    static public PendingIntent getImmutableService(Context context, int requestCode, Intent intent, int flags) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getService(
                context,
                requestCode,
                intent,
                flags
        );
    }

    static public PendingIntent getImmutableBroadcast(Context context, int requestCode, Intent intent) {
        //https://developer.android.com/about/versions/12/behavior-changes-12#pending-intent-mutability
        int flag = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flag = PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                flag
        );
    }

    static public PendingIntent getImmutableActivity(Context context, int requestCode, Intent intent) {
        //https://developer.android.com/about/versions/12/behavior-changes-12#pending-intent-mutability
        int flag = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flag = PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                flag
        );
    }


    static public boolean is24HourFormat(Context context) {
        return android.text.format.DateFormat.is24HourFormat(context);
    }

    static public String getTimeFormatted(Context context, Calendar calendar) {
        Calendar now_in_one_week = Calendar.getInstance();
        now_in_one_week.add(Calendar.DAY_OF_MONTH, 7);
        if (calendar.after(now_in_one_week)) {
            return "";
        }
        String localPattern;
        if (Build.VERSION.SDK_INT >= 18) {
            if (is24HourFormat(context)) {
                localPattern = getBestDateTimePattern(Locale.getDefault(), "EE HH:mm");
            } else {
                localPattern = getBestDateTimePattern(Locale.getDefault(), "EE hh:mm a");
            }
        } else {
            DateFormat formatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
            localPattern = ((SimpleDateFormat) formatter).toLocalizedPattern();
        }

        SimpleDateFormat hourDateFormat = new SimpleDateFormat(localPattern, Locale.getDefault());
        return hourDateFormat.format(calendar.getTime());
    }

    static public String formatTime(String format, Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(calendar.getTime());
    }

    static public String[] getWeekdayStrings() {
        return getWeekdayStringsForLocale(Locale.getDefault());
    }

    static public String[] getWeekdayStringsForLocale(Locale locale) {
        DateFormatSymbols symbols = new DateFormatSymbols(locale);
        String[] dayNames = symbols.getShortWeekdays();
        for (int i = 1; i < dayNames.length; i++) {
            dayNames[i] = dayNames[i].substring(0, 1);
        }
        return dayNames;
    }

    static public int getFirstDayOfWeek() {
        return Calendar.getInstance().getFirstDayOfWeek();
    }

    static public Sensor getLightSensor(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    public static boolean isDebuggable(Context context) {
        return (context != null &&
                0 != (context.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
    }

    public static boolean isEmulator() {
        Log.d(TAG, Build.FINGERPRINT + "|" + Build.MODEL + "|" + Build.PRODUCT);
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT)
                || "sdk_gphone_x86".equals(Build.PRODUCT)
                || "sdk_gphone_x86_64".equals(Build.MODEL)
                || "sdk_gphone64_x86_64".equals(Build.MODEL)
                || "sdk_gtablet_x86_64".equals(Build.PRODUCT);
    }

    public static int getScreenOffTimeout(Context context) {
        return System.getInt(context.getContentResolver(), System.SCREEN_OFF_TIMEOUT, -1);
    }

    public static boolean isDaydreamEnabled(final Context c) {
        return 1 == android.provider.Settings.Secure.getInt(c.getContentResolver(), SCREENSAVER_ENABLED, -1);
    }

    public static boolean isDaydreamEnabledOnDock(final Context c) {
        return 1 == android.provider.Settings.Secure.getInt(c.getContentResolver(), "screensaver_activate_on_dock", -1);
    }

    public static boolean isDaydreamEnabledOnSleep(final Context c) {
        return 1 == android.provider.Settings.Secure.getInt(c.getContentResolver(), "screensaver_activate_on_sleep", -1);
    }

    static String getSelectedDaydreamClassName(final Context c) {
        String names = android.provider.Settings.Secure.getString(c.getContentResolver(), SCREENSAVER_COMPONENTS);
        if (names != null && !names.isEmpty()) {
            ComponentName componentName = componentsFromString(names)[0];
            return componentName == null ? null : componentName.getClassName();
        }
        return null;
    }

    private static ComponentName[] componentsFromString(String names) {
        String[] namesArray = names.split(",");
        ComponentName[] componentNames = new ComponentName[namesArray.length];
        for (int i = 0; i < namesArray.length; i++) {
            componentNames[i] = ComponentName.unflattenFromString(namesArray[i]);
        }
        return componentNames;
    }

    public static boolean isConfiguredAsDaydream(final Context c) {
        if (1 == android.provider.Settings.Secure.getInt(c.getContentResolver(), SCREENSAVER_ENABLED, -1)) {
            String classname = getSelectedDaydreamClassName(c);
            Log.i(TAG, "Daydream is active " + classname);
            return "com.firebirdberlin.nightdream.NightDreamService".equals(classname);
        }
        return false;
    }

    static public void toggleComponentState(Context context, Class component, boolean on) {
        ComponentName receiver = new ComponentName(context, component);
        PackageManager pm = context.getPackageManager();
        int new_state = (on) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        pm.setComponentEnabledSetting(receiver, new_state, PackageManager.DONT_KILL_APP);
    }

    public static long getFirstInstallTime(Context context) {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .firstInstallTime;
        } catch (NameNotFoundException e) {
            return 0L;
        }
    }

    public static boolean wasInstalledBefore(Context context, int year, int month, int day) {
        long firstInstallTime = getFirstInstallTime(context);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis() < firstInstallTime;
    }

    public static long getDaysSinceFirstInstall(Context context) {
        long firstInstall = getFirstInstallTime(context);
        long msDiff = Calendar.getInstance().getTimeInMillis() - firstInstall;
        return TimeUnit.MILLISECONDS.toDays(msDiff);
    }

    public static boolean hasNetworkConnection(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());

            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return true;
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return true;
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    return true;
                }
            }
        } else {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
        }
        return false;
    }

    public static boolean hasFastNetworkConnection(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());

            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return true;
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    return true;
                }
            }
        } else {

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

            return (hasNetworkConnection(context) &&
                    (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI ||
                            activeNetwork.getType() == ConnectivityManager.TYPE_ETHERNET));
        }
        return false;
    }

    public static void turnScreenOn(Context c) {
        try {
            @SuppressWarnings("deprecation")
            PowerManager.WakeLock wl =
                    ((PowerManager) c.getSystemService(POWER_SERVICE))
                            .newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                                            PowerManager.ACQUIRE_CAUSES_WAKEUP,
                                    "nightdream:WAKE_LOCK_TAG");
            wl.acquire();
            wl.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isScreenLocked(Context context) {
        KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return myKM.inKeyguardRestrictedInputMode();
    }

    public static int pixelsToDp(Context context, float px) {
        DisplayMetrics displaymetrics = context.getResources().getDisplayMetrics();
        return (int) (px / displaymetrics.density);
    }

    public static int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()
        );
    }

    public static int spToPx(Context context, float sp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics()
        );
    }

    public static int getNearestEvenIntValue(float value) {
        int r = (int) Math.ceil(value);
        if (r % 2 != 0) {
            r = (int) Math.floor(value);
        }
        return r;
    }

    public static void colorizeView(View view, int color) {
        colorizeView(view, color, PorterDuff.Mode.SRC_ATOP);
    }

    public static void colorizeView(View view, int color, PorterDuff.Mode mode) {
        if (view == null) {
            return;
        }
        final Paint colorPaint = new Paint();
        PorterDuffColorFilter filter = new PorterDuffColorFilter(color, mode);
        colorPaint.setColorFilter(filter);
        view.setLayerType(View.LAYER_TYPE_HARDWARE, colorPaint);
    }

    public static int getHeightOfView(View contentview) {
        contentview.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        return contentview.getMeasuredHeight();
    }

    public static Dialog createDialogTheme(Dialog dialogTheme) {
        Window window = dialogTheme.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.border_dialog);
        }
        return dialogTheme;
    }

    public static boolean isCharging(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        try {
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            return (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean languageIs(String... languages) {
        for (String language : languages) {
            if (language.equals(Locale.getDefault().getLanguage())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEmpty(String string) {
        return (string == null || string.isEmpty());
    }

    public static boolean contains(String haystack, String needle) {
        if (haystack != null && needle != null) {
            return haystack.contains(needle);
        }
        return false;
    }

    public static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public static boolean equalsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (needle.equals(haystack)) {
                return true;
            }
        }
        return false;
    }

    public static void hideSystemUI(Context context) {
        hideSystemUI(((AppCompatActivity) context).getWindow());
    }

    public static void hideSystemUI(Window window) {
        if (window == null) return;
        if (Build.VERSION.SDK_INT >= 19) {

            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public static Point getDisplaySize(Context context) {
        Point size = new Point();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getSize(size);
        return size;
    }

    public static Rect getSafeWindowRect(Activity context) {
        Rect rect = new Rect();
        Point displaySize = getDisplaySize(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowInsets insets = context.getWindow().getDecorView().getRootWindowInsets();
            if (insets != null) {
                DisplayCutout displayCutout = insets.getDisplayCutout();
                if (displayCutout != null) {
                    rect.bottom = displaySize.y - displayCutout.getSafeInsetBottom();
                    rect.left = displayCutout.getSafeInsetLeft();
                    rect.right = displaySize.x - displayCutout.getSafeInsetRight();
                    rect.top = displayCutout.getSafeInsetTop();
                    return rect;
                }
            }
        }
        rect.left = 0;
        rect.top = 0;
        rect.bottom = displaySize.y;
        rect.right = displaySize.x;
        return rect;
    }

    public static Rect getNotchRect(Activity context) {
        Rect rect = new Rect();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowInsets insets = context.getWindow().getDecorView().getRootWindowInsets();
            if (insets != null) {
                DisplayCutout displayCutout = insets.getDisplayCutout();
                if (displayCutout != null) {
                    List<Rect> bounding = displayCutout.getBoundingRects();
                    rect = bounding.get(0);
                    return rect;
                }
            }
        }
        rect.left = 0;
        rect.top = 0;
        rect.bottom = 0;
        rect.right = 0;
        return rect;
    }

    public static int getSmallestDisplaySize(Context context) {
        Point size = getDisplaySize(context);
        if (size.x < size.y) {
            return size.x;
        } else {
            return size.y;
        }
    }

    public static void logToFile(Context context, String logFileName, String text) {

        // allow logging only in debug mode
        if (!isDebuggable(context)) {
            return;
        }

        String filePath = context.getFilesDir().getPath() + "/" + logFileName;
        Log.d(TAG, "log file location: " + filePath);
        File logFile = new File(filePath);
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                Log.i(TAG, "error creating log file ", e);
            }
        }

        if (logFile.exists()) {

            Date time = Calendar.getInstance().getTime();
            String formattedTime = LOG_DATE_FORMAT.format(time);

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
                writer.append(String.format("%s %s", formattedTime, text));
                writer.newLine();
                writer.flush();
                writer.close();
            } catch (IOException e) {
                Log.i(TAG, "error writing to log file ", e);
            }
        }
    }

    public static boolean isScreenOn(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT <= 19) {
            return deprecatedIsScreenOn(pm);
        }
        return pm.isInteractive();
    }

    public static void logResponseHeaders(Map<String, List<String>> responseHeaders) {
        for (String key : responseHeaders.keySet()) {
            List<String> v = responseHeaders.get(key);
            Log.i(TAG, "header: " + key + "=" + (v != null && !v.isEmpty() ? v.get(0) : "null"));
        }
    }

    @SuppressWarnings("deprecation")
    protected static boolean deprecatedIsScreenOn(PowerManager pm) {
        return pm.isScreenOn();
    }

    public static String getSoundFileTitleFromUri(Context context, String uriString) {
        Log.w(TAG, "uri: " + uriString);
        Uri uri = null;
        try {
            uri = Uri.parse(uriString);
        } catch (NullPointerException ignore) {
        }
        return getSoundFileTitleFromUri(context, uri);
    }

    public static String getSoundFileTitleFromUri(Context context, Uri uri) {
        if (uri == null) return "";

        // get the name from android content
        if ("content".equals(uri.getScheme())) {
            try {
                Ringtone ringtone = RingtoneManager.getRingtone(context, uri);
                String title = ringtone.getTitle(context);
                ringtone.stop();
                return title;
            } catch (RuntimeException ignored) {
            }
        }

        // get the name from meta data
        String filePath = uri.getPath();
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(filePath);
            String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            if (title != null && !title.isEmpty()) {
                return title;
            }
        } catch (RuntimeException ignored) {
        }

        // get the file name
        return uri.getLastPathSegment();
    }

    public static Uri getDefaultAlarmToneUri() {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    }

    public static NotificationCompat.Builder buildNotification(Context context, String channel_id) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return new NotificationCompat.Builder(context);
        } else {
            return new NotificationCompat.Builder(context, channel_id);
        }
    }

    public static void registerEventBus(Object subscriber) {
        EventBus bus = EventBus.getDefault();
        if (!bus.isRegistered(subscriber)) {
            bus.register(subscriber);
        }
    }

    public static void unregisterEventBus(Object subscriber) {
        EventBus.getDefault().unregister(subscriber);
    }

    public static void startForegroundService(Context context, Intent i) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(i);
        } else {
            context.startService(i);
        }
    }

    public static Notification getForegroundServiceNotification(Context context, int resIdText) {
        createNotificationChannels(context);
        Notification note =
                Utility.buildNotification(context, Config.NOTIFICATION_CHANNEL_ID_SERVICES)
                        .setOngoing(true)
                        .setContentTitle(context.getString(R.string.app_name))
                        .setContentText(context.getString(resIdText))
                        .setSmallIcon(R.drawable.ic_expert)
                        .setAutoCancel(true)
                        .setCategory(NotificationCompat.CATEGORY_SERVICE)
                        .setPriority(NotificationCompat.PRIORITY_MIN)
                        .build();

        note.flags |= Notification.FLAG_NO_CLEAR;
        note.flags |= Notification.FLAG_FOREGROUND_SERVICE;
        return note;
    }

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);

        NotificationChannel channelAlarms = notificationManager.getNotificationChannel(Config.NOTIFICATION_CHANNEL_ID_ALARMS);
        if (channelAlarms == null) {
            channelAlarms = prepareNotificationChannel(
                    context,
                    Config.NOTIFICATION_CHANNEL_ID_ALARMS,
                    R.string.notification_channel_name_alarms,
                    R.string.notification_channel_desc_alarms,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channelAlarms);
        }

        NotificationChannel channelRadio = notificationManager.getNotificationChannel(Config.NOTIFICATION_CHANNEL_ID_RADIO);
        if (channelRadio == null) {
            channelRadio = prepareNotificationChannel(
                    context,
                    Config.NOTIFICATION_CHANNEL_ID_RADIO,
                    R.string.notification_channel_name_radio,
                    R.string.notification_channel_desc_radio,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channelRadio);
        }

        NotificationChannel channelMessages = notificationManager.getNotificationChannel(Config.NOTIFICATION_CHANNEL_ID_DEVMSG);
        if (channelMessages == null) {
            channelMessages = prepareNotificationChannel(
                    context,
                    Config.NOTIFICATION_CHANNEL_ID_DEVMSG,
                    R.string.notification_channel_name_devmsg,
                    R.string.notification_channel_desc_devmsg,
                    NotificationManager.IMPORTANCE_LOW
            );
            channelMessages.setShowBadge(true);
            notificationManager.createNotificationChannel(channelMessages);
        }

        NotificationChannel channelServices = notificationManager.getNotificationChannel(Config.NOTIFICATION_CHANNEL_ID_SERVICES);
        if (channelServices == null) {
            channelServices = prepareNotificationChannel(
                    context,
                    Config.NOTIFICATION_CHANNEL_ID_SERVICES,
                    R.string.notification_channel_name_services,
                    R.string.notification_channel_desc_services,
                    NotificationManager.IMPORTANCE_MIN
            );
            notificationManager.createNotificationChannel(channelServices);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel prepareNotificationChannel(
            Context context, String channelName, int idName, int idDesc, int importance) {
        String name = context.getString(idName);
        String description = context.getString(idDesc);
        NotificationChannel mChannel = new NotificationChannel(channelName, name, importance);
        mChannel.setDescription(description);
        mChannel.enableLights(false);
        mChannel.enableVibration(false);
        mChannel.setSound(null, null);
        mChannel.setShowBadge(false);
        mChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        return mChannel;
    }

    private static void playSound(Context context) {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(context, notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isInCall(Context context){
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return manager.getMode() == AudioManager.MODE_IN_CALL;
    }

    public static void logIntent(String TAG, String msg, Intent data) {
        Log.d(TAG, msg);
        if (data.getAction() != null) {
            Log.d(TAG, String.format("> %s = '%s'", "action", data.getAction()));
        }
        Bundle bundle = data.getExtras();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                String strValue = (value != null) ? value.toString() : "";
                Log.d(TAG, String.format("> %s = '%s'", key, strValue));
            }
        }
    }

    public static void setIconSize(Context context, ImageView icon) {
        int dim = Utility.dpToPx(context, 48);
        icon.getLayoutParams().height = dim;
        icon.getLayoutParams().width = dim;
    }

    public static boolean isAirplaneModeOn(Context context) {
        return android.provider.Settings.Global.getInt(
                context.getContentResolver(),
                android.provider.Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0;
    }

    public static int getRandomMaterialColor(Context context) {
        int[] colors = context.getResources().getIntArray(R.array.materialColors);
        return colors[new Random().nextInt(colors.length)];
    }

    public static int getContrastColor(int color) {
        double y = (299 * Color.red(color) + 587 * Color.green(color) + 114 * Color.blue(color)) / 1000;
        return y >= 128 ? Color.BLACK : Color.WHITE;
    }

    public static boolean isLowRamDevice(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return (ActivityManagerCompat.isLowRamDevice(activityManager));
    }

    public static ArrayList<File> listFiles(File path, final String fileEnding) {
        FileFilter fileFilter = path1 -> {
            if (path1.isDirectory() && !path1.getName().startsWith(".")) return true;
            if (fileEnding != null) {
                return path1.isFile() && path1.getName().toLowerCase().endsWith(fileEnding);
            } else {
                return path1.isFile();
            }
        };
        Stack<File> dirs = new Stack<>();
        if (path.isDirectory()) dirs.push(path);
        ArrayList<File> results = new ArrayList<>();
        while (dirs.size() > 0) {
            File dir = dirs.pop();
            File[] files = dir.listFiles(fileFilter);
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory() && !file.getName().startsWith(".")) {
                        dirs.push(file);
                    } else if (file.isFile()) {
                        results.add(file);
                    }
                }
            }
        }
        return results;
    }

    public static int getCameraPhotoOrientation(File imageFile) {
        try {
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            return getCameraPhotoOrientation(exif);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getCameraPhotoOrientation(FileDescriptor fileDescriptor) {
        try {
            ExifInterface exif = new ExifInterface(fileDescriptor);
            return getCameraPhotoOrientation(exif);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int getCameraPhotoOrientation(ExifInterface exif) {
        int rotate = 0;
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotate = 270;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotate = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotate = 90;
                break;
        }

        Log.i("RotateImage", "Exif orientation: " + orientation);
        Log.i("RotateImage", "Rotate value: " + rotate);
        return rotate;
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static int getDominantColor(Bitmap bitmap) {
        if (bitmap == null) return -1;
        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, 1, 1, true);
        final int color = newBitmap.getPixel(0, 0);
        newBitmap.recycle();
        return color;
    }

    public static int getDominantColorFromPalette(Bitmap bitmap) {
        try {
            Palette p = Palette.from(bitmap).generate();
            return p.getDominantColor(Color.RED);
        } catch (IllegalArgumentException e) {
            return Color.RED;
        }
    }

    public static int getDarkMutedColorFromPalette(Bitmap bitmap, int defaultColor) {
        try {
            Palette p = Palette.from(bitmap).generate();
            return p.getDarkMutedColor(defaultColor);
        } catch (IllegalArgumentException e) {
            return defaultColor;
        }
    }

    public static int getVibrantColorFromPalette(Bitmap bitmap, int defaultColor) {
        try {
            Palette p = Palette.from(bitmap).maximumColorCount(256).generate();
            Palette.Swatch vibrant = p.getVibrantSwatch();
            if (vibrant != null) {
                return vibrant.getRgb();
            }
        } catch (IllegalArgumentException ignored) {
        }

        return defaultColor;
    }

    public static int getScreenOrientation(Context context) {
        /*
        returns
            Configuration.ORIENTATION_LANDSCAPE,
            Configuration.ORIENTATION_PORTRAIT,
            Configuration.ORIENTATION_UNDEFINED,
        */
        return context.getResources().getConfiguration().orientation;

    }

    public static boolean hasPermission(Context context, String permission) {
        return (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean hasPermissionCanDrawOverlays(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return (android.provider.Settings.canDrawOverlays(context));
        }
        return true;
    }

    public static void requestPermissionCanDrawOverlays(Context context) {
        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
    }

    @SuppressLint("MissingPermission")
    public static Location getLastKnownLocation(Context context) {
        if (!hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            return null;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            List<String> providers = locationManager.getProviders(true);
            for (String provider : providers) {
                if (LocationManager.GPS_PROVIDER.equals(provider)) {
                    continue;
                }
                return locationManager.getLastKnownLocation(provider);
            }
        }
        return null;
    }

    public static long millisToTimeTick(long lowerThreshold) {
        return millisUntil(60000, lowerThreshold);
    }

    public static long millisUntil(long unit, long lowerThreshold) {
        long now = java.lang.System.currentTimeMillis();
        long millis = unit - now % unit;
        if (millis < lowerThreshold) {
            return millis + unit;
        }
        return millis;
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = mContext.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public void getSystemBrightnessMode() {
        system_brightness_mode = System.getInt(mContext.getContentResolver(),
                System.SCREEN_BRIGHTNESS_MODE,
                System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }

    public void setManualBrightnessMode() {
        System.putInt(mContext.getContentResolver(), System.SCREEN_BRIGHTNESS_MODE,
                System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    public void setAutoBrightnessMode() {
        System.putInt(mContext.getContentResolver(), System.SCREEN_BRIGHTNESS_MODE,
                System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }

    public void restoreSystemBrightnessMode() {
        System.putInt(mContext.getContentResolver(), System.SCREEN_BRIGHTNESS_MODE,
                system_brightness_mode);
    }

    public static boolean areSystemAnimationsEnabled(Context context) {
        float duration = android.provider.Settings.Global.getFloat(
                context.getContentResolver(),
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1
        );
        float transition = android.provider.Settings.Global.getFloat(
                context.getContentResolver(),
                android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE, 1
        );
        return (duration != 0 && transition != 0);
    }
}
