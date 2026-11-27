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

package com.firebirdberlin.nightdream.models;

import com.firebirdberlin.nightdream.ui.ClockLayout;

import org.junit.Assert;
import org.junit.Test;

public class AnalogClockConfigTest {

    @Test
    public void testToClockStyle() {
        Assert.assertEquals(AnalogClockConfig.Style.SIMPLE,
                AnalogClockConfig.toClockStyle(ClockLayout.LAYOUT_ID_ANALOG2));
        Assert.assertEquals(AnalogClockConfig.Style.ARC,
                AnalogClockConfig.toClockStyle(ClockLayout.LAYOUT_ID_ANALOG3));
        Assert.assertEquals(AnalogClockConfig.Style.DEFAULT,
                AnalogClockConfig.toClockStyle(ClockLayout.LAYOUT_ID_ANALOG4));
        Assert.assertEquals(AnalogClockConfig.Style.MINIMALISTIC,
                AnalogClockConfig.toClockStyle(ClockLayout.LAYOUT_ID_ANALOG));
    }

    @Test
    public void testDigitStyleEnum() {
        Assert.assertEquals(AnalogClockConfig.DigitStyle.NONE, AnalogClockConfig.DigitStyle.fromValue(0));
        Assert.assertEquals(AnalogClockConfig.DigitStyle.ARABIC, AnalogClockConfig.DigitStyle.fromValue(1));
        Assert.assertEquals(AnalogClockConfig.DigitStyle.ROMAN, AnalogClockConfig.DigitStyle.fromValue(2));
        Assert.assertEquals(AnalogClockConfig.DigitStyle.NONE, AnalogClockConfig.DigitStyle.fromValue(99));
    }

    @Test
    public void testTickStyleEnum() {
        Assert.assertEquals(AnalogClockConfig.TickStyle.NONE, AnalogClockConfig.TickStyle.fromValue(0));
        Assert.assertEquals(AnalogClockConfig.TickStyle.DASH, AnalogClockConfig.TickStyle.fromValue(1));
        Assert.assertEquals(AnalogClockConfig.TickStyle.CIRCLE, AnalogClockConfig.TickStyle.fromValue(2));
        Assert.assertEquals(AnalogClockConfig.TickStyle.NONE, AnalogClockConfig.TickStyle.fromValue(-1));
    }

    @Test
    public void testDecorationEnum() {
        Assert.assertEquals(AnalogClockConfig.Decoration.NONE, AnalogClockConfig.Decoration.fromValue(0));
        Assert.assertEquals(AnalogClockConfig.Decoration.MINUTE_HAND, AnalogClockConfig.Decoration.fromValue(1));
        Assert.assertEquals(AnalogClockConfig.Decoration.LABELS, AnalogClockConfig.Decoration.fromValue(2));
        Assert.assertEquals(AnalogClockConfig.Decoration.NONE, AnalogClockConfig.Decoration.fromValue(100));
    }

    @Test
    public void testHandShapeEnum() {
        Assert.assertEquals(AnalogClockConfig.HandShape.BAR, AnalogClockConfig.HandShape.fromValue(0));
        Assert.assertEquals(AnalogClockConfig.HandShape.TRIANGLE, AnalogClockConfig.HandShape.fromValue(1));
        Assert.assertEquals(AnalogClockConfig.HandShape.ARC, AnalogClockConfig.HandShape.fromValue(2));
        Assert.assertEquals(AnalogClockConfig.HandShape.BAR, AnalogClockConfig.HandShape.fromValue(5));
    }

    @Test
    public void testInitStyle() {
        AnalogClockConfig config = new AnalogClockConfig();
        config.initStyle(AnalogClockConfig.Style.MINIMALISTIC);

        Assert.assertEquals(AnalogClockConfig.DigitStyle.NONE, config.digitStyle);
        Assert.assertEquals(AnalogClockConfig.HandShape.BAR, config.handShape);
        Assert.assertEquals(0.0f, config.innerCircleRadius, 0.001f);
    }

    @Test
    public void testJsonImportExport() {
        AnalogClockConfig config = new AnalogClockConfig();
        config.initStyle(AnalogClockConfig.Style.ARC);
        config.showSecondHand = true;
        config.fontSize = 0.12f;

        String json = config.toJson();
        Assert.assertNotNull(json);

        AnalogClockConfig restored = new AnalogClockConfig();
        restored.importFromJson(json);

        Assert.assertEquals(AnalogClockConfig.HandShape.ARC, restored.handShape);
        Assert.assertTrue(restored.showSecondHand);
        Assert.assertEquals(0.12f, restored.fontSize, 0.001f);
    }
}
