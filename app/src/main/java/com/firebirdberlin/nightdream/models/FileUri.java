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

package com.firebirdberlin.nightdream.models;

import android.net.Uri;

import java.io.File;

public class FileUri {
    public Uri uri;
    public String name;


    public FileUri(File file) {
        this.uri = Uri.fromFile(file);
        this.name = file.getName();
    }

    public FileUri(Uri uri, String name) {
        this.uri = uri;
        this.name = name;
    }
}
