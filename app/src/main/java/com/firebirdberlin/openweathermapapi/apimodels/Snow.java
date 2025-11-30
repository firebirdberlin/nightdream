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

public class Snow {
    @SerializedName("1h")
    private Double volume1h; // Use Double (object type) to allow null if not present
    @SerializedName("3h")
    private Double volume3h; // Use Double (object type) to allow null if not present

    public Double getVolume1h() {
        return volume1h;
    }

    public void setVolume1h(Double volume1h) {
        this.volume1h = volume1h;
    }

    public Double getVolume3h() {
        return volume3h;
    }

    public void setVolume3h(Double volume3h) {
        this.volume3h = volume3h;
    }
}