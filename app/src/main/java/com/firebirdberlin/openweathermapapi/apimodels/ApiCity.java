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

import com.firebirdberlin.openweathermapapi.models.City; // Import your existing City model
import com.google.gson.annotations.SerializedName;

public class ApiCity {
    private int id;
    private String name;
    private ApiCoord coord; // Renamed to ApiCoord
    private String country; // API uses 'country', will map to your City's 'countryCode'
    private long population;
    private long timezone;
    private long sunrise;
    private long sunset;

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

    public ApiCoord getCoord() { // Renamed to ApiCoord
        return coord;
    }

    public void setCoord(ApiCoord coord) { // Renamed to ApiCoord
        this.coord = coord;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public long getPopulation() {
        return population;
    }

    public void setPopulation(long population) {
        this.population = population;
    }

    public long getTimezone() {
        return timezone;
    }

    public void setTimezone(long timezone) {
        this.timezone = timezone;
    }

    public long getSunrise() {
        return sunrise;
    }

    public void setSunrise(long sunrise) {
        this.sunrise = sunrise;
    }

    public long getSunset() {
        return sunset;
    }

    public void setSunset(long sunset) {
        this.sunset = sunset;
    }

    /**
     * Converts this ApiCity object into your application's City model.
     *
     * @return An instance of com.firebirdberlin.openweathermapapi.models.City
     */
    public City toAppCity() {
        City appCity = new City();
        appCity.id = this.id;
        appCity.name = this.name;
        appCity.countryCode = this.country; // Map API 'country' to app 'countryCode'

        if (this.coord != null) {
            appCity.lat = this.coord.getLat();
            appCity.lon = this.coord.getLon();
        } else {
            // Handle case where coord might be null if API response is incomplete
            appCity.lat = 0.0f;
            appCity.lon = 0.0f;
        }
        return appCity;
    }
}