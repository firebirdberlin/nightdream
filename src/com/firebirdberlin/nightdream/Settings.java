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
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.firebirdberlin.nightdream.events.OnSleepTimeChanged;
import com.firebirdberlin.nightdream.models.BatteryValue;
import com.firebirdberlin.nightdream.models.FontCache;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.ui.ClockLayout;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;
import com.firebirdberlin.radiostreamapi.models.FavoriteRadioStations;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static android.text.format.DateFormat.getBestDateTimePattern;

public class Settings {
    public static final String PREFS_KEY = "NightDream preferences";
    public String getWeatherProviderString() {
        return settings.getString("weatherProvider", "0");
    }
    public WeatherProvider getWeatherProvider() {
        String provider = settings.getString("weatherProvider", "0");
        switch (provider) {
            case "0":
            default:
                return WeatherProvider.OPEN_WEATHER_MAP;
            case "1":
                return WeatherProvider.DARK_SKY;
        }
    }

    public final static int BACKGROUND_BLACK = 1;
    public final static int BACKGROUND_GRADIENT = 2;
    public final static int BACKGROUND_IMAGE = 3;
    public final static int BACKGROUND_SLIDESHOW = 4;

    public final static int SLIDESHOW_STYLE_CROPPED = 1;
    public final static int SLIDESHOW_STYLE_CENTER = 2;
    public final static int SLIDESHOW_STYLE_ANIMATED = 3;

    public final static int NIGHT_MODE_ACTIVATION_MANUAL = 0;
    public final static int NIGHT_MODE_ACTIVATION_AUTOMATIC = 1;
    public final static int NIGHT_MODE_ACTIVATION_SCHEDULED = 2;
    private final static String TAG = "NightDream.Settings";
    private static final String FAVORITE_RADIO_STATIONS_KEY = "favoriteRadioStations";
    public boolean activateDoNotDisturb = false;
    public boolean activateDoNotDisturbAllowPriority = true;
    public boolean alwaysOnStartWithLockedUI = false;
    public boolean allow_screen_off = false;
    public boolean alarmFadeIn = true;
    boolean autostartForNotifications = true;
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
    boolean showBatteryWarning = true;
    public boolean showDate = true;
    public boolean showDivider = false;
    public boolean showWeather = false;
    public boolean showApparentTemperature = false;
    public boolean showTemperature = true;
    public boolean showWindSpeed = false;
    public boolean useDeviceLock = false;
    public boolean stopAlarmOnTap = true;
    public boolean stopAlarmOnLongPress = false;
    public boolean useAlarmSwipeGesture = false;
    public boolean showAlarmsPersistently = false;
    public boolean isUIlocked = false;
    public boolean radioStreamMusicIsAllowedForAlarms = false;
    private boolean radioStreamActivateWiFi = false;
    public boolean radioStreamRequireWiFi = true;
    public boolean scheduledAutoStartEnabled = false;
    public boolean scheduledAutoStartChargerRequired = true;
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
    public float scaleClockPortrait = -1.f;
    public float scaleClockLandscape = -1.f;
    public int alarmVolume = 3;
    public int alarmVolumeReductionPercent = 0;
    public int alarmFadeInDurationSeconds = 10;
    private int background_mode = 1;
    public int slideshowStyle = 1;
    public boolean background_mode_auto_color = true;
    int batteryTimeout = -1;
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
    public int scheduledAutoStartTimeRangeStartInMinutes = -1;
    public int scheduledAutoStartTimeRangeEndInMinutes = -1;
    public int nightModeTimeRangeStartInMinutes = -1;
    public int nightModeTimeRangeEndInMinutes = -1;
    public int nextAlarmTimeMinutes = 0;
    public int sleepTimeInMinutesDefaultValue = 30;
    public long lastReviewRequestTime = 0L;
    private long nextAlwaysOnTime = 0L;
    public long sleepTimeInMillis = 0L; // 5 min
    public long snoozeTimeInMillis = 300000; // 5 min
    public long autoSnoozeTimeInMillis = 120000; // 2 min
    public String AlarmToneUri = "";
    public String AlarmToneName = "";
    public String backgroundImageURI = "";
    public Typeface typeface;
    public String dateFormat;
    public String timeFormat;
    public WeatherEntry weatherEntry;
    public String weatherCityID;
    public Set<Integer> autostartWeekdays;
    public Set<Integer> alwaysOnWeekdays;
    public Set<Integer> scheduledAutostartWeekdays;
    public double NOISE_AMPLITUDE_WAKE  = Config.NOISE_AMPLITUDE_WAKE;
    public double NOISE_AMPLITUDE_SLEEP = Config.NOISE_AMPLITUDE_SLEEP;
    public boolean purchasedWeatherData = false;
    private boolean purchasedDonation = false;
    private Context mContext;
    SharedPreferences settings;
    public int clockLayout;
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

