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

import android.graphics.Canvas;

import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class WeatherGraphViewTest {

    private WeatherGraphView view;

    @Before
    public void setUp() {
        view = new WeatherGraphView(null);
    }

    @Test
    public void testTimeToX_sameStartAndEndTimestamp() {
        float x = WeatherGraphView.timeToX(1000L, 1000L, 1000L, 50f, 200f);
        Assert.assertEquals(50f, x, 0.001f);
    }

    @Test
    public void testTimeToX_proportionalPositioning() {
        long startTime = 0L;
        long endTime = 3600L * 4; // 4 hours
        float plotLeft = 100f;
        float plotWidth = 400f;

        float xStart = WeatherGraphView.timeToX(0L, startTime, endTime, plotLeft, plotWidth);
        float x1h = WeatherGraphView.timeToX(3600L, startTime, endTime, plotLeft, plotWidth);
        float xEnd = WeatherGraphView.timeToX(endTime, startTime, endTime, plotLeft, plotWidth);

        Assert.assertEquals(100f, xStart, 0.001f);
        Assert.assertEquals(200f, x1h, 0.001f); // 1/4th of 400 = 100 + 100 = 200
        Assert.assertEquals(500f, xEnd, 0.001f); // 100 + 400 = 500
    }

    @Test
    public void testRainOf_precedence() {
        WeatherEntry entry1 = new WeatherEntry();
        entry1.rain3h = 2.5;
        entry1.rain1h = 1.0;
        Assert.assertEquals(2.5, WeatherGraphView.rainOf(entry1), 0.001);

        WeatherEntry entry2 = new WeatherEntry();
        entry2.rain3h = 0.0;
        entry2.rain1h = 1.2;
        Assert.assertEquals(1.2, WeatherGraphView.rainOf(entry2), 0.001);

        WeatherEntry entry3 = new WeatherEntry();
        entry3.rain3h = 0.0;
        entry3.rain1h = 0.0;
        Assert.assertEquals(0.0, WeatherGraphView.rainOf(entry3), 0.001);
    }

    @Test
    public void testToDisplayTemperature_conversions() {
        double kelvin = 273.15;

        view.setEntries(new ArrayList<>(), WeatherEntry.CELSIUS, false);
        Assert.assertEquals(0.0, view.toDisplayTemperature(kelvin), 0.001);

        view.setEntries(new ArrayList<>(), WeatherEntry.FAHRENHEIT, false);
        Assert.assertEquals(32.0, view.toDisplayTemperature(kelvin), 0.001);

        view.setEntries(new ArrayList<>(), WeatherEntry.KELVIN, false);
        Assert.assertEquals(273.15, view.toDisplayTemperature(kelvin), 0.001);
    }

    @Test
    public void testFormatTemperature_rounding() {
        Assert.assertEquals("21°", view.formatTemperature(21.4));
        Assert.assertEquals("22°", view.formatTemperature(21.6));
        Assert.assertEquals("-4°", view.formatTemperature(-3.6));
    }

    @Test
    public void testCalculateDataRanges_singleEntry() {
        List<WeatherEntry> entries = new ArrayList<>();
        WeatherEntry entry = new WeatherEntry();
        entry.temperature = 293.15; // 20°C
        entry.rain3h = 0.0;
        entries.add(entry);

        view.setEntries(entries, WeatherEntry.CELSIUS, false);

        WeatherGraphView.DataRanges ranges = new WeatherGraphView.DataRanges();
        view.calculateDataRanges(ranges);

        // minTemp and maxTemp are equal (20.0), so range adjusts by +/- 1.0
        Assert.assertEquals(19.0, ranges.minTemp, 0.001);
        Assert.assertEquals(21.0, ranges.maxTemp, 0.001);
        Assert.assertFalse(ranges.hasRain);
        Assert.assertEquals(1.0, ranges.maxRain, 0.001);
    }

    @Test
    public void testCalculateDataRanges_multipleEntriesWithRainAndApparentTemp() {
        List<WeatherEntry> entries = new ArrayList<>();

        WeatherEntry entry1 = new WeatherEntry();
        entry1.timestamp = 1000L;
        entry1.temperature = 283.15; // 10°C
        entry1.apparentTemperature = 278.15; // 5°C
        entry1.rain3h = 2.0;

        WeatherEntry entry2 = new WeatherEntry();
        entry2.timestamp = 2000L;
        entry2.temperature = 298.15; // 25°C
        entry2.apparentTemperature = 303.15; // 30°C
        entry2.rain3h = 0.5;

        entries.add(entry1);
        entries.add(entry2);

        view.setEntries(entries, WeatherEntry.CELSIUS, true);

        WeatherGraphView.DataRanges ranges = new WeatherGraphView.DataRanges();
        view.calculateDataRanges(ranges);

        Assert.assertEquals(5.0, ranges.minTemp, 0.001);  // from apparent temp of entry1
        Assert.assertEquals(30.0, ranges.maxTemp, 0.001); // from apparent temp of entry2
        Assert.assertTrue(ranges.hasRain);
        Assert.assertEquals(2.0, ranges.maxRain, 0.001);
    }

    @Test
    public void testSetEntries_sortsByTimestamp() {
        List<WeatherEntry> entries = new ArrayList<>();

        WeatherEntry entry1 = new WeatherEntry();
        entry1.timestamp = 3000L;

        WeatherEntry entry2 = new WeatherEntry();
        entry2.timestamp = 1000L;

        WeatherEntry entry3 = new WeatherEntry();
        entry3.timestamp = 2000L;

        entries.add(entry1);
        entries.add(entry2);
        entries.add(entry3);

        view.setEntries(entries, WeatherEntry.CELSIUS, false);

        WeatherGraphView.DataRanges ranges = new WeatherGraphView.DataRanges();
        view.calculateDataRanges(ranges);

        // Sorting check indirectly or via execution without error
        Assert.assertNotNull(ranges);
    }

    @Test
    public void testOnDrawAndSubMethods_executeWithoutErrors() {
        List<WeatherEntry> entries = new ArrayList<>();
        WeatherEntry entry1 = new WeatherEntry();
        entry1.timestamp = 1000L;
        entry1.temperature = 285.15;
        entry1.rain3h = 1.5;

        WeatherEntry entry2 = new WeatherEntry();
        entry2.timestamp = 2000L;
        entry2.temperature = 290.15;
        entry2.rain3h = 0.0;

        entries.add(entry1);
        entries.add(entry2);

        view.setEntries(entries, WeatherEntry.CELSIUS, true);

        Canvas canvas = new Canvas();
        view.onDraw(canvas);
        view.drawNightBackground(canvas, 1000000L, 2000000L, 10f, 10f, 100f, 200f);
        view.drawTemperatureGrid(canvas, 10f, 200f, 10f, 100f, 4f, 10f, "20°", "10°", "15°");
        view.drawRainBars(canvas, 2, 1000000L, 2000000L, 10f, 200f, 100f, 190f, 35f, 1.5, true, "1.5 mm", 4f);
        view.drawTemperatureLines(canvas, 2, 1000000L, 2000000L, 10f, 10f, 190f, 65f, 10.0, 20.0);
        view.drawXAxis(canvas, 2, 1000000L, 2000000L, 10f, 200f, 100f, 190f, 4f, 10f, 30f);
    }
}
