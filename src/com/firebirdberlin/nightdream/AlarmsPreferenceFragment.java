package com.firebirdberlin.nightdream;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import androidx.recyclerview.widget.RecyclerView;


public class AlarmsPreferenceFragment extends PreferenceFragmentCompat {
    public static final String PREFS_KEY = "NightDream preferences";

    Settings settings = null;

    SharedPreferences.OnSharedPreferenceChangeListener prefChangedListener =
            (sharedPreferences, key) -> {
                if ("notifyForUpcomingAlarms".equals(key)) {
                    setupNotificationPermissionPreference();
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
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        prefs.registerOnSharedPreferenceChangeListener(prefChangedListener);
        setupAlarmClockPreferences();
        setupNotificationPermissionPreference();
    }

    private void setupAlarmClockPreferences() {
        boolean on = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < 29);
        showPreference("radioStreamActivateWiFi", on);
    }
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                }
            });

    private void setupNotificationPermissionPreference() {
        Preference preference = findPreference("permission_request_post_notifications");
        SwitchPreferenceCompat toggle = (SwitchPreferenceCompat) findPreference("notifyForUpcomingAlarms");
        Context context = getContext();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            boolean isSet = Utility.hasPermission(context, Manifest.permission.POST_NOTIFICATIONS);
            preference.setVisible( !isSet && toggle.isChecked() );
            preference.setOnPreferenceClickListener(preference1 -> {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return false;
            });
        } else {
            preference.setVisible(false);
        }


    }
    private void showPreference(String key, boolean visible) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setVisible(visible);
        }
    }
}
