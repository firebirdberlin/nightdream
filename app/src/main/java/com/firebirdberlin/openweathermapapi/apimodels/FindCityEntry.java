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

import java.util.List;

public class FindCityEntry {
    private int id;
    private String name;
    private ApiCoord coord; // Reuse existing ApiCoord
    private FindSys sys; // Specific Sys for this endpoint

    // Other fields (main, weather, wind, etc.) are ignored as they are not needed for your City model.

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ApiCoord getCoord() {
        return coord;
    }

    public void setCoord(ApiCoord coord) {
        this.coord = coord;
    }

    public FindSys getSys() {
        return sys;
    }

    public void setSys(FindSys sys) {
        this.sys = sys;
    }
}