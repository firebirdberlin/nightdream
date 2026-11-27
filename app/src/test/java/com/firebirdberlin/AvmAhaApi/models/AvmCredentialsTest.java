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

public class AvmCredentialsTest {

    @Test
    public void testGetSecretFormat() {
        AvmCredentials creds = new AvmCredentials("192.168.178.1", "admin", "password123");
        String secret = creds.getSecret("12345678");

        Assert.assertNotNull(secret);
        Assert.assertTrue(secret.startsWith("12345678-"));
        Assert.assertEquals(41, secret.length());
    }
}
