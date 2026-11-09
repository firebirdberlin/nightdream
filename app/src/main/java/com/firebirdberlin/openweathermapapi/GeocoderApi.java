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
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.firebirdberlin.nightdream.BuildConfig;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.util.HttpReader;
import com.firebirdberlin.openweathermapapi.models.City;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GeocoderApi {

    private static final String TAG = GeocoderApi.class.getSimpleName();
    private static final long CACHE_EXPIRATION_30_DAYS = 1000L * 60 * 60 * 24 * 30; // 30 days

    /**
     * Wrapper function to find cities based on the current build flavor.
     *
     * @param context The application context.
     * @param query   The city name or part of it to search for.
     * @return A list of City objects, or an empty list if no cities are found or an error occurs.
     */
    public static List<City> findCities(Context context, String query) {
        if ("noGms".equals(BuildConfig.FLAVOR)) {
            return findCitiesByNameOSM(context, query);
        } else {
            return findCitiesByName(context, query);
        }
    }

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

        List<Address> addresses;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            CountDownLatch latch = new CountDownLatch(1);
            List<Address> resultList = new ArrayList<>();
            geocoder.getFromLocationName(query, 10, new Geocoder.GeocodeListener() {
                @Override
                public void onGeocode(@NonNull List<Address> addressesList) {
                    resultList.addAll(addressesList);
                    latch.countDown();
                }

                @Override
                public void onError(@Nullable String errorMessage) {
                    Log.e(TAG, "Error finding locations for query: " + query + ", "
                            + errorMessage);
                    latch.countDown();
                }
            });
            try {
                if (!latch.await(5, TimeUnit.SECONDS)) {
                    Log.w(TAG, "Geocoder timed out for query: " + query);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "Geocoder interrupted for query: " + query, e);
            }
            addresses = resultList;
        } else {
            addresses = getFromLocationNameLegacy(geocoder, query, 10);
        }

        if (!Utility.isEmpty(addresses)) {
            for (Address address : addresses) {
                City city = new City();
                city.name = address.getLocality(); // Use getLocality for city name
                city.countryCode = address.getCountryCode();
                city.countryName = address.getCountryName();
                city.postalCode = address.getPostalCode();
                city.lat = address.getLatitude();
                city.lon = address.getLongitude();
                // Note: Geocoder.getFromLocationName does not provide a city ID like
                // OpenWeatherMap. We'll leave city.id as its default value.
                cities.add(city);
            }
        }

        return cities;
    }

    @SuppressWarnings("deprecation")
    private static List<Address> getFromLocationNameLegacy(Geocoder geocoder, String query,
                                                          int maxResults) {
        try {
            return geocoder.getFromLocationName(query, maxResults);
        } catch (IOException e) {
            Log.e(TAG, "Error finding locations for query: " + query, e);
            return null;
        }
    }

    /**
     * Finds cities based on a query string using the OpenStreetMap Nominatim API and maps them to
     * City objects.
     *
     * @param context The application context (not directly used for Nominatim, but kept for
     *                signature consistency).
     * @param query   The city name or part of it to search for.
     * @return A list of City objects, or an empty list if no cities are found or an error occurs.
     */
    static List<City> findCitiesByNameOSM(Context context, String query) {
        List<City> cities = new ArrayList<>();

        try {
            // Construct the URL for the Nominatim API query
            final String NOMINATIM_SEARCH_BASE_URL = "https://nominatim.openstreetmap.org/search?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "format";
            final String LIMIT_PARAM = "limit";
            final String ADDRESSDETAILS_PARAM = "addressdetails"; // To get detailed address info

            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            // Request address details to better extract city, country, etc.
            String builtUri = NOMINATIM_SEARCH_BASE_URL +
                    QUERY_PARAM + "=" + encodedQuery + "&" +
                    FORMAT_PARAM + "=" + "json" + "&" +
                    LIMIT_PARAM + "=" + "10" + "&" +
                    ADDRESSDETAILS_PARAM + "=" + "1";

            HttpReader httpReader = new HttpReader(context, "nominatim_search_cache.json");
            httpReader.setCacheExpirationTimeMillis(CACHE_EXPIRATION_30_DAYS);
            String jsonResponse = httpReader.readUrl(builtUri, false);

            if (Utility.isEmpty(jsonResponse)) {
                Log.e(TAG, "No response from Nominatim API for query: " + query);
                return cities;
            }

            // Parse JSON response using Gson
            JsonArray jsonArray = JsonParser.parseString(jsonResponse).getAsJsonArray();

            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();

                City city = new City();
                city.lat = jsonObject.get("lat").getAsDouble();
                city.lon = jsonObject.get("lon").getAsDouble();

                String countryCode = null;
                String countryName = null;
                String cityName = null;
                String postalCode = null;

                if (jsonObject.has("address")) {
                    JsonObject addressObject = jsonObject.getAsJsonObject("address");
                    Log.i(TAG, "addressObject: " + addressObject.toString());
                    if (addressObject.has("city")) {
                        cityName = addressObject.get("city").getAsString();
                    } else if (addressObject.has("town")) {
                        cityName = addressObject.get("town").getAsString();
                    } else if (addressObject.has("village")) {
                        cityName = addressObject.get("village").getAsString();
                    } else if (addressObject.has("county")) { // sometimes it's just county
                        cityName = addressObject.get("county").getAsString();
                    } else if (addressObject.has("state")) { // sometimes only state is available
                        cityName = addressObject.get("state").getAsString();
                    }

                    if (addressObject.has("country_code")) {
                        countryCode = addressObject.get("country_code").getAsString()
                                .toUpperCase(Locale.ROOT);
                    }
                    if (addressObject.has("country")) {
                        countryName = addressObject.get("country").getAsString();
                    }
                    if (addressObject.has("postcode")) {
                        postalCode = addressObject.get("postcode").getAsString();
                    }
                }

                // Fallback to name property if city name is still null from address object
                if (cityName == null && jsonObject.has("name")) {
                    cityName = jsonObject.get("name").getAsString();
                }

                // Final fallback if nothing else works, use the first part of display_name
                if (cityName == null && jsonObject.has("display_name")) {
                    String displayName = jsonObject.get("display_name").getAsString();
                    String[] parts = displayName.split(", ");
                    if (parts.length > 0) {
                        cityName = parts[0];
                    }
                }

                city.name = cityName;
                city.countryCode = countryCode;
                city.countryName = countryName;
                city.postalCode = postalCode;

                // Add to list only if a city name could be determined and coordinates are valid
                if (!Utility.isEmpty(city.name) && city.lat != 0.0 && city.lon != 0.0) {
                    cities.add(city);
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "Error finding locations for query (Nominatim): " + query, e);
        }
        return cities;
    }

    /**
     * Finds city details based on latitude and longitude using Android's Geocoder.
     *
     * @param context The application context.
     * @param lat     Latitude of the location.
     * @param lon     Longitude of the location.
     * @return A City object with available details, or null if no address is found or an error
     *         occurs.
     */
    public static City findCityByCoordinates(Context context, double lat, double lon) {
        if ("noGms".equals(BuildConfig.FLAVOR)) {
            return findCityByCoordinatesOSM(context, lat, lon);
        } else {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            Address address = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                CountDownLatch latch = new CountDownLatch(1);
                List<Address> resultList = new ArrayList<>();
                geocoder.getFromLocation(lat, lon, 1, new Geocoder.GeocodeListener() {
                    @Override
                    public void onGeocode(@NonNull List<Address> addressesList) {
                        resultList.addAll(addressesList);
                        latch.countDown();
                    }

                    @Override
                    public void onError(@Nullable String errorMessage) {
                        Log.e(TAG, "Error finding location for coordinates: " + lat + ", "
                                + lon + ", " + errorMessage);
                        latch.countDown();
                    }
                });
                try {
                    if (!latch.await(5, TimeUnit.SECONDS)) {
                        Log.w(TAG, "Geocoder timed out for coordinates: " + lat + ", " + lon);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.e(TAG, "Geocoder interrupted for coordinates: " + lat + ", " + lon, e);
                }
                if (!Utility.isEmpty(resultList)) {
                    address = resultList.get(0);
                }
            } else {
                List<Address> addressList = getFromLocationLegacy(geocoder, lat, lon, 1);
                if (!Utility.isEmpty(addressList)) {
                    address = addressList.get(0);
                }
            }

            if (address != null) {
                City city = new City();
                city.name = address.getLocality();
                city.countryCode = address.getCountryCode();
                city.countryName = address.getCountryName();
                city.postalCode = address.getPostalCode();
                city.lat = address.getLatitude();
                city.lon = address.getLongitude();
                return city;
            }
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private static List<Address> getFromLocationLegacy(Geocoder geocoder, double lat, double lon,
                                                       int maxResults) {
        try {
            return geocoder.getFromLocation(lat, lon, maxResults);
        } catch (IOException e) {
            Log.e(TAG, "Error finding location for coordinates: " + lat + ", " + lon, e);
            return null;
        }
    }

    /**
     * Finds city details based on latitude and longitude using the OpenStreetMap Nominatim API.
     *
     * @param context The application context (not directly used for Nominatim, but kept for
     *                signature consistency).
     * @param lat     Latitude of the location.
     * @param lon     Longitude of the location.
     * @return A City object with available details, or null if no address is found or an error
     *         occurs.
     */
    static City findCityByCoordinatesOSM(Context context, double lat, double lon) {
        // Construct the URL for the Nominatim reverse geocoding API
        String jsonResponse = getJsonResponse(context, lat, lon);

        if (Utility.isEmpty(jsonResponse)) {
            Log.e(TAG, "No response from Nominatim Reverse API for coordinates: "
                    + lat + ", " + lon);
            return null;
        }

        // Parse JSON response using Gson
        JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

        // Check if display_name exists, which means a location was found
        if (jsonObject.has("display_name")) {
            City city = new City();
            city.lat = lat; // Use the provided lat/lon as Nominatim returns approximate values
            city.lon = lon;

            String countryCode = null;
            String countryName = null;
            String cityName = null;
            String postalCode = null;

            if (jsonObject.has("address")) {
                JsonObject addressObject = jsonObject.getAsJsonObject("address");
                Log.i(TAG, "addressObject (reverse): " + addressObject.toString());
                if (addressObject.has("city")) {
                    cityName = addressObject.get("city").getAsString();
                } else if (addressObject.has("town")) {
                    cityName = addressObject.get("town").getAsString();
                } else if (addressObject.has("village")) {
                    cityName = addressObject.get("village").getAsString();
                } else if (addressObject.has("county")) { // sometimes it's just county
                    cityName = addressObject.get("county").getAsString();
                } else if (addressObject.has("state")) { // sometimes only state is available
                    cityName = addressObject.get("state").getAsString();
                }

                if (addressObject.has("country_code")) {
                    countryCode = addressObject.get("country_code").getAsString()
                            .toUpperCase(Locale.ROOT);
                }
                if (addressObject.has("country")) {
                    countryName = addressObject.get("country").getAsString();
                }
                if (addressObject.has("postcode")) {
                    postalCode = addressObject.get("postcode").getAsString();
                }
            }

            // Fallback to display_name if city name is still null
            if (cityName == null && jsonObject.has("display_name")) {
                String displayName = jsonObject.get("display_name").getAsString();
                String[] parts = displayName.split(", ");
                if (parts.length > 0) {
                    cityName = parts[0];
                }
            }

            city.name = cityName;
            city.countryCode = countryCode;
            city.countryName = countryName;
            city.postalCode = postalCode;

            if (!Utility.isEmpty(city.name)) {
                return city;
            }
        }
        return null;
    }

    private static String getJsonResponse(Context context, double lat, double lon) {
        final String NOMINATIM_REVERSE_BASE_URL = "https://nominatim.openstreetmap.org/reverse?";
        final String LAT_PARAM = "lat";
        final String LON_PARAM = "lon";
        final String FORMAT_PARAM = "format";
        final String ADDRESSDETAILS_PARAM = "addressdetails"; // To get detailed address info

        String builtUri = NOMINATIM_REVERSE_BASE_URL +
                LAT_PARAM + "=" + lat + "&" +
                LON_PARAM + "=" + lon + "&" +
                FORMAT_PARAM + "=" + "json" + "&" +
                ADDRESSDETAILS_PARAM + "=" + "1";

        HttpReader httpReader = new HttpReader(context, "nominatim_reverse_cache.json");
        httpReader.setCacheExpirationTimeMillis(CACHE_EXPIRATION_30_DAYS);
        return httpReader.readUrl(builtUri, false);
    }
}