    public void reload() {
        AlarmToneUri = settings.getString("AlarmToneUri",
                android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI.toString()
        );
        AlarmToneName = settings.getString("AlarmToneName", "");
        activateDoNotDisturb = settings.getBoolean("activateDoNotDisturb", false);
        activateDoNotDisturbAllowPriority = settings.getBoolean("activateDoNotDisturbAllowPriority", true);
        allow_screen_off = settings.getBoolean("allow_screen_off", false);
        alwaysOnStartWithLockedUI = settings.getBoolean("alwaysOnStartWithLockedUI", false);
        reactivate_screen_on_noise = settings.getBoolean("reactivate_screen_on_noise", false);
        alarmVolume = settings.getInt("alarmVolume", 3);
        alarmVolumeReductionPercent = settings.getInt("alarmVolumeReductionPercent", 0);
        alarmFadeIn = settings.getBoolean("alarmFadeIn", true);
        alarmFadeInDurationSeconds = settings.getInt("alarmFadeInDurationSeconds", 10);
        ambientNoiseDetection = settings.getBoolean("ambientNoiseDetection", false);
        autostartForNotifications = settings.getBoolean("autostartForNotifications", false);
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
        scheduledAutoStartTimeRangeEndInMinutes = settings.getInt("scheduledAutoStartTimeRange_end_minutes", -1);
        scheduledAutoStartTimeRangeStartInMinutes = settings.getInt("scheduledAutoStartTimeRange_start_minutes", -1);
        background_mode = Integer.parseInt(settings.getString("backgroundMode", "1"));
        slideshowStyle = Integer.parseInt(settings.getString("slideshowStyle", "1"));
        background_mode_auto_color = settings.getBoolean("autoAccentColor", true);
        handle_power = settings.getBoolean("handle_power", false);
        handle_power_disconnection = settings.getBoolean("handle_power_disconnection", false);
        handle_power_disconnection_at_time_range_end =
                settings.getBoolean("handle_power_disconnection_at_time_range_end", true);
        hideBackgroundImage = settings.getBoolean("hideBackgroundImage", true);
        scheduledAutoStartEnabled = settings.getBoolean("scheduledAutoStartEnabled", false);
        scheduledAutoStartChargerRequired = settings.getBoolean("scheduledAutoStartChargerRequired", true);
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
        nextAlwaysOnTime = settings.getLong("nextAlwaysOnTime", 0L);
        purchasedWeatherData = settings.getBoolean("purchasedWeatherData", false);
        if (Utility.isEmulator()) purchasedWeatherData = true;
        purchasedDonation = settings.getBoolean("purchasedDonation", false);
        reactivate_on_ambient_light_value = settings.getInt("reactivate_on_ambient_light_value", reactivate_on_ambient_light_value);
        persistentBatteryValueWhileCharging = settings.getBoolean("persistentBatteryValueWhileCharging", true);
        restless_mode = settings.getBoolean("restlessMode", true);
        final String defaultSecondaryColorString = "#C2C2C2";
        secondaryColor = settings.getInt("secondaryColor", Color.parseColor(defaultSecondaryColorString));
        secondaryColorNight = settings.getInt("secondaryColorNight", Color.parseColor(defaultSecondaryColorString));
        scaleClock = settings.getFloat("scaleClock", 1.f);
        scaleClockPortrait = settings.getFloat("scaleClockPortrait", -1.f);
        scaleClockLandscape = settings.getFloat("scaleClockLandscape", -1.f);
        sensitivity = 10-settings.getInt("NoiseSensitivity", 4);
        showBatteryWarning = settings.getBoolean("showBatteryWarning", true);
        showDate = settings.getBoolean("showDate", true);
        showDivider = settings.getBoolean("showDivider", false);
        showWeather = settings.getBoolean("showWeather", false);
        showApparentTemperature = settings.getBoolean("showApparentTemperature", false);
        showTemperature = settings.getBoolean("showTemperature", true);
        showWindSpeed = settings.getBoolean("showWindSpeed", false);
        snoozeTimeInMillis =  60000L * settings.getInt("snoozeTimeInMinutes", 5);
        autoSnoozeTimeInMillis =  60000L * settings.getInt("autoSnoozeTimeInMinutes", 2);

        String time = settings.getString("sleepTimeInMinutesDefaultValue", "30");
        sleepTimeInMinutesDefaultValue = time.isEmpty() ? -1 : Integer.valueOf(time);
        sleepTimeInMillis = settings.getLong("sleepTimeInMillis", 0L);

        speedUnit = Integer.parseInt(settings.getString("speedUnit", "1"));
        screenOrientation = Integer.parseInt(settings.getString("screenOrientation", "-1"));
        temperatureUnit = Integer.parseInt(settings.getString("temperatureUnit", "1"));
        useDeviceLock = settings.getBoolean("useDeviceLock", false);
        nightModeActivationMode = Integer.parseInt(settings.getString("nightModeActivationMode", "0"));
        useAlarmSwipeGesture = settings.getBoolean("useAlarmSwipeGesture", false);
        showAlarmsPersistently = settings.getBoolean("showAlarmsPersistently", false);
        radioStreamMusicIsAllowedForAlarms = settings.getBoolean("radioStreamMusicIsAllowedForAlarms", false);
        radioStreamActivateWiFi = settings.getBoolean("radioStreamActivateWiFi", false);
        radioStreamRequireWiFi = settings.getBoolean("radioStreamRequireWiFi", false);
        isUIlocked = settings.getBoolean("isUIlocked", false);
        dateFormat = settings.getString("dateFormat", getDefaultDateFormat());
        timeFormat = settings.getString("timeFormat", getDefaultTimeFormat());
        weatherCityID = settings.getString("weatherCityID", "");
        batteryTimeout = Integer.parseInt(settings.getString("batteryTimeout", "-1"));

        NOISE_AMPLITUDE_SLEEP *= sensitivity;
        NOISE_AMPLITUDE_WAKE  *= sensitivity;

        typeface = loadTypeface();
        weatherEntry = getWeatherEntry();

        HashSet<String> defaultOptions = new HashSet<>();
        defaultOptions.addAll(
                Arrays.asList(
                        mContext.getResources().getStringArray(R.array.optionsStopAlarmsValuesDefault)
                )
        );
        Set<String> optionsStopAlarms = settings.getStringSet("optionsStopAlarms", defaultOptions);
        stopAlarmOnTap = optionsStopAlarms.contains("0");
        stopAlarmOnLongPress = optionsStopAlarms.contains("1");
        if (!stopAlarmOnTap && !stopAlarmOnLongPress) {
            stopAlarmOnTap = true;
        }
        setPowerSourceOptions();
        Set<String> defaultWeekdays = new HashSet<>(Arrays.asList("1", "2", "3", "4", "5", "6", "7"));
        Set<String> autostartWeekdaysStrings = settings.getStringSet("autostartWeekdays", defaultWeekdays);
        autostartWeekdays = new HashSet<>();
        for (String weekday : autostartWeekdaysStrings) {
            autostartWeekdays.add(Integer.valueOf(weekday));
        }
        Set<String> alwaysOnWeekdaysStrings = settings.getStringSet("always_on_weekdays", defaultWeekdays);
        alwaysOnWeekdays = new HashSet<>();
        for (String weekday : alwaysOnWeekdaysStrings) {
            alwaysOnWeekdays.add(Integer.valueOf(weekday));
        }
        Set<String> scheduledAutostartWeekdaysStrings = settings.getStringSet("scheduledAutoStartWeekdays", defaultWeekdays);
        scheduledAutostartWeekdays = new HashSet<>();
        for (String weekday : scheduledAutostartWeekdaysStrings) {
            scheduledAutostartWeekdays.add(Integer.valueOf(weekday));
        }
    }

