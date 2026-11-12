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

public class TickerTouchHandlerTest {

    @Test
    public void testIsDragExceeded() {
        int touchSlop = 8;
        Assert.assertFalse(TickerTouchHandler.isDragExceeded(100f, 100f, 104f, 102f, touchSlop));
        Assert.assertTrue(TickerTouchHandler.isDragExceeded(100f, 100f, 115f, 100f, touchSlop));
        Assert.assertTrue(TickerTouchHandler.isDragExceeded(100f, 100f, 100f, 85f, touchSlop));
    }

    @Test
    public void testIsValidTap() {
        Assert.assertTrue(TickerTouchHandler.isValidTap(true, 200L, 500));
        Assert.assertFalse(TickerTouchHandler.isValidTap(true, 600L, 500));
        Assert.assertFalse(TickerTouchHandler.isValidTap(false, 200L, 500));
    }

    @Test
    public void testHandlerState() {
        TickerTouchHandler handler = new TickerTouchHandler(null);
        handler.setDownX(10f);
        handler.setDownY(20f);
        handler.setClickValid(true);
        handler.setDragging(true);
        handler.setPendingUrl("http://example.com");

        Assert.assertEquals(10f, handler.getDownX(), 0.001f);
        Assert.assertEquals(20f, handler.getDownY(), 0.001f);
        Assert.assertTrue(handler.isClickValid());
        Assert.assertTrue(handler.isDragging());
        Assert.assertEquals("http://example.com", handler.getPendingUrl());

        handler.reset();
        Assert.assertFalse(handler.isClickValid());
        Assert.assertFalse(handler.isDragging());
        Assert.assertNull(handler.getPendingUrl());
    }
}
