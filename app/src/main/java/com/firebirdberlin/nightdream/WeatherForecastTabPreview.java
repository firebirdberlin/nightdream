/*
 * NightDream
 * Copyright (C) 2026 Stefan Fruhner
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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WeatherForecastTabPreview extends Fragment {

    private Context context;
    private RecyclerView recyclerView;
    private WeatherForecastAdapter adapter;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_weather_forecast_preview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerView);
        if (recyclerView != null && context != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            adapter = new WeatherForecastAdapter(context);
            adapter.setShowSubscriptionHeader(true);
            adapter.setCollapseAll(true);
        }
    }

    public void setupForecastPreview(Settings settings) {
        if (adapter != null && recyclerView != null) {
            adapter.setEntries(getFakeEntries(), settings);
            recyclerView.setAdapter(adapter);
        }
    }

    List<WeatherEntry> getFakeEntries() {
        List<WeatherEntry> entries = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int currentHour = cal.get(Calendar.HOUR_OF_DAY);
        int nextHour = ((currentHour / 3) + 1) * 3;
        if (nextHour >= 24) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
        } else {
            cal.set(Calendar.HOUR_OF_DAY, nextHour);
        }

        int month = cal.get(Calendar.MONTH);
        boolean isWinter = (month == Calendar.DECEMBER
                || month == Calendar.JANUARY
                || month == Calendar.FEBRUARY);

        // Generate 7 days * 8 entries/day = 56 entries
        for (int i = 0; i < 56; i++) {
            WeatherEntry entry = new WeatherEntry();
            entry.cityName = "Ecotopia";
            entry.timestamp = cal.getTimeInMillis() / 1000;
            entry.request_timestamp = System.currentTimeMillis();

            int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
            double baseTemp = 288.15; // ~15°C
            double tempVariation = 5.0 * Math.sin(2.0 * Math.PI * (hourOfDay - 6) / 24.0);
            entry.temperature = baseTemp + tempVariation;
            entry.apparentTemperature = entry.temperature - 1.0;

            entry.humidity = 50 + (int) (20 * Math.sin(i));
            entry.clouds = 40 + (int) (30 * Math.cos(i));
            entry.windSpeed = 2.0 + (i % 4);
            entry.windDirection = (i * 45) % 360;

            if (isWinter) {
                entry.weatherIcon = (hourOfDay >= 6 && hourOfDay < 18) ? "13d" : "13n";
                if (i % 3 == 0) {
                    entry.rain3h = 1.2;
                }
            } else {
                entry.weatherIcon = (hourOfDay >= 6 && hourOfDay < 18) ? "10d" : "10n";
                if (i % 4 == 0) {
                    entry.rain3h = 1.5;
                }
            }

            entries.add(entry);
            cal.add(Calendar.HOUR_OF_DAY, 3);
        }

        return entries;
    }
}
