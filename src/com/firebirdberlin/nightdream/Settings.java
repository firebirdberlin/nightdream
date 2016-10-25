package com.firebirdberlin.nightdream;

import java.util.Calendar;
import java.util.Locale;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.Manifest;
import android.os.Build;
import android.support.v4.content.ContextCompat;

import static android.text.format.DateFormat.getBestDateTimePattern;

import com.firebirdberlin.nightdream.models.BatteryValue;

public class Settings {
    public static final String PREFS_KEY = "NightDream preferences";
    public final static int BACKGROUND_BLACK = 1;
    public final static int BACKGROUND_GRADIENT = 2;
    public final static int BACKGROUND_IMAGE = 3;

    Context mContext;
    SharedPreferences settings;

    public boolean allow_screen_off = false;
    private boolean reactivate_screen_on_noise = false;
    private boolean ambientNoiseDetection;
    public boolean autoBrightness = false;
    public boolean force_auto_rotation = false;
    public boolean handle_power = false;
    public boolean handle_power_disconnection = true;
    public boolean handle_power_desk = false;
    public boolean handle_power_car = false;
    public boolean handle_power_ac = false;
    public boolean handle_power_usb = false;
    public boolean handle_power_wireless = false;
    public boolean muteRinger = false;
    public boolean restless_mode = true;
    public boolean showDate = true;
    public boolean showWeather = false;
    public boolean useInternalAlarm = true;
    public float dim_offset = 0.8f;
    public float location_lon = 0.f;
    public float location_lat = 0.f;
    public long location_time = -1L;
    public float minIlluminance = 15.f; // lux
    public float scaleClock = 1.f;
    public int background_mode = 1;
    public int clockColor;
    public int reactivate_on_ambient_light_value = 30; // lux
    public int secondaryColor;
    public int sensitivity = 1;
    public int tertiaryColor;
    public long autostartTimeRangeStart = -1L;
    public long autostartTimeRangeEnd = -1L;
    public long nextAlarmTime = 0L;
    public long lastReviewRequestTime = 0L;
    public String AlarmToneUri = "";
    private String bgpath = "";
    public String backgroundImageURI = "";
    public Typeface typeface;
    public String dateFormat;
    public BatteryValue batteryReferenceValue;

    public double NOISE_AMPLITUDE_WAKE  = Config.NOISE_AMPLITUDE_WAKE;
    public double NOISE_AMPLITUDE_SLEEP = Config.NOISE_AMPLITUDE_SLEEP;


    public Settings(Context context){
        this.mContext = context;
        settings = context.getSharedPreferences(PREFS_KEY, 0);

        if ( !settings.contains("useInternalAlarm") ) {
            boolean on = getUseInternalAlarmDefault();
            setUseInternalAlarm(on);
        }

        reload();
    }

    public void reload() {
        AlarmToneUri = settings.getString("AlarmToneUri",
                android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI.toString());
        allow_screen_off = settings.getBoolean("allow_screen_off", false);
        reactivate_screen_on_noise = settings.getBoolean("reactivate_screen_on_noise", false);
        ambientNoiseDetection = settings.getBoolean("ambientNoiseDetection", false);
        autoBrightness = settings.getBoolean("autoBrightness", false);
        autostartTimeRangeStart = settings.getLong("autostart_time_range_start", autostartTimeRangeStart);
        autostartTimeRangeEnd = settings.getLong("autostart_time_range_end", autostartTimeRangeEnd);
        background_mode = Integer.parseInt(settings.getString("backgroundMode", "1"));
        force_auto_rotation = settings.getBoolean("force_auto_rotation", false);
        handle_power = settings.getBoolean("handle_power", false);
        handle_power_disconnection = settings.getBoolean("handle_power_disconnection", false);
        handle_power_desk = settings.getBoolean("handle_power_desk", false);
        handle_power_car = settings.getBoolean("handle_power_car", false);
        handle_power_ac = settings.getBoolean("handle_power_ac", false);
        handle_power_usb = settings.getBoolean("handle_power_usb", false);
        handle_power_wireless = settings.getBoolean("handle_power_wireless", false);
        bgpath = settings.getString("BackgroundImage", "");
        backgroundImageURI = settings.getString("backgroundImageURI", "");
        clockColor = settings.getInt("clockColor", Color.parseColor("#33B5E5"));
        dim_offset = settings.getFloat("dimOffset", dim_offset);
        location_lat = settings.getFloat("location_lat", 0.f);
        location_lon = settings.getFloat("location_lon", 0.f);
        location_time = settings.getLong("location_time", -1L);
        minIlluminance = settings.getFloat("minIlluminance", 15.f);
        muteRinger = settings.getBoolean("Night.muteRinger", false);
        nextAlarmTime = settings.getLong("nextAlarmTime", 0L);
        lastReviewRequestTime = settings.getLong("lastReviewRequestTime", 0L);
        reactivate_on_ambient_light_value = settings.getInt("reactivate_on_ambient_light_value", reactivate_on_ambient_light_value);
        restless_mode = settings.getBoolean("restlessMode", true);
        secondaryColor = settings.getInt("secondaryColor", Color.parseColor("#C2C2C2"));
        scaleClock = settings.getFloat("scaleClock", 1.f);
        sensitivity = 10-settings.getInt("NoiseSensitivity", 4);
        showDate = settings.getBoolean("showDate", true);
        showWeather = settings.getBoolean("showWeather", false);
        tertiaryColor= settings.getInt("tertiaryColor", Color.parseColor("#C2C2C2"));
        useInternalAlarm = settings.getBoolean("useInternalAlarm", false);
        dateFormat = settings.getString("dateFormat", getDefaultDateFormat());

        NOISE_AMPLITUDE_SLEEP *= sensitivity;
        NOISE_AMPLITUDE_WAKE  *= sensitivity;

        typeface = loadTypeface();
    }

