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

package com.firebirdberlin.nightdream.ui;


import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.WindSpeedConversion;
import com.firebirdberlin.nightdream.models.FontCache;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class WeatherForecastLayout extends LinearLayout {

    private static final String TAG = "WeatherForecastLayout";
    private Context context = null;
    private TextView timeView = null;
    private TextView iconClouds = null;
    private TextView iconText = null;
    private TextView iconWind = null;
    private TextView iconRain3h = null;
    private String timeFormat = "HH:mm";
    private DirectionIconView iconWindDirection = null;
    private TextView cloudText = null;
    private TextView temperatureText = null;
    private TextView descriptionText = null;
    private TextView humidityText = null;
    private TextView windText = null;
    private TextView rainText = null;
    private TextView sunsetTimeText = null;
    private TextView sunriseTimeText = null;
    private LinearLayout sunsetLayout = null;
    private LinearLayout sunriseLayout = null;
    private int temperatureUnit = WeatherEntry.CELSIUS;
    private int speedUnit = WeatherEntry.METERS_PER_SECOND;
    private WeatherEntry weatherEntry = null;

    public WeatherForecastLayout(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public WeatherForecastLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View child = inflater.inflate(R.layout.weather_forecast_layout, null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        addView(child, lp);

        timeView = findViewById(R.id.timeView);
        iconText = findViewById(R.id.iconText);
        iconWind = findViewById(R.id.iconWind);
        iconRain3h = findViewById(R.id.iconRain3h);
        iconClouds = findViewById(R.id.iconClouds);
        iconWindDirection = findViewById(R.id.iconWindDirection);
        cloudText = findViewById(R.id.cloudText);
        temperatureText = findViewById(R.id.temperatureText);
        descriptionText = findViewById(R.id.descriptionText);
        humidityText = findViewById(R.id.humidityText);
        rainText = findViewById(R.id.rainText);
        windText = findViewById(R.id.windText);
        sunriseTimeText = findViewById(R.id.sunriseTime);
        sunsetTimeText = findViewById(R.id.sunsetTime);
        sunriseLayout = findViewById(R.id.layoutSunrise);
        sunsetLayout = findViewById(R.id.layoutSunset);
        Typeface typeface = FontCache.get(context, "fonts/meteocons.ttf");
        iconClouds.setTypeface(typeface);
        iconText.setTypeface(typeface);
        iconWind.setTypeface(typeface);
        iconRain3h.setTypeface(typeface);
    }

    public void setTimeFormat(String format) {
        timeFormat = format;
    }

    public void setTemperature(boolean on, int unit) {
        this.temperatureUnit = unit;
        temperatureText.setVisibility((on) ? View.VISIBLE : View.GONE);
    }

    public void setDescriptionText(String value) {
        descriptionText.setText(value);
        descriptionText.setVisibility((value != null) ? View.VISIBLE : View.GONE);
    }

    public void setSunrise(boolean on, long time) {
        Calendar mCalendar = Calendar.getInstance();
        mCalendar.setTimeInMillis(time);
        sunriseTimeText.setText(Utility.formatTime(timeFormat, mCalendar));
        sunriseLayout.setVisibility((on) ? View.VISIBLE : View.GONE);
    }

    public void setSunset(boolean on, long time) {
        Calendar mCalendar = Calendar.getInstance();
        mCalendar.setTimeInMillis(time);
        sunsetTimeText.setText(Utility.formatTime(timeFormat, mCalendar));
        sunsetLayout.setVisibility((on) ? View.VISIBLE : View.GONE);
    }

    public void setWindSpeed(boolean on, int unit) {
        this.speedUnit = unit;

        iconWind.setVisibility((on) ? View.VISIBLE : View.GONE);
        iconWindDirection.setVisibility((on) ? View.VISIBLE : View.GONE);

        windText.setVisibility((on) ? View.VISIBLE : View.GONE);
    }

    public void clear() {
        timeView.setText("");
        iconClouds.setText("");
        iconRain3h.setText("");
        iconText.setText("");
        iconWind.setText("");
        iconWindDirection.setDirection(DirectionIconView.INVALID);
        cloudText.setText("");
        temperatureText.setText("");
        humidityText.setText("");
        rainText.setText("");
        windText.setText("");
        timeView.invalidate();
        iconText.invalidate();
        iconWind.invalidate();
        iconWindDirection.invalidate();
        temperatureText.invalidate();
        humidityText.invalidate();
        windText.invalidate();
    }

    public void setTypeface(Typeface typeface) {
        temperatureText.setTypeface(typeface);
        humidityText.setTypeface(typeface);
        windText.setTypeface(typeface);
        rainText.setTypeface(typeface);
    }

    public void update(WeatherEntry entry) {
        // Log.d("WeatherForecastLayout", entry.toString());
        // Log.d("WeatherForecastLayout", entry.formatTemperatureText(temperatureUnit));
        this.weatherEntry = entry;
        if (iconText == null || temperatureText == null) return;
        iconWindDirection.setColor(Color.WHITE);
        Calendar mCalendar = Calendar.getInstance();
        mCalendar.setTimeInMillis(entry.timestamp * 1000);

        SimpleDateFormat sdf = new SimpleDateFormat(timeFormat, java.util.Locale.getDefault());
        String text = sdf.format(mCalendar.getTime());
        timeView.setText(text);

        iconText.setText(entry.weatherIconMeteoconsSymbol);
        temperatureText.setText(entry.formatTemperatureText(temperatureUnit));
        if (!entry.formatHumidityText().isEmpty()) {
            humidityText.setText(entry.formatHumidityText());
        } else {
            humidityText.setVisibility(View.GONE);
        }
        iconWind.setText("F");

        iconWindDirection.setDirection(entry.windDirection);
        windText.setText(formatWindText(entry));

        if (entry != null && entry.rain3h >= 0.) {
            iconRain3h.setText("R");
            rainText.setText(formatRainText(entry.rain3h));
        } else if (entry != null && entry.rain1h > 0.) {
            iconRain3h.setText("R");
            rainText.setText(formatRainText(entry.rain1h));
        } else {
            iconRain3h.setText("");
            iconRain3h.setVisibility(View.GONE);
            rainText.setText("");
            rainText.setVisibility(View.GONE);
        }

        if (entry != null && entry.clouds >= 0) {
            iconClouds.setText("Y");
            cloudText.setText(formatCloudsText(entry));
        } else {
            iconClouds.setText("");
            cloudText.setText("");
        }

        if (weatherEntry != null) {
            iconWind.setVisibility((weatherEntry.windDirection >= 0) ? View.GONE : View.VISIBLE);
            iconWindDirection.setVisibility((weatherEntry.windDirection >= 0) ? View.VISIBLE : View.GONE);
        }
        fixIconWindDirectionSize();
    }

    private String formatRainText(double rainValue) {
        return String.format(Locale.getDefault(), "%.1f mm", rainValue);
    }

    private String formatCloudsText(WeatherEntry entry) {
        return String.format(Locale.getDefault(), "%3d %%", entry.clouds);
    }

    private String formatWindText(WeatherEntry entry) {
        String windSpeedBeaufort = String.format(Locale.getDefault(), "%d Bft", WindSpeedConversion.metersPerSecondToBeaufort(entry.windSpeed));
        String formatted = entry.formatWindText(speedUnit);
        switch (speedUnit) {
            case WeatherEntry.MILES_PER_HOUR:
            case WeatherEntry.KM_PER_HOUR:
            case WeatherEntry.KNOT:
                return String.format("%s (%s)", formatted, windSpeedBeaufort);
            case WeatherEntry.BEAUFORT:
            case WeatherEntry.METERS_PER_SECOND:
            default:
                return String.format(Locale.getDefault(), "%.1f m/s (%s)", entry.windSpeed, windSpeedBeaufort);
        }
    }

    private void fixIconWindDirectionSize() {
        temperatureText.post(new Runnable() {
            public void run() {
                int height = temperatureText.getHeight();
                iconWindDirection.setLayoutParams(new LinearLayout.LayoutParams(height, height));
                iconWindDirection.requestLayout();
                iconWindDirection.invalidate();
            }
        });
    }
}
