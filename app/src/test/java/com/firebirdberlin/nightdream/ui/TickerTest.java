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
    public void testGetPreviousHeadline_normal() {
        List<String> headlines = Arrays.asList("Headline 1", "Headline 2", "Headline 3");
        int[] outIndex = {1};

        String prev1 = Ticker.getPreviousHeadline(headlines, outIndex[0], true, outIndex);
        Assert.assertEquals(" +++ Headline 1", prev1);
        Assert.assertEquals(0, outIndex[0]);

        String prev2 = Ticker.getPreviousHeadline(headlines, outIndex[0], false, outIndex);
        Assert.assertEquals("Headline 3", prev2);
        Assert.assertEquals(2, outIndex[0]);
    }

    @Test
    public void testGetPreviousHeadline_skipsEmpty() {
        List<String> headlines = Arrays.asList("Valid Headline", "", "   ");
        int[] outIndex = {2};

        String prev = Ticker.getPreviousHeadline(headlines, outIndex[0], false, outIndex);
        Assert.assertEquals("Valid Headline", prev);
        Assert.assertEquals(0, outIndex[0]);
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

    @Test
    public void testCalculateLeftPosition_centeredInBatterySavingMode() {
        int paddingPx = 30;
        // Battery saving mode (isAnimationActive = false) and text shorter than available width (1000 - 60 = 940)
        int left = Ticker.calculateLeftPosition(400, 1000, paddingPx, false);
        Assert.assertEquals(300, left);

        // Battery saving mode and text longer than available width
        int leftLong = Ticker.calculateLeftPosition(1200, 1000, paddingPx, false);
        Assert.assertEquals(30, leftLong);

        // Normal animation mode (isAnimationActive = true)
        int leftActive = Ticker.calculateLeftPosition(400, 1000, paddingPx, true);
        Assert.assertEquals(0, leftActive);
    }

    @Test
    public void testCalculateLayoutWidth() {
        int paddingPx = 30;
        // At rest (!isDraggingOrScrolled): long text layout width is capped to availableWidth (940) so '...' is displayed
        int widthRest = Ticker.calculateLayoutWidth(1200, 1000, paddingPx, false, false);
        Assert.assertEquals(940, widthRest);

        // While dragging/scrolled (isDraggingOrScrolled = true): layout width is full text width (1200) for full swiping
        int widthDragging = Ticker.calculateLayoutWidth(1200, 1000, paddingPx, false, true);
        Assert.assertEquals(1200, widthDragging);

        // Short text (400 <= 940): layout width is text width (400)
        int widthShort = Ticker.calculateLayoutWidth(400, 1000, paddingPx, false, false);
        Assert.assertEquals(400, widthShort);
    }

    @Test
    public void testCalculateMaxScroll() {
        int paddingPx = 30;
        // Available width = 1000 - 60 = 940. Long text 1200 -> maxScroll = 1200 - 940 = 260
        Assert.assertEquals(260, Ticker.calculateMaxScroll(1200, 1000, paddingPx));

        // Short text 400 <= 940 -> maxScroll = 0
        Assert.assertEquals(0, Ticker.calculateMaxScroll(400, 1000, paddingPx));
    }

    @Test
    public void testDetermineDragAction() {
        int maxScroll = 200;
        int touchSlop = 8;

        // Swiping right past touchSlop -> PREVIOUS
        Assert.assertEquals(Ticker.DRAG_ACTION_PREVIOUS, Ticker.determineDragAction(20f, maxScroll, touchSlop));

        // Swiping left past maxScroll + touchSlop (-208) -> NEXT
        Assert.assertEquals(Ticker.DRAG_ACTION_NEXT, Ticker.determineDragAction(-220f, maxScroll, touchSlop));

        // Scrolling within range [0, -200] -> KEEP_SCROLL
        Assert.assertEquals(Ticker.DRAG_ACTION_KEEP_SCROLL, Ticker.determineDragAction(-100f, maxScroll, touchSlop));
    }

    @Test
    public void testClampScrollTranslationX() {
        int maxScroll = 200;

        Assert.assertEquals(0f, Ticker.clampScrollTranslationX(50f, maxScroll), 0.01f);
        Assert.assertEquals(-200f, Ticker.clampScrollTranslationX(-300f, maxScroll), 0.01f);
        Assert.assertEquals(-100f, Ticker.clampScrollTranslationX(-100f, maxScroll), 0.01f);
    }
}
