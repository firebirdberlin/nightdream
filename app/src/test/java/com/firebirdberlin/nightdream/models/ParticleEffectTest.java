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
        Assert.assertEquals(0, ParticleEffect.NONE.getValue());
        Assert.assertEquals(1, ParticleEffect.SNOW.getValue());
        Assert.assertEquals(2, ParticleEffect.GOLDEN_STARS.getValue());
    }

    @Test
    public void testFromValueInt() {
        Assert.assertEquals(ParticleEffect.NONE, ParticleEffect.fromValue(0));
        Assert.assertEquals(ParticleEffect.SNOW, ParticleEffect.fromValue(1));
        Assert.assertEquals(ParticleEffect.GOLDEN_STARS, ParticleEffect.fromValue(2));
        Assert.assertEquals(ParticleEffect.NONE, ParticleEffect.fromValue(99));
    }

    @Test
    public void testFromValueString() {
        Assert.assertEquals(ParticleEffect.NONE, ParticleEffect.fromValue("0", ParticleEffect.SNOW));
        Assert.assertEquals(ParticleEffect.SNOW, ParticleEffect.fromValue("1", ParticleEffect.NONE));
        Assert.assertEquals(ParticleEffect.GOLDEN_STARS, ParticleEffect.fromValue("2", ParticleEffect.NONE));
        Assert.assertEquals(ParticleEffect.SNOW, ParticleEffect.fromValue("invalid", ParticleEffect.SNOW));
        String nullStr = null;
        Assert.assertEquals(ParticleEffect.SNOW, ParticleEffect.fromValue(nullStr, ParticleEffect.SNOW));
    }
}