    private void setPowerSourceOptions() {

        SharedPreferences.Editor edit = settings.edit();
        HashSet<String> defaultOptions = new HashSet<>();
        defaultOptions.addAll(
                Arrays.asList(
                        mContext.getResources().getStringArray(R.array.optionsPowerSource)
                )
        );

        if (preferencesContain(
                "handle_power_ac",
                "handle_power_usb",
                "handle_power_wireless",
                "handle_power_desk",
                "handle_power_car"
        )) {
            boolean handle;
            handle = settings.getBoolean("handle_power_ac", false);
            if (handle) defaultOptions.add("0"); else defaultOptions.remove("0");
            handle = settings.getBoolean("handle_power_usb", false);
            if (handle) defaultOptions.add("1"); else defaultOptions.remove("1");
            handle = settings.getBoolean("handle_power_wireless", false);
            if (handle) defaultOptions.add("2"); else defaultOptions.remove("2");
            handle = settings.getBoolean("handle_power_desk", false);
            if (handle) defaultOptions.add("3"); else defaultOptions.remove("3");
            handle = settings.getBoolean("handle_power_car", false);
            if (handle) defaultOptions.add("4"); else defaultOptions.remove("4");
            edit.putStringSet("optionsPowerSource", defaultOptions);
            edit.remove("handle_power_ac");
            edit.remove("handle_power_usb");
            edit.remove("handle_power_wireless");
            edit.remove("handle_power_desk");
            edit.remove("handle_power_car");
            edit.apply();
        }

        Set<String> optionsPowerSource = settings.getStringSet("optionsPowerSource", defaultOptions);
        handle_power_ac = optionsPowerSource.contains("0");
        handle_power_usb = optionsPowerSource.contains("1");
        handle_power_wireless = optionsPowerSource.contains("2");
        handle_power_desk = optionsPowerSource.contains("3");
        handle_power_car = optionsPowerSource.contains("4");
    }

