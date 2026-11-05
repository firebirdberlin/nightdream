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

import com.firebirdberlin.nightdream.Utility;

import org.junit.Assert;
import org.junit.Test;

public class AirQualityRepositoryTest {

    @Test
    public void testCalculateDistanceKm() {
        // Berlin (52.5200, 13.4050) to Potsdam (52.4014, 13.0602) is approx 27 km
        double distance = Utility.calculateDistanceKm(52.5200, 13.4050, 52.4014, 13.0602);
        Assert.assertEquals(27.0, distance, 3.0);

        // Same point distance should be 0
        double samePoint = Utility.calculateDistanceKm(52.5200, 13.4050, 52.5200, 13.4050);
        Assert.assertEquals(0.0, samePoint, 0.001);

        // Test AirQualityStation calculateDistanceKm
        AirQualityStation station = new AirQualityStation("1", "code", "Berlin", "Berlin", 52.5200, 13.4050);
        double stationDist = station.calculateDistanceKm(52.4014, 13.0602);
        Assert.assertEquals(27.0, stationDist, 3.0);
    }
}
