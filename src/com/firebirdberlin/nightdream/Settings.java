package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import java.util.Calendar;

public class Settings {
    public static final String PREFS_KEY = "NightDream preferences";
    public final static int BACKGROUND_BLACK = 1;
    public final static int BACKGROUND_GRADIENT = 2;
    public final static int BACKGROUND_IMAGE = 3;

    Context mContext;
    SharedPreferences settings;

    public boolean allow_screen_off = false;
    public boolean ambientNoiseDetection;
    public boolean autoBrightness = false;
    public boolean force_auto_rotation = false;
    public boolean handle_power = false;
    public boolean handle_power_desk = false;
    public boolean handle_power_car = false;
    public boolean handle_power_ac = false;
    public boolean handle_power_usb = false;
    public boolean handle_power_wireless = false;
    public boolean muteRinger = false;
    public boolean showDate = true;
    public boolean useInternalAlarm = true;
    public float dim_offset = 0.f;
    public float minIlluminance = 15.f; // lux
    public float scaleClock = 1.f;
    public int background_mode = 1;
    public int clockColor;
    public int secondaryColor;
    public int sensitivity = 1;
    public int tertiaryColor;
    public long autostartTimeRangeStart = 0L;
    public long autostartTimeRangeEnd = 0L;
    public long nextAlarmTime = 0L;
    public long lastReviewRequestTime = 0L;
    public String AlarmToneUri = "";
    public String bgpath = "";
    public Typeface typeface;

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
        AlarmToneUri = settings.getString("AlarmToneUri", "");
        allow_screen_off = settings.getBoolean("allow_screen_off", false);
        ambientNoiseDetection = settings.getBoolean("ambientNoiseDetection", false);
        autoBrightness = settings.getBoolean("autoBrightness", false);
        autostartTimeRangeStart = settings.getLong("autostart_time_range_start", 0L);
        autostartTimeRangeEnd = settings.getLong("autostart_time_range_end", 0L);
        force_auto_rotation = settings.getBoolean("force_auto_rotation", false);
        handle_power = settings.getBoolean("handle_power", false);
        handle_power_desk = settings.getBoolean("handle_power_desk", false);
        handle_power_car = settings.getBoolean("handle_power_car", false);
        handle_power_ac = settings.getBoolean("handle_power_ac", false);
        handle_power_usb = settings.getBoolean("handle_power_usb", false);
        handle_power_wireless = settings.getBoolean("handle_power_wireless", false);
        bgpath = settings.getString("BackgroundImage", "");
        clockColor = settings.getInt("clockColor", Color.parseColor("#33B5E5"));
        dim_offset = settings.getFloat("dimOffset", 0.f);
        minIlluminance = settings.getFloat("minIlluminance", 15.f);
        muteRinger = settings.getBoolean("Night.muteRinger", false);
        nextAlarmTime = settings.getLong("nextAlarmTime", 0L);
        lastReviewRequestTime = settings.getLong("lastReviewRequestTime", 0L);
        secondaryColor = settings.getInt("secondaryColor", Color.parseColor("#C2C2C2"));
        scaleClock = settings.getFloat("scaleClock", 1.f);
        sensitivity = 10-settings.getInt("NoiseSensitivity", 4);
        showDate = settings.getBoolean("showDate", true);
        tertiaryColor= settings.getInt("tertiaryColor", Color.parseColor("#C2C2C2"));
        useInternalAlarm = settings.getBoolean("useInternalAlarm", false);

        NOISE_AMPLITUDE_SLEEP *= sensitivity;
        NOISE_AMPLITUDE_WAKE  *= sensitivity;

        if (Build.VERSION.SDK_INT < 14){
            background_mode = settings.getInt("BackgroundMode", BACKGROUND_BLACK);
        } else {
            background_mode = Integer.parseInt(settings.getString("backgroundMode", "1"));
        }
        typeface = loadTypeface();
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

    public void setAlarmToneUri(String uri) {
        AlarmToneUri = uri;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putString("AlarmToneUri", uri);
        prefEditor.commit();
    }

    public void setBackgroundImage(String uri) {
        bgpath = uri;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putString("BackgroundImage", uri);
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
}
