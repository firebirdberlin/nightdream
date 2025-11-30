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

package com.firebirdberlin.openweathermapapi.apimodels;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OpenWeatherMapForecastResponse {
    private String cod;
    private double message;
    private int cnt;
    private List<ListEntry> list;
    private ApiCity city; // Renamed to ApiCity

    public String getCod() {
        return cod;
    }

    public void setCod(String cod) {
        this.cod = cod;
    }

    public double getMessage() {
        return message;
    }

    public void setMessage(double message) {
        this.message = message;
    }

    public int getCnt() {
        return cnt;
    }

    public void setCnt(int cnt) {
        this.cnt = cnt;
    }

    public List<ListEntry> getList() {
        return list;
    }

    public void setList(List<ListEntry> list) {
        this.list = list;
    }

    public ApiCity getCity() { // Renamed to ApiCity
        return city;
    }

    public void setCity(ApiCity city) { // Renamed to ApiCity
        this.city = city;
    }
}