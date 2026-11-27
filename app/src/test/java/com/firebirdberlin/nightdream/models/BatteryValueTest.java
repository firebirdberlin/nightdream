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

import org.junit.Assert;
import org.junit.Test;

public class BatteryValueTest {

    @Test
    public void testBatteryValueConstructorAndPercentage() {
        BatteryValue bv = new BatteryValue(75, 100, 2, 1);
        Assert.assertEquals(75, bv.level);
        Assert.assertEquals(100, bv.scale);
        Assert.assertEquals(75.0f, bv.getPercentage(), 0.001f);
        Assert.assertEquals(0.75f, bv.levelNormalized, 0.001f);
    }

    @Test
    public void testGetEstimateMillis() {
        BatteryValue ref = new BatteryValue(20, 100, 2, BatteryValue.BATTERY_PLUGGED_AC);
        ref.time = System.currentTimeMillis() - 120000; // 2 minutes ago

        BatteryValue current = new BatteryValue(40, 100, 2, BatteryValue.BATTERY_PLUGGED_AC);

        long estimate = current.getEstimateMillis(ref);
        Assert.assertTrue("Estimate should be around 360000 ms", Math.abs(estimate - 360000L) < 5000);
    }

    @Test
    public void testGetEstimateMillisInvalid() {
        BatteryValue ref = new BatteryValue(-1, 100, 2, BatteryValue.BATTERY_PLUGGED_AC);
        BatteryValue current = new BatteryValue(40, 100, 2, BatteryValue.BATTERY_PLUGGED_AC);
        Assert.assertEquals(-1L, current.getEstimateMillis(ref));

        BatteryValue refDiff = new BatteryValue(20, 100, 2, BatteryValue.BATTERY_PLUGGED_USB);
        Assert.assertEquals(-1L, current.getEstimateMillis(refDiff));
    }

    @Test
    public void testGetDischargingEstimateMillis() {
        BatteryValue ref = new BatteryValue(80, 100, 3, -1);
        ref.time = System.currentTimeMillis() - 120000; // 2 minutes ago

        BatteryValue current = new BatteryValue(60, 100, 3, -1);
        long estimate = current.getDischargingEstimateMillis(ref);
        Assert.assertTrue("Discharge estimate should be around 360000 ms", Math.abs(estimate - 360000L) < 5000);
    }

    @Test
    public void testJsonSerialization() {
        BatteryValue bv = new BatteryValue(85, 100, 2, BatteryValue.BATTERY_PLUGGED_AC);
        bv.isCharging = true;
        bv.isChargingAC = true;

        String json = bv.toJson();
        Assert.assertNotNull(json);

        BatteryValue restored = BatteryValue.fromJson(json);
        Assert.assertNotNull(restored);
        Assert.assertEquals(bv.level, restored.level);
        Assert.assertEquals(bv.scale, restored.scale);
        Assert.assertEquals(bv.chargingMethod, restored.chargingMethod);
        Assert.assertEquals(bv.isCharging, restored.isCharging);
        Assert.assertEquals(bv.isChargingAC, restored.isChargingAC);
    }
}
