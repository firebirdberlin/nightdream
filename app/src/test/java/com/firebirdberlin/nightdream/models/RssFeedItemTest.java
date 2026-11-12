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

public class RssFeedItemTest {

    @Test
    public void testRssFeedItemGetters() {
        RssFeedItem item = new RssFeedItem("Test Title", "https://example.com/rss", "Mon, 01 Jan 2026 00:00:00 GMT");
        Assert.assertEquals("Test Title", item.getTitle());
        Assert.assertEquals("https://example.com/rss", item.getLink());
        Assert.assertEquals("Mon, 01 Jan 2026 00:00:00 GMT", item.getPubDate());
    }

    @Test
    public void testRssFeedItemWithNulls() {
        RssFeedItem item = new RssFeedItem(null, null, null);
        Assert.assertNull(item.getTitle());
        Assert.assertNull(item.getLink());
        Assert.assertNull(item.getPubDate());
    }
}
