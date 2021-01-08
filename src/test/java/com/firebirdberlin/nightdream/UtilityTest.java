package com.firebirdberlin.nightdream;

import junit.framework.TestCase;

import org.junit.Assert;

import java.util.Locale;



public class UtilityTest extends TestCase {
    public void testGetWeekdayStringsForLocaleDe() throws Exception {
        String[] weekdays = Utility.getWeekdayStringsForLocale(new Locale("de"));

        for (int i = 0; i < weekdays.length; i++) {
            System.out.println(weekdays[i]);
        }

        String[] expected = {"", "S", "M", "D", "M", "D", "F", "S"};
        assertEquals(8, weekdays.length);
        Assert.assertArrayEquals(expected, weekdays);
    }

    public void testGetWeekdayStringsForLocaleUs() throws Exception {
        String[] weekdays = Utility.getWeekdayStringsForLocale(new Locale("us"));
        String[] expected = {"", "S", "M", "T", "W", "T", "F", "S"};

        assertEquals(8, weekdays.length);
        Assert.assertArrayEquals(expected, weekdays);
    }

    public void testContainsAny() {
        String haystack = "haystack 0 1";
        assertTrue(Utility.containsAny(haystack, " ", "0"));
        assertFalse(Utility.containsAny(haystack, "3", "4", "z"));
    }
}