package com.firebirdberlin.nightdream.models;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

import java.util.Hashtable;

public class FontCache {

    private static Hashtable<String, Typeface> fontCache = new Hashtable<>();

    private static final String TAG = "FontCache";

    public static Typeface get(Context context, String name) {
        final String cacheKey = name;
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
            //Log.d(TAG, "cache put: cacheKey=" + cacheKey + " value=" + (tf != null ? tf.toString() : "null"));
            fontCache.put(cacheKey, tf);
        } else {
            //Log.d(TAG, "cache hit: name=" + name + " obj=" + tf.toString());
        }
        return tf;
    }
}