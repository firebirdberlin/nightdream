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

public class ParticleEffectTest {

    @Test
    public void testGetValues() {
        Assert.assertEquals(1, ParticleEffect.SNOW.getValue());
        Assert.assertEquals(2, ParticleEffect.GOLDEN_STARS.getValue());
    }

    @Test
    public void testFromValueInt() {
        Assert.assertEquals(ParticleEffect.SNOW, ParticleEffect.fromValue(1));
        Assert.assertEquals(ParticleEffect.GOLDEN_STARS, ParticleEffect.fromValue(2));
        Assert.assertEquals(ParticleEffect.SNOW, ParticleEffect.fromValue(99));
    }

    @Test
    public void testFromValueString() {
        Assert.assertEquals(ParticleEffect.SNOW, ParticleEffect.fromValue("1", ParticleEffect.GOLDEN_STARS));
        Assert.assertEquals(ParticleEffect.GOLDEN_STARS, ParticleEffect.fromValue("2", ParticleEffect.SNOW));
        Assert.assertEquals(ParticleEffect.SNOW, ParticleEffect.fromValue("invalid", ParticleEffect.SNOW));
        String nullStr = null;
        Assert.assertEquals(ParticleEffect.SNOW, ParticleEffect.fromValue(nullStr, ParticleEffect.SNOW));
    }

    @Test
    public void testLegacyNoneValuesConvertedToSnow() {
        Assert.assertEquals(ParticleEffect.SNOW, ParticleEffect.fromValue(0, ParticleEffect.GOLDEN_STARS));
        Assert.assertEquals(ParticleEffect.SNOW, ParticleEffect.fromValue("0", ParticleEffect.GOLDEN_STARS));
        Assert.assertEquals(ParticleEffect.SNOW, ParticleEffect.fromValue("none", ParticleEffect.GOLDEN_STARS));
        Assert.assertEquals(ParticleEffect.SNOW, ParticleEffect.fromValue("NONE", ParticleEffect.GOLDEN_STARS));
    }

    @Test
    public void testToPreferenceValue() {
        Assert.assertEquals("1", ParticleEffect.toPreferenceValue(ParticleEffect.SNOW));
        Assert.assertEquals("2", ParticleEffect.toPreferenceValue(ParticleEffect.GOLDEN_STARS));
        Assert.assertEquals("1", ParticleEffect.toPreferenceValue(null));
    }

    @Test
    public void testIsLegacyOrInvalid() {
        Assert.assertTrue(ParticleEffect.isLegacyOrInvalid("0"));
        Assert.assertTrue(ParticleEffect.isLegacyOrInvalid("none"));
        Assert.assertTrue(ParticleEffect.isLegacyOrInvalid("invalid"));
        Assert.assertFalse(ParticleEffect.isLegacyOrInvalid("1"));
        Assert.assertFalse(ParticleEffect.isLegacyOrInvalid("2"));
    }

    @Test
    public void testFromValueNullDefaultFallback() {
        Assert.assertEquals(ParticleEffect.SNOW, ParticleEffect.fromValue(99, null));
        Assert.assertEquals(ParticleEffect.SNOW, ParticleEffect.fromValue("invalid", null));
    }
}
