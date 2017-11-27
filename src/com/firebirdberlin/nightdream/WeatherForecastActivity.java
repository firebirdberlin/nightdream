package com.firebirdberlin.nightdream;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.ui.WeatherForecastLayout;
import com.firebirdberlin.openweathermapapi.ForecastRequestTask;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;

public class WeatherForecastActivity extends Activity
                                     implements ForecastRequestTask.AsyncResponse {
    final static String TAG = "WeatherForecastActivity";

    private LinearLayout scrollView = null;

    public static void start(Context context, String cityID) {
        Intent intent = new Intent(context, WeatherForecastActivity.class);
        intent.putExtra("cityID", cityID);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_forecast);
        scrollView = (LinearLayout) findViewById(R.id.scroll_view);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onResume()");
        Intent intent = getIntent();
        String cityID = intent.getStringExtra("cityID");
        new ForecastRequestTask(this).execute(cityID);
    }

    public void onRequestFinished(List<WeatherEntry> entries) {
        Log.i(TAG, "onRequestFinished()");
        Log.i(TAG, String.format(" > got %d entries", entries.size()));
        scrollView.removeAllViews();
        Settings settings = new Settings(this);

        String timeFormat = settings.getTimeFormat();
        if ( !is24HourFormat() ) timeFormat += " a";

        if (entries.size() > 0 ) {
            WeatherEntry firstEntry = entries.get(0);
            actionBarSetup(firstEntry.cityName);
        }

        int day = -1;
        for (WeatherEntry entry: entries) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ActionBar ab = getActionBar();
            ab.setTitle(R.string.forecast);
            ab.setSubtitle(subtitle);
        } else {
            setTitle(R.string.forecast);
        }
    }

    private boolean is24HourFormat() {
        return android.text.format.DateFormat.is24HourFormat(this);
    }
}
