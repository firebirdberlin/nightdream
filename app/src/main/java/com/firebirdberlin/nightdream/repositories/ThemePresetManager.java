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

package com.firebirdberlin.nightdream.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;

import com.firebirdberlin.nightdream.PurchaseManager;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.models.AnalogClockConfig;
import com.firebirdberlin.nightdream.models.ThemePreset;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ThemePresetManager {
    private static final String TAG = "ThemePresetManager";
    public static final String PREF_ACTIVE_PRESET = "activeThemePreset";

    private static final Map<String, ThemePreset> PRESETS = new HashMap<>();

    static {
        // Warm Champagne Gold: #E5C158, Secondary Warm: #C8A846
        int dayGold = Color.rgb(229, 193, 88);
        int daySecondary = Color.rgb(200, 168, 70);

        // Jack-o'-lantern Orange: #FF7518, Ghost Violet: #BA68C8
        int halloweenPrimary = Color.rgb(255, 117, 24);
        int halloweenSecondary = Color.rgb(186, 104, 200);

        ThemePreset halloweenAnalog = new ThemePreset(
                ThemePreset.PRESET_HALLOWEEN_ANALOG,
                "Halloween (Analog Clock)",
                1, // Analog Clock
                halloweenPrimary,
                halloweenSecondary,
                Settings.BACKGROUND_IMAGE,
                "bg_halloween",
                50, // translucent card
                "",
                2,     // Golden Stars
                0.45f, // sky region (upper 45%)
                1.4f,  // star size scale
                50     // 50 stars
        );

        ThemePreset halloweenDigital = new ThemePreset(
                ThemePreset.PRESET_HALLOWEEN_DIGITAL,
                "Halloween (Digital Clock)",
                0, // Digital Clock
                halloweenPrimary,
                halloweenSecondary,
                Settings.BACKGROUND_IMAGE,
                "bg_halloween",
                50, // translucent card
                "fonts/dancingscript_regular.ttf",
                2,     // Golden Stars
                0.45f, // sky region (upper 45%)
                1.4f,  // star size scale
                50     // 50 stars
        );

        ThemePreset xmasAnalog = new ThemePreset(
                ThemePreset.PRESET_CHRISTMAS_ANALOG,
                "Christmas (Analog Clock)",
                1, // Analog Clock
                dayGold,
                daySecondary,
                Settings.BACKGROUND_IMAGE,
                "bg_christmas",
                50, // translucent card
                "",
                1  // Falling Snow
        );

        ThemePreset xmasDigital = new ThemePreset(
                ThemePreset.PRESET_CHRISTMAS_DIGITAL,
                "Christmas (Digital Clock)",
                0, // Digital Clock
                dayGold,
                daySecondary,
                Settings.BACKGROUND_IMAGE,
                "bg_christmas",
                50, // translucent card
                "fonts/dancingscript_regular.ttf",
                2  // Golden Stars
        );

        PRESETS.put(halloweenAnalog.id, halloweenAnalog);
        PRESETS.put(halloweenDigital.id, halloweenDigital);
        PRESETS.put(xmasAnalog.id, xmasAnalog);
        PRESETS.put(xmasDigital.id, xmasDigital);
    }

    public static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(Settings.PREFS_KEY, Context.MODE_PRIVATE);
    }

    public static boolean isUserEligible(Context context) {
        PurchaseManager pm = PurchaseManager.getInstance(context);
        return pm.isPurchased(PurchaseManager.ITEM_PRO)
                || pm.isPurchased(PurchaseManager.ITEM_DONATION)
                || pm.isPurchased(PurchaseManager.ITEM_ONE_YEAR_SUBSCRIPTION)
                || pm.isPurchased(PurchaseManager.ITEM_ACTIONS)
                || pm.isPurchased(PurchaseManager.ITEM_WEATHER_DATA);
    }

    public static Map<String, ThemePreset> getAvailablePresets() {
        return PRESETS;
    }

    public static ThemePreset getPreset(String id) {
        return PRESETS.get(id);
    }

    public static boolean isPresetActive(Context context) {
        return !ThemePreset.PRESET_NONE.equals(getActivePresetId(context));
    }

    public static String getActivePresetId(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getString(PREF_ACTIVE_PRESET, ThemePreset.PRESET_NONE);
    }

    public static void switchTheme(Context context, String newThemeId) {
        SharedPreferences prefs = getPreferences(context);
        String currentThemeId = prefs.getString(PREF_ACTIVE_PRESET, ThemePreset.PRESET_NONE);

        if (currentThemeId.equals(newThemeId)) {
            return; // Already active
        }

        SharedPreferences.Editor editor = prefs.edit();

        // 1. Save current settings to current theme profile JSON
        saveThemeProfileJson(context, prefs, editor, currentThemeId);

        // 2. Check if new theme needs purchase validation
        if (!ThemePreset.PRESET_NONE.equals(newThemeId) && !isUserEligible(context)) {
            Log.w(TAG, "User is not eligible for premium themes.");
            return;
        }

        // 3. Load new theme profile JSON or apply preset defaults if first time
        if (ThemePreset.PRESET_NONE.equals(newThemeId)) {
            boolean hasProfile = prefs.getBoolean("profile_saved_" + ThemePreset.PRESET_NONE, false);
            if (hasProfile) {
                String json = prefs.getString("profile_json_" + ThemePreset.PRESET_NONE, "");
                restorePreferencesFromJson(context, json);
            } else {
                editor.putInt("clockColor", Color.parseColor("#33B5E5"));
                editor.putInt("primaryColorNight", Color.parseColor("#33B5E5"));
                editor.putInt("secondaryColor", Color.parseColor("#C2C2C2"));
                editor.putInt("secondaryColorNight", Color.parseColor("#C2C2C2"));
                editor.remove("font");
                editor.putInt("clockBackgroundTransparency", 100);
                editor.putBoolean("theme_animation_enabled", false);
                editor.putString("theme_particle_effect", "0");
            }
            editor.putString(PREF_ACTIVE_PRESET, ThemePreset.PRESET_NONE);
        } else {
            ThemePreset preset = getPreset(newThemeId);
            if (preset != null) {
                boolean hasProfile = prefs.getBoolean("profile_saved_" + newThemeId, false);
                if (hasProfile) {
                    String json = prefs.getString("profile_json_" + newThemeId, "");
                    restorePreferencesFromJson(context, json);
                } else {
                    applyPresetDefaults(context, prefs, editor, preset);
                }
                editor.putString(PREF_ACTIVE_PRESET, newThemeId);
            }
        }

        editor.apply();
        new Settings(context);
    }

    public static void resetActiveThemeDefaults(Context context) {
        SharedPreferences prefs = getPreferences(context);
        String activeThemeId = prefs.getString(PREF_ACTIVE_PRESET, ThemePreset.PRESET_NONE);

        SharedPreferences.Editor editor = prefs.edit();
        // Clear saved profile for active theme so it re-initializes defaults
        editor.remove("profile_saved_" + activeThemeId);
        editor.remove("profile_json_" + activeThemeId);

        if (ThemePreset.PRESET_NONE.equals(activeThemeId)) {
            editor.putInt("clockColor", Color.parseColor("#33B5E5"));
            editor.putInt("primaryColorNight", Color.parseColor("#33B5E5"));
            editor.putInt("secondaryColor", Color.parseColor("#C2C2C2"));
            editor.putInt("secondaryColorNight", Color.parseColor("#C2C2C2"));
            editor.remove("font");
            editor.putInt("clockBackgroundTransparency", 100);
            editor.putBoolean("theme_animation_enabled", false);
            editor.putString("theme_particle_effect", "0");
        } else {
            ThemePreset preset = getPreset(activeThemeId);
            if (preset != null) {
                clearBackgroundImagesDir(context, activeThemeId);
                applyPresetDefaults(context, prefs, editor, preset);
            }
        }

        editor.apply();
        new Settings(context);
    }

    private static void clearBackgroundImagesDir(Context context, String themeId) {
        if (themeId == null || ThemePreset.PRESET_NONE.equals(themeId)) {
            return; // For the standard theme no clearing is necessary
        }
        File backgroundDir = new File(context.getFilesDir(), "backgroundImages");
        if (backgroundDir.exists() && backgroundDir.isDirectory()) {
            File[] files = backgroundDir.listFiles();
            if (files != null) {
                String prefix = "theme_" + themeId + "_";
                for (File f : files) {
                    if (f.getName().startsWith(prefix)) {
                        f.delete();
                    }
                }
            }
        }
    }

    private static void applyPresetDefaults(Context context, SharedPreferences prefs, SharedPreferences.Editor editor, ThemePreset preset) {
        clearBackgroundImagesDir(context, preset.id);

        editor.putInt("clockColor", preset.primaryColor);
        if (ThemePreset.PRESET_HALLOWEEN_ANALOG.equals(preset.id) || ThemePreset.PRESET_HALLOWEEN_DIGITAL.equals(preset.id)) {
            editor.putInt("primaryColorNight", Color.rgb(204, 88, 3));   // #CC5803
            editor.putInt("secondaryColorNight", Color.rgb(123, 31, 162)); // #7B1FA2
        } else {
            editor.putInt("primaryColorNight", Color.rgb(156, 124, 56));
            editor.putInt("secondaryColorNight", Color.rgb(120, 95, 42));
        }
        editor.putInt("secondaryColor", preset.secondaryColor);
        editor.putString("clockLayout", String.valueOf(preset.clockLayoutType));
        editor.putString("backgroundMode", String.valueOf(preset.backgroundMode));
        editor.putInt("clockBackgroundTransparency", preset.clockBackgroundTransparency);
        editor.putBoolean("autoAccentColor", false);
        editor.putBoolean("theme_animation_enabled", true);
        editor.putString("theme_particle_effect", String.valueOf(preset.particleEffect));

        if (preset.fontPath != null && !preset.fontPath.isEmpty()) {
            editor.putString("font", preset.fontPath);
        } else {
            editor.remove("font");
        }

        editor.putBoolean("profile_saved_" + preset.id, true);

        // Clear any pride/rainbow textures for preset themes
        Settings settings = new Settings(context);
        for (int i = 0; i <= 9; i++) {
            settings.setTextureId(Settings.TEXTURE_NONE, i);
        }

        if (ThemePreset.PRESET_CHRISTMAS_ANALOG.equals(preset.id) || ThemePreset.PRESET_HALLOWEEN_ANALOG.equals(preset.id)) {
            for (AnalogClockConfig.Style style : AnalogClockConfig.Style.values()) {
                AnalogClockConfig config = new AnalogClockConfig(context, style);
                config.decoration = AnalogClockConfig.Decoration.GOLD;
                config.digitPosition = 0.82f;
                config.digitStyle = AnalogClockConfig.DigitStyle.ARABIC;
                config.emphasizeHour12 = true;
                config.showSecondHand = true;
                config.handShape = AnalogClockConfig.HandShape.TRIANGLE;
                config.handLengthHours = 0.72f;
                config.handLengthMinutes = 0.92f;
                config.handWidthHours = 0.045f;
                config.handWidthMinutes = 0.03f;
                config.highlightQuarterOfHour = true;
                config.innerCircleRadius = 0.05f;
                config.tickStartMinutes = 0.94f;
                config.tickStyleMinutes = AnalogClockConfig.TickStyle.DASH;
                config.tickLengthMinutes = 0.04f;
                config.tickStartHours = 0.92f;
                config.tickWidthHours = 0.025f;
                config.tickWidthMinutes = 0.01f;
                config.tickStyleHours = AnalogClockConfig.TickStyle.CIRCLE;
                config.tickLengthHours = 0.06f;
                config.outerCircleRadius = 1.0f;
                config.outerCircleWidth = 0.02f;
                config.fontSize = 0.085f;
                config.fontUri = "file:///android_asset/fonts/dancingscript_regular.ttf";
                config.save();
            }
        }
        
        // Save initial profile JSON snapshot
        saveThemeProfileJson(context, prefs, editor, preset.id);
    }

    private static void saveThemeProfileJson(Context context, SharedPreferences prefs, SharedPreferences.Editor editor, String themeId) {
        String json = serializePreferencesToJson(context);
        editor.putString("profile_json_" + themeId, json);
        editor.putBoolean("profile_saved_" + themeId, true);
    }

    private static List<String> getSharedPreferencesNames(Context context) {
        Set<String> names = new HashSet<>();
        names.add(Settings.PREFS_KEY);
        names.add("defaults");
        names.add("DEFAULT");
        names.add("SIMPLE");
        names.add("ARC");
        names.add("MINIMALISTIC");

        File dataDir = context.getDataDir();
        File prefsDir = new File(dataDir, "shared_prefs");

        if (prefsDir.exists() && prefsDir.isDirectory()) {
            File[] files = prefsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    if (fileName.endsWith(".xml")) {
                        String name = fileName.substring(0, fileName.length() - 4);
                        names.add(name);
                    }
                }
            }
        }

        return new ArrayList<>(names);
    }

    private static String serializePreferencesToJson(Context context) {
        try {
            JSONObject root = new JSONObject();
            JSONObject allPreferencesObj = new JSONObject();

            List<String> prefNames = getSharedPreferencesNames(context);
            for (String prefName : prefNames) {
                SharedPreferences sp = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
                Map<String, ?> allEntries = sp.getAll();

                JSONObject prefEntryObj = new JSONObject();
                JSONObject typesObj = new JSONObject();
                JSONObject valuesObj = new JSONObject();

                for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                    String key = entry.getKey();
                    if (key == null) continue;

                    // Skip theme internal profile metadata keys
                    if (key.equals(PREF_ACTIVE_PRESET) || key.startsWith("profile_saved_") || key.startsWith("profile_json_")) {
                        continue;
                    }

                    Object value = entry.getValue();
                    if (value == null) continue;

                    if (value instanceof Boolean) {
                        typesObj.put(key, "boolean");
                        valuesObj.put(key, (Boolean) value);
                    } else if (value instanceof Integer) {
                        typesObj.put(key, "int");
                        valuesObj.put(key, (Integer) value);
                    } else if (value instanceof Long) {
                        typesObj.put(key, "long");
                        valuesObj.put(key, (Long) value);
                    } else if (value instanceof Float) {
                        typesObj.put(key, "float");
                        valuesObj.put(key, ((Float) value).doubleValue());
                    } else if (value instanceof String) {
                        typesObj.put(key, "string");
                        valuesObj.put(key, (String) value);
                    } else if (value instanceof Set) {
                        typesObj.put(key, "set");
                        JSONArray arr = new JSONArray();
                        for (Object item : (Set<?>) value) {
                            arr.put(item.toString());
                        }
                        valuesObj.put(key, arr);
                    }
                }

                prefEntryObj.put("types", typesObj);
                prefEntryObj.put("values", valuesObj);
                allPreferencesObj.put(prefName, prefEntryObj);
            }

            root.put("preferences", allPreferencesObj);
            return root.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to serialize preferences to JSON", e);
            return "";
        }
    }

    private static void restorePreferencesFromJson(Context context, String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) return;
        try {
            JSONObject rootJson = new JSONObject(jsonString);
            if (!rootJson.has("preferences")) return;

            JSONObject preferencesObj = rootJson.getJSONObject("preferences");
            Iterator<String> prefNames = preferencesObj.keys();
            while (prefNames.hasNext()) {
                String prefName = prefNames.next();
                JSONObject prefEntryObj = preferencesObj.getJSONObject(prefName);

                JSONObject typesObj = prefEntryObj.optJSONObject("types");
                JSONObject valuesObj = prefEntryObj.optJSONObject("values");

                if (valuesObj == null) {
                    valuesObj = prefEntryObj;
                }

                SharedPreferences sp = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);

                // Preserve local purchase status cache in "defaults"
                Map<String, Boolean> purchaseCache = new HashMap<>();
                Map<String, ?> currentEntries = sp.getAll();
                if ("defaults".equals(prefName)) {
                    for (Map.Entry<String, ?> entry : currentEntries.entrySet()) {
                        if (entry.getKey().startsWith("purchased_") && entry.getValue() instanceof Boolean) {
                            purchaseCache.put(entry.getKey(), (Boolean) entry.getValue());
                        }
                    }
                }

                // Preserve theme metadata cache in PREFS_KEY
                Map<String, Object> themeMetaCache = new HashMap<>();
                if (Settings.PREFS_KEY.equals(prefName)) {
                    for (Map.Entry<String, ?> entry : currentEntries.entrySet()) {
                        String k = entry.getKey();
                        if (k.equals(PREF_ACTIVE_PRESET) || k.startsWith("profile_saved_") || k.startsWith("profile_json_")) {
                            themeMetaCache.put(k, entry.getValue());
                        }
                    }
                }

                SharedPreferences.Editor editor = sp.edit();
                editor.clear();
                editor.apply();

                Iterator<String> keys = valuesObj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();

                    if ("defaults".equals(prefName) && key.startsWith("purchased_")) {
                        continue;
                    }
                    if (Settings.PREFS_KEY.equals(prefName) && (key.equals(PREF_ACTIVE_PRESET) || key.startsWith("profile_saved_") || key.startsWith("profile_json_"))) {
                        continue;
                    }

                    String type = (typesObj != null && typesObj.has(key)) ? typesObj.getString(key) : null;
                    Object valueObj = valuesObj.get(key);

                    if ("boolean".equals(type) || valueObj instanceof Boolean) {
                        editor.putBoolean(key, valuesObj.getBoolean(key));
                    } else if ("int".equals(type) || (valueObj instanceof Integer && type == null)) {
                        editor.putInt(key, valuesObj.getInt(key));
                    } else if ("long".equals(type) || valueObj instanceof Long) {
                        editor.putLong(key, valuesObj.getLong(key));
                    } else if ("float".equals(type) || valueObj instanceof Double) {
                        editor.putFloat(key, (float) valuesObj.getDouble(key));
                    } else if ("set".equals(type) || valueObj instanceof JSONArray) {
                        JSONArray arr = valuesObj.getJSONArray(key);
                        Set<String> set = new HashSet<>();
                        for (int i = 0; i < arr.length(); i++) {
                            set.add(arr.getString(i));
                        }
                        editor.putStringSet(key, set);
                    } else {
                        editor.putString(key, valuesObj.getString(key));
                    }
                }

                // Restore purchase cache
                for (Map.Entry<String, Boolean> entry : purchaseCache.entrySet()) {
                    editor.putBoolean(entry.getKey(), entry.getValue());
                }

                // Restore theme metadata cache
                for (Map.Entry<String, Object> entry : themeMetaCache.entrySet()) {
                    Object val = entry.getValue();
                    String k = entry.getKey();
                    if (val instanceof Boolean) editor.putBoolean(k, (Boolean) val);
                    else if (val instanceof Integer) editor.putInt(k, (Integer) val);
                    else if (val instanceof Long) editor.putLong(k, (Long) val);
                    else if (val instanceof Float) editor.putFloat(k, (Float) val);
                    else if (val instanceof String) editor.putString(k, (String) val);
                }

                editor.commit();
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to restore preferences from JSON", e);
        }
    }
}
