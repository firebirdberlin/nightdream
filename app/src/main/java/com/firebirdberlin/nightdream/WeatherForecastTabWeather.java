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
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.util.List;

public class WeatherForecastTabWeather extends Fragment {

    private static final String TAG = WeatherForecastTabWeather.class.getSimpleName();
    private static final int FADE_DURATION = 1000;
    private RecyclerView recyclerView;
    private WeatherForecastAdapter adapter;
    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_weather_forecast_scrollview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerView);
        if (recyclerView != null && context != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            adapter = new WeatherForecastAdapter(context);
            recyclerView.setAdapter(adapter);
        }
    }

    public void onRequestFinished(@Nullable List<WeatherEntry> entries, @Nullable Settings settings) {
        Log.d(TAG, "onRequestFinished() List<WeatherEntry>");
        if (recyclerView != null) {
            recyclerView.setAlpha(0f);
        }

        if (adapter != null) {
            adapter.setEntries(entries, settings);
        }

        if (recyclerView != null) {
            recyclerView.animate().setDuration(FADE_DURATION).alpha(1f);
        }
    }
}
