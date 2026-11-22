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

package com.firebirdberlin.nightdream.ui;

import org.junit.Assert;
import org.junit.Test;

public class PowerNapSwipeTest {

    @Test
    public void testPowerNapMinutesCalculationSwipingRight() {
        float startX = 150f;
        float endX = 850f;

        // At start (x = startX) -> 1 minute
        int minAtStart = AlarmClockView.calculatePowerNapMinutes(startX, startX, endX, true);
        Assert.assertEquals(1, minAtStart);

        // At end (x = endX) -> 60 minutes
        int minAtEnd = AlarmClockView.calculatePowerNapMinutes(endX, startX, endX, true);
        Assert.assertEquals(60, minAtEnd);

        // Beyond end -> 60 minutes
        int minBeyondEnd = AlarmClockView.calculatePowerNapMinutes(endX + 100f, startX, endX, true);
        Assert.assertEquals(60, minBeyondEnd);

        // All intermediate values when swiping right must be multiples of 5 or 1 minute
        for (float x = startX; x <= endX; x += 10f) {
            int mins = AlarmClockView.calculatePowerNapMinutes(x, startX, endX, true);
            Assert.assertTrue(mins >= 1 && mins <= 60);
            Assert.assertTrue("Value " + mins + " must be multiple of 5 or 1",
                    mins == 1 || mins % 5 == 0);
        }
    }

    @Test
    public void testPowerNapMinutesCalculationSwipingLeft() {
        float startX = 150f;
        float endX = 850f;

        // At start (x = startX) -> 1 minute
        int minAtStart = AlarmClockView.calculatePowerNapMinutes(startX, startX, endX, false);
        Assert.assertEquals(1, minAtStart);

        // At end (x = endX) -> 60 minutes
        int minAtEnd = AlarmClockView.calculatePowerNapMinutes(endX, startX, endX, false);
        Assert.assertEquals(60, minAtEnd);

        // Swiping left provides minute precision
        int minMid = AlarmClockView.calculatePowerNapMinutes(startX + (endX - startX) * 0.5f,
                startX, endX, false);
        Assert.assertEquals(31, minMid);
    }

    @Test
    public void testPowerNapRemainingTimeFormatting() {
        // > 60 minutes: returns null (indicating show day and time as usual)
        Assert.assertNull(formatPowerNapRemainingTime(61 * 60 * 1000L));

        // 5 to 60 minutes: show remaining minutes
        Assert.assertEquals("60 min", formatPowerNapRemainingTime(60 * 60 * 1000L));
        Assert.assertEquals("20 min", formatPowerNapRemainingTime(20 * 60 * 1000L));
        Assert.assertEquals("5 min", formatPowerNapRemainingTime(5 * 60 * 1000L));

        // < 5 minutes: count down with seconds
        Assert.assertEquals("04:59", formatPowerNapRemainingTime(4 * 60 * 1000L + 59 * 1000L));
        Assert.assertEquals("01:05", formatPowerNapRemainingTime(60 * 1000L + 5 * 1000L));
        Assert.assertEquals("00:45", formatPowerNapRemainingTime(45 * 1000L));
        Assert.assertEquals("00:00", formatPowerNapRemainingTime(0L));
    }

    private static String formatPowerNapRemainingTime(long remainingMs) {
        if (remainingMs <= 60 * 60 * 1000L) {
            long validRemainingMs = Math.max(0L, remainingMs);
            long totalSeconds = validRemainingMs / 1000;
            long totalMinutes = (validRemainingMs + 59999) / 60000;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;

            if (validRemainingMs < 5 * 60 * 1000L) {
                return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds);
            } else {
                return String.format(java.util.Locale.US, "%d min", totalMinutes);
            }
        }
        return null;
    }
}
