package com.firebirdberlin.nightdream;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.firebirdberlin.nightdream.models.BatteryValue;
import com.firebirdberlin.nightdream.models.FontCache;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;
import com.firebirdberlin.radiostreamapi.models.FavoriteRadioStations;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

import org.json.JSONException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static android.text.format.DateFormat.getBestDateTimePattern;

public class Settings {
    public static final String PREFS_KEY = "NightDream preferences";
    public final static int BACKGROUND_BLACK = 1;
    public final static int BACKGROUND_GRADIENT = 2;
    public final static int BACKGROUND_IMAGE = 3;
    public final static int NIGHT_MODE_ACTIVATION_MANUAL = 0;
    public final static int NIGHT_MODE_ACTIVATION_AUTOMATIC = 1;
    public final static int NIGHT_MODE_ACTIVATION_SCHEDULED = 2;
    private final static String TAG = "NightDream.Settings";
    private static final String FAVORITE_RADIO_STATIONS_KEY = "favoriteRadioStations";
    public boolean allow_screen_off = false;
    public boolean alarmFadeIn = true;
    public boolean standbyEnabledWhileConnected = false;
    public boolean standbyEnabledWhileDisconnected = false;
    public boolean standbyEnabledWhileDisconnectedScreenUp = false;
    public boolean autoBrightness = false;
    public boolean clockLayoutMirrorText = false;
    public boolean doubleTapToFinish = false;
    public boolean handle_power = false;
    public boolean handle_power_disconnection = true;
    public boolean handle_power_disconnection_at_time_range_end = true;
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
    public boolean showDivider = true;
    public boolean showWeather = false;
    public boolean showTemperature = true;
    public boolean showWindSpeed = false;
    public boolean useDeviceLock = false;
    public boolean useInternalAlarm = true;
    public boolean useAlarmSwipeGesture = true;
    public boolean useRadioAlarmClock = false;
    public boolean isUIlocked = false;
    public boolean radioStreamMusicIsAllowedForAlarms = false;
    public boolean radioStreamActivateWiFi = false;
    public float dim_offset = 0.8f;
    public float nightModeBrightness = 0.f;
    public float maxBrightness = 1.f;
    public float maxBrightnessBattery = 1.f;
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
    public int nightModeActivationMode;
    public int reactivate_on_ambient_light_value = 30; // lux
    public int secondaryColor;
    public int secondaryColorNight;
    public int sensitivity = 1;
    public int temperatureUnit = WeatherEntry.CELSIUS;
    public int speedUnit = WeatherEntry.METERS_PER_SECOND;
    public int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    public int alwaysOnBatteryLevel = 0;
    public int alwaysOnTimeRangeStartInMinutes = -1;
    public int alwaysOnTimeRangeEndInMinutes = -1;
    public int autostartTimeRangeStartInMinutes = -1;
    public int autostartTimeRangeEndInMinutes = -1;
    public int nightModeTimeRangeStartInMinutes = -1;
    public int nightModeTimeRangeEndInMinutes = -1;
    public int nextAlarmTimeMinutes = 0;
    public long lastReviewRequestTime = 0L;
    public long snoozeTimeInMillis = 300000; // 5 min
    public int sleepTimeInMinutesDefaultValue = 30;
    public String AlarmToneUri = "";
    public String AlarmToneName = "";
    public String fontUri = "";
    public String fontName = "";
    public String radioStreamURL = "";
    public String radioStreamURLUI = "";
    public String backgroundImageURI = "";
    public Typeface typeface;
    public String dateFormat;
    public String timeFormat12h;
    public String timeFormat24h;
    public WeatherEntry weatherEntry;
    public String weatherCityID;
    public long lastWeatherRequestTime = -1L;
    public double NOISE_AMPLITUDE_WAKE  = Config.NOISE_AMPLITUDE_WAKE;
    public double NOISE_AMPLITUDE_SLEEP = Config.NOISE_AMPLITUDE_SLEEP;
    public boolean purchasedWeatherData = false;
    public boolean purchasedWebRadio = false;
    Context mContext;
    SharedPreferences settings;
    private int clockLayout;
    private boolean reactivate_screen_on_noise = false;
    private boolean ambientNoiseDetection;
    private String bgpath = "";

    public Settings(Context context){
        this.mContext = context;
        settings = context.getSharedPreferences(PREFS_KEY, 0);
        reload();
    }

