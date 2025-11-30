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

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

import java.util.Hashtable;

public class FontCache {

    private static Hashtable<String, Typeface> fontCache = new Hashtable<>();

    private static final String TAG = "FontCache";

    public static Typeface get(Context context, String name) {
        Log.i(TAG, "getting font:"  + name);
        final String cacheKey = name.replace("file:///android_asset/", "");
        Typeface tf = fontCache.get(cacheKey);
        if (tf == null) {
            //Log.d(TAG, "cache miss: cacheKey=" +  cacheKey);
            final String ASSET_PATH = "file:///android_asset/";

            if (name.contains(ASSET_PATH)) {
                name = name.replace(ASSET_PATH, "");
            }

            try {
                if (name.startsWith("file://")) {
                    tf = Typeface.createFromFile(name.replace("file://", ""));
                } else {
                    tf = Typeface.createFromAsset(context.getAssets(), name);
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
                return null;
            }
            Log.d(TAG, "cache put: cacheKey=" + cacheKey + " value=" + (tf != null ? tf.toString() : "null"));
            fontCache.put(cacheKey, tf);
        } else {
            Log.d(TAG, "cache hit: name=" + name + " obj=" + tf.toString());
        }
        return tf;
    }
}