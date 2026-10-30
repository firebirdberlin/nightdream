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

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.firebirdberlin.openweathermapapi.GeocoderApi;
import com.firebirdberlin.openweathermapapi.models.City;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AirQualityRepository {

    private static final String TAG = AirQualityRepository.class.getSimpleName();
    public static final double MAX_STATION_DISTANCE_KM = 50.0;

    // Germany Bounding Box
    private static final double GERMANY_MIN_LAT = 47.27;
    private static final double GERMANY_MAX_LAT = 55.09;
    private static final double GERMANY_MIN_LON = 5.87;
    private static final double GERMANY_MAX_LON = 15.04;

    private final Context context;
    private final AirQualityApiClient apiClient;

    public AirQualityRepository(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.apiClient = new AirQualityApiClient(context);
    }

    public AirQualityRepository(@NonNull Context context, @NonNull AirQualityApiClient apiClient) {
        this.context = context.getApplicationContext();
        this.apiClient = apiClient;
    }

    public static boolean isLocationInGermany(@Nullable String countryCode, double lat, double lon) {
        if ("DE".equalsIgnoreCase(countryCode)) {
            return true;
        }
        return lat >= GERMANY_MIN_LAT && lat <= GERMANY_MAX_LAT
                && lon >= GERMANY_MIN_LON && lon <= GERMANY_MAX_LON;
    }

    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double earthRadiusKm = 6371.0;
        return earthRadiusKm * c;
    }

    @Nullable
    public AirQualityData getAirQualityForLocation(double latitude, double longitude) {
        City city = GeocoderApi.findCityByCoordinates(context, latitude, longitude);
        String countryCode = city != null ? city.countryCode : null;

        if (!isLocationInGermany(countryCode, latitude, longitude)) {
            Log.d(TAG, "Location (" + latitude + ", " + longitude + ") is outside Germany. Skipping.");
            return null;
        }

        List<AirQualityStation> allStations = apiClient.fetchStations();
        if (allStations.isEmpty()) {
            Log.w(TAG, "Station list is empty.");
            return null;
        }

        List<AirQualityStation> nearbyStations = new ArrayList<>();
        for (AirQualityStation station : allStations) {
            double dist = calculateDistanceKm(latitude, longitude, station.latitude, station.longitude);
            if (dist <= MAX_STATION_DISTANCE_KM) {
                station.distanceKm = dist;
                nearbyStations.add(station);
            }
        }

        if (nearbyStations.isEmpty()) {
            Log.d(TAG, "No station found within " + MAX_STATION_DISTANCE_KM + " km.");
            return null;
        }

        Collections.sort(nearbyStations, new Comparator<AirQualityStation>() {
            @Override
            public int compare(AirQualityStation s1, AirQualityStation s2) {
                return Double.compare(s1.distanceKm, s2.distanceKm);
            }
        });

        AirQualityData bestFallback = null;

        for (AirQualityStation station : nearbyStations) {
            List<AirQualityData> readings = apiClient.fetchAirQuality(station);
            AirQualityData recent = AirQualityParser.getMostRecentReading(readings);

            if (recent != null && recent.hasAnyData()) {
                if (recent.hasFullData()) {
                    return recent;
                } else if (bestFallback == null) {
                    bestFallback = recent;
                }
            }
        }

        return bestFallback;
    }
}
