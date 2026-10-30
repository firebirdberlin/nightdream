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

package com.firebirdberlin.nightdream.airquality;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.firebirdberlin.nightdream.Utility;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AirQualityParser {

    private static final String TAG = AirQualityParser.class.getSimpleName();

    private static final int COMPONENT_PM10 = 1;
    private static final int COMPONENT_O3 = 3;
    private static final int COMPONENT_NO2 = 5;
    private static final int COMPONENT_PM25 = 9;

    @NonNull
    public static List<AirQualityStation> parseStations(@Nullable String jsonString) {
        List<AirQualityStation> stations = new ArrayList<>();
        if (Utility.isEmpty(jsonString)) {
            return stations;
        }

        try {
            JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();
            if (!root.has("stations") || !root.get("stations").isJsonObject()) {
                return stations;
            }

            JsonObject stationsObj = root.getAsJsonObject("stations");
            for (Map.Entry<String, JsonElement> entry : stationsObj.entrySet()) {
                if (!entry.getValue().isJsonArray()) {
                    continue;
                }
                JsonArray arr = entry.getValue().getAsJsonArray();
                if (arr.size() < 9) {
                    continue;
                }

                String id = arr.get(0).getAsString();
                String code = arr.get(1).getAsString();
                String name = arr.get(2).getAsString();
                String city = !arr.get(3).isJsonNull() ? arr.get(3).getAsString() : null;
                String lonStr = arr.get(7).getAsString();
                String latStr = arr.get(8).getAsString();

                if (Utility.isEmpty(code) || Utility.isEmpty(name)) {
                    continue;
                }

                try {
                    double longitude = Double.parseDouble(lonStr);
                    double latitude = Double.parseDouble(latStr);
                    stations.add(new AirQualityStation(id, code, name, city, latitude, longitude));
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing stations JSON", e);
        }

        return stations;
    }

    @NonNull
    public static List<AirQualityData> parseAirQuality(@Nullable String jsonString,
                                                       @NonNull AirQualityStation station) {
        List<AirQualityData> readings = new ArrayList<>();
        if (Utility.isEmpty(jsonString)) {
            return readings;
        }

        try {
            JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();
            if (!root.has("data") || !root.get("data").isJsonObject()) {
                return readings;
            }

            JsonObject dataObj = root.getAsJsonObject("data");
            for (Map.Entry<String, JsonElement> stationEntry : dataObj.entrySet()) {
                if (!stationEntry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject timeMap = stationEntry.getValue().getAsJsonObject();
                for (Map.Entry<String, JsonElement> timeEntry : timeMap.entrySet()) {
                    if (!timeEntry.getValue().isJsonArray()) {
                        continue;
                    }
                    JsonArray tuple = timeEntry.getValue().getAsJsonArray();
                    if (tuple.size() < 2) {
                        continue;
                    }

                    String timeEndCet = tuple.get(0).getAsString();
                    int lqiIndex = -1;
                    try {
                        lqiIndex = tuple.get(1).getAsInt();
                    } catch (Exception ignored) {
                    }

                    Double pm25 = null;
                    Double pm10 = null;
                    Double no2 = null;
                    Double o3 = null;

                    for (int i = 3; i < tuple.size(); i++) {
                        if (!tuple.get(i).isJsonArray()) {
                            continue;
                        }
                        JsonArray compArr = tuple.get(i).getAsJsonArray();
                        if (compArr.size() < 2) {
                            continue;
                        }
                        int componentId = compArr.get(0).getAsInt();
                        double val = compArr.get(1).getAsDouble();

                        if (componentId == COMPONENT_PM25) {
                            pm25 = val;
                        } else if (componentId == COMPONENT_PM10) {
                            pm10 = val;
                        } else if (componentId == COMPONENT_NO2) {
                            no2 = val;
                        } else if (componentId == COMPONENT_O3) {
                            o3 = val;
                        }
                    }

                    readings.add(new AirQualityData(
                            station.code,
                            station.getDisplayName(),
                            station.distanceKm,
                            timeEndCet,
                            lqiIndex,
                            pm25, pm10, no2, o3
                    ));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing air quality JSON", e);
        }

        return readings;
    }

    @Nullable
    public static AirQualityData getMostRecentReading(@NonNull List<AirQualityData> readings) {
        if (readings.isEmpty()) {
            return null;
        }
        for (int i = readings.size() - 1; i >= 0; i--) {
            AirQualityData data = readings.get(i);
            if (data.hasAnyData()) {
                return data;
            }
        }
        return readings.get(readings.size() - 1);
    }
}