    private static FavoriteRadioStations getFavoriteRadioStations(SharedPreferences preferences) {
        String json = preferences.getString(FAVORITE_RADIO_STATIONS_KEY, null);
        if (json != null) {
            try {
                FavoriteRadioStations stations = FavoriteRadioStations.fromJson(json);
                return stations;
            } catch (JSONException e) {
                Log.e(TAG, "error converting json to FavoriteRadioStations", e);
            }
        }
        return new FavoriteRadioStations();

    }

    private String getFontUri() {
        String fontUri = settings.getString("fontUri", "file:///android_asset/fonts/7_segment_digital.ttf");
        if ("file:///android_asset/fonts/7segment.ttf".equals(fontUri)) {
            return "file:///android_asset/fonts/7_segment_digital.ttf";
        }
        return fontUri;
    }

    public void reload() {
        AlarmToneUri = settings.getString("AlarmToneUri",
                android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI.toString());
        AlarmToneName = settings.getString("AlarmToneName", "");
        fontUri = getFontUri();
        fontName = settings.getString("fontName", "7-Segment Digital Font");
        allow_screen_off = settings.getBoolean("allow_screen_off", false);
        reactivate_screen_on_noise = settings.getBoolean("reactivate_screen_on_noise", false);
        alarmVolume = settings.getInt("alarmVolume", 3);
        alarmFadeIn = settings.getBoolean("alarmFadeIn", true);
        ambientNoiseDetection = settings.getBoolean("ambientNoiseDetection", false);
        standbyEnabledWhileConnected = settings.getBoolean("standbyEnabledWhileConnected", false);
        standbyEnabledWhileDisconnected = settings.getBoolean("standbyEnabledWhileDisconnected", false);
        standbyEnabledWhileDisconnectedScreenUp = settings.getBoolean("standbyEnabledWhileDisconnectedScreenUp", false);
        autoBrightness = settings.getBoolean("autoBrightness", false);
        clockLayoutMirrorText = settings.getBoolean("clockLayoutMirrorText", false);
        doubleTapToFinish = settings.getBoolean("doubleTapToFinish", false);
        alwaysOnTimeRangeStartInMinutes = settings.getInt("always_on_time_range_start_minutes", -1);
        alwaysOnTimeRangeEndInMinutes = settings.getInt("always_on_time_range_end_minutes", -1);
        alwaysOnBatteryLevel = settings.getInt("alwaysOnBatteryLevel", 0);

        autostartTimeRangeStartInMinutes = settings.getInt("autostart_time_range_start_minutes", -1);
        autostartTimeRangeEndInMinutes = settings.getInt("autostart_time_range_end_minutes", -1);
        background_mode = Integer.parseInt(settings.getString("backgroundMode", "1"));
        handle_power = settings.getBoolean("handle_power", false);
        handle_power_disconnection = settings.getBoolean("handle_power_disconnection", false);
        handle_power_disconnection_at_time_range_end =
                settings.getBoolean("handle_power_disconnection_at_time_range_end", true);
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

        initNextAlarmTime();
        nightModeBrightness = settings.getFloat("nightModeBrightness", nightModeBrightness);
        maxBrightness = 0.01f * settings.getInt("maxBrightness", 100);
        maxBrightnessBattery = 0.01f * settings.getInt("maxBrightnessBattery", 25);
        nightModeTimeRangeStartInMinutes = settings.getInt("nightmode_timerange_start_minutes", -1);
        nightModeTimeRangeEndInMinutes = settings.getInt("nightmode_timerange_end_minutes", -1);
        lastReviewRequestTime = settings.getLong("lastReviewRequestTime", 0L);
        purchasedWeatherData = settings.getBoolean("purchasedWeatherData", false);
        purchasedWebRadio = settings.getBoolean("purchasedWebRadio", false);
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
        showDivider = settings.getBoolean("showDivider", true);
        showWeather = settings.getBoolean("showWeather", false);
        showTemperature = settings.getBoolean("showTemperature", true);
        showWindSpeed = settings.getBoolean("showWindSpeed", false);
        snoozeTimeInMillis =  60000L * settings.getInt("snoozeTimeInMinutes", 5);

        Log.w(TAG, "fontUri2: " + fontUri);
        String time = settings.getString("sleepTimeInMinutesDefaultValue", "30");
        sleepTimeInMinutesDefaultValue = time.isEmpty() ? -1 : Integer.valueOf(time);

        speedUnit = Integer.parseInt(settings.getString("speedUnit", "1"));
        screenOrientation = Integer.parseInt(settings.getString("screenOrientation", "-1"));
        temperatureUnit = Integer.parseInt(settings.getString("temperatureUnit", "1"));
        useDeviceLock = settings.getBoolean("useDeviceLock", false);
        nightModeActivationMode = Integer.parseInt(settings.getString("nightModeActivationMode", "1"));
        useInternalAlarm = settings.getBoolean("useInternalAlarm", true);
        useAlarmSwipeGesture = settings.getBoolean("useAlarmSwipeGesture", true);
        useRadioAlarmClock = settings.getBoolean("useRadioAlarmClock", false);
        radioStreamMusicIsAllowedForAlarms = settings.getBoolean("radioStreamMusicIsAllowedForAlarms", false);
        radioStreamActivateWiFi = settings.getBoolean("radioStreamActivateWiFi", false);
        isUIlocked = settings.getBoolean("isUIlocked", false);
        dateFormat = settings.getString("dateFormat", getDefaultDateFormat());
        timeFormat12h = settings.getString("timeFormat_12h", "h:mm");
        timeFormat24h = settings.getString("timeFormat_24h", "HH:mm");
        weatherCityID = settings.getString("weatherCityID", "");
        lastWeatherRequestTime = settings.getLong("lastWeatherRequestTime", -1L);

        NOISE_AMPLITUDE_SLEEP *= sensitivity;
        NOISE_AMPLITUDE_WAKE  *= sensitivity;

        typeface = loadTypeface();
        weatherEntry = getWeatherEntry();
    }