    private boolean preferencesContain(String... keys) {
        for (String key: keys) {
            if (settings.contains(key)) {
                return true;
            }
        }
        return false;
    }

    public void setShowDivider(boolean on) {
        showDivider = on;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putBoolean("showDivider", on);
        prefEditor.apply();
    }

    public void setTextureId(int textureId, int clockLayoutId) {
        SharedPreferences.Editor prefEditor = settings.edit();
        String key = getKeyForClockLayout("textureId", clockLayoutId);
        prefEditor.putInt(key, textureId);
        prefEditor.apply();
    }

    public int getTextureId(int clockLayoutId) {
        String key = getKeyForClockLayout("textureId", clockLayoutId);
        return settings.getInt(key, 0);
    }

    public int getTextureResId(int clockLayoutId) {
        int textureId = getTextureId(clockLayoutId);
        switch (textureId) {
            case 0:
            default:
                return -1;
            case 1:
                return R.drawable.gold;
            case 2:
                return R.drawable.copper;
            case 3:
                return R.drawable.rust;
        }
    }

    public void setGlowRadius(int radius, int clockLayoutId) {
        SharedPreferences.Editor prefEditor = settings.edit();
        String key = getKeyForClockLayout("glowRadius", clockLayoutId);
        prefEditor.putInt(key, radius);
        prefEditor.apply();
    }

