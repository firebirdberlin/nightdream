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

import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.util.HttpReader;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import java.util.TimeZone;

public class AirQualityApiClient {

    private static final String TAG = AirQualityApiClient.class.getSimpleName();
    private static final String BASE_URL = "https://luftdaten.umweltbundesamt.de/api-proxy/";

    private static final long META_CACHE_EXPIRATION_MS = 1000L * 60L * 60L * 24L; // 24 Hours
    private static final long AQ_CACHE_EXPIRATION_MS = 1000L * 60L * 60L; // 60 Minutes

    private final Context context;

    public AirQualityApiClient(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    public List<AirQualityStation> fetchStations() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("CET"));
        String todayStr = dateFormat.format(new Date());

        String url = BASE_URL + "meta/json?use=airquality&date_from=" + todayStr
                + "&date_to=" + todayStr + "&time_from=1&time_to=24&lang=de";

        HttpReader httpReader = new HttpReader(context, "airquality_meta.json");
        httpReader.setCacheExpirationTimeMillis(META_CACHE_EXPIRATION_MS);

        String response = httpReader.readUrl(url, false);
        return AirQualityParser.parseStations(response);
    }

    @NonNull
    public List<AirQualityData> fetchAirQuality(@NonNull AirQualityStation station) {
        return fetchAirQualityForDateRange(station, null, null);
    }

    @NonNull
    public List<AirQualityData> fetchAirQualityForDateRange(@NonNull AirQualityStation station,
                                                            @Nullable String dateFrom,
                                                            @Nullable String dateTo) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("CET"));

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        int currentHourCet = cal.get(Calendar.HOUR_OF_DAY);

        if (Utility.isEmpty(dateFrom)) {
            dateFrom = dateFormat.format(cal.getTime());
        }
        if (Utility.isEmpty(dateTo)) {
            dateTo = dateFormat.format(cal.getTime());
        }

        // If current hour is early morning (e.g. <= 5 AM CET), query from yesterday to ensure recent readings are found
        if (currentHourCet < 5 && dateFrom.equals(dateTo)) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
            dateFrom = dateFormat.format(cal.getTime());
        }

        String url = BASE_URL + "airquality/json?date_from=" + dateFrom
                + "&date_to=" + dateTo + "&time_from=1&time_to=24&station=" + station.code;

        Log.d(TAG, "Requesting air quality for station " + station.code + ": " + url);

        HttpReader httpReader = new HttpReader(context, "airquality_data.json");
        httpReader.setCacheExpirationTimeMillis(AQ_CACHE_EXPIRATION_MS);

        String response = httpReader.readUrl(url, false);
        return AirQualityParser.parseAirQuality(response, station);
    }
}
