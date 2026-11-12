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

package com.firebirdberlin.nightdream.ui;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TickerTest {

    @Test
    public void testGetNextHeadline_normal() {
        List<String> headlines = Arrays.asList("Headline 1", "Headline 2", "Headline 3");
        int[] outIndex = {-1};

        String next1 = Ticker.getNextHeadline(headlines, outIndex[0], true, outIndex);
        Assert.assertEquals(" +++ Headline 1", next1);
        Assert.assertEquals(0, outIndex[0]);

        String next2 = Ticker.getNextHeadline(headlines, outIndex[0], false, outIndex);
        Assert.assertEquals("Headline 2", next2);
        Assert.assertEquals(1, outIndex[0]);
    }

    @Test
    public void testGetNextHeadline_skipsEmpty() {
        List<String> headlines = Arrays.asList("", "   ", "Valid Headline");
        int[] outIndex = {-1};

        String next = Ticker.getNextHeadline(headlines, outIndex[0], false, outIndex);
        Assert.assertEquals("Valid Headline", next);
        Assert.assertEquals(2, outIndex[0]);
    }

    @Test
    public void testGetNextHeadline_emptyList() {
        List<String> headlines = Collections.emptyList();
        int[] outIndex = {-1};

        String next = Ticker.getNextHeadline(headlines, -1, true, outIndex);
        Assert.assertEquals("", next);
    }

    @Test
    public void testIsDragExceeded() {
        int touchSlop = 8;
        // Within touch slop
        Assert.assertFalse(Ticker.isDragExceeded(100f, 100f, 104f, 102f, touchSlop));
        // Exceeding touch slop horizontally
        Assert.assertTrue(Ticker.isDragExceeded(100f, 100f, 115f, 100f, touchSlop));
        // Exceeding touch slop vertically
        Assert.assertTrue(Ticker.isDragExceeded(100f, 100f, 100f, 85f, touchSlop));
    }

    @Test
    public void testIsValidTap() {
        // Valid tap: valid flag true, duration <= 500ms
        Assert.assertTrue(Ticker.isValidTap(true, 200L, 500));
        // Invalid: duration > 500ms (long press)
        Assert.assertFalse(Ticker.isValidTap(true, 600L, 500));
        // Invalid: click valid flag false (dragged)
        Assert.assertFalse(Ticker.isValidTap(false, 200L, 500));
    }

    @Test
    public void testShouldFillGap() {
        // maxRight < containerWidth -> needs fill
        Assert.assertTrue(Ticker.shouldFillGap(800f, 1000));
        // maxRight >= containerWidth -> no fill needed
        Assert.assertFalse(Ticker.shouldFillGap(1000f, 1000));
        Assert.assertFalse(Ticker.shouldFillGap(1200f, 1000));
    }

    @Test
    public void testShouldFillLeftGap() {
        // minLeft > 0 -> needs fill on left
        Assert.assertTrue(Ticker.shouldFillLeftGap(100f));
        // minLeft <= 0 -> no fill needed on left
        Assert.assertFalse(Ticker.shouldFillLeftGap(0f));
        Assert.assertFalse(Ticker.shouldFillLeftGap(-50f));
    }
}