    public int getGlowRadius(int clockLayoutId) {
        String key = getKeyForClockLayout("glowRadius", clockLayoutId);
        return settings.getInt(key, 0);
    }

    public int getClockLayoutID(boolean preview) {
        if (preview) {
            return clockLayout;
        } else if (clockLayout == ClockLayout.LAYOUT_ID_CALENDAR && !purchasedDonation) {
            return ClockLayout.LAYOUT_ID_CALENDAR;
        } else if (clockLayout >= 2 && !purchasedWeatherData) {
            return ClockLayout.LAYOUT_ID_DIGITAL;
        }

        return clockLayout;
    }

    public int getBackgroundMode() {
        if (Utility.isLowRamDevice(mContext)) {
            return BACKGROUND_BLACK;
        } else
        if (background_mode != BACKGROUND_SLIDESHOW || purchasedWeatherData) {
            return background_mode;
        }

        return BACKGROUND_BLACK;
    }

    public String getTimeFormat() {
        return timeFormat;
    }

    private String getDefaultTimeFormat() {
        boolean is24hr = android.text.format.DateFormat.is24HourFormat(mContext);
        return is24hr ? "HH:mm" : "h:mm";

    }

    public String getFullTimeFormat() {
        String timeFormat = getTimeFormat();
        if (!is24HourFormat()) {
            timeFormat += " a";
        }
        return timeFormat;
    }

    public boolean is24HourFormat() {
        return timeFormat.startsWith("H");
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
        if (bv == null) {
            return;
        }
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putLong("batteryReferenceTime", bv.time);
        prefEditor.putInt("batteryReferenceMethod", bv.level);
        prefEditor.putInt("batteryReferenceScale", bv.scale);
        prefEditor.putInt("batteryReferenceChargingMethod", bv.chargingMethod);
        prefEditor.putInt("batteryReferenceStatus", bv.status);
        prefEditor.apply();
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

    private Typeface loadTypeface() {
        final String ASSET_PATH = "file:///android_asset/";
        if (settings.contains("typeface")) {

            int typeface = Integer.parseInt(settings.getString("typeface", "8"));
            String path = mapIntToTypefacePath(typeface);
            if (path != null) {
                String name = path.substring(6);
                setFontUri(ASSET_PATH + path, name, clockLayout);
                SharedPreferences.Editor prefEditor = settings.edit();
                prefEditor.remove("typeface");
                prefEditor.apply();
                return FontCache.get(mContext, path);
            }
        }
        String path = getFontUri(clockLayout);
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
            case 8:
                return "fonts/dseg14classic.ttf";
            default: return null;
        }
    }

    public void setUILocked(boolean on) {
        isUIlocked = on;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putBoolean("isUIlocked", on);
        prefEditor.apply();
    }

    public void setFontUri(String uriString, String name, int clockLayoutId) {
        SharedPreferences.Editor prefEditor = settings.edit();
        String keyFontUri = getKeyForClockLayout("fontUri", clockLayoutId);
        String keyFontName = getKeyForClockLayout("fontName", clockLayoutId);
        if (uriString != null) {
            prefEditor.putString(keyFontUri, uriString);
            prefEditor.putString(keyFontName, name);
        } else {
            prefEditor.remove(keyFontUri);
            prefEditor.remove(keyFontName);
        }
        prefEditor.apply();
    }

    public String getFontUri(int clockLayoutId) {
        String key = getKeyForClockLayout("fontUri", clockLayoutId);
        String def = "file:///android_asset/fonts/dseg14classic.ttf";
        if ("fontUri:6".equals(key)) {
            def = "file:///android_asset/fonts/roboto_thin.ttf";
        }
        String fontUri = settings.getString(key, def);
        if ("file:///android_asset/fonts/7segment.ttf".equals(fontUri)) {
            return "file:///android_asset/fonts/7_segment_digital.ttf";
        }
        return fontUri;
    }

