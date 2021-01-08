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
}