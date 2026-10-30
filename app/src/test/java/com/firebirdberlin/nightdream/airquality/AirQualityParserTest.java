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

package com.firebirdberlin.nightdream.airquality;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class AirQualityParserTest {

    private static final String SAMPLE_META_JSON = "{\n"
            + "  \"stations\": {\n"
            + "    \"21\": [\n"
            + "      \"21\", \"DEBB021\", \"Potsdam-Zentrum\", \"Potsdam\", \"PDBA\", \"2009-04-15\", null,\n"
            + "      \"13.0602\", \"52.4014\", \"4\", \"1\", \"1\", \"BB\", \"Brandenburg\", \"städtisch\"\n"
            + "    ],\n"
            + "    \"857\": [\n"
            + "      \"857\", \"DEHH081\", \"Hamburg-Sternschanze\", \"Hamburg\", \"HHST\", \"2012-01-01\", null,\n"
            + "      \"9.9620\", \"53.5600\", \"1\", \"1\", \"1\", \"HH\", \"Hamburg\", \"städtisch\"\n"
            + "    ]\n"
            + "  }\n"
            + "}";

    private static final String SAMPLE_AQ_JSON = "{\n"
            + "  \"data\": {\n"
            + "    \"857\": {\n"
            + "      \"2025-02-15 11:00:00\": [\n"
            + "        \"2025-02-15 12:00:00\", 2, 0,\n"
            + "        [9, 22.0, 2, \"2.429\"],\n"
            + "        [1, 26.0, 1, \"1.941\"],\n"
            + "        [5, 24.0, 1, \"1.684\"],\n"
            + "        [3, 35.0, 1, \"1.234\"]\n"
            + "      ]\n"
            + "    }\n"
            + "  }\n"
            + "}";

    @Test
    public void testParseStations() {
        List<AirQualityStation> stations = AirQualityParser.parseStations(SAMPLE_META_JSON);
        Assert.assertEquals(2, stations.size());

        AirQualityStation s1 = stations.get(0);
        if (!"DEBB021".equals(s1.code)) {
            s1 = stations.get(1);
        }

        Assert.assertEquals("DEBB021", s1.code);
        Assert.assertEquals("Potsdam-Zentrum", s1.name);
        Assert.assertEquals("Potsdam", s1.city);
        Assert.assertEquals(52.4014, s1.latitude, 0.0001);
        Assert.assertEquals(13.0602, s1.longitude, 0.0001);
    }

    @Test
    public void testParseAirQuality() {
        AirQualityStation station = new AirQualityStation(
                "857", "DEHH081", "Hamburg-Sternschanze", "Hamburg", 53.5600, 9.9620);
        List<AirQualityData> readings = AirQualityParser.parseAirQuality(SAMPLE_AQ_JSON, station);

        Assert.assertEquals(1, readings.size());

        AirQualityData data = readings.get(0);
        Assert.assertEquals("DEHH081", data.stationCode);
        Assert.assertEquals(2, data.lqiIndex);
        Assert.assertTrue(data.hasLqi());
        Assert.assertEquals("2025-02-15 12:00:00", data.timeEndCet);

        Assert.assertNotNull(data.pm25);
        Assert.assertEquals(22.0, data.pm25, 0.01);

        Assert.assertNotNull(data.pm10);
        Assert.assertEquals(26.0, data.pm10, 0.01);

        Assert.assertNotNull(data.no2);
        Assert.assertEquals(24.0, data.no2, 0.01);

        Assert.assertNotNull(data.o3);
        Assert.assertEquals(35.0, data.o3, 0.01);
    }

    @Test
    public void testGetMostRecentReading() {
        AirQualityStation station = new AirQualityStation(
                "857", "DEHH081", "Hamburg-Sternschanze", "Hamburg", 53.5600, 9.9620);
        List<AirQualityData> readings = AirQualityParser.parseAirQuality(SAMPLE_AQ_JSON, station);

        AirQualityData recent = AirQualityParser.getMostRecentReading(readings);
        Assert.assertNotNull(recent);
        Assert.assertTrue(recent.hasAnyData());
        Assert.assertEquals(2, recent.lqiIndex);
    }
}