    public String getFontName(int clockLayoutId) {
        String key = getKeyForClockLayout("fontName", clockLayoutId);
        String def = mContext.getString(R.string.typeface_14_segment);
        if ("fontUri:6".equals(key)) {
            def = mContext.getString(R.string.typeface_roboto_thin);
        }
        return settings.getString(key, def);
    }

    private String getKeyForClockLayout(String key, int clockLayoutId) {
        if (clockLayoutId == ClockLayout.LAYOUT_ID_DIGITAL) {
            return key;
        }
        return String.format("%s:%d", key, clockLayoutId);
    }

    public void setBrightnessOffset(float value){
        dim_offset = value;

        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putFloat("dimOffset", value);
        prefEditor.putInt("brightness_offset", (int) (value * 100));
        // TODO This decreases the night mode brightness ... do we want to keep it ?
        if (!autoBrightness && value < nightModeBrightness) {
            nightModeBrightness = value;
            prefEditor.putFloat("nightModeBrightness", value);
        }
        prefEditor.apply();
    }

    public void setNightModeBrightness(float value){
        nightModeBrightness = value;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putFloat("nightModeBrightness", value);
        if (!autoBrightness && value > dim_offset) {
            dim_offset = value;
            prefEditor.putFloat("dimOffset", value);
            prefEditor.putInt("brightness_offset", (int) (value * 100));
        }
        prefEditor.apply();
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
        clearBackgroundImageCache();
        bgpath = uri;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putString("BackgroundImage", uri);
        prefEditor.apply();
    }

    public void setBackgroundImageURI(String uri) {
        clearBackgroundImageCache();
        backgroundImageURI = uri;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putString("backgroundImageURI", uri);
        prefEditor.apply();
    }

    public void clearBackgroundImageCache() {
        File cacheFile = new File(mContext.getCacheDir(), Config.backgroundImageCacheFilename);
        if (cacheFile.exists()) {
            cacheFile.delete();
        }
    }

    public void setLastReviewRequestTime(long reviewRequestTime) {
        lastReviewRequestTime = reviewRequestTime;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putLong("lastReviewRequestTime", lastReviewRequestTime);
        prefEditor.commit();
    }

    public void setSleepTimeInMinutesDefaultValue(int sleepTimeInMinutes) {
        sleepTimeInMinutesDefaultValue = sleepTimeInMinutes;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putString("sleepTimeInMinutesDefaultValue", String.valueOf(sleepTimeInMinutes));
        prefEditor.apply();
    }

    public void setSleepTimeInMillis(long sleepTimeInMillis) {
        this.sleepTimeInMillis = sleepTimeInMillis;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putLong("sleepTimeInMillis", sleepTimeInMillis);
        prefEditor.apply();
        EventBus.getDefault().post(new OnSleepTimeChanged(sleepTimeInMillis));
    }

    public void updateNextAlwaysOnTime() {
        long now = System.currentTimeMillis();
        nextAlwaysOnTime = now;
        nextAlwaysOnTime += Utility.getScreenOffTimeout(mContext);
        nextAlwaysOnTime += 20000;

        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putLong("nextAlwaysOnTime", nextAlwaysOnTime);
        prefEditor.commit();
    }

    public void deleteNextAlwaysOnTime() {
        nextAlwaysOnTime = 0L;

        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putLong("nextAlwaysOnTime", nextAlwaysOnTime);
        prefEditor.commit();
    }

    public boolean isAlwaysOnAllowed() {
        Calendar now = Calendar.getInstance();
        boolean isAllowed = true;
        Log.d(TAG, String.format("batteryTimeout : %d", batteryTimeout));
        if (batteryTimeout > 0 && nextAlwaysOnTime > 0L) {
            Calendar alwaysOnTime = Calendar.getInstance();
            alwaysOnTime.setTimeInMillis(nextAlwaysOnTime);
            isAllowed = now.after(alwaysOnTime);
        }
        return isAllowed;
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
        prefEditor.apply();
    }

