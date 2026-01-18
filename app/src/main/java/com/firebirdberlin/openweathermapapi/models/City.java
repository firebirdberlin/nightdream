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

package com.firebirdberlin.openweathermapapi.models;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.util.Objects;

public class City {

    public int id = -1;
    public String name;
    public String countryCode;
    public String countryName;
    public String postalCode;
    public double lat = 0.0f;
    public double lon = 0.0f;


    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (countryCode != null && !countryCode.isEmpty()) {
            sb.append(" (").append(countryCode).append(")");
        }
        if (postalCode != null && !postalCode.isEmpty()) {
            sb.append(" [").append(postalCode).append("]");
        }
        sb.append(String.format(" (%.2f, %.2f)", lat, lon));
        return sb.toString();
    }

    public static City fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, City.class);
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        City city = (City) o;
        return id == city.id && Objects.equals(name, city.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