    public void setShowDivider(boolean on) {
        showDivider = on;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putBoolean("showDivider", on);
        prefEditor.apply();
    }

    private boolean is24HourMode() {
        return android.text.format.DateFormat.is24HourFormat(mContext);
    }

    public int getClockLayoutID(boolean preview) {
        if (clockLayout >= 2 && !preview && !purchasedWeatherData) {
            return 1;
        }

        return clockLayout;
    }

    public String getTimeFormat() {
        if (is24HourMode() ) {
            return timeFormat24h;
        }
        return timeFormat12h;
    }

    public String getFullTimeFormat() {
        String timeFormat = getTimeFormat();
        if (!is24HourFormat()) {
            timeFormat += " a";
        }
        return timeFormat;
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

    public void removeBatteryReference() {
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.remove("batteryReferenceTime");
        prefEditor.remove("batteryReferenceMethod");
        prefEditor.remove("batteryReferenceScale");
        prefEditor.remove("batteryReferenceChargingMethod");
        prefEditor.remove("batteryReferenceStatus");
        prefEditor.commit();
    }

    public void disableSettingsNeedingBackgroundService() {
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putBoolean("handle_power", false);
        prefEditor.putBoolean("standbyEnabledWhileDisconnected", false);
        prefEditor.commit();
    }
    private String getDefaultDateFormat() {
        // Return the date format as used in versions previous to the version code 72
        if (Build.VERSION.SDK_INT >= 18){
            return getBestDateTimePattern(Locale.getDefault(), "EEEEddLLLL");

        }
        DateFormat formatter = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
        return ((SimpleDateFormat)formatter).toLocalizedPattern();
    }

    public boolean is24HourFormat() {
        return android.text.format.DateFormat.is24HourFormat(mContext);
    }

    private Typeface loadTypeface() {
        final String ASSET_PATH = "file:///android_asset/";
        if (settings.contains("typeface") ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {

            int typeface = Integer.parseInt(settings.getString("typeface", "6"));
            String path = mapIntToTypefacePath(typeface);
            if (path != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    String name = path.substring(6);
                    setFontUri(ASSET_PATH + path, name);
                    SharedPreferences.Editor prefEditor = settings.edit();
                    prefEditor.remove("typeface");
                    prefEditor.commit();
                }
                return FontCache.get(mContext, path);
            }
        }
        String path = getFontUri();
        return FontCache.get(mContext, path);
    }

    private String mapIntToTypefacePath(int typeface) {
        switch (typeface) {
            case 1:
            case 3:
            case 5:
                return "fonts/roboto_regular.ttf";
            case 2: return "fonts/roboto_light.ttf";
            case 4: return "fonts/roboto_thin.ttf";
            case 6:
                return "fonts/7_segment_digital.ttf";
            case 7:
                return "fonts/dancingscript_regular.ttf";
            default: return null;
        }
    }

    public void setUILocked(boolean on) {
        isUIlocked = on;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putBoolean("isUIlocked", on);
        prefEditor.apply();
    }

    public void setFontUri(String uriString, String name) {
        SharedPreferences.Editor prefEditor = settings.edit();
        fontUri = uriString;
        fontName = name;
        if (uriString != null) {
            prefEditor.putString("fontUri", uriString);
            prefEditor.putString("fontName", name);
        } else {
            prefEditor.remove("fontUri");
            prefEditor.remove("fontName");
        }
        prefEditor.commit();
    }

    public void setBrightnessOffset(float value){
        dim_offset = value;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putFloat("dimOffset", value);
        prefEditor.putInt("brightness_offset", (int) (value * 100));
        prefEditor.commit();
    }

    public void setNightModeBrightness(float value){
        nightModeBrightness = value;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putFloat("nightModeBrightness", value);
        prefEditor.commit();
    }

    public void setMinIlluminance(float value) {
        minIlluminance = value;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putFloat("minIlluminance", value);
        prefEditor.commit();
    }

    private void initNextAlarmTime() {
        long nextAlarmTime = settings.getLong("nextAlarmTime", 0L);
        nextAlarmTimeMinutes = 0;
        if (nextAlarmTime > 0L) {
            nextAlarmTimeMinutes = new SimpleTime(nextAlarmTime).toMinutes();
        } else {
            nextAlarmTimeMinutes = settings.getInt("nextAlarmTimeMinutes", 0);
        }
    }

    public Calendar getAlarmTime() {
        return new SimpleTime(nextAlarmTimeMinutes).getCalendar();
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

    public Location getLocation() {
        Location l = new Location(location_provider);
        l.setLongitude(location_lon);
        l.setLatitude(location_lat);
        l.setTime(location_time);
        return l;
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

    public String getValidCityID() {
        if (this.weatherCityID != null && !this.weatherCityID.isEmpty()) {
            return this.weatherCityID;
        } else if (this.weatherEntry != null && this.weatherEntry.cityID > 0) {
            return String.valueOf(this.weatherEntry.cityID);
        }
        return null;
    }

    public WeatherEntry getWeatherEntry() {
        this.weatherEntry = new WeatherEntry();
        this.weatherEntry.timestamp = settings.getLong("weather_time", -1L);
        if (this.weatherEntry.timestamp > -1L) {
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

    public void setLastWeatherRequestTime(long time) {
        this.lastWeatherRequestTime = time;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putLong("lastWeatherRequestTime", time);
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

    public RadioStation getFavoriteRadioStation(int radioStationIndex) {
        FavoriteRadioStations stations = getFavoriteRadioStations();

        RadioStation station = null;
        if (stations != null) {
             station = stations.get(radioStationIndex);
        }
        if (station == null && radioStationIndex == 0) {
            station = getLegacyRadioStation();
        }
        return station;
    }

    public boolean hasLegacyRadioStation() {
        String json = settings.getString("radioStreamURLUI_json", null);
        return json != null;
    }

    private RadioStation getLegacyRadioStation() {
        String json = settings.getString("radioStreamURLUI_json", null);
        if (json != null) {
            try {
                return RadioStation.fromJson(json);
            } catch (JSONException e) {
                Log.e(TAG, "error converting json to station", e);
            }
        }
        return null;
    }

    public FavoriteRadioStations getFavoriteRadioStations() {
        return getFavoriteRadioStations(settings);
    }

    private void setFavoriteRadioStations(FavoriteRadioStations stations) {
        try {
            String json = stations.toJson();
            Log.i(TAG, json);
            SharedPreferences.Editor prefEditor = settings.edit();
            prefEditor.putString(FAVORITE_RADIO_STATIONS_KEY, json);
            prefEditor.commit();
        } catch (JSONException e) {
            Log.e(TAG, "error converting FavoriteRadioStations to json", e);
        }
    }

    public void persistFavoriteRadioStation(RadioStation station, int stationIndex) {
        Log.i(TAG, "setPersistentFavoriteRadioStation index=" + stationIndex);
        FavoriteRadioStations stations = getFavoriteRadioStations(settings);
        stations.set(stationIndex, station);
        setFavoriteRadioStations(stations);
    }

    public void deleteFavoriteRadioStation(int stationIndex) {
        persistFavoriteRadioStation(null, stationIndex);
    }

    public void upgradeLegacyRadioStationToFirstFavoriteRadioStation() {
        FavoriteRadioStations stations = getFavoriteRadioStations();
        RadioStation firstRadioStation = null;
        if (stations != null) {
            firstRadioStation = stations.get(0);
        }
        if (firstRadioStation == null) {
            RadioStation legacyStation = getLegacyRadioStation();
            if (legacyStation != null) {
                persistFavoriteRadioStation(legacyStation, 0);
            }
        }
    }
}
