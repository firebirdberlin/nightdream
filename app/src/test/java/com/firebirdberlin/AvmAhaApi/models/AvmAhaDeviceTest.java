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

package com.firebirdberlin.AvmAhaApi.models;

import org.junit.Assert;
import org.junit.Test;

public class AvmAhaDeviceTest {

    @Test
    public void testStateAndPresence() {
        AvmAhaDevice device = new AvmAhaDevice();
        device.setState(AvmAhaDevice.STATE_ON);
        device.present = "1";

        Assert.assertTrue(device.isOn());
        Assert.assertTrue(device.isPresent());

        device.setState(AvmAhaDevice.STATE_OFF);
        Assert.assertFalse(device.isOn());
    }

    @Test
    public void testBitmaskCapabilities() {
        AvmAhaDevice device = new AvmAhaDevice();
        device.functionbitmask = (1 << 15) | (1 << 16) | (1 << 17) | (1 << 9) | (1 << 2);

        Assert.assertTrue(device.isSwitchable());
        Assert.assertTrue(device.hasLevel());
        Assert.assertTrue(device.hasColor());
        Assert.assertTrue(device.isSwitch());
        Assert.assertTrue(device.isLightBulb());
    }

    @Test
    public void testToString() {
        AvmAhaDevice device = new AvmAhaDevice();
        device.manufacturer = "AVM";
        device.productname = "FRITZ!DECT 200";
        device.name = "Plug";
        device.state = "1";
        device.functionbitmask = 512;

        String str = device.toString();
        Assert.assertTrue(str.contains("AVM"));
        Assert.assertTrue(str.contains("FRITZ!DECT 200"));
        Assert.assertTrue(str.contains("Plug"));
    }
}
