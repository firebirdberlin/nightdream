package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.firebirdberlin.nightdream.ui.ClockLayoutPreviewPreference;
import com.firebirdberlin.nightdream.widget.ClockWidgetProvider;

import de.firebirdberlin.preference.InlineSeekBarPreference;

public class WeatherPreferenceFragment extends PreferenceFragmentCompat {
    public static final String PREFS_KEY = "NightDream preferences";

    Settings settings = null;

    SharedPreferences.OnSharedPreferenceChangeListener prefChangedListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if ("weatherProvider".equals(key)) {
                        setupWeatherProviderPreference();
                    }
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
        if (settings.getWeatherProviderString().equals("0")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://openweathermap.org"));
            prefAttribution.setIntent(intent);
            prefAttribution.setTitle("Powered by OpenWeatherMap");
            prefAttribution.setSummary("https://openweathermap.org");
        }
        else if (settings.getWeatherProviderString().equals("1")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://darksky.net/poweredby/"));
            prefAttribution.setIntent(intent);
            prefAttribution.setTitle("Powered by Dark Sky");
            prefAttribution.setSummary("https://darksky.net/poweredby/");
        }
    }

}
