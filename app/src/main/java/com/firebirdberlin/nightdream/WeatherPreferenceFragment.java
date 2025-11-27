package com.firebirdberlin.nightdream;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;


public class WeatherPreferenceFragment extends PreferenceFragmentCompat {
    public static final String PREFS_KEY = "NightDream preferences";
    public static final String TAG = "WeatherPreferenceFragment";

    Settings settings = null;

    SharedPreferences.OnSharedPreferenceChangeListener prefChangedListener =
            (sharedPreferences, key) -> {
                if ("weatherProvider".equals(key)) {
                    setupWeatherProviderPreference();
                    updateOpenWeatherMapApiKeyVisibility(sharedPreferences);
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final RecyclerView recyclerView = getListView();
        if (recyclerView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(recyclerView, (v, insets) -> {
                int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
    }

    private void init() {
        settings = new Settings(getContext());
        setupWeatherProviderPreference();
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        prefs.registerOnSharedPreferenceChangeListener(prefChangedListener);
        updateOpenWeatherMapApiKeyVisibility(prefs);
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

    private void updateOpenWeatherMapApiKeyVisibility(SharedPreferences sharedPreferences) {
        if (!isAdded()) {
            return;
        }

        Preference openWeatherMapApiKeyPref = findPreference("openWeatherMapApiKey");
        ListPreference weatherProviderPref = findPreference("weatherProvider");

        if (openWeatherMapApiKeyPref == null || weatherProviderPref == null) {
            return;
        }

        String currentProvider = sharedPreferences.getString("weatherProvider", "0"); // "0" is default value from XML
        boolean showApiKeyInput = "0".equals(currentProvider) && "noGms".equals(BuildConfig.FLAVOR);

        showPreference("openWeatherMapApiKey", showApiKeyInput);
    }

    private void showPreference(String key, boolean visible) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setVisible(visible);
            Log.i(TAG, "Preference " + key + " visibility set to " + visible + ". Current isVisible: " + preference.isVisible());
        } else {
            Log.w(TAG, "WARNING: preference " + key + " not found.");
        }
    }
}
