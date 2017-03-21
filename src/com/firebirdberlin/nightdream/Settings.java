package com.firebirdberlin.nightdream;

import java.util.Calendar;
import java.util.Locale;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.Manifest;
import android.os.Build;
import android.support.v4.content.ContextCompat;

import static android.text.format.DateFormat.getBestDateTimePattern;

import com.firebirdberlin.nightdream.models.BatteryValue;
import com.firebirdberlin.nightdream.models.WeatherEntry;

public class Settings {
    public static final String PREFS_KEY = "NightDream preferences";
    private final static String TAG = "NightDream.Settings";
    public final static int BACKGROUND_BLACK = 1;
    public final static int BACKGROUND_GRADIENT = 2;
    public final static int BACKGROUND_IMAGE = 3;
    public final static int NIGHT_MODE_ACTIVATION_MANUAL = 0;
    public final static int NIGHT_MODE_ACTIVATION_AUTOMATIC = 1;

    Context mContext;
    SharedPreferences settings;

    public boolean allow_screen_off = false;
    private boolean reactivate_screen_on_noise = false;
    public boolean alarmFadeIn = true;
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
    public boolean hideBackgroundImage = true;
    public boolean muteRinger = false;
    public boolean persistentBatteryValueWhileCharging = true;
    public boolean restless_mode = true;
    public boolean showDate = true;
    public boolean showWeather = false;
    public boolean showTemperature = true;
    public boolean showWindSpeed = false;
    public boolean useInternalAlarm = true;
    public boolean useRadioAlarmClock = false;
    public float dim_offset = 0.8f;
    public float location_lon = 0.f;
    public float location_lat = 0.f;
    public long location_time = -1L;
    public String location_provider = LocationManager.NETWORK_PROVIDER;
    public float minIlluminance = 15.f; // lux
    public float scaleClock = 1.f;
    public float scaleClockPortrait = 1.f;
    public float scaleClockLandscape = 1.5f;
    public int alarmVolume = 3;
    public int background_mode = 1;
    public int clockColor;
    public int clockColorNight;
    public int clockLayout;
    public int nightModeActivationMode;
    public int reactivate_on_ambient_light_value = 30; // lux
    public int secondaryColor;
    public int secondaryColorNight;
    public int sensitivity = 1;
    public int temperatureUnit = WeatherEntry.CELSIUS;
    public int speedUnit = WeatherEntry.METERS_PER_SECOND;
    public int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    public long autostartTimeRangeStart = -1L;
    public long autostartTimeRangeEnd = -1L;
    public long nextAlarmTime = 0L;
    public long lastReviewRequestTime = 0L;
    public String AlarmToneUri = "";
    public String radioStreamURL = "";
    public String radioStreamURLUI = "";
    private String bgpath = "";
    public String backgroundImageURI = "";
    public Typeface typeface;
    public String dateFormat;
    public String timeFormat;
    public BatteryValue batteryReferenceValue;
    public WeatherEntry weatherEntry;
    public String weatherCityID;

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
        alarmVolume = settings.getInt("alarmVolume", 3);
        alarmFadeIn = settings.getBoolean("alarmFadeIn", true);
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
        hideBackgroundImage = settings.getBoolean("hideBackgroundImage", true);
        bgpath = settings.getString("BackgroundImage", "");
        backgroundImageURI = settings.getString("backgroundImageURI", "");
        final String defaultColorString = "#33B5E5";
        clockColor = settings.getInt("clockColor", Color.parseColor(defaultColorString));
        clockColorNight = settings.getInt("primaryColorNight", Color.parseColor(defaultColorString));
        clockLayout = Integer.parseInt(settings.getString("clockLayout", "0"));
        dim_offset = settings.getFloat("dimOffset", dim_offset);
        location_lat = settings.getFloat("location_lat", 0.f);
        location_lon = settings.getFloat("location_lon", 0.f);
        location_time = settings.getLong("location_time", -1L);
        location_provider = settings.getString("location_provider", LocationManager.NETWORK_PROVIDER);
        minIlluminance = settings.getFloat("minIlluminance", 15.f);
        muteRinger = settings.getBoolean("Night.muteRinger", false);
        nextAlarmTime = settings.getLong("nextAlarmTime", 0L);
        lastReviewRequestTime = settings.getLong("lastReviewRequestTime", 0L);
        radioStreamURL = settings.getString("radioStreamURL", "");
        radioStreamURLUI = settings.getString("radioStreamURLUI", "");
        reactivate_on_ambient_light_value = settings.getInt("reactivate_on_ambient_light_value", reactivate_on_ambient_light_value);
        persistentBatteryValueWhileCharging = settings.getBoolean("persistentBatteryValueWhileCharging", true);
        restless_mode = settings.getBoolean("restlessMode", true);
        final String defaultSecondaryColorString = "#C2C2C2";
        secondaryColor = settings.getInt("secondaryColor", Color.parseColor(defaultSecondaryColorString));
        secondaryColorNight = settings.getInt("secondaryColorNight", Color.parseColor(defaultSecondaryColorString));
        scaleClock = settings.getFloat("scaleClock", 1.f);
        scaleClockPortrait = settings.getFloat("scaleClockPortrait", 1.f);
        scaleClockLandscape = settings.getFloat("scaleClockLandscape", 1.5f);
        sensitivity = 10-settings.getInt("NoiseSensitivity", 4);
        showDate = settings.getBoolean("showDate", true);
        showWeather = settings.getBoolean("showWeather", false);
        showTemperature = settings.getBoolean("showTemperature", true);
        showWindSpeed = settings.getBoolean("showWindSpeed", false);
        temperatureUnit = Integer.parseInt(settings.getString("temperatureUnit", "1"));
        speedUnit = Integer.parseInt(settings.getString("speedUnit", "1"));
        screenOrientation = Integer.parseInt(settings.getString("screenOrientation", "-1"));
        nightModeActivationMode = Integer.parseInt(settings.getString("nightModeActivationMode", "1"));
        useInternalAlarm = settings.getBoolean("useInternalAlarm", false);
        useRadioAlarmClock = settings.getBoolean("useRadioAlarmClock", false);
        dateFormat = settings.getString("dateFormat", getDefaultDateFormat());
        timeFormat = settings.getString("timeFormat", getDefaultTimeFormat());
        weatherCityID = settings.getString("weatherCityID", "");

