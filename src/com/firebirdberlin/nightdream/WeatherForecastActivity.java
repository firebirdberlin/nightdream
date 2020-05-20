package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentManager;

import com.firebirdberlin.nightdream.ui.WeatherForecastLayout;
import com.firebirdberlin.openweathermapapi.ForecastRequestTask;
import com.firebirdberlin.openweathermapapi.WeatherLocationDialogFragment;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;

public class WeatherForecastActivity
        extends AppCompatActivity
        implements ForecastRequestTask.AsyncResponse,
        WeatherLocationDialogFragment.WeatherLocationDialogListener {
    final static String TAG = "WeatherForecastActivity";

    private LinearLayout scrollView = null;
    private Settings settings;

    public static void start(Context context) {
        Intent intent = new Intent(context, WeatherForecastActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_forecast);
        scrollView = findViewById(R.id.scroll_view);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onResume()");
        settings = new Settings(this);
        City city = settings.getValidCity();
        if (city != null) {
            new ForecastRequestTask(this, settings.getWeatherProvider()).execute(city.toJson());
        }
    }

    public void onRequestFinished(List<WeatherEntry> entries) {
        Log.i(TAG, "onRequestFinished()");
        Log.i(TAG, String.format(" > got %d entries", entries.size()));
        scrollView.removeAllViews();

        String timeFormat = settings.getFullTimeFormat();

        if (entries.size() > 0 ) {
            WeatherEntry firstEntry = entries.get(0);
            actionBarSetup(firstEntry.cityName);
        }

        int day = -1;
        long now = System.currentTimeMillis();
        for (WeatherEntry entry: entries) {
            if (entry.timestamp * 1000 < now - 600000) {
                continue;
            }
            TextView dateView = new TextView(this);
            if (Build.VERSION.SDK_INT >= 23) {
                dateView.setTextAppearance(android.R.style.TextAppearance_Medium);
                dateView.setTextColor(getResources().getColor(R.color.blue, null));
            } else {
                dateView.setTextAppearance(this, android.R.style.TextAppearance_Medium);
                dateView.setTextColor(getResources().getColor(R.color.blue));
            }
            dateView.setTypeface(null, Typeface.BOLD);

            Calendar mCalendar = Calendar.getInstance();
            mCalendar.setTimeInMillis(entry.timestamp * 1000);
            if (day != mCalendar.get(Calendar.DAY_OF_MONTH) ) {
                day = mCalendar.get(Calendar.DAY_OF_MONTH);
                DateFormat sdf = DateFormat.getDateInstance(DateFormat.FULL);
                String text = sdf.format(mCalendar.getTime());
                dateView.setText(text);
                scrollView.addView(dateView);

            }

            WeatherForecastLayout layout = new WeatherForecastLayout(this);
            layout.setTimeFormat(timeFormat);
            layout.setTemperature(true, settings.temperatureUnit);
            layout.setWindSpeed(true, settings.speedUnit);
            layout.update(entry);
            scrollView.addView(layout);
        }
        scrollView.invalidate();
    }

    private void actionBarSetup(String subtitle) {
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.forecast);
            ab.setSubtitle(subtitle);
        }
    }

    final int MENU_ITEM_SEARCH = 3000;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

            menu.add(Menu.NONE, MENU_ITEM_SEARCH , Menu.NONE, getString(R.string.weather_city_id_title))
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            final MenuItem item = menu.getItem(0);
            return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //return super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case MENU_ITEM_SEARCH:
                showWeatherLocationDialog();
                return true;

            default:
                return false;
        }
    }

    private void showWeatherLocationDialog() {
        FragmentManager fm = getSupportFragmentManager();
        WeatherLocationDialogFragment dialog = new WeatherLocationDialogFragment();
        dialog.show(fm, "weather_location_selection_dialog");
    }

    public void onWeatherLocationSelected(City city) {

        settings.setWeatherLocation(city);
        if (city != null) {
            new ForecastRequestTask(this, settings.getWeatherProvider()).execute(city.toJson());
        }
    };

}
