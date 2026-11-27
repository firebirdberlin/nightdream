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

package com.firebirdberlin.nightdream;

import org.junit.Assert;
import org.junit.Test;

public class WindSpeedConversionTest {

    @Test
    public void testMetersPerSecondToBeaufort() {
        Assert.assertEquals(0, WindSpeedConversion.metersPerSecondToBeaufort(0.0));
        Assert.assertEquals(0, WindSpeedConversion.metersPerSecondToBeaufort(0.29));
        Assert.assertEquals(1, WindSpeedConversion.metersPerSecondToBeaufort(0.3));
        Assert.assertEquals(1, WindSpeedConversion.metersPerSecondToBeaufort(1.59));
        Assert.assertEquals(2, WindSpeedConversion.metersPerSecondToBeaufort(1.6));
        Assert.assertEquals(3, WindSpeedConversion.metersPerSecondToBeaufort(3.4));
        Assert.assertEquals(4, WindSpeedConversion.metersPerSecondToBeaufort(5.5));
        Assert.assertEquals(5, WindSpeedConversion.metersPerSecondToBeaufort(8.0));
        Assert.assertEquals(6, WindSpeedConversion.metersPerSecondToBeaufort(10.8));
        Assert.assertEquals(7, WindSpeedConversion.metersPerSecondToBeaufort(13.9));
        Assert.assertEquals(8, WindSpeedConversion.metersPerSecondToBeaufort(17.2));
        Assert.assertEquals(9, WindSpeedConversion.metersPerSecondToBeaufort(20.8));
        Assert.assertEquals(10, WindSpeedConversion.metersPerSecondToBeaufort(24.5));
        Assert.assertEquals(11, WindSpeedConversion.metersPerSecondToBeaufort(28.5));
        Assert.assertEquals(12, WindSpeedConversion.metersPerSecondToBeaufort(32.7));
        Assert.assertEquals(12, WindSpeedConversion.metersPerSecondToBeaufort(50.0));
    }

    @Test
    public void testMetersPerSecondToMilesPerHour() {
        Assert.assertEquals(0.0, WindSpeedConversion.metersPerSecondToMilesPerHour(0.0), 0.0001);
        Assert.assertEquals(22.3694, WindSpeedConversion.metersPerSecondToMilesPerHour(10.0), 0.01);
    }

    @Test
    public void testMetersPerSecondToKilometersPerHour() {
        Assert.assertEquals(0.0, WindSpeedConversion.metersPerSecondToKilometersPerHour(0.0), 0.0001);
        Assert.assertEquals(36.0, WindSpeedConversion.metersPerSecondToKilometersPerHour(10.0), 0.0001);
    }

    @Test
    public void testMetersPerSecondToKnot() {
        Assert.assertEquals(0.0, WindSpeedConversion.metersPerSecondToKnot(0.0), 0.0001);
        Assert.assertEquals(19.4384, WindSpeedConversion.metersPerSecondToKnot(10.0), 0.001);
    }
}
