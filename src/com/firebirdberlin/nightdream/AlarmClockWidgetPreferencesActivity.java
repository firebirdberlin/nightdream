package com.firebirdberlin.nightdream;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.appcompat.app.ActionBar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.firebirdberlin.nightdream.widget.AlarmClockWidgetProvider;
import com.firebirdberlin.nightdream.widget.ClockWidgetProvider;
import com.rarepebble.colorpicker.ColorPreference;

import java.util.Locale;
import java.util.Map;

public class AlarmClockWidgetPreferencesActivity extends BillingHelperActivity {
    public static String TAG = "WidgetPreferencesActivity";

    public int appWidgetId = -1;
    private SettingsFragment fragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.appWidgetId = getWidgetId();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.widget_preferences_activity);
        if (savedInstanceState == null) {
            fragment = SettingsFragment.newInstance(appWidgetId);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, fragment)
                    .commit();

        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    public void close(View view) {
        updateWidget();
    }

    public void cancel(View view) {
        fragment.restoreValues();
        int appWidgetId = getWidgetId();
        Intent resultValue = new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_CANCELED, resultValue);
        finish();
    }


    @Override
    protected void onPurchasesInitialized() {
        Log.d(TAG, "onPurchasesInitialized()");
        /*
        runOnUiThread(() -> {
                    fragment.togglePurchasePreferences(
                            isPurchased(BillingHelperActivity.ITEM_WEATHER_DATA)
                    );
                }
        );
         */
    }

    @Override
    protected void onItemPurchased(String sku) {
        super.onItemPurchased(sku);
        Log.d(TAG, "onItemPurchased( " + sku + ")");
        /*
        runOnUiThread(() -> {
                    fragment.togglePurchasePreferences(
                            isPurchased(BillingHelperActivity.ITEM_WEATHER_DATA)
                    );
                }
        );
         */
    }

    private void updateWidget() {
        int appWidgetId = getWidgetId();
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.alarm_clock_widget);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.updateAppWidget(appWidgetId, views);

        Intent resultValue = new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
        AlarmClockWidgetProvider.updateAllWidgets(this);
    }

    private int getWidgetId() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        return appWidgetId;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private Map<String, Object> backupValues = null;

        public static SettingsFragment newInstance(int appWidgetId) {
            Bundle args = new Bundle();
            args.putInt("appWidgetId", appWidgetId);
            SettingsFragment f = new SettingsFragment();
            f.setArguments(args);
            return f;
        }

        public void restoreValues() {
            SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
            for (Map.Entry<String, Object> entry : backupValues.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof String) {
                    editor.putString(key, (String) value);
                } else
                if (value instanceof Integer) {
                    editor.putInt(key, (Integer) value);
                } else
                if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                } else
                if (value instanceof Float) {
                    editor.putFloat(key, (Float) value);
                }
        }
            editor.apply();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            int appWidgetId = getArguments().getInt("appWidgetId", -1);
            PreferenceManager manager = getPreferenceManager();
            manager.setSharedPreferencesName(
                    String.format(Locale.ENGLISH, "preferences_alarm_clock_widget_%d", appWidgetId)
            );
            SharedPreferences prefs = manager.getSharedPreferences();
            backupValues = (Map<String, Object>) prefs.getAll();
            setPreferencesFromResource(R.xml.alarm_clock_widget_root_preferences, rootKey);
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            if (preference instanceof ColorPreference) {
                ColorPreference cp = (ColorPreference) preference;
                cp.showDialog(this, 0);
            }
            else super.onDisplayPreferenceDialog(preference);
        }

        private void showPreference(String key, boolean visible) {
            Log.d(TAG, "showPreference(" + key + "," + visible + ")");
            Preference preference = findPreference(key);
            if (preference != null) {
                preference.setVisible(visible);
            }
        }

        private void enablePreference(String key, boolean visible) {
            Log.d(TAG, "enablePreference(" + key + "," + visible + ")");
            Preference preference = findPreference(key);
            if (preference != null) {
                preference.setEnabled(visible);
            }
        }
    }
}