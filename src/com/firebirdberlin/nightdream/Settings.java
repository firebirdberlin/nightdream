package com.firebirdberlin.nightdream;

import android.os.Build;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public class Settings {
    Context mContext;
    SharedPreferences settings;

    public boolean allow_screen_off = false;
    public boolean autoBrightness = false;
    public boolean ambientNoiseDetection;
    public boolean muteRinger = true;
    public boolean showDate = true;
    public boolean whiteClock = false;
    public float dim_offset = 0.f;
    public int background_mode = 1;
    public int clockColor;
    public int sensitivity = 1;
    public String bgpath = "";

    public double NOISE_AMPLITUDE_WAKE  = Config.NOISE_AMPLITUDE_WAKE;
    public double NOISE_AMPLITUDE_SLEEP = Config.NOISE_AMPLITUDE_SLEEP;


    public Settings(Context context){
        this.mContext = context;
        settings = context.getSharedPreferences(NightDreamSettingsActivity.PREFS_KEY, 0);
        reload();
    }

    public void reload() {
        allow_screen_off = settings.getBoolean("allow_screen_off", false);
        ambientNoiseDetection = settings.getBoolean("ambientNoiseDetection", false);
        autoBrightness = settings.getBoolean("autoBrightness", false);
        bgpath = settings.getString("BackgroundImage", "");
        clockColor = settings.getInt("clockColor", Color.parseColor("#33B5E5"));
        dim_offset = settings.getFloat("dimOffset", 0.f);
        muteRinger = settings.getBoolean("Night.muteRinger", true);
        sensitivity = 10-settings.getInt("NoiseSensitivity", 4);
        showDate = settings.getBoolean("showDate", true);
        whiteClock = settings.getBoolean("whiteClock", false);

        NOISE_AMPLITUDE_SLEEP *= sensitivity;
        NOISE_AMPLITUDE_WAKE  *= sensitivity;

        if (Build.VERSION.SDK_INT < 14){
            background_mode = settings.getInt("BackgroundMode", NightDreamSettingsActivity.BACKGROUND_BLACK);
        } else {
            background_mode = Integer.parseInt(settings.getString("backgroundMode", "1"));
        }
    }
}