        NOISE_AMPLITUDE_SLEEP *= sensitivity;
        NOISE_AMPLITUDE_WAKE  *= sensitivity;

        typeface = loadTypeface();
        weatherEntry = getWeatherEntry();
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

    private String getDefaultTimeFormat() {
        if ( is24HourFormat() ) {
            return "kk:mm";
        }
        return "h:mm";
    }

    public boolean is24HourFormat() {
        return android.text.format.DateFormat.is24HourFormat(mContext);
    }

    public long getDateAsLong(int year, int month, int day) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.MONTH, month - 1);
            calendar.set(Calendar.YEAR, year);
            return calendar.getTimeInMillis();
    }

    private Typeface loadTypeface() {
        int typeface = Integer.parseInt(settings.getString("typeface", "1"));
        String path = mapIntToTypefacePath(typeface);
        return Typeface.createFromAsset(mContext.getAssets(), path);
    }

    private String mapIntToTypefacePath(int typeface) {
        switch (typeface) {
            case 1: return "fonts/roboto_regular.ttf";
            case 2: return "fonts/roboto_light.ttf";
            case 3: return "fonts/roboto_condensed.ttf";
            case 4: return "fonts/roboto_thin.ttf";
            case 5: return "fonts/roboto_medium.ttf";
            case 6: return "fonts/7segment.ttf";
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

    public void setAlarmTime(long alarmTime) {
        nextAlarmTime = alarmTime;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putLong("nextAlarmTime", alarmTime);
        prefEditor.commit();
    }

    public Calendar getAlarmTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nextAlarmTime);
        return calendar;
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

    public void setScaleClock(float factor, int orientation) {
        SharedPreferences.Editor prefEditor = settings.edit();
        switch(orientation){
            case Configuration.ORIENTATION_LANDSCAPE:
                scaleClockLandscape = factor;
                prefEditor.putFloat("scaleClockLandscape", factor);
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                scaleClockPortrait = factor;
                prefEditor.putFloat("scaleClockPortrait", factor);
                break;
            default:
                scaleClock = factor;
                prefEditor.putFloat("scaleClock", factor);
                break;
        }
        prefEditor.commit();
    }

    public float getScaleClock(int orientation) {
        switch(orientation){
            case Configuration.ORIENTATION_LANDSCAPE:
                return scaleClockLandscape;
            case Configuration.ORIENTATION_PORTRAIT:
                return scaleClockPortrait;
            default:
                return scaleClock;
        }
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

    public void setFetchWeatherData(boolean on) {
        showWeather = on;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putBoolean("showWeather", on);
        prefEditor.commit();
    }

    public void setLocation(Location location) {
        location_lon = (float) location.getLongitude();
        location_lat = (float) location.getLatitude();
        location_time = location.getTime();
        location_provider = location.getProvider();
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putFloat("location_lon", (float) location.getLongitude());
        prefEditor.putFloat("location_lat", (float) location.getLatitude());
        prefEditor.putLong("location_time", location.getTime());
        prefEditor.putString("location_provider", location.getProvider());
        prefEditor.commit();
    }

    public Location getLocation() {
        Location l = new Location(location_provider);
        l.setLongitude(location_lon);
        l.setLatitude(location_lat);
        l.setTime(location_time);
        return l;
    }

    public void setWeatherEntry(WeatherEntry entry) {
        this.weatherEntry = entry;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putFloat("weather_lon", entry.lon);
        prefEditor.putFloat("weather_lat", entry.lat);
        prefEditor.putLong("weather_time", entry.timestamp);
        prefEditor.putLong("weather_sunrise_time", entry.sunriseTime);
        prefEditor.putLong("weather_sunset_time", entry.sunsetTime);
        prefEditor.putString("weather_icon", entry.weatherIcon);
        prefEditor.putString("weather_city_name", entry.cityName);
        prefEditor.putInt("weather_city_id", entry.cityID);
        prefEditor.putFloat("weather_temperature", (float) entry.temperature);
        prefEditor.putFloat("weather_wind_speed", (float) entry.windSpeed);
        prefEditor.putInt("weather_wind_direction", entry.windDirection);
        prefEditor.commit();
    }

    public WeatherEntry getWeatherEntry() {
        this.weatherEntry = new WeatherEntry();
        this.weatherEntry.timestamp = settings.getLong("weather_time", -1L);
        if ( this.weatherEntry.timestamp > -1L ) {
            this.weatherEntry.lon = settings.getFloat("weather_lon", this.weatherEntry.lon);
            this.weatherEntry.lat = settings.getFloat("weather_lat", this.weatherEntry.lat);
            this.weatherEntry.sunriseTime = settings.getLong("weather_sunrise_time", this.weatherEntry.sunriseTime);
            this.weatherEntry.sunsetTime = settings.getLong("weather_sunset_time", this.weatherEntry.sunsetTime);
            this.weatherEntry.weatherIcon = settings.getString("weather_icon", this.weatherEntry.weatherIcon);
            this.weatherEntry.cityName = settings.getString("weather_city_name", this.weatherEntry.cityName);
            this.weatherEntry.cityID = settings.getInt("weather_city_id", this.weatherEntry.cityID);
            this.weatherEntry.temperature = settings.getFloat("weather_temperature", (float) this.weatherEntry.temperature);
            this.weatherEntry.windSpeed = settings.getFloat("weather_wind_speed", (float) this.weatherEntry.windSpeed);
            this.weatherEntry.windDirection = settings.getInt("weather_wind_direction", this.weatherEntry.windDirection);
        }
        return this.weatherEntry;
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
