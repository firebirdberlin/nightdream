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

import com.firebirdberlin.nightdream.widget.ClockWidgetProvider;

import java.util.Locale;
import java.util.Map;

public class WidgetPreferencesActivity extends BillingHelperActivity {
    public static String TAG = "WidgetPreferencesActivity";

    public int appWidgetId = -1;
    private SettingsFragment fragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.appWidgetId = getWidgetId();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.widget_preferences_activity);
        if (savedInstanceState == null) {
            fragment = new SettingsFragment(this, false, appWidgetId);
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
        runOnUiThread(() -> {
                    fragment.togglePurchasePreferences(
                            isPurchased(BillingHelperActivity.ITEM_WEATHER_DATA)
                    );
                }
        );
    }

    @Override
    protected void onItemPurchased(String sku) {
        super.onItemPurchased(sku);
        Log.d(TAG, "onItemPurchased( " + sku + ")");
        runOnUiThread(() -> {
                    fragment.togglePurchasePreferences(
                            isPurchased(BillingHelperActivity.ITEM_WEATHER_DATA)
                    );
                }
        );
    }

    private void updateWidget() {
        int appWidgetId = getWidgetId();
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.clock_widget);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.updateAppWidget(appWidgetId, views);

        Intent resultValue = new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
        ClockWidgetProvider.updateAllWidgets(this);
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
        private final int appWidgetId;
        private BillingHelperActivity activity;
        private Map<String, Object> backupValues = null;
        Preference.OnPreferenceClickListener purchasePreferenceClickListener =
                preference -> {
                    Log.d(TAG, "showPurchaseDialog");
                    activity.showPurchaseDialog();
                    return true;
                };
        public boolean is_purchased;

        public SettingsFragment(BillingHelperActivity activity, boolean is_purchased, int appWidgetId) {
            this.activity = activity;
            this.is_purchased = is_purchased;
            this.appWidgetId = appWidgetId;
        }

        @Override
        public void onResume() {
            Log.d(TAG, "onResume");
            super.onResume();
            initPurchasePreference("purchaseDesignPackage");
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
            PreferenceManager manager = getPreferenceManager();
            manager.setSharedPreferencesName(
                    String.format(Locale.ENGLISH, "preferences_widget_%d", appWidgetId)
            );
            SharedPreferences prefs = manager.getSharedPreferences();
            backupValues = (Map<String, Object>) prefs.getAll();
            setPreferencesFromResource(R.xml.widget_root_preferences, rootKey);
        }

        private void togglePurchasePreferences(boolean is_purchased) {
            this.is_purchased = is_purchased;
            Log.d(TAG, "togglePurchasePreferences()");

            showPreference("purchaseDesignPackage", !is_purchased);
            enablePreference("category_appearance", is_purchased);
        }

        private void initPurchasePreference(String key) {
            Log.d(TAG, "initPurchasePreference(" + key + ")");
            Preference purchasePreference = findPreference(key);
            if (purchasePreference != null) {
                purchasePreference.setOnPreferenceClickListener(purchasePreferenceClickListener);
            }
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