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

import org.junit.Assert;
import org.junit.Test;

public class AirQualityRepositoryTest {

    @Test
    public void testIsLocationInGermany() {
        // By Country Code
        Assert.assertTrue(AirQualityRepository.isLocationInGermany("DE", 52.5200, 13.4050));
        Assert.assertTrue(AirQualityRepository.isLocationInGermany("de", 52.5200, 13.4050));
        Assert.assertFalse(AirQualityRepository.isLocationInGermany("US", 37.7749, -122.4194));

        // By Bounding Box fallback
        Assert.assertTrue(AirQualityRepository.isLocationInGermany(null, 52.5200, 13.4050)); // Berlin
        Assert.assertTrue(AirQualityRepository.isLocationInGermany(null, 53.5511, 9.9937));  // Hamburg
        Assert.assertFalse(AirQualityRepository.isLocationInGermany(null, 48.8566, 2.3522));  // Paris
        Assert.assertFalse(AirQualityRepository.isLocationInGermany(null, 48.2082, 16.3738)); // Vienna
    }

    @Test
    public void testCalculateDistanceKm() {
        // Berlin (52.5200, 13.4050) to Potsdam (52.4014, 13.0602) is approx 27 km
        double distance = AirQualityRepository.calculateDistanceKm(52.5200, 13.4050, 52.4014, 13.0602);
        Assert.assertEquals(27.0, distance, 3.0);

        // Same point distance should be 0
        double samePoint = AirQualityRepository.calculateDistanceKm(52.5200, 13.4050, 52.5200, 13.4050);
        Assert.assertEquals(0.0, samePoint, 0.001);
    }
}
