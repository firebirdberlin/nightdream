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
    public void testPowerNapMinutesCalculation() {
        float startX = 150f;
        float endX = 1000f;
        float availableWidth = endX - startX;

        // Test start (progress = 0) -> 1 minute
        float xStart = startX;
        float progressStart = Math.max(0.0f, Math.min(1.0f, (xStart - startX) / availableWidth));
        int napMinutesStart = 1 + Math.round(progressStart * 59.0f);
        napMinutesStart = Math.max(1, Math.min(60, napMinutesStart));
        Assert.assertEquals(1, napMinutesStart);

        // Test end (progress = 1) -> 60 minutes
        float xEnd = endX;
        float progressEnd = Math.max(0.0f, Math.min(1.0f, (xEnd - startX) / availableWidth));
        int napMinutesEnd = 1 + Math.round(progressEnd * 59.0f);
        napMinutesEnd = Math.max(1, Math.min(60, napMinutesEnd));
        Assert.assertEquals(60, napMinutesEnd);

        // Test middle (progress = 0.5) -> ~30-31 minutes
        float xMid = startX + availableWidth * 0.5f;
        float progressMid = Math.max(0.0f, Math.min(1.0f, (xMid - startX) / availableWidth));
        int napMinutesMid = 1 + Math.round(progressMid * 59.0f);
        napMinutesMid = Math.max(1, Math.min(60, napMinutesMid));
        Assert.assertTrue(napMinutesMid >= 25 && napMinutesMid <= 35);
    }
}
