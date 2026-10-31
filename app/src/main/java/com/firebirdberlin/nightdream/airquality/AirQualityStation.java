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

import com.firebirdberlin.nightdream.Utility;

public class AirQualityStation {

    @NonNull
    public final String id;
    @NonNull
    public final String code;
    @NonNull
    public final String name;
    @Nullable
    public final String city;
    public final double latitude;
    public final double longitude;
    public double distanceKm;

    public AirQualityStation(@NonNull String id,
                             @NonNull String code,
                             @NonNull String name,
                             @Nullable String city,
                             double latitude,
                             double longitude) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceKm = 0.0;
    }

    @NonNull
    public String getDisplayName() {
        if (city != null && !city.isEmpty() && !name.contains(city)) {
            return city + " (" + name + ")";
        }
        return name;
    }

    public double calculateDistanceKm(double lat, double lon) {
        return Utility.calculateDistanceKm(latitude, longitude, lat, lon);
    }

    public double distanceTo(double lat, double lon) {
        return calculateDistanceKm(lat, lon);
    }
}
