package com.firebirdberlin.nightdream;

import junit.framework.TestCase;

import org.junit.Assert;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/**
 * Created by stefan on 29.10.17.
 */
public class UtilityTest extends TestCase {
    public void testGetWeekdayStringsforLocaleDe() throws Exception {
        String[] weekdays = Utility.getWeekdayStringsForLocale(new Locale("de"));

        for (int i = 0; i < weekdays.length; i++) {
            System.out.println(weekdays[i]);
        }

        String[] expected = {"", "S", "M", "D", "M", "D", "F", "S"};
        assertEquals(8, weekdays.length);
        Assert.assertArrayEquals(expected, weekdays);
    }

    public void testGetWeekdayStringsforLocaleUs() throws Exception {
        String[] weekdays = Utility.getWeekdayStringsForLocale(new Locale("us"));
        String[] expected = {"", "S", "M", "T", "W", "T", "F", "S"};

        assertEquals(8, weekdays.length);
        Assert.assertArrayEquals(expected, weekdays);
    }

}