    public boolean reactivateScreenOnNoise() {
        return reactivate_screen_on_noise && hasPermission(Manifest.permission.RECORD_AUDIO);
    }

    public void setReactivateScreenOnNoise(boolean on) {
        reactivate_screen_on_noise = on;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putBoolean("reactivate_screen_on_noise", on);
        prefEditor.apply();
    }

    public void setFetchWeatherData(boolean on) {
        showWeather = on;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putBoolean("showWeather", on);
        prefEditor.apply();
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
        prefEditor.apply();
    }

    public City getCityForWeather() {
        String json = settings.getString("weatherCityID_json", "");
        return City.fromJson(json);
    }

    void initWeatherAutoLocationEnabled() {
        if (!settings.contains("weatherAutoLocationEnabled") && showWeather) {
            City city = getCityForWeather();
            boolean on = (city == null || city.id == 0);
            setWeatherAutoLocationEnabled(on);
        }
    }

    void setWeatherAutoLocationEnabled(boolean on) {
        SharedPreferences.Editor edit = settings.edit();
        edit.putBoolean("weatherAutoLocationEnabled", on);
        edit.apply();
    }

    public boolean getWeatherAutoLocationEnabled() {
        return settings.getBoolean("weatherAutoLocationEnabled", false);
    }

    void setWeatherLocation(City city) {
        SharedPreferences.Editor prefEditor = settings.edit();
        if (city != null) {
            prefEditor.putString("weatherCityID", String.valueOf(city.id));
            prefEditor.putString("weatherCityID_name", city.name);
            prefEditor.putString("weatherCityID_json", city.toJson());
        } else {
            prefEditor.putString("weatherCityID", "");
            prefEditor.putString("weatherCityID_name", "");
            prefEditor.putString("weatherCityID_json", "");
        }
        prefEditor.apply();
    }

    public enum WeatherProvider {OPEN_WEATHER_MAP, DARK_SKY}

    public WeatherEntry getWeatherEntry() {
        this.weatherEntry = new WeatherEntry();
        this.weatherEntry.timestamp = settings.getLong("weather_time", -1L);
        this.weatherEntry.request_timestamp = settings.getLong("weather_request_time", -1L);
        if (this.weatherEntry.timestamp > -1L) {
            this.weatherEntry.lon = settings.getFloat("weather_lon", this.weatherEntry.lon);
            this.weatherEntry.lat = settings.getFloat("weather_lat", this.weatherEntry.lat);
            this.weatherEntry.sunriseTime = settings.getLong("weather_sunrise_time", this.weatherEntry.sunriseTime);
            this.weatherEntry.sunsetTime = settings.getLong("weather_sunset_time", this.weatherEntry.sunsetTime);
            this.weatherEntry.weatherIcon = settings.getString("weather_icon", this.weatherEntry.weatherIcon);
            this.weatherEntry.description = settings.getString("weather_description", this.weatherEntry.description);
            this.weatherEntry.cityName = settings.getString("weather_city_name", this.weatherEntry.cityName);
            this.weatherEntry.cityID = settings.getInt("weather_city_id", this.weatherEntry.cityID);
            this.weatherEntry.temperature = settings.getFloat("weather_temperature", (float) this.weatherEntry.temperature);
            this.weatherEntry.apparentTemperature = settings.getFloat("weather_felt_temperature", (float) this.weatherEntry.apparentTemperature);
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
        prefEditor.putLong("weather_request_time", entry.request_timestamp);
        prefEditor.putLong("weather_sunrise_time", entry.sunriseTime);
        prefEditor.putLong("weather_sunset_time", entry.sunsetTime);
        prefEditor.putString("weather_icon", entry.weatherIcon);
        prefEditor.putString("weather_city_name", entry.cityName);
        prefEditor.putString("weather_description", entry.description);
        prefEditor.putInt("weather_city_id", entry.cityID);
        prefEditor.putFloat("weather_temperature", (float) entry.temperature);
        prefEditor.putFloat("weather_felt_temperature", (float) entry.apparentTemperature);
        prefEditor.putFloat("weather_wind_speed", (float) entry.windSpeed);
        prefEditor.putInt("weather_wind_direction", entry.windDirection);
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
        return station;
    }

    public FavoriteRadioStations getFavoriteRadioStations() {
        return getFavoriteRadioStations(settings);
    }

    static void setFavoriteWeatherLocations(Context context, List<City> cities) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = preferences.edit();
        while (cities.size() > 5) {
            cities.remove(0);
        }
        for (int i = 0; i < 5; i++) {
            String key = String.format("favoriteWeatherLocation_%d", i);
            City city = (i < cities.size()) ? cities.get(i) : null;
            if (city == null) {
                edit.putString(key, "");
            } else {
                edit.putString(key, city.toJson());
            }
        }
        edit.apply();
    }

