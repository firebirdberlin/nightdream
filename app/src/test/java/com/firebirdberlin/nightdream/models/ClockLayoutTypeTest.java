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

package com.firebirdberlin.nightdream.models;

import org.junit.Assert;
import org.junit.Test;

public class ClockLayoutTypeTest {

    @Test
    public void testGetValues() {
        Assert.assertEquals(0, ClockLayoutType.DIGITAL.getValue());
        Assert.assertEquals(1, ClockLayoutType.ANALOG.getValue());
        Assert.assertEquals(2, ClockLayoutType.ANALOG2.getValue());
        Assert.assertEquals(3, ClockLayoutType.ANALOG3.getValue());
        Assert.assertEquals(4, ClockLayoutType.ANALOG4.getValue());
        Assert.assertEquals(5, ClockLayoutType.DIGITAL_FLIP.getValue());
        Assert.assertEquals(6, ClockLayoutType.CALENDAR.getValue());
        Assert.assertEquals(7, ClockLayoutType.DIGITAL2.getValue());
        Assert.assertEquals(8, ClockLayoutType.DIGITAL3.getValue());
        Assert.assertEquals(9, ClockLayoutType.DIGITAL_ANIMATED.getValue());
    }

    @Test
    public void testFromValueInt() {
        Assert.assertEquals(ClockLayoutType.DIGITAL, ClockLayoutType.fromValue(0));
        Assert.assertEquals(ClockLayoutType.ANALOG, ClockLayoutType.fromValue(1));
        Assert.assertEquals(ClockLayoutType.DIGITAL_ANIMATED, ClockLayoutType.fromValue(9));
        Assert.assertEquals(ClockLayoutType.DIGITAL, ClockLayoutType.fromValue(99));
    }

    @Test
    public void testFromValueString() {
        Assert.assertEquals(ClockLayoutType.DIGITAL, ClockLayoutType.fromValue("0", ClockLayoutType.ANALOG));
        Assert.assertEquals(ClockLayoutType.ANALOG, ClockLayoutType.fromValue("1", ClockLayoutType.DIGITAL));
        Assert.assertEquals(ClockLayoutType.DIGITAL, ClockLayoutType.fromValue("invalid", ClockLayoutType.DIGITAL));
        String nullStr = null;
        Assert.assertEquals(ClockLayoutType.ANALOG, ClockLayoutType.fromValue(nullStr, ClockLayoutType.ANALOG));
    }
}
