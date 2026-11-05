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

import com.firebirdberlin.nightdream.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

@RunWith(JUnit4.class)
public class WeatherEntryTest {

    private WeatherEntry weatherEntry;
    private long testTimestampEpochSecond;
    private float testLat;
    private float testLon;

    @Before
    public void setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
        Locale.setDefault(Locale.US);
        weatherEntry = new WeatherEntry();
        testTimestampEpochSecond = 1698393600L; // 2023-10-27 12:00:00 UTC
        testLat = 52.5200f; // Berlin latitude
        testLon = 13.4050f; // Berlin longitude

        weatherEntry.timestamp = testTimestampEpochSecond;
        weatherEntry.lat = testLat;
        weatherEntry.lon = testLon;
    }

    @Test
    public void getSunriseTime_dateMatchesTimestampDate() {
        long sunriseTimeMillis = weatherEntry.getSunriseTime();

        LocalDate expectedDate = Instant.ofEpochSecond(testTimestampEpochSecond)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        LocalDate actualDate = Instant.ofEpochMilli(sunriseTimeMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void getSunsetTime_dateMatchesTimestampDate() {
        long sunsetTimeMillis = weatherEntry.getSunsetTime();

        LocalDate expectedDate = Instant.ofEpochSecond(testTimestampEpochSecond)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        LocalDate actualDate = Instant.ofEpochMilli(sunsetTimeMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void isNight_returnsCorrectValue() {
        long explicitSunriseMillis = weatherEntry.getSunriseTime();
        long explicitSunsetMillis = weatherEntry.getSunsetTime();

        // Scenario 1: Daytime
        weatherEntry.timestamp = 1698393600L; // 2023-10-27 12:00:00 UTC
        assertFalse("Should be day at 14:00 Berlin time", weatherEntry.isNight());

        // Scenario 2: Nighttime (after sunset)
        weatherEntry.timestamp = 1698436800L; // 2023-10-27 20:00:00 UTC
        assertTrue("Should be night at 22:00 Berlin time", weatherEntry.isNight());

        // Scenario 3: Nighttime (before sunrise)
        weatherEntry.timestamp = 1698364800L; // 2023-10-27 03:00:00 UTC
        assertTrue("Should be night at 05:00 Berlin time (before sunrise)", weatherEntry.isNight());

        // Scenario 4: Exactly at sunrise
        weatherEntry.timestamp = explicitSunriseMillis / 1000;
        assertFalse("Should be day exactly at sunrise", weatherEntry.isNight());

        // Scenario 5: Exactly at sunset
        weatherEntry.timestamp = explicitSunsetMillis / 1000;
        assertTrue("Should be night exactly at sunset", weatherEntry.isNight());
    }

    @Test
    public void formatTemperatureText_units() {
        weatherEntry.temperature = 293.15; // 20 °C, 68 °F, 293 K
        weatherEntry.apparentTemperature = 295.15;

        assertEquals("20°C", weatherEntry.formatTemperatureText(WeatherEntry.CELSIUS));
        assertEquals("68°F", weatherEntry.formatTemperatureText(WeatherEntry.FAHRENHEIT));
        assertEquals("293 K", weatherEntry.formatTemperatureText(WeatherEntry.KELVIN));

        String apparent = weatherEntry.formatTemperatureText(WeatherEntry.CELSIUS, true);
        assertEquals("20°C (22°C)", apparent);
    }

    @Test
    public void formatHumidityText_validAndInvalid() {
        weatherEntry.humidity = 55;
        assertEquals("55 %", weatherEntry.formatHumidityText());

        weatherEntry.humidity = -1;
        assertEquals("", weatherEntry.formatHumidityText());
    }

    @Test
    public void formatWindText_units() {
        weatherEntry.windSpeed = 5.0; // m/s
        assertEquals("5.0 m/s", weatherEntry.formatWindText(WeatherEntry.METERS_PER_SECOND));
        assertEquals("11.2 mi/h", weatherEntry.formatWindText(WeatherEntry.MILES_PER_HOUR));
        assertEquals("18.0 km/h", weatherEntry.formatWindText(WeatherEntry.KM_PER_HOUR));
        assertEquals("9.7 kn", weatherEntry.formatWindText(WeatherEntry.KNOT));
        assertEquals("3 Bft", weatherEntry.formatWindText(WeatherEntry.BEAUFORT));
    }

    @Test
    public void isValid_and_ageMillis() {
        weatherEntry.request_timestamp = System.currentTimeMillis();
        weatherEntry.timestamp = System.currentTimeMillis() / 1000;
        assertTrue(weatherEntry.isValid());
        assertTrue(weatherEntry.ageMillis() >= 0L);

        weatherEntry.timestamp = WeatherEntry.INVALID;
        assertFalse(weatherEntry.isValid());
    }

    @Test
    public void moonMethods_executeWithoutError() {
        assertNotNull(weatherEntry.getMoonIllumination());
        weatherEntry.getMoonriseTime();
        weatherEntry.getMoonsetTime();
        weatherEntry.isMoonUp();
    }

    @Test
    public void json_serialization_and_deserialization() {
        weatherEntry.cityName = "Berlin";
        weatherEntry.clouds = 20;
        weatherEntry.description = "clear sky";

        String json = weatherEntry.toJson();
        assertNotNull(json);

        WeatherEntry parsed = WeatherEntry.fromJson(json);
        assertNotNull(parsed);
        assertEquals("Berlin", parsed.cityName);
        assertEquals(20, parsed.clouds);
        assertEquals("clear sky", parsed.description);

        assertNull(WeatherEntry.fromJson(null));
        assertNull(WeatherEntry.fromJson(""));
    }

    @Test
    public void getWeatherIconIdentifier_modes() {
        weatherEntry.weatherIconMeteoconsSymbol = "A";
        assertEquals("A", weatherEntry.getWeatherIconIdentifier(Settings.WEATHER_ICON_MODE_DEFAULT, true));
        assertEquals("weather_day_avd", weatherEntry.getWeatherIconIdentifier(Settings.WEATHER_ICON_MODE_ANIMATED, false));
    }

    private void assertNull(Object obj) {
        org.junit.Assert.assertNull(obj);
    }
}
