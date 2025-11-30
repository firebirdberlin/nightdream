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

package com.firebirdberlin.radiostreamapi.models;


public class PlaylistInfo {

    public enum Format {M3U, PLS, ASHX, ASX};

    public enum Error {INVALID_URL, INVALID_CONTENT, UNREACHABLE_URL, UNSUPPORTED_FORMAT}

    public boolean valid = true;

    public Error error = null;

    public String streamUrl;

    public String description;

    public Format format;

    public Integer bitrateHint;
}
