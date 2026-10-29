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
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.firebirdberlin.nightdream.ui.DirectionIconView;
import com.firebirdberlin.nightdream.ui.WeatherForecastLayout;
import com.firebirdberlin.nightdream.ui.WeatherGraphView;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class WeatherForecastAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = WeatherForecastAdapter.class.getSimpleName();
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ENTRY = 1;
    private static final int TYPE_SUBSCRIPTION_HEADER = 2;

    private final Context context;
    private final List<DayGroup> dayGroups = new ArrayList<>();
    private final List<ListItem> items = new ArrayList<>();
    private Settings settings;
    private boolean showSubscriptionHeader = false;
    private boolean collapseAll = false;

    public void setShowSubscriptionHeader(boolean show) {
        this.showSubscriptionHeader = show;
    }

    public void setCollapseAll(boolean collapse) {
        this.collapseAll = collapse;
    }

    public interface ListItem {
        int getType();
    }

    public static class SubscriptionHeaderItem implements ListItem {
        @Override
        public int getType() {
            return TYPE_SUBSCRIPTION_HEADER;
        }
    }

    public static class DayGroup implements ListItem {
        public final String dateHeader;
        public final List<WeatherEntry> entries = new ArrayList<>();
        public boolean isCollapsed = true; // collapsed by default, except for today

        public double minTemperature = Double.MAX_VALUE;
        public double maxTemperature = -Double.MAX_VALUE;
        public double totalRain = 0.0;
        public int minHumidity = Integer.MAX_VALUE;
        public int maxHumidity = -1;
        public double minWindSpeed = Double.MAX_VALUE;
        public double maxWindSpeed = -Double.MAX_VALUE;
        public int avgWindDirection = -1;

        public DayGroup(String dateHeader, boolean isCollapsed) {
            this.dateHeader = dateHeader;
            this.isCollapsed = isCollapsed;
        }

        public void addEntry(WeatherEntry entry) {
            entries.add(entry);
            if (entry.temperature < minTemperature) {
                minTemperature = entry.temperature;
            }
            if (entry.temperature > maxTemperature) {
                maxTemperature = entry.temperature;
            }

            double rain = entry.rain3h > 0 ? entry.rain3h : (entry.rain1h > 0 ? entry.rain1h : 0.0);
            totalRain += rain;

            if (entry.humidity >= 0) {
                if (entry.humidity < minHumidity) {
                    minHumidity = entry.humidity;
                }
                if (entry.humidity > maxHumidity) {
                    maxHumidity = entry.humidity;
                }
            }

            if (entry.windSpeed < minWindSpeed) {
                minWindSpeed = entry.windSpeed;
            }
            if (entry.windSpeed > maxWindSpeed) {
                maxWindSpeed = entry.windSpeed;
            }
        }

        public void computeSummary() {
            if (minHumidity == Integer.MAX_VALUE) {
                minHumidity = -1;
            }
            if (minTemperature == Double.MAX_VALUE) {
                minTemperature = 0.0;
            }
            if (maxTemperature == -Double.MAX_VALUE) {
                maxTemperature = 0.0;
            }
            if (minWindSpeed == Double.MAX_VALUE) {
                minWindSpeed = 0.0;
            }
            if (maxWindSpeed == -Double.MAX_VALUE) {
                maxWindSpeed = 0.0;
            }

            double sumSin = 0.0;
            double sumCos = 0.0;
            int count = 0;
            for (WeatherEntry e : entries) {
                if (e.windDirection >= 0) {
                    double rad = Math.toRadians(e.windDirection);
                    sumSin += Math.sin(rad);
                    sumCos += Math.cos(rad);
                    count++;
                }
            }
            if (count > 0) {
                double avgRad = Math.atan2(sumSin / count, sumCos / count);
                double avgDeg = Math.toDegrees(avgRad);
                if (avgDeg < 0) {
                    avgDeg += 360.0;
                }
                avgWindDirection = (int) Math.round(avgDeg) % 360;
            } else {
                avgWindDirection = -1;
            }
        }

        @Override
        public int getType() {
            return TYPE_HEADER;
        }
    }

    public static class EntryItem implements ListItem {
        public final WeatherEntry entry;

        public EntryItem(WeatherEntry entry) {
            this.entry = entry;
        }

        @Override
        public int getType() {
            return TYPE_ENTRY;
        }
    }

    public WeatherForecastAdapter(@NonNull Context context) {
        this.context = context;
    }

    public void setEntries(@Nullable List<WeatherEntry> entries, @Nullable Settings settings) {
        this.settings = settings;
        this.dayGroups.clear();
        this.items.clear();

        if (entries == null || entries.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        long now = System.currentTimeMillis();
        int currentDay = -1;
        int currentYear = -1;
        DayGroup currentGroup = null;

        for (WeatherEntry entry : entries) {
            if (entry == null || entry.timestamp * 1000 < now - 600000) {
                continue;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(entry.timestamp * 1000);
            int year = calendar.get(Calendar.YEAR);
            int day = calendar.get(Calendar.DAY_OF_YEAR);

            if (day != currentDay || year != currentYear) {
                currentDay = day;
                currentYear = year;
                DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL);
                String header = dateFormat.format(calendar.getTime());
                boolean isToday = Utility.isToday(calendar);
                currentGroup = new DayGroup(header, collapseAll || !isToday);
                dayGroups.add(currentGroup);
            }

            currentGroup.addEntry(entry);
        }

        for (DayGroup group : dayGroups) {
            group.computeSummary();
        }

        rebuildItems();
    }

    private void rebuildItems() {
        items.clear();
        if (showSubscriptionHeader) {
            items.add(new SubscriptionHeaderItem());
        }
        for (DayGroup group : dayGroups) {
            items.add(group);
            if (!group.isCollapsed) {
                for (WeatherEntry entry : group.entries) {
                    items.add(new EntryItem(entry));
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_weather_header, parent, false);
            return new HeaderViewHolder(view);
        } else if (viewType == TYPE_SUBSCRIPTION_HEADER) {
            View view = inflater.inflate(R.layout.item_weather_subscription_header, parent, false);
            return new SubscriptionHeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_weather_forecast_card, parent, false);
            return new EntryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem item = items.get(position);
        if (holder instanceof HeaderViewHolder headerHolder) {
            DayGroup dayGroup = (DayGroup) item;
            headerHolder.dateHeaderView.setText(dayGroup.dateHeader);

            int tempUnit = (settings != null) ? settings.temperatureUnit : WeatherEntry.CELSIUS;
            int speedUnit = (settings != null) ? settings.speedUnit : WeatherEntry.METERS_PER_SECOND;

            String minTempStr = formatTemperature(tempUnit, dayGroup.minTemperature);
            String maxTempStr = formatTemperature(tempUnit, dayGroup.maxTemperature);
            if (dayGroup.minTemperature == dayGroup.maxTemperature) {
                headerHolder.summaryTemperatureView.setText(minTempStr);
            } else {
                headerHolder.summaryTemperatureView.setText(String.format("%s - %s", minTempStr, maxTempStr));
            }

            if (dayGroup.totalRain > 0.0) {
                headerHolder.summaryRainView.setText(String.format(Locale.getDefault(), "%.1f mm", dayGroup.totalRain));
                headerHolder.summaryRainView.setVisibility(View.VISIBLE);
            } else {
                headerHolder.summaryRainView.setVisibility(View.GONE);
            }

            if (dayGroup.minHumidity >= 0) {
                if (dayGroup.minHumidity == dayGroup.maxHumidity) {
                    headerHolder.summaryHumidityView.setText(String.format(Locale.getDefault(), "%d %%", dayGroup.minHumidity));
                } else {
                    headerHolder.summaryHumidityView.setText(String.format(Locale.getDefault(), "%d %% - %d %%", dayGroup.minHumidity, dayGroup.maxHumidity));
                }
                headerHolder.summaryHumidityView.setVisibility(View.VISIBLE);
            } else {
                headerHolder.summaryHumidityView.setVisibility(View.GONE);
            }

            String minWindStr = formatWindSpeedText(dayGroup.minWindSpeed, speedUnit);
            String maxWindStr = formatWindSpeedText(dayGroup.maxWindSpeed, speedUnit);
            if (dayGroup.minWindSpeed == dayGroup.maxWindSpeed) {
                headerHolder.summaryWindView.setText(minWindStr);
            } else {
                headerHolder.summaryWindView.setText(String.format("%s - %s", minWindStr, maxWindStr));
            }

            if (dayGroup.avgWindDirection >= 0) {
                headerHolder.summaryWindDirectionView.setVisibility(View.VISIBLE);
                headerHolder.summaryWindDirectionView.setDirection(dayGroup.avgWindDirection);
                headerHolder.summaryWindDirectionView.setColor(Color.WHITE);
            } else {
                headerHolder.summaryWindDirectionView.setVisibility(View.GONE);
            }

            String graphTimeFormat = (settings != null) ? settings.getFullTimeFormat() : "HH:mm";
            boolean showApparent = (settings != null) && settings.showApparentTemperature;
            headerHolder.summaryWeatherGraph.setTimeFormat(graphTimeFormat);
            if (dayGroup.entries.size() > 1) {
                headerHolder.summaryWeatherGraph.setEntries(dayGroup.entries, tempUnit, showApparent);
                headerHolder.summaryWeatherGraph.setVisibility(View.VISIBLE);
            } else {
                headerHolder.summaryWeatherGraph.setEntries(null, tempUnit, showApparent);
                headerHolder.summaryWeatherGraph.setVisibility(View.GONE);
            }

            if (dayGroup.isCollapsed) {
                headerHolder.summaryLayout.setVisibility(View.VISIBLE);
                headerHolder.expandIndicatorView.setText("▼");
            } else {
                headerHolder.summaryLayout.setVisibility(View.GONE);
                headerHolder.expandIndicatorView.setText("▲");
            }

            headerHolder.itemView.setOnClickListener(v -> {
                dayGroup.isCollapsed = !dayGroup.isCollapsed;
                rebuildItems();
            });
        } else if (holder instanceof EntryViewHolder entryHolder) {
            EntryItem entryItem = (EntryItem) item;

            String timeFormat = (settings != null) ? settings.getFullTimeFormat() : "HH:mm";
            int tempUnit = (settings != null) ? settings.temperatureUnit : WeatherEntry.CELSIUS;
            boolean showApparent = (settings != null) && settings.showApparentTemperature;
            int speedUnit = (settings != null) ? settings.speedUnit : WeatherEntry.METERS_PER_SECOND;

            entryHolder.weatherForecastLayout.setTimeFormat(timeFormat);
            entryHolder.weatherForecastLayout.setTemperature(true, showApparent, tempUnit);
            entryHolder.weatherForecastLayout.setWindSpeed(true, speedUnit);
            entryHolder.weatherForecastLayout.update(entryItem.entry);
        } else if (holder instanceof SubscriptionHeaderViewHolder) {
            View.OnClickListener purchaseListener = view1 -> {
                if (context instanceof WeatherForecastActivity) {
                    ((WeatherForecastActivity) context).showSubscriptionDialog();
                }
                Log.d(TAG, "onclick showSubscriptionDialog");
            };

            int color = Utility.getRandomMaterialColor(context);
            int textColor = Utility.getContrastColor(color);

            TextView title = holder.itemView.findViewById(R.id.title);
            if (title != null) {
                title.setTextColor(textColor);
                title.setBackgroundColor(color);
                title.setOnClickListener(purchaseListener);

                Drawable icon = ContextCompat.getDrawable(context, R.drawable.ic_googleplay);
                if (icon != null) {
                    icon.setColorFilter(new PorterDuffColorFilter(textColor, PorterDuff.Mode.SRC_ATOP));
                    title.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, icon, null);
                }
            }

            TextView upgradeNow = holder.itemView.findViewById(R.id.upgrade_now);
            if (upgradeNow != null) {
                upgradeNow.setOnClickListener(purchaseListener);
                upgradeNow.setBackgroundColor(color);
                upgradeNow.setTextColor(textColor);
            }
        }
    }

    private static String formatTemperature(int temperatureUnit, double temp) {
        return switch (temperatureUnit) {
            case WeatherEntry.CELSIUS ->
                    String.format(Locale.getDefault(), "%d°C", Math.round(temp - 273.15));
            case WeatherEntry.FAHRENHEIT ->
                    String.format(Locale.getDefault(), "%d°F", Math.round(temp * 1.8 - 459.67));
            default -> String.format(Locale.getDefault(), "%d K", Math.round(temp));
        };
    }

    private static String formatWindSpeedText(double windSpeed, int speedUnit) {
        return switch (speedUnit) {
            case WeatherEntry.BEAUFORT ->
                    String.format(Locale.getDefault(), "%d Bft", WindSpeedConversion.metersPerSecondToBeaufort(windSpeed));
            case WeatherEntry.MILES_PER_HOUR -> {
                double mph = WindSpeedConversion.metersPerSecondToMilesPerHour(windSpeed);
                yield String.format(Locale.getDefault(), "%.1f mi/h", mph);
            }
            case WeatherEntry.KM_PER_HOUR -> {
                double kmph = WindSpeedConversion.metersPerSecondToKilometersPerHour(windSpeed);
                yield String.format(Locale.getDefault(), "%.1f km/h", kmph);
            }
            case WeatherEntry.KNOT -> {
                double kn = WindSpeedConversion.metersPerSecondToKnot(windSpeed);
                yield String.format(Locale.getDefault(), "%.1f kn", kn);
            }
            default -> String.format(Locale.getDefault(), "%.1f m/s", windSpeed);
        };
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public final TextView dateHeaderView;
        public final View summaryLayout;
        public final TextView summaryTemperatureView;
        public final TextView summaryRainView;
        public final TextView summaryHumidityView;
        public final TextView summaryWindView;
        public final DirectionIconView summaryWindDirectionView;
        public final WeatherGraphView summaryWeatherGraph;
        public final TextView expandIndicatorView;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            dateHeaderView = itemView.findViewById(R.id.dateHeaderView);
            summaryLayout = itemView.findViewById(R.id.summaryLayout);
            summaryTemperatureView = itemView.findViewById(R.id.summaryTemperatureView);
            summaryRainView = itemView.findViewById(R.id.summaryRainView);
            summaryHumidityView = itemView.findViewById(R.id.summaryHumidityView);
            summaryWindView = itemView.findViewById(R.id.summaryWindView);
            summaryWindDirectionView = itemView.findViewById(R.id.summaryWindDirectionView);
            summaryWeatherGraph = itemView.findViewById(R.id.summaryWeatherGraph);
            expandIndicatorView = itemView.findViewById(R.id.expandIndicatorView);
        }
    }

    public static class EntryViewHolder extends RecyclerView.ViewHolder {
        public final WeatherForecastLayout weatherForecastLayout;

        public EntryViewHolder(@NonNull View itemView) {
            super(itemView);
            weatherForecastLayout = itemView.findViewById(R.id.weatherForecastLayout);
        }
    }

    public static class SubscriptionHeaderViewHolder extends RecyclerView.ViewHolder {
        public SubscriptionHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
