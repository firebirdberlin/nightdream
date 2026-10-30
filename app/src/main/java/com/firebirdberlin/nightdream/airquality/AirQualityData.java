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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public class AirQualityData {

    public static final int LQI_VERY_GOOD = 1;
    public static final int LQI_GOOD = 2;
    public static final int LQI_MODERATE = 3;
    public static final int LQI_POOR = 4;
    public static final int LQI_VERY_POOR = 5;

    @NonNull
    public final String stationCode;
    @NonNull
    public final String stationName;
    public final double distanceKm;
    @Nullable
    public final String timeEndCet;
    public final int lqiIndex;
    @Nullable
    public final Double pm25;
    @Nullable
    public final Double pm10;
    @Nullable
    public final Double no2;
    @Nullable
    public final Double o3;

    public AirQualityData(@NonNull String stationCode,
                          @NonNull String stationName,
                          double distanceKm,
                          @Nullable String timeEndCet,
                          int lqiIndex,
                          @Nullable Double pm25,
                          @Nullable Double pm10,
                          @Nullable Double no2,
                          @Nullable Double o3) {
        this.stationCode = stationCode;
        this.stationName = stationName;
        this.distanceKm = distanceKm;
        this.timeEndCet = timeEndCet;
        this.lqiIndex = lqiIndex;
        this.pm25 = pm25;
        this.pm10 = pm10;
        this.no2 = no2;
        this.o3 = o3;
    }

    public boolean hasLqi() {
        return lqiIndex >= LQI_VERY_GOOD && lqiIndex <= LQI_VERY_POOR;
    }

    public boolean hasAnyData() {
        return hasLqi() || pm25 != null || pm10 != null || no2 != null || o3 != null;
    }

    public boolean hasFullData() {
        return hasLqi() && pm25 != null && pm10 != null && no2 != null && o3 != null;
    }

    @NonNull
    public static String formatValue(@Nullable Double value) {
        if (value == null) {
            return "–";
        }
        return String.format(Locale.getDefault(), "%.0f", value);
    }
}
