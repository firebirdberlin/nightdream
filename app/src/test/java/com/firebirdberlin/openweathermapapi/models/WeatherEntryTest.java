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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@RunWith(JUnit4.class)
public class WeatherEntryTest {

    private WeatherEntry weatherEntry;
    private long testTimestampEpochSecond; // in seconds
    private float testLat;
    private float testLon;

    @Before
    public void setUp() {
        weatherEntry = new WeatherEntry();
        // A specific timestamp for testing: 2023-10-27 12:00:00 UTC
        testTimestampEpochSecond = 1698393600L;
        testLat = 52.5200f; // Berlin latitude
        testLon = 13.4050f; // Berlin longitude

        weatherEntry.timestamp = testTimestampEpochSecond;
        weatherEntry.lat = testLat;
        weatherEntry.lon = testLon;
    }

    @Test
    public void getSunriseTime_dateMatchesTimestampDate() {
        long sunriseTimeMillis = weatherEntry.getSunriseTime();

        // Convert testTimestamp to LocalDate
        LocalDate expectedDate = Instant.ofEpochSecond(testTimestampEpochSecond)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        // Convert actual sunrise time to LocalDate
        LocalDate actualDate = Instant.ofEpochMilli(sunriseTimeMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void getSunsetTime_dateMatchesTimestampDate() {
        long sunsetTimeMillis = weatherEntry.getSunsetTime();

        // Convert testTimestamp to LocalDate
        LocalDate expectedDate = Instant.ofEpochSecond(testTimestampEpochSecond)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        // Convert actual sunset time to LocalDate
        LocalDate actualDate = Instant.ofEpochMilli(sunsetTimeMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void getSunriseTime_withPredefinedValue_returnsPredefinedValue() {
        long predefinedSunrise = 1698375600000L; // Example: 2023-10-27 05:00:00 UTC
        weatherEntry.sunriseTime = predefinedSunrise / 1000; // stored in seconds

        long actualSunrise = weatherEntry.getSunriseTime();
        assertEquals(predefinedSunrise, actualSunrise);
    }

    @Test
    public void getSunsetTime_withPredefinedValue_returnsPredefinedValue() {
        long predefinedSunset = 1698418800000L; // Example: 2023-10-27 17:00:00 UTC
        weatherEntry.sunsetTime = predefinedSunset / 1000; // stored in seconds

        long actualSunset = weatherEntry.getSunsetTime();
        assertEquals(predefinedSunset, actualSunset);
    }

    @Test
    public void isNight_returnsCorrectValue() {
        // Berlin, 2023-10-27, timezone Europe/Berlin (CEST/CET)
        // Sunrise around 07:14 (CEST -> CET transition on Oct 29)
        // Sunset around 17:39 (CEST -> CET transition on Oct 29)

        // Test during the day (e.g., 12:00 UTC, which is 14:00 in Berlin in Oct, CEST)
        weatherEntry.timestamp = 1698393600L; // 2023-10-27 12:00:00 UTC
        weatherEntry.lat = 52.5200f; // Berlin latitude
        weatherEntry.lon = 13.4050f; // Berlin longitude

        // Set explicit sunrise/sunset for testing to avoid suncalc library differences
        // These values are in milliseconds
        long explicitSunriseMillis = Instant.parse("2023-10-27T05:14:00Z").toEpochMilli(); // 07:14 Berlin time
        long explicitSunsetMillis = Instant.parse("2023-10-27T15:39:00Z").toEpochMilli(); // 17:39 Berlin time

        weatherEntry.sunriseTime = explicitSunriseMillis / 1000;
        weatherEntry.sunsetTime = explicitSunsetMillis / 1000;

        // Scenario 1: Daytime (12:00 UTC / 14:00 Berlin time)
        weatherEntry.timestamp = 1698393600L; // 2023-10-27 12:00:00 UTC
        assertFalse("Should be day at 14:00 Berlin time", weatherEntry.isNight());

        // Scenario 2: Nighttime (after sunset, e.g., 20:00 UTC / 22:00 Berlin time)
        weatherEntry.timestamp = 1698412800L; // 2023-10-27 20:00:00 UTC
        assertTrue("Should be night at 22:00 Berlin time", weatherEntry.isNight());

        // Scenario 3: Nighttime (before sunrise, e.g., 03:00 UTC / 05:00 Berlin time on the *same calendar day*)
        // Note: For suncalc, sunrise/sunset times are for the *day* of the timestamp.
        // If the current timestamp is before the calculated sunrise of the *same day*, it's considered night.
        weatherEntry.timestamp = 1698364800L; // 2023-10-27 03:00:00 UTC
        assertTrue("Should be night at 05:00 Berlin time (before sunrise)", weatherEntry.isNight());

        // Scenario 4: Exactly at sunrise (should be day)
        weatherEntry.timestamp = explicitSunriseMillis / 1000;
        assertFalse("Should be day exactly at sunrise", weatherEntry.isNight());

        // Scenario 5: Exactly at sunset (should be night)
        weatherEntry.timestamp = explicitSunsetMillis / 1000;
        assertTrue("Should be night exactly at sunset", weatherEntry.isNight());

        // Scenario 6: Invalid sunrise/sunset (should return false)
        weatherEntry.sunriseTime = 0L;
        weatherEntry.sunsetTime = 0L;
        weatherEntry.timestamp = 1698393600L; // Daytime timestamp
        assertFalse("Should return false if sunrise/sunset are invalid", weatherEntry.isNight());
    }
}
