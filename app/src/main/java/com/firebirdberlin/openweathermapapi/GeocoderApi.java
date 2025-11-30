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

package com.firebirdberlin.openweathermapapi;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.firebirdberlin.openweathermapapi.models.City;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GeocoderApi {

    private static final String TAG = "GeocoderApi";

    /**
     * Finds cities based on a query string using Android's Geocoder and maps them to City objects.
     *
     * @param context The application context.
     * @param query   The city name or part of it to search for.
     * @return A list of City objects, or an empty list if no cities are found or an error occurs.
     */
    static List<City> findCitiesByName(Context context, String query) {
        List<City> cities = new ArrayList<>();
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 10); // Get up to 10 results

            if (addresses != null) {
                for (Address address : addresses) {
                    City city = new City();
                    city.name = address.getLocality(); // Use getLocality for city name
                    city.countryCode = address.getCountryCode();
                    city.countryName = address.getCountryName();
                    city.postalCode = address.getPostalCode();
                    city.lat = address.getLatitude();
                    city.lon = address.getLongitude();
                    // Note: Geocoder.getFromLocationName does not provide a city ID like OpenWeatherMap.
                    // We'll leave city.id as its default value (likely 0 or null depending on City class definition).
                    cities.add(city);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error finding locations for query: " + query, e);
        }

        return cities;
    }

    /**
     * Finds city details based on latitude and longitude using Android's Geocoder.
     *
     * @param context The application context.
     * @param lat     Latitude of the location.
     * @param lon     Longitude of the location.
     * @return A City object with available details, or null if no address is found or an error occurs.
     */
    public static City findCityByCoordinates(Context context, double lat, double lon) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        Address address = null;
        try {
            List<Address> addressList = geocoder.getFromLocation(lat, lon, 1);
            if (addressList != null && !addressList.isEmpty()) {
                address = addressList.get(0);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error finding location for coordinates: " + lat + ", " + lon, e);
        }

        if (address != null) {
            City city = new City();
            city.name = address.getLocality();
            city.countryCode = address.getCountryCode();
            city.countryName = address.getCountryName();
            city.postalCode = address.getPostalCode();
            city.lat = address.getLatitude();
            city.lon = address.getLongitude();
            // City ID is not available from Geocoder for coordinates
            return city;
        }
        return null;
    }
}
