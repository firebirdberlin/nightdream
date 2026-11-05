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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class CityTest {

    @Test
    public void isGermany_countryCodeDE_returnsTrue() {
        City city = new City();
        city.countryCode = "DE";
        assertTrue(city.isGermany());
        assertTrue(City.isGermany(city));
    }

    @Test
    public void isGermany_countryCodeLowercaseDe_returnsTrue() {
        City city = new City();
        city.countryCode = "de";
        assertTrue(city.isGermany());
        assertTrue(City.isGermany(city));
    }

    @Test
    public void isGermany_countryNameGermany_returnsTrue() {
        City city = new City();
        city.countryName = "Germany";
        assertTrue(city.isGermany());
        assertTrue(City.isGermany(city));
    }

    @Test
    public void isGermany_countryNameDeutschland_returnsTrue() {
        City city = new City();
        city.countryName = "Deutschland";
        assertTrue(city.isGermany());
        assertTrue(City.isGermany(city));
    }

    @Test
    public void isGermany_otherCountry_returnsFalse() {
        City city = new City();
        city.countryCode = "US";
        city.countryName = "United States";
        assertFalse(city.isGermany());
        assertFalse(City.isGermany(city));
    }

    @Test
    public void isGermany_nullCity_returnsTrue() {
        assertTrue(City.isGermany(null));
    }

    @Test
    public void isGermany_nullCountryCodeAndName_returnsFalse() {
        City city = new City();
        city.countryCode = null;
        city.countryName = null;
        assertFalse(city.isGermany());
        assertFalse(City.isGermany(city));
    }

    @Test
    public void jsonSerialization_and_deserialization() {
        City city = new City();
        city.id = 12345;
        city.name = "Berlin";
        city.countryCode = "DE";
        city.countryName = "Germany";
        city.postalCode = "10115";
        city.lat = 52.52f;
        city.lon = 13.40f;

        String json = city.toJson();
        assertNotNull(json);

        City parsed = City.fromJson(json);
        assertNotNull(parsed);
        assertEquals(city.id, parsed.id);
        assertEquals(city.name, parsed.name);
        assertEquals(city.countryCode, parsed.countryCode);
        assertEquals(city.countryName, parsed.countryName);
        assertEquals(city.postalCode, parsed.postalCode);
        assertEquals(city.lat, parsed.lat, 0.001f);
        assertEquals(city.lon, parsed.lon, 0.001f);
    }

    @Test
    public void equals_and_hashCode() {
        City c1 = new City();
        c1.id = 1;
        c1.name = "Berlin";

        City c2 = new City();
        c2.id = 1;
        c2.name = "Berlin";

        City c3 = new City();
        c3.id = 2;
        c3.name = "Munich";

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
        assertFalse(c1.equals(c3));
    }
}
