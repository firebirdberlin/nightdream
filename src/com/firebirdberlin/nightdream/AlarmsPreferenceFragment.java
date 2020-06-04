package com.firebirdberlin.nightdream;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;


public class AlarmsPreferenceFragment extends PreferenceFragmentCompat {
    public static final String PREFS_KEY = "NightDream preferences";

    Settings settings = null;

    SharedPreferences.OnSharedPreferenceChangeListener prefChangedListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                }
            };



    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(PREFS_KEY);
        setPreferencesFromResource(R.xml.preferences_alarms, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        settings = new Settings(getContext());
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        prefs.registerOnSharedPreferenceChangeListener(prefChangedListener);
        setupAlarmClockPreferences();
    }

    private void setupAlarmClockPreferences() {
        boolean on = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < 29);
        showPreference("radioStreamActivateWiFi", on);
    }

    private void showPreference(String key, boolean visible) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setVisible(visible);
        }
    }
}