    static ArrayList<City> getFavoriteWeatherLocations(Context context) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);
        ArrayList<City> cities = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String key = String.format("favoriteWeatherLocation_%d", i);
            String json = preferences.getString(key, "");
            if (!json.isEmpty()) {
                cities.add(City.fromJson(json));
            }
        }
        return cities;
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

    public static void setDefaultAlarmTone(Context context, String uriString) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putString("defaultAlarmTone", uriString);
        edit.apply();
    }

    public static void setDefaultAlarmRadioStation(Context context, int stationIndex) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putInt("defaultAlarmRadioStation", stationIndex);
        edit.apply();
    }

    static int getDefaultRadioStation(Context context) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);
        if (preferences == null) {
            return -1;
        }
        return preferences.getInt("defaultAlarmRadioStation", -1);
    }

    public static void setLastActiveRadioStation(Context context, int stationIndex) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putInt("lastActiveRadioStation", stationIndex);
        edit.apply();
    }

    public static int getLastActiveRadioStation(Context context) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);
        if (preferences == null) {
            return -1;
        }
        return preferences.getInt("lastActiveRadioStation", -1);
    }
    static String getDefaultAlarmTone(Context context) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);
        if (preferences == null) {
            return null;
        }
        return preferences.getString("defaultAlarmTone", null);
    }

    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        return context.getSharedPreferences("defaults", Context.MODE_PRIVATE);
    }

    public static boolean useNotificationStatusBar(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_KEY, 0);
        if (preferences == null) {
            return true;
        }
        return preferences.getBoolean("showNotificationsInStatusBar", true);
    }

    public static boolean groupSimilarNotifications(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_KEY, 0);
        if (preferences == null) {
            return false;
        }
        return preferences.getBoolean("groupSimilarNotifications", false);
    }
    public static int getMinNotificationImportance(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_KEY, 0);
        if (preferences == null) {
            return 3;
        }
        String val = preferences.getString("minNotificationImportance", "3");
        return Integer.valueOf(val);
    }

    public static void setFlashlightIsOn(Context context, boolean isOn) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putBoolean("flashlightIsOn", isOn);
        edit.apply();
    }

    public static boolean getFlashlightIsOn(Context context) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);
        if (preferences == null) {
            return false;
        }
        return preferences.getBoolean("flashlightIsOn", false);
    }

    public boolean getShallRadioStreamActivateWiFi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < 29) {
            return false;
        }
        return radioStreamActivateWiFi;
    }

    public static void storeWeatherDataPurchase(Context context, boolean weatherIsPurchased, boolean donationIsPurchased) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_KEY, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("purchasedWeatherData", weatherIsPurchased);
        editor.putBoolean("purchasedDonation", donationIsPurchased);
        editor.apply();
        Log.i(TAG, String.format("purchasedWeatherData = %b", weatherIsPurchased));
    }
}
