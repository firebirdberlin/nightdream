package com.firebirdberlin.nightdream;

import android.os.Build;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

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
    public boolean muteRinger = true;
    public boolean showDate = true;
    public float dim_offset = 0.f;
    public float minIlluminance = 15.f; // lux
    public int background_mode = 1;
    public int clockColor;
    public int secondaryColor;
    public int sensitivity = 1;
    public String bgpath = "";

    public double NOISE_AMPLITUDE_WAKE  = Config.NOISE_AMPLITUDE_WAKE;
    public double NOISE_AMPLITUDE_SLEEP = Config.NOISE_AMPLITUDE_SLEEP;


    public Settings(Context context){
        this.mContext = context;
        settings = context.getSharedPreferences(PREFS_KEY, 0);
        reload();
    }

    public void reload() {
        allow_screen_off = settings.getBoolean("allow_screen_off", false);
        ambientNoiseDetection = settings.getBoolean("ambientNoiseDetection", false);
        autoBrightness = settings.getBoolean("autoBrightness", false);
        bgpath = settings.getString("BackgroundImage", "");
        clockColor = settings.getInt("clockColor", Color.parseColor("#33B5E5"));
        dim_offset = settings.getFloat("dimOffset", 0.f);
        minIlluminance = settings.getFloat("minIlluminance", 15.f);
        muteRinger = settings.getBoolean("Night.muteRinger", true);
        secondaryColor = settings.getInt("secondaryColor", Color.parseColor("#C2C2C2"));
        sensitivity = 10-settings.getInt("NoiseSensitivity", 4);
        showDate = settings.getBoolean("showDate", true);

        NOISE_AMPLITUDE_SLEEP *= sensitivity;
        NOISE_AMPLITUDE_WAKE  *= sensitivity;

        if (Build.VERSION.SDK_INT < 14){
            background_mode = settings.getInt("BackgroundMode", BACKGROUND_BLACK);
        } else {
            background_mode = Integer.parseInt(settings.getString("backgroundMode", "1"));
        }
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
}
