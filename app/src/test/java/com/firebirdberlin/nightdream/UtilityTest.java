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

package com.firebirdberlin.nightdream;

import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;



public class UtilityTest {
    @Test
    public void testGetWeekdayStringsForLocaleDe() {
        String[] weekdays = Utility.getWeekdayStringsForLocale(new Locale("de"));

        for (int i = 0; i < weekdays.length; i++) {
            System.out.println(weekdays[i]);
        }

        String[] expected = {"", "S", "M", "D", "M", "D", "F", "S"};
        Assert.assertEquals(8, weekdays.length);
        Assert.assertArrayEquals(expected, weekdays);
    }

    @Test
    public void testGetWeekdayStringsForLocaleUs() {
        String[] weekdays = Utility.getWeekdayStringsForLocale(new Locale("us"));
        String[] expected = {"", "S", "M", "T", "W", "T", "F", "S"};

        Assert.assertEquals(8, weekdays.length);
        Assert.assertArrayEquals(expected, weekdays);
    }

    @Test
    public void testContainsAny() {
        String haystack = "haystack 0 1";
        Assert.assertTrue(Utility.containsAny(haystack, " ", "0"));
        Assert.assertFalse(Utility.containsAny(haystack, "3", "4", "z"));
    }

    @Test
    public void testEqualsAny() {
        Assert.assertTrue(Utility.equalsAny("0", " ", "0"));
        Assert.assertTrue(Utility.equalsAny(" ", " ", "0", "1", "2", "3"));
        Assert.assertTrue(Utility.equalsAny("0", " ", "0", "1", "2", "3"));
        Assert.assertTrue(Utility.equalsAny("1", " ", "0", "1", "2", "3"));
        Assert.assertTrue(Utility.equalsAny("2", " ", "0", "1", "2", "3"));
        Assert.assertTrue(Utility.equalsAny("3", " ", "0", "1", "2", "3"));
        Assert.assertFalse(Utility.equalsAny("0", "3", "4", "z"));
    }
}