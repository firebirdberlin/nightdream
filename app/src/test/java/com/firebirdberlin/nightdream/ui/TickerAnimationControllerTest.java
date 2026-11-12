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

public class TickerAnimationControllerTest {

    @Test
    public void testInitialState() {
        TickerAnimationController controller = new TickerAnimationController(null);
        Assert.assertEquals(5L, controller.getAnimationSpeed());
        Assert.assertEquals(-1, controller.getIndex());
        Assert.assertFalse(controller.isPaused());
        Assert.assertFalse(controller.isRunning());
        Assert.assertTrue(controller.getHeadlines().isEmpty());
        Assert.assertTrue(controller.getUrls().isEmpty());
    }

    @Test
    public void testSpeedAndConfig() {
        TickerAnimationController controller = new TickerAnimationController(null);
        controller.setTickerSpeed(10L);
        Assert.assertEquals(10L, controller.getAnimationSpeed());

        controller.setIndex(2);
        Assert.assertEquals(2, controller.getIndex());

        controller.setPaused(true);
        Assert.assertTrue(controller.isPaused());

        controller.setRunning(true);
        Assert.assertTrue(controller.isRunning());
    }

    @Test
    public void testAddHeadline() {
        TickerAnimationController controller = new TickerAnimationController(null);
        controller.addHeadline("Test Headline");
        Assert.assertEquals(1, controller.getHeadlines().size());
        Assert.assertEquals("Test Headline", controller.getHeadlines().get(0));

        controller.clear();
        Assert.assertTrue(controller.getHeadlines().isEmpty());
        Assert.assertEquals(-1, controller.getIndex());
    }
}
