package com.firebirdberlin.nightdream;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference.SummaryProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;


public class SmartHomePreferenceFragment extends PreferenceFragmentCompat {
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
        setPreferencesFromResource(R.xml.preferences_smarthome, rootKey);
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

        EditTextPreference passwordPreference = findPreference("smart_home_avm_password");
        if (passwordPreference != null) {
            passwordPreference.setSummaryProvider(new SummaryProvider() {
                @Override
                public CharSequence provideSummary(Preference preference) {
                    String text = "";
                    for (int i = 0; i < ((EditTextPreference) preference).getText().toString().length(); i++) {
                        text += "*";
                    }
                    return text;
                }
            });
            passwordPreference.setOnBindEditTextListener(
                    new EditTextPreference.OnBindEditTextListener() {
                        @Override
                        public void onBindEditText(@NonNull final EditText editText) {
                            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        }
                    }
            );
        }
    }

    private void showPreference(String key, boolean visible) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setVisible(visible);
        }
    }
}
