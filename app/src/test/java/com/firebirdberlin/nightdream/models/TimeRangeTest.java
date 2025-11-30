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

package com.firebirdberlin.nightdream.models;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;

public class TimeRangeTest {
    private Calendar getCalendar(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    @Test
    public void testInRange() {
        Calendar start = getCalendar(10, 20);
        Calendar end = getCalendar(12, 20);
        TimeRange range = new TimeRange(start, end);

        Assert.assertFalse(range.inRange(getCalendar(10, 19)));
        Assert.assertTrue(range.inRange(getCalendar(10, 20)));
        Assert.assertTrue(range.inRange(getCalendar(10, 48)));
        Assert.assertTrue(range.inRange(getCalendar(12, 19)));
        Assert.assertFalse(range.inRange(getCalendar(12, 20)));
        Assert.assertFalse(range.inRange(getCalendar(12, 21)));
    }

    @Test
    public void testInRange1() {
        Calendar start = getCalendar(0, 0);
        Calendar end = getCalendar(0, 0);

        // always in range
        TimeRange range = new TimeRange(start, end);
        Assert.assertTrue(range.inRange(getCalendar(0, 0)));
        Assert.assertTrue(range.inRange(getCalendar(12, 0)));
        Assert.assertTrue(range.inRange(getCalendar(15, 23)));
    }

    @Test
    public void testInRange2() {
        Calendar now = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        start.add(Calendar.DATE, -1);
        start.add(Calendar.HOUR_OF_DAY, -2);
        end.add(Calendar.DATE, -2);
        end.add(Calendar.HOUR_OF_DAY, -1);

        // always in range
        TimeRange range = new TimeRange(start, end);
        Assert.assertFalse(range.inRange(now));
    }

    @Test
    public void testInRange3() {
        Calendar now = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        start.add(Calendar.DATE, -1);
        start.add(Calendar.HOUR_OF_DAY, +1);
        end.add(Calendar.DATE, -2);
        end.add(Calendar.HOUR_OF_DAY, +2);

        // always in range
        TimeRange range = new TimeRange(start, end);
        Assert.assertFalse(range.inRange(now));
    }

    @Test
    public void testInRange4() {
        Calendar now = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        start.add(Calendar.DATE, -1);
        start.add(Calendar.HOUR_OF_DAY, -1);
        end.add(Calendar.DATE, -2);
        end.add(Calendar.HOUR_OF_DAY, +1);

        // always in range
        TimeRange range = new TimeRange(start, end);
        Assert.assertTrue(range.inRange(now));
    }


    @Test
    public void testGetNextEvent__now__inRange() {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        start.add(Calendar.HOUR_OF_DAY, -1);
        end.add(Calendar.HOUR_OF_DAY, +1);
        Calendar expected = (Calendar) end.clone();

        start.add(Calendar.DATE, -10);
        end.add(Calendar.DATE, -5);

        TimeRange range = new TimeRange(start, end);
        Assert.assertEquals(expected, range.getNextEvent());
    }

    @Test
    public void testGetNextEvent__start__today() {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        start.add(Calendar.HOUR_OF_DAY, +1);
        Calendar expected = (Calendar) start.clone();

        end.add(Calendar.HOUR_OF_DAY, +2);
        start.add(Calendar.DATE, -10);
        end.add(Calendar.DATE, -5);

        TimeRange range = new TimeRange(start, end);
        Assert.assertEquals(expected, range.getNextEvent());
    }

    @Test
    public void testGetNextEvent__start__tomorrow() {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        start.add(Calendar.HOUR_OF_DAY, -2);
        end.add(Calendar.HOUR_OF_DAY, -1);

        TimeRange range = new TimeRange(start, end);
        Calendar expected = sameTimeTomorrow(start);

        Assert.assertEquals(expected, range.getNextEvent());
    }

    @Test
    public void testGetNextEvent__end__tomorrow() {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        end.add(Calendar.HOUR_OF_DAY, +23);

        TimeRange range = new TimeRange(start, end);
        Assert.assertEquals(end, range.getNextEvent());
    }

    Calendar sameTimeTomorrow(Calendar from) {
        Calendar cal = (Calendar) from.clone();
        cal.setTimeInMillis(from.getTimeInMillis());
        cal.add(Calendar.DATE, 1);
        return cal;
    }
}