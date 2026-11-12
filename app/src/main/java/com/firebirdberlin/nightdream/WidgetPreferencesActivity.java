/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
    private static final String TAG = WidgetPreferencesActivity.class.getSimpleName();

    public int appWidgetId = -1;
    private SettingsFragment fragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.appWidgetId = getWidgetId();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.widget_preferences_activity);
        if (savedInstanceState == null) {
            fragment = SettingsFragment.newInstance(appWidgetId, false);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, fragment)
                    .commit();
        } else {
            fragment = (SettingsFragment) getSupportFragmentManager().findFragmentById(R.id.settings);
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
        if (fragment == null) {
            fragment = (SettingsFragment) getSupportFragmentManager().findFragmentById(R.id.settings);
        }
        if (fragment != null) {
            fragment.restoreValues();
        }
        int appWidgetId = getWidgetId();
        Intent resultValue = new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_CANCELED, resultValue);
        finish();
    }


    @Override
    protected void onPurchasesInitialized() {
        Log.d(TAG, "onPurchasesInitialized()");
        runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    fragment = SettingsFragment.newInstance(appWidgetId,
                            isPurchased(PurchaseManager.ITEM_WEATHER_DATA)
                    );

                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.settings, fragment)
                            .commit();
                }
        );
    }

    @Override
    protected void onItemPurchased(String sku) {
        super.onItemPurchased(sku);
        Log.d(TAG, "onItemPurchased( " + sku + ")");
        runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    fragment = SettingsFragment.newInstance(appWidgetId,
                            isPurchased(PurchaseManager.ITEM_WEATHER_DATA)
                    );

                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.settings, fragment)
                            .commit();
                }
        );
    }

    private void updateWidget() {
        int appWidgetId = getWidgetId();
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.clock_widget);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        if (appWidgetManager != null) {
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        Intent resultValue = new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
        ClockWidgetProvider.updateAllWidgets(this);
    }

    private int getWidgetId() {
        Intent intent = getIntent();
        if (intent == null) {
            return AppWidgetManager.INVALID_APPWIDGET_ID;
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return AppWidgetManager.INVALID_APPWIDGET_ID;
        }
        return extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private Map<String, Object> backupValues = null;
        Preference.OnPreferenceClickListener purchasePreferenceClickListener =
                preference -> {
                    WidgetPreferencesActivity activity = (WidgetPreferencesActivity) getActivity();
                    if (activity != null) {
                        Log.d(TAG, "showSubscriptionDialog()");
                        activity.showSubscriptionDialog();
                    }
                    return true;
                };

        public static SettingsFragment newInstance(int appWidgetId, boolean isPurchased) {
            Bundle args = new Bundle();
            args.putInt("appWidgetId", appWidgetId);
            args.putBoolean("is_purchased", isPurchased);
            SettingsFragment f = new SettingsFragment();
            f.setArguments(args);
            return f;
        }

        @Override
        public void onResume() {
            Log.d(TAG, "onResume");
            super.onResume();
            initPurchasePreference();
        }

        public void restoreValues() {
            if (Utility.isEmpty(backupValues)) return;
            PreferenceManager manager = getPreferenceManager();
            if (manager == null) return;
            SharedPreferences prefs = manager.getSharedPreferences();
            if (prefs == null) return;
            SharedPreferences.Editor editor = prefs.edit();
            if (editor == null) return;
            for (Map.Entry<String, Object> entry : backupValues.entrySet()) {
                if (entry == null) {
                    continue;
                }
                String key = entry.getKey();
                Object value = entry.getValue();
                if (key == null) {
                    continue;
                }
                if (value instanceof String) {
                    editor.putString(key, (String) value);
                } else if (value instanceof Integer) {
                    editor.putInt(key, (Integer) value);
                } else if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                } else if (value instanceof Float) {
                    editor.putFloat(key, (Float) value);
                }
            }
            editor.apply();
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            int appWidgetId = 0;
            Bundle args = getArguments();
            if (args != null) {
                appWidgetId = args.getInt("appWidgetId", 0);
            }
            PreferenceManager manager = getPreferenceManager();
            if (manager != null) {
                manager.setSharedPreferencesName(
                        String.format(Locale.ENGLISH, "preferences_widget_%d", appWidgetId)
                );
                SharedPreferences prefs = manager.getSharedPreferences();
                if (prefs != null) {
                    backupValues = (Map<String, Object>) prefs.getAll();
                }
            }
            setPreferencesFromResource(R.xml.widget_root_preferences, rootKey);
        }

        private void initPurchasePreference() {
            Log.d(TAG, "initPurchasePreference(" + "purchaseDesignPackage" + ")");
            Preference purchasePreference = findPreference("purchaseDesignPackage");
            if (purchasePreference != null) {
                purchasePreference.setOnPreferenceClickListener(purchasePreferenceClickListener);
            }
            boolean is_purchased = false;
            Bundle args = getArguments();
            if (args != null) {
                is_purchased = args.getBoolean("is_purchased", false);
            }
            showPreference(!is_purchased);
            enablePreference(is_purchased);
        }

        private void showPreference(boolean visible) {
            Log.d(TAG, "showPreference(" + "purchaseDesignPackage" + "," + visible + ")");
            Preference preference = findPreference("purchaseDesignPackage");
            if (preference != null) {
                preference.setVisible(visible);
            }
        }

        private void enablePreference(boolean visible) {
            Log.d(TAG, "enablePreference(" + "category_appearance" + "," + visible + ")");
            Preference preference = findPreference("category_appearance");
            if (preference != null) {
                preference.setEnabled(visible);
            }
        }
    }
}