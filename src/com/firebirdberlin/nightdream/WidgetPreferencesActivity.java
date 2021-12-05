package com.firebirdberlin.nightdream;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.firebirdberlin.nightdream.widget.ClockWidgetProvider;

import java.util.Locale;

public class WidgetPreferencesActivity extends AppCompatActivity {

    public int appWidgetId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.appWidgetId = getWidgetId();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.widget_preferences_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment(appWidgetId))
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
        int appWidgetId = getWidgetId();
        Intent resultValue = new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_CANCELED, resultValue);
        finish();
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

        public SettingsFragment(int appWidgetId) {
            this.appWidgetId = appWidgetId;
        }


        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            PreferenceManager manager = getPreferenceManager();
            manager.setSharedPreferencesName(
                    String.format(Locale.ENGLISH,"preferences_widget_%d", appWidgetId)
            );
            setPreferencesFromResource(R.xml.widget_root_preferences, rootKey);
        }
    }
}