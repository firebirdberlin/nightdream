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

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.firebirdberlin.nightdream.ui.WeatherForecastLayout;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;

public class WeatherForecastTabWeather extends Fragment {
    final static String TAG = "WeatherForeTabWeather";
    private LinearLayout scrollViewLayout = null;
    private ScrollView scrollView = null;
    private Context context;
    private int fadeDuration = 2000;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_weather_forecast_scrollview, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        scrollViewLayout = view.findViewById(R.id.scroll_view_layout);
        scrollView = view.findViewById(R.id.scroll_view);
    }

    private void hide() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            // avoid flickering during build
            scrollView.setAlpha(0);
        }
    }

    public void onRequestFinished(List<WeatherEntry> entries, Settings settings) {
        Log.d(TAG, "onRequestFinished() List<WeatherEntry>");
        hide();

        scrollViewLayout.removeAllViews();
        addWeatherEntries(entries, settings);
    }

    private void addWeatherEntries(List<WeatherEntry> entries, Settings settings) {
        Log.d(TAG, "addWeatherEntries");
        Log.d(TAG, String.format(" > got %d entries", entries.size()));

        if (context != null) {
            String timeFormat = settings.getFullTimeFormat();

            int day = -1;
            long now = System.currentTimeMillis();
            for (WeatherEntry entry : entries) {
                if (entry.timestamp * 1000 < now - 600000) {
                    continue;
                }
                TextView dateView = new TextView(context);
                if (Build.VERSION.SDK_INT >= 23) {
                    dateView.setTextAppearance(android.R.style.TextAppearance_Medium);
                    if (getActivity() != null) {
                        dateView.setTextColor(getActivity().getResources().getColor(R.color.blue, null));
                    }
                } else {
                    dateView.setTextAppearance(context, android.R.style.TextAppearance_Medium);
                    if (getActivity() != null) {
                        dateView.setTextColor(getActivity().getResources().getColor(R.color.blue));
                    }
                }
                dateView.setTypeface(null, Typeface.BOLD);

                Calendar mCalendar = Calendar.getInstance();
                mCalendar.setTimeInMillis(entry.timestamp * 1000);
                if (day != mCalendar.get(Calendar.DAY_OF_MONTH)) {
                    day = mCalendar.get(Calendar.DAY_OF_MONTH);
                    DateFormat sdf = DateFormat.getDateInstance(DateFormat.FULL);
                    String text = sdf.format(mCalendar.getTime());
                    dateView.setText(text);
                    scrollViewLayout.addView(dateView);
                }

                WeatherForecastLayout layout = new WeatherForecastLayout(context);
                layout.setTimeFormat(timeFormat);
                layout.setTemperature(true, settings.temperatureUnit);
                layout.setWindSpeed(true, settings.speedUnit);
                layout.update(entry);
                scrollViewLayout.addView(layout);
            }

            // avoid flickering of the UI
            scrollView.animate().setDuration(fadeDuration).alpha(1);
            fadeDuration = 1000;
        }
    }

}
