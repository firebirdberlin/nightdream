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

public class AirQualityDataTest {

    @Test
    public void testHasLqi() {
        AirQualityData validData = new AirQualityData("DE01", "Station", 1.0, "2026-03-30 12:00:00", 2, 10.0, 20.0, 15.0, 30.0);
        Assert.assertTrue(validData.hasLqi());
        Assert.assertTrue(validData.hasAnyData());
        Assert.assertTrue(validData.hasFullData());

        AirQualityData noneData = new AirQualityData("DE01", "Station", 1.0, null, -1, null, null, null, null);
        Assert.assertFalse(noneData.hasLqi());
        Assert.assertFalse(noneData.hasAnyData());
        Assert.assertFalse(noneData.hasFullData());

        AirQualityData partialData = new AirQualityData("DE01", "Station", 1.0, null, -1, 10.0, null, null, null);
        Assert.assertFalse(partialData.hasLqi());
        Assert.assertTrue(partialData.hasAnyData());
        Assert.assertFalse(partialData.hasFullData());
    }

    @Test
    public void testFormatValue() {
        Assert.assertEquals("–", AirQualityData.formatValue(null));
        Assert.assertEquals("25", AirQualityData.formatValue(24.8));
    }
}
