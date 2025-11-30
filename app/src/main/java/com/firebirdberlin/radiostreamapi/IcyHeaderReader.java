/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
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

package com.firebirdberlin.radiostreamapi;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

public class IcyHeaderReader {

    public static IcyHeaderInfo getHeaderInfos(Map<String, List<String>> responseHeaders) {

        IcyHeaderInfo info = new IcyHeaderInfo();

        String icyName = getHeaderField("icy-name", responseHeaders);
        String xRadioName = getHeaderField("X-RadioName", responseHeaders);
        info.setName(icyName != null && !icyName.isEmpty() ? icyName : xRadioName);
        info.setDescription(getHeaderField("icy-description", responseHeaders));
        info.setUrl(getHeaderField("icy-url", responseHeaders));
        String brStr = getHeaderField("icy-br", responseHeaders);
        if (brStr != null) {
            try {
                info.setBitrate(Integer.valueOf(brStr));
            } catch (NumberFormatException e) {

            }
        }

        return info;
    }

    private static String getHeaderField(String key, Map<String, List<String>> responseHeaders) {
        if (responseHeaders == null) {
            return null;
        }
        List<String> entry = responseHeaders.get(key);
        // in case of multiple entries return last
        return (entry != null && !entry.isEmpty() ? entry.get(entry.size() - 1) : null);
    }
}
