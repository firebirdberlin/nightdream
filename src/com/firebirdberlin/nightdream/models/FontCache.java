package com.firebirdberlin.nightdream.models;

import android.content.Context;
import android.graphics.Typeface;

import java.util.Hashtable;

public class FontCache {

    private static Hashtable<String, Typeface> fontCache = new Hashtable<>();

    public static Typeface get(Context context, String name) {
        Typeface tf = fontCache.get(name);
        if (tf == null) {
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
            fontCache.put(name, tf);
        }
        return tf;
    }
}