    public BatteryValue loadBatteryReference() {
        long time = settings.getLong("batteryReferenceTime", 0L);
        int level = settings.getInt("batteryReferenceMethod", -1);
        int scale = settings.getInt("batteryReferenceScale", -1);
        int chargingMethod = settings.getInt("batteryReferenceChargingMethod", -1);
        int status = settings.getInt("batteryReferenceStatus", -1);
        BatteryValue bv = new BatteryValue(level, scale, status, chargingMethod);
        bv.time = time;
        return bv;
    }

    public void saveBatteryReference(BatteryValue bv) {
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putLong("batteryReferenceTime", bv.time);
        prefEditor.putInt("batteryReferenceMethod", bv.level);
        prefEditor.putInt("batteryReferenceScale", bv.scale);
        prefEditor.putInt("batteryReferenceChargingMethod", bv.chargingMethod);
        prefEditor.putInt("batteryReferenceStatus", bv.status);
        prefEditor.commit();
    }

    private boolean getUseInternalAlarmDefault() {
        try {
            long installed = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).firstInstallTime;
            if (installed < getDateAsLong(2016, 6, 17)) {
                return true;
            }
        }
        catch (NameNotFoundException e ) {
        }
        return false;
    }

    private String getDefaultDateFormat() {
        // Return the date format as used in versions previous to the version code 72
        if (Build.VERSION.SDK_INT >= 18){
            return getBestDateTimePattern(Locale.getDefault(), "EEEEddLLLL");

        }
        DateFormat formatter = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
        return ((SimpleDateFormat)formatter).toLocalizedPattern();
    }

    public long getDateAsLong(int year, int month, int day) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.MONTH, month - 1);
            calendar.set(Calendar.YEAR, year);
            return calendar.getTimeInMillis();
    }

    private Typeface loadTypeface() {
        int typeface = 0;
        if (Build.VERSION.SDK_INT >= 14){
            typeface = Integer.parseInt(settings.getString("typeface", "1"));
        }
        String typefaceName = mapIntToTypefaceName(typeface);
        return Typeface.create(typefaceName, Typeface.NORMAL);
    }

    private String mapIntToTypefaceName(int typeface) {
        switch (typeface) {
            case 1: return "sans-serif";
            case 2: return "sans-serif-light";
            case 3: return "sans-serif-condensed";
            case 4: return "sans-serif-thin";
            case 5: return "sans-serif-medium";
            default: return null;
        }
    }

    public void setUseInternalAlarm(boolean on) {
        useInternalAlarm = on;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putBoolean("useInternalAlarm", on);
        prefEditor.commit();
    }

    public void setBrightnessOffset(float value){
        dim_offset = value;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putFloat("dimOffset", value);
        prefEditor.putInt("brightness_offset", (int) (value * 100));
        prefEditor.commit();
    }

    public void setMinIlluminance(float value) {
        minIlluminance = value;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putFloat("minIlluminance", value);
        prefEditor.commit();
    }

    public void setAutoStartTime(long start, long end) {
        autostartTimeRangeStart = start;
        autostartTimeRangeEnd = end;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putLong("autostart_time_range_start", start);
        prefEditor.putLong("autostart_time_range_end", end);
        prefEditor.commit();
    }

    public void setAlarmTime(long alarmTime) {
        nextAlarmTime = alarmTime;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putLong("nextAlarmTime", alarmTime);
        prefEditor.commit();
    }

    public void setBackgroundImage(String uri) {
        bgpath = uri;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putString("BackgroundImage", uri);
        prefEditor.commit();
    }

    public void setBackgroundImageURI(String uri) {
        backgroundImageURI = uri;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putString("backgroundImageURI", uri);
        prefEditor.commit();
    }

    public void setLastReviewRequestTime(long reviewRequestTime) {
        lastReviewRequestTime = reviewRequestTime;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putLong("lastReviewRequestTime", lastReviewRequestTime);
        prefEditor.commit();
    }

    public void setScaleClock(float factor) {
        scaleClock = factor;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putFloat("scaleClock", scaleClock);
        prefEditor.commit();
    }

    public void clear() {
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.clear();
        prefEditor.putLong("lastReviewRequestTime", lastReviewRequestTime);
        prefEditor.commit();
    }

    public boolean useAmbientNoiseDetection() {
        return ambientNoiseDetection && hasPermission(Manifest.permission.RECORD_AUDIO);
    }

    public void setUseAmbientNoiseDetection(boolean on) {
        ambientNoiseDetection = on;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putBoolean("ambientNoiseDetection", on);
        prefEditor.commit();
    }

    public boolean reactivateScreenOnNoise() {
        return reactivate_screen_on_noise && hasPermission(Manifest.permission.RECORD_AUDIO);
    }

    public void setReactivateScreenOnNoise(boolean on) {
        reactivate_screen_on_noise = on;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putBoolean("reactivate_screen_on_noise", on);
        prefEditor.commit();
    }

    public void setLocation(float lon, float lat, long time) {
        location_lon = lon;
        location_lat = lat;
        location_time = time;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putFloat("location_lon", lon);
        prefEditor.putFloat("location_lat", lat);
        prefEditor.putLong("location_time", time);
        prefEditor.commit();
    }

    public String backgroundImagePath() {
        if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            return bgpath;
        }
        return "";
    }

    public boolean hasPermission(String permission) {
        return (ContextCompat.checkSelfPermission(mContext, permission)
                 == PackageManager.PERMISSION_GRANTED);
    }

}
