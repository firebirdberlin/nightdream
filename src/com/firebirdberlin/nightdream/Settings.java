package com.firebirdberlin.nightdream;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

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
    public final static int BACKGROUND_BLACK = 1;
    public final static int BACKGROUND_GRADIENT = 2;
    public final static int BACKGROUND_IMAGE = 3;
    public final static int BACKGROUND_SLIDESHOW = 4;
    public final static int BACKGROUND_TRANSPARENT = 5;
    public final static int SLIDESHOW_STYLE_CROPPED = 1;
    public final static int SLIDESHOW_STYLE_CENTER = 2;
    public final static int SLIDESHOW_STYLE_ANIMATED = 3;
    public final static int NIGHT_MODE_ACTIVATION_MANUAL = 0;
    public final static int NIGHT_MODE_ACTIVATION_AUTOMATIC = 1;
    public final static int NIGHT_MODE_ACTIVATION_SCHEDULED = 2;

    public final static int WEATHER_ICON_MODE_DEFAULT = 1;
    public final static int WEATHER_ICON_MODE_COLORED = 2;
    public final static int WEATHER_ICON_MODE_ANIMATED = 3;

    private final static String TAG = "NightDream.Settings";
    private static final String FAVORITE_RADIO_STATIONS_KEY = "favoriteRadioStations";
    public boolean activateDoNotDisturb = false;
    public boolean activateDoNotDisturbAllowPriority = true;
    public boolean alwaysOnStartWithLockedUI = false;
    public boolean allow_screen_off = false;
    public boolean alarmFadeIn = true;
    public boolean standbyEnabledWhileDisconnected = false;
    public boolean standbyEnabledWhileDisconnectedScreenUp = false;
    public boolean autoBrightness = false;
    public boolean clockLayoutMirrorText = false;
    public boolean doubleTapToFinish = false;
    public boolean speakTime = false;
    public boolean handle_power = false;
    public boolean handle_power_disconnection = true;
    public boolean handle_power_disconnection_at_time_range_end = true;
    public boolean handle_power_desk = false;
    public boolean handle_power_car = false;
    public boolean handle_power_ac = false;
    public boolean handle_power_usb = false;
    public boolean handle_power_wireless = false;
    public boolean hideBackgroundImage = false;
    public boolean muteRinger = false;
    public boolean persistentBatteryValueWhileCharging = true;
    public ScreenProtectionModes screenProtection = ScreenProtectionModes.MOVE;
    public boolean showDate = true;
    public boolean showWeather = false;
    public boolean showApparentTemperature = false;
    public boolean showTemperature = true;
    public boolean showWindSpeed = false;
    public boolean showPollen = false;
    public boolean useDeviceLock = false;
    public boolean stopAlarmOnTap = true;
    public boolean stopAlarmOnLongPress = false;
    public boolean useAlarmSwipeGesture = false;
    public boolean showAlarmsPersistently = false;
    public boolean isUIlocked = false;
    public boolean radioStreamMusicIsAllowedForAlarms = false;
    public boolean radioStreamRequireWiFi = true;
    public boolean scheduledAutoStartEnabled = false;
    public boolean scheduledAutoStartChargerRequired = true;
    public float dim_offset = 0.8f;
    public float nightModeBrightness = 0.f;
    public float maxBrightness = 1.f;
    public float maxBrightnessBattery = 1.f;
    public double location_lon = 0.d;
    public double location_lat = 0.d;
    public long location_time = -1L;
    public String location_provider = LocationManager.NETWORK_PROVIDER;
    public float minIlluminance = 15.f; // lux
    public float scaleClock = 1.f;
    public float scaleClockPortrait = -1.f;
    public float scaleClockLandscape = -1.f;

    public int alarmVolume = 3;
    public int alarmVolumeReductionPercent = 0;
    public int alarmFadeInDurationSeconds = 10;
    public int slideshowStyle = 1;
    public int backgroundImageDuration = 4;
    public int clockBackgroundTransparency = 4;
    public boolean fade_clock = false;
    public boolean background_zoomin = false;
    public boolean background_fadein = false;
    public boolean background_movein = false;
    public boolean background_exif = false;
    public int background_movein_style = 1;
    public int weather_icon = 1;
    public int background_filter = 1;
    public boolean background_mode_auto_color = false;
    public int clockColor;
    public int clockColorNight;
    public int nightModeActivationMode;
    public int reactivate_on_ambient_light_value = 30; // lux
    public int secondaryColor;
    public int secondaryColorNight;
    public String rssURL="";
    public String rssCharSet="";
    public int rssIntervalMode;
    public boolean rssEnabled = false;
    public long rssTickerSpeed=10L;
    public float rssTextSize;
    public int gradientStartColor;
    public int gradientEndColor;
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
    public long sleepTimeInMillis = 0L; // 5 min
    public long snoozeTimeInMillis = 300000; // 5 min
    public long autoSnoozeTimeInMillis = 120000; // 2 min
    public int autoSnoozeCycles = 20;
    public String AlarmToneUri = "";
    public String AlarmToneName = "";
    public String backgroundImageURI = "";
    public String dateFormat;
    public String timeFormat;
    public WeatherEntry weatherEntry;
    public String weatherCityID;
    public Set<Integer> autostartWeekdays;
    public Set<Integer> alwaysOnWeekdays;
    public Set<Integer> scheduledAutostartWeekdays;
    public double NOISE_AMPLITUDE_WAKE = Config.NOISE_AMPLITUDE_WAKE;
    public double NOISE_AMPLITUDE_SLEEP = Config.NOISE_AMPLITUDE_SLEEP;
    public boolean purchasedWeatherData = false;
    public int clockLayout;
    boolean autostartForNotifications = true;
    boolean showBatteryWarning = true;
    int batteryTimeout = 5;
    SharedPreferences settings;
    private boolean purchasedDonation = false;
    private boolean radioStreamActivateWiFi = false;
    private int background_mode = BACKGROUND_BLACK;
    private long nextAlwaysOnTime = 0L;
    private Context mContext;
    private boolean reactivate_screen_on_noise = false;
    private boolean ambientNoiseDetection;
    private String bgpath = "";

    public Settings(Context context) {
        this.mContext = context;
        settings = context.getSharedPreferences(PREFS_KEY, 0);
        reload();
    }

    public String getString(String key) {
        return settings.getString(key, null);
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

    public static long getSnoozeTimeMillis(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_KEY, 0);
        return 60000L * preferences.getInt("snoozeTimeInMinutes", 5);
    }

    public static int getAutoSnoozeCycles(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_KEY, 0);
        return preferences.getInt("autoSnoozeCycles", 20);
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

    public static int getDefaultRadioStation(Context context) {
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

    public static String getDefaultAlarmTone(Context context) {
        SharedPreferences preferences = getDefaultSharedPreferences(context);
        if (preferences == null) {
            return null;
        }
        return preferences.getString("defaultAlarmTone", null);
    }

    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        return context.getSharedPreferences("defaults", Context.MODE_PRIVATE);
    }

    public static boolean showNotification(Context context) {
        Resources res = context.getResources();
        boolean enabled = res.getBoolean(R.bool.use_NotificationListenerService);
        SharedPreferences preferences = context.getSharedPreferences(PREFS_KEY, 0);
        if (preferences == null) {
            return enabled;
        }
        return enabled && preferences.getBoolean("showNotification", true);
    }

    public static boolean showNotificationPreview(Context context) {
        Resources res = context.getResources();
        boolean enabled = res.getBoolean(R.bool.use_NotificationListenerService);
        SharedPreferences preferences = context.getSharedPreferences(PREFS_KEY, 0);
        if (preferences == null) {
            return false;
        }
        return enabled && preferences.getBoolean("showNotificationPreview", false);
    }

    public static boolean showMediaStyleNotification(Context context) {
        Resources res = context.getResources();
        boolean enabled = res.getBoolean(R.bool.use_NotificationListenerService);
        SharedPreferences preferences = context.getSharedPreferences(PREFS_KEY, 0);
        if (preferences == null) {
            return false;
        }
        return enabled && preferences.getBoolean("showMediaStyleNotification", false);
    }

    public static boolean useNotificationStatusBar(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_KEY, 0);
        if (preferences == null) {
            return true;
        }
        return preferences.getBoolean("showNotificationsInStatusBar", true);
    }

    public static int getNotificationContainerResourceId(Context context) {
        return Settings.useNotificationStatusBar(context) ?  R.id.notificationstatusbar: R.id.notificationbar;
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

    public static void storeWeatherDataPurchase(Context context, boolean weatherIsPurchased, boolean donationIsPurchased) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_KEY, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("purchasedWeatherData", weatherIsPurchased);
        editor.putBoolean("purchasedDonation", donationIsPurchased);
        editor.apply();
    }

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
            case "2":
                return WeatherProvider.BRIGHT_SKY;
            case "3":
                return WeatherProvider.MET_NO;

        }
    }

    public void reload() {
        AlarmToneUri = settings.getString(
                "AlarmToneUri",
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
        speakTime = settings.getBoolean("speakTime", false);
        alwaysOnTimeRangeStartInMinutes = settings.getInt("always_on_time_range_start_minutes", -1);
        alwaysOnTimeRangeEndInMinutes = settings.getInt("always_on_time_range_end_minutes", -1);
        alwaysOnBatteryLevel = settings.getInt("alwaysOnBatteryLevel", 0);

        autostartTimeRangeStartInMinutes = settings.getInt("autostart_time_range_start_minutes", -1);
        autostartTimeRangeEndInMinutes = settings.getInt("autostart_time_range_end_minutes", -1);
        scheduledAutoStartTimeRangeEndInMinutes = settings.getInt("scheduledAutoStartTimeRange_end_minutes", -1);
        scheduledAutoStartTimeRangeStartInMinutes = settings.getInt("scheduledAutoStartTimeRange_start_minutes", -1);
        background_mode = Integer.parseInt(settings.getString("backgroundMode", "1"));
        slideshowStyle = Integer.parseInt(settings.getString("slideshowStyle", "1"));
        backgroundImageDuration = Integer.parseInt(settings.getString("backgroundImageDuration", "4"));
        clockBackgroundTransparency = 255 - settings.getInt("clockBackgroundTransparency", 100);
        background_fadein = getBackgroundImageFadeIn();
        background_zoomin = getBackgroundImageZoomIn();
        background_movein = getBackgroundImageMoveIn();
        background_movein_style = Integer.parseInt(settings.getString("backgroundMovein", "1"));
        background_filter = Integer.parseInt(settings.getString("backgroundImageFilter", "1"));
        background_exif = settings.getBoolean("backgroundEXIF", false);
        fade_clock = settings.getBoolean("fadeClock", false);
        background_mode_auto_color = settings.getBoolean("autoAccentColor", true);
        rssURL = settings.getString("rssURL", "");
        rssCharSet = settings.getString("rssCharSetMode", "utf-8");
        rssEnabled = settings.getBoolean("enableRSS", false);
        rssIntervalMode = Integer.parseInt(settings.getString("rssIntervalMode", "60"));
        rssTickerSpeed = Long.parseLong(settings.getString("rssTickerSpeed", "10"));
        rssTextSize = Float.parseFloat(settings.getString("rssTextSizeMode", "0"));
        handle_power = settings.getBoolean("handle_power", false);
        handle_power_disconnection = settings.getBoolean("handle_power_disconnection", false);
        handle_power_disconnection_at_time_range_end =
                settings.getBoolean("handle_power_disconnection_at_time_range_end", true);
        hideBackgroundImage = settings.getBoolean("hideBackgroundImage", false);
        scheduledAutoStartEnabled = settings.getBoolean("scheduledAutoStartEnabled", false);
        scheduledAutoStartChargerRequired = settings.getBoolean("scheduledAutoStartChargerRequired", true);
        bgpath = settings.getString("BackgroundImage", "");
        backgroundImageURI = settings.getString("backgroundImageURI", "");
        final String defaultColorString = "#33B5E5";
        clockColor = settings.getInt("clockColor", Color.parseColor(defaultColorString));
        clockColorNight = settings.getInt("primaryColorNight", Color.parseColor(defaultColorString));
        clockLayout = Integer.parseInt(settings.getString("clockLayout", "0"));
        dim_offset = settings.getFloat("dimOffset", dim_offset);
        location_lat = getDouble("location_lat", 0.d);
        location_lon = getDouble("location_lon", 0.d);
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
        screenProtection = getScreenProtection();
        final String defaultSecondaryColorString = "#C2C2C2";
        secondaryColor = settings.getInt("secondaryColor", Color.parseColor(defaultSecondaryColorString));
        secondaryColorNight = settings.getInt("secondaryColorNight", Color.parseColor(defaultSecondaryColorString));
        gradientStartColor = settings.getInt("gradientStartColor", Color.parseColor("#000000"));
        gradientEndColor = settings.getInt("gradientEndColor", Color.parseColor("#303030"));
        scaleClock = settings.getFloat("scaleClock", 1.f);
        scaleClockPortrait = settings.getFloat("scaleClockPortrait", -1.f);
        scaleClockLandscape = settings.getFloat("scaleClockLandscape", -1.f);
        sensitivity = 10 - settings.getInt("NoiseSensitivity", 4);

        showBatteryWarning = settings.getBoolean("showBatteryWarning", true);
        showDate = settings.getBoolean("showDate", true);
        showWeather = settings.getBoolean("showWeather", false);
        showApparentTemperature = settings.getBoolean("showApparentTemperature", false);
        showTemperature = settings.getBoolean("showTemperature", true);
        showWindSpeed = settings.getBoolean("showWindSpeed", false);
        showPollen = settings.getBoolean("showPollen", false);
        snoozeTimeInMillis = 60000L * settings.getInt("snoozeTimeInMinutes", 5);
        autoSnoozeTimeInMillis = 60000L * settings.getInt("autoSnoozeTimeInMinutes", 2);
        autoSnoozeCycles = settings.getInt("autoSnoozeCycles", 20);

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
        weather_icon = Integer.parseInt(settings.getString("weatherIconMode", "1"));
        batteryTimeout = getBatteryTimeoutMinutes();

        NOISE_AMPLITUDE_SLEEP *= sensitivity;
        NOISE_AMPLITUDE_WAKE *= sensitivity;

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

    int getBatteryTimeoutMinutes() {
        return Integer.parseInt(settings.getString("batteryTimeout", "5"));
    }

    ScreenProtectionModes getScreenProtection() {
        if (settings.contains("restlessMode") && !settings.getBoolean("restlessMode", true)) {
            settings.edit().putString("screenProtection", "0").apply();
            settings.edit().remove("restlessMode").apply();
        }

        int mode = Integer.parseInt(settings.getString("screenProtection", "1"));
        switch (mode) {
            case 0:
                return ScreenProtectionModes.NONE;
            case 1:
            default:
                return ScreenProtectionModes.MOVE;
            case 2:
                return ScreenProtectionModes.FADE;
        }
    }

    boolean getBackgroundImageZoomIn() {
        boolean on = settings.getBoolean("backgroundImageZoomIn", false);
        if (slideshowStyle == SLIDESHOW_STYLE_ANIMATED) {
            on = true;
            setBackgroundZoomIn(true);
            setSlideShowStyle(SLIDESHOW_STYLE_CENTER);
        }
        return on;
    }

    boolean getBackgroundImageFadeIn() {
        return settings.getBoolean("backgroundImageFadeIn", false);
    }

    boolean getBackgroundImageMoveIn() {
        return settings.getBoolean("backgroundImageMoveIn", false);
    }

    public void setSlideShowStyle(int status) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("slideshowStyle", status);
        editor.apply();
    }

    public void setBackgroundZoomIn(boolean status) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("zoominBackgroundImage", status);
        editor.apply();
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
            if (handle) defaultOptions.add("0");
            else defaultOptions.remove("0");
            handle = settings.getBoolean("handle_power_usb", false);
            if (handle) defaultOptions.add("1");
            else defaultOptions.remove("1");
            handle = settings.getBoolean("handle_power_wireless", false);
            if (handle) defaultOptions.add("2");
            else defaultOptions.remove("2");
            handle = settings.getBoolean("handle_power_desk", false);
            if (handle) defaultOptions.add("3");
            else defaultOptions.remove("3");
            handle = settings.getBoolean("handle_power_car", false);
            if (handle) defaultOptions.add("4");
            else defaultOptions.remove("4");
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
        for (String key : keys) {
            if (settings.contains(key)) {
                return true;
            }
        }
        return false;
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

    public void setWeatherIconSizeFactor(int radius, int clockLayoutId) {
        String key = getKeyForClockLayout("weatherIconSizeFactor", clockLayoutId);
        settings.edit().putInt(key, radius).apply();
    }

    public int getWeatherIconSizeFactor(int clockLayoutId) {
        String key = getKeyForClockLayout("weatherIconSizeFactor", clockLayoutId);
        return settings.getInt(key, 4);
    }

    public void setShowDivider(boolean showDivider, int clockLayoutId) {
        SharedPreferences.Editor prefEditor = settings.edit();
        String key = getKeyForClockLayout("showDivider", clockLayoutId);
        prefEditor.putBoolean(key, showDivider);
        prefEditor.apply();
    }

    public boolean getShowDivider(int clockLayoutId) {
        String key = getKeyForClockLayout("showDivider", clockLayoutId);
        return settings.getBoolean(key, false);
    }

    public void setShowSeconds(boolean showSeconds, int clockLayoutId) {
        SharedPreferences.Editor prefEditor = settings.edit();
        String key = getKeyForClockLayout("showSeconds", clockLayoutId);
        prefEditor.putBoolean(key, showSeconds);
        prefEditor.apply();
    }

    public boolean getShowSeconds(int clockLayoutId) {
        String key = getKeyForClockLayout("showSeconds", clockLayoutId);
        return settings.getBoolean(key, false);
    }

    public int getClockLayoutID(boolean preview) {
        return getValidatedClockLayoutID(clockLayout, preview);
    }

    public int getValidatedClockLayoutID(int clockLayoutId, boolean preview) {
        if (preview) {
            return clockLayoutId;
        } else if (clockLayoutId == ClockLayout.LAYOUT_ID_CALENDAR && !purchasedDonation) {
            return ClockLayout.LAYOUT_ID_DIGITAL;
        } else if (clockLayoutId >= 2 && !purchasedWeatherData) {
            return ClockLayout.LAYOUT_ID_DIGITAL;
        }

        return clockLayoutId;
    }

    public int getBackgroundMode() {
        if (Utility.isLowRamDevice(mContext)) {
            return BACKGROUND_BLACK;
        } else if (background_mode != BACKGROUND_SLIDESHOW || purchasedWeatherData) {
            return background_mode;
        }

        return BACKGROUND_BLACK;
    }

    public String getTimeFormat() {
        return timeFormat;
    }

    public String getTimeFormat(int layoutId) {
        String timeFormat = getTimeFormat();
        if (getShowSeconds(layoutId)) {
            return timeFormat.replace(":mm", ":mm:ss");
        }
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

    public void disableSettingsNeedingBackgroundService() {
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putBoolean("handle_power", false);
        prefEditor.putBoolean("standbyEnabledWhileDisconnected", false);
        prefEditor.apply();
    }

    private String getDefaultDateFormat() {
        // Return the date format as used in versions previous to the version code 72
        if (Build.VERSION.SDK_INT >= 18) {
            return getBestDateTimePattern(Locale.getDefault(), "EEEEddLLLL");

        }
        DateFormat formatter = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
        return ((SimpleDateFormat) formatter).toLocalizedPattern();
    }

    public Typeface loadTypeface() {
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

    public Typeface loadTypeface(int clockLayoutId) {
        String path = getFontUri(clockLayoutId);
        return FontCache.get(mContext, path);
    }

    private String mapIntToTypefacePath(int typeface) {
        switch (typeface) {
            case 1:
            case 3:
            case 5:
                return "fonts/roboto_regular.ttf";
            case 2:
                return "fonts/roboto_light.ttf";
            case 4:
                return "fonts/roboto_thin.ttf";
            case 6:
                return "fonts/7_segment_digital.ttf";
            case 7:
                return "fonts/dancingscript_regular.ttf";
            case 8:
                return "fonts/dseg14classic.ttf";
            default:
                return null;
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
        String def = "fonts/dseg14classic.ttf";
        if ("fontUri:6".equals(key)) {
            def = "fonts/roboto_thin.ttf";
        }
        String fontUri = settings.getString(key, def);
        if ("file:///android_asset/fonts/7segment.ttf".equals(fontUri)) {
            return "fonts/7_segment_digital.ttf";
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
        if (clockLayoutId == ClockLayout.LAYOUT_ID_DIGITAL ) {
            return key;
        }
        return String.format(java.util.Locale.getDefault(),"%s:%d", key, clockLayoutId);
    }

    public void setBrightnessOffset(float value) {
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

    public void setNightModeBrightness(float value) {
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
        prefEditor.apply();
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

    public File getBackgroundImageDir() {
        String dir = settings.getString("backgroundImageDir", "");
        if (dir.equals("")) {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        } else {
            return new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + dir);
        }
    }

    public void setBackgroundImageDir(String uri) {
        clearBackgroundImageCache();
        String dir = "";
        try {
            String[] path = uri.split(":");
            dir = path[1];
        } catch (IndexOutOfBoundsException ignore) {
        }
        settings.edit().putString("backgroundImageDir", dir).apply();
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
        prefEditor.apply();
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
        prefEditor.apply();
    }

    public void deleteNextAlwaysOnTime() {
        nextAlwaysOnTime = 0L;

        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putLong("nextAlwaysOnTime", nextAlwaysOnTime);
        prefEditor.apply();
    }

    public boolean isAlwaysOnAllowed() {
        Calendar now = Calendar.getInstance();
        boolean isAllowed = true;
        if (batteryTimeout > 0 && nextAlwaysOnTime > 0L) {
            Calendar alwaysOnTime = Calendar.getInstance();
            alwaysOnTime.setTimeInMillis(nextAlwaysOnTime);
            isAllowed = now.after(alwaysOnTime);
        }
        return isAllowed;
    }

    public void setPositionClock(float x, float y, int orientation) {

        SharedPreferences.Editor editor = settings.edit();
        switch (orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                editor.putInt("xPositionLandscape", (int) x);
                editor.putInt("yPositionLandscape", (int) y);
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                editor.putInt("xPositionPortrait", (int) x);
                editor.putInt("yPositionPortrait", (int) y);
                break;
            default:
                editor.putInt("xPosition", (int) x);
                editor.putInt("yPosition", (int) y);
                break;
        }
        editor.apply();
    }

    public Point getClockPosition(int orientation) {
        switch (orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                if (settings.contains("xPositionLandscape")) {
                    int x = settings.getInt("xPositionLandscape", 0);
                    int y = settings.getInt("yPositionLandscape", 0);
                    return new Point(x, y);
                } else {
                    return null;
                }
            case Configuration.ORIENTATION_PORTRAIT:
                if (settings.contains("xPositionPortrait")) {
                    int x = settings.getInt("xPositionPortrait", 0);
                    int y = settings.getInt("yPositionPortrait", 0);
                    return new Point(x, y);
                } else {
                    return null;
                }
            default:
                if (settings.contains("xPosition")) {
                    int x = settings.getInt("xPosition", 0);
                    int y = settings.getInt("yPosition", 0);
                    return new Point(x, y);
                } else {
                    return null;
                }
        }
    }

    public void setScaleClock(float factor, int orientation) {
        SharedPreferences.Editor prefEditor = settings.edit();
        switch (orientation) {
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
        prefEditor.apply();
    }

    public float getScaleClock(int orientation) {
        switch (orientation) {
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
        prefEditor.apply();
    }

    public boolean useAmbientNoiseDetection() {
        return Config.USE_RECORD_AUDIO && ambientNoiseDetection && hasPermission(Manifest.permission.RECORD_AUDIO);
    }

    public void setUseAmbientNoiseDetection(boolean on) {
        ambientNoiseDetection = on;
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putBoolean("ambientNoiseDetection", on);
        prefEditor.apply();
    }

    public boolean reactivateScreenOnNoise() {
        return Config.USE_RECORD_AUDIO && reactivate_screen_on_noise && hasPermission(Manifest.permission.RECORD_AUDIO);
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

    private double getDouble(final String key, final double defaultValue){
        if ( !settings.contains(key))
            return defaultValue;

        try {
            return Double.longBitsToDouble(settings.getLong(key, 0));
        }catch (Exception ex){
            return (double) settings.getFloat(key, 0.f);
        }
    }

    public void setLocation(Location location) {
        if (location == null) return;
        location_lon = location.getLongitude();
        location_lat = location.getLatitude();
        location_time = location.getTime();
        location_provider = location.getProvider();
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putLong("location_lon", Double.doubleToRawLongBits(location.getLongitude()));
        prefEditor.putLong("location_lat", Double.doubleToRawLongBits(location.getLatitude()));
        prefEditor.putLong("location_time", location.getTime());
        prefEditor.putString("location_provider", location.getProvider());
        prefEditor.apply();
    }

    public City getCityForWeather() {
        String json = settings.getString("weatherCityID_json", "");
        if (json.isEmpty()) return null;
        return City.fromJson(json);
    }

    void initWeatherAutoLocationEnabled() {
        if (!settings.contains("weatherAutoLocationEnabled") && showWeather) {
            City city = getCityForWeather();
            boolean on = (city == null || city.id == 0);
            setWeatherAutoLocationEnabled(on);
        }
    }

    public boolean getWeatherAutoLocationEnabled() {
        return settings.getBoolean("weatherAutoLocationEnabled", false);
    }

    void setWeatherAutoLocationEnabled(boolean on) {
        SharedPreferences.Editor edit = settings.edit();
        edit.putBoolean("weatherAutoLocationEnabled", on);
        edit.apply();
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
            this.weatherEntry.weatherIconMeteoconsSymbol = settings.getString("weather_icon_meteocons_symbol", this.weatherEntry.weatherIconMeteoconsSymbol);
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
        prefEditor.putString("weather_icon_meteocons_symbol", entry.weatherIconMeteoconsSymbol);
        prefEditor.putString("weather_city_name", entry.cityName);
        prefEditor.putString("weather_description", entry.description);
        prefEditor.putInt("weather_city_id", entry.cityID);
        prefEditor.putFloat("weather_temperature", (float) entry.temperature);
        prefEditor.putFloat("weather_felt_temperature", (float) entry.apparentTemperature);
        prefEditor.putFloat("weather_wind_speed", (float) entry.windSpeed);
        prefEditor.putInt("weather_wind_direction", entry.windDirection);
        prefEditor.apply();
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

    private void setFavoriteRadioStations(FavoriteRadioStations stations) {
        try {
            String json = stations.toJson();
            SharedPreferences.Editor prefEditor = settings.edit();
            prefEditor.putString(FAVORITE_RADIO_STATIONS_KEY, json);
            prefEditor.apply();
        } catch (JSONException e) {
            Log.e(TAG, "error converting FavoriteRadioStations to json", e);
        }
    }

    public void persistFavoriteRadioStation(RadioStation station, int stationIndex) {
        FavoriteRadioStations stations = getFavoriteRadioStations(settings);
        stations.set(stationIndex, station);
        setFavoriteRadioStations(stations);
    }

    public void deleteFavoriteRadioStation(int stationIndex) {
        persistFavoriteRadioStation(null, stationIndex);
    }

    public static BatteryValue loadBatteryReference(Context context) {
        SharedPreferences settings = getDefaultSharedPreferences(context);
        String json = settings.getString("batteryReference", "");
        return BatteryValue.fromJson(json);
    }

    public static void saveBatteryReference(Context context, BatteryValue bv) {
        if (bv == null) {
            return;
        }
        SharedPreferences settings = getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putString("batteryReference", bv.toJson());
        prefEditor.apply();
    }

    public boolean getShallRadioStreamActivateWiFi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < 29) {
            return false;
        }
        return radioStreamActivateWiFi;
    }

    public enum WeatherProvider {OPEN_WEATHER_MAP, DARK_SKY, BRIGHT_SKY, MET_NO}

    public enum ScreenProtectionModes {NONE, MOVE, FADE}
}
