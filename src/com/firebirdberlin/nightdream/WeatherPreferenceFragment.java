package com.firebirdberlin.nightdream;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;


public class WeatherPreferenceFragment extends PreferenceFragmentCompat {
    public static final String PREFS_KEY = "NightDream preferences";

    Settings settings = null;

    SharedPreferences.OnSharedPreferenceChangeListener prefChangedListener =
            (sharedPreferences, key) -> {
                if ("weatherProvider".equals(key)) {
                    setupWeatherProviderPreference();
                }
            };



    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(PREFS_KEY);
        setPreferencesFromResource(R.xml.preferences_weather, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        settings = new Settings(getContext());
        setupWeatherProviderPreference();
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        prefs.registerOnSharedPreferenceChangeListener(prefChangedListener);
    }


    private void setupWeatherProviderPreference() {
        Preference pref = findPreference("weatherProvider");
        if (!isAdded() || pref == null) {
            return;
        }
        Preference prefAttribution = findPreference("weatherProviderAttribution");

        switch (settings.getWeatherProviderString()) {
            case "0": {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://openweathermap.org"));
                prefAttribution.setIntent(intent);
                prefAttribution.setTitle("Powered by OpenWeatherMap");
                prefAttribution.setSummary("https://openweathermap.org");
                break;
            }
            case "1": // DarkSky reaches end of service at March 31st
            case "2": {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://brightsky.dev/"));
                prefAttribution.setIntent(intent);
                prefAttribution.setTitle("Powered by Bright Sky");
                prefAttribution.setSummary("https://brightsky.dev/");
                break;
            }
            case "3": {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://met.no/"));
                prefAttribution.setIntent(intent);
                prefAttribution.setTitle("Powered by Met.no");
                prefAttribution.setSummary("https://met.no/");
                break;
            }
        }
    }

}
