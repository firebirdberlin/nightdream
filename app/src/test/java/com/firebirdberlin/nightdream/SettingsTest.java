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

package com.firebirdberlin.nightdream;

import android.content.SharedPreferences;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SettingsTest {

    private static class TestSharedPreferences implements SharedPreferences {
        private final Map<String, Object> map = new HashMap<>();

        @Override
        public Map<String, ?> getAll() { return map; }

        @Override
        public String getString(String key, String defValue) {
            Object val = map.get(key);
            return val instanceof String ? (String) val : defValue;
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            Object val = map.get(key);
            return val instanceof Set ? (Set<String>) val : defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object val = map.get(key);
            return val instanceof Integer ? (Integer) val : defValue;
        }

        @Override
        public long getLong(String key, long defValue) { return 0; }

        @Override
        public float getFloat(String key, float defValue) {
            Object val = map.get(key);
            return val instanceof Float ? (Float) val : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) { return false; }

        @Override
        public boolean contains(String key) { return map.containsKey(key); }

        @Override
        public Editor edit() { return new TestEditor(this); }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}
    }

    private static class TestEditor implements SharedPreferences.Editor {
        private final TestSharedPreferences prefs;
        private final Map<String, Object> changes = new HashMap<>();

        public TestEditor(TestSharedPreferences prefs) {
            this.prefs = prefs;
        }

        @Override
        public SharedPreferences.Editor putString(String key, String value) { changes.put(key, value); return this; }

        @Override
        public SharedPreferences.Editor putStringSet(String key, Set<String> values) { changes.put(key, values); return this; }

        @Override
        public SharedPreferences.Editor putInt(String key, int value) { changes.put(key, value); return this; }

        @Override
        public SharedPreferences.Editor putLong(String key, long value) { changes.put(key, value); return this; }

        @Override
        public SharedPreferences.Editor putFloat(String key, float value) { changes.put(key, value); return this; }

        @Override
        public SharedPreferences.Editor putBoolean(String key, boolean value) { changes.put(key, value); return this; }

        @Override
        public SharedPreferences.Editor remove(String key) { changes.remove(key); return this; }

        @Override
        public SharedPreferences.Editor clear() { changes.clear(); return this; }

        @Override
        public boolean commit() { apply(); return true; }

        @Override
        public void apply() {
            prefs.map.putAll(changes);
        }
    }

    @Test
    public void testGetBrightnessOffsetDefault() {
        TestSharedPreferences prefs = new TestSharedPreferences();
        Settings settings = new Settings(prefs);

        Assert.assertEquals(0.8f, settings.getBrightnessOffset(), 0.001f);
    }

    @Test
    public void testGetBrightnessOffsetFromPrefs() {
        TestSharedPreferences prefs = new TestSharedPreferences();
        prefs.map.put("brightness_offset", 75);

        Settings settings = new Settings(prefs);
        Assert.assertEquals(0.75f, settings.getBrightnessOffset(), 0.001f);
    }

    @Test
    public void testSetBrightnessOffset() {
        TestSharedPreferences prefs = new TestSharedPreferences();
        Settings settings = new Settings(prefs);
        settings.setBrightnessOffset(0.65f);

        Assert.assertEquals(0.65f, settings.getBrightnessOffset(), 0.001f);
        Assert.assertEquals(65, prefs.getInt("brightness_offset", 0));
    }

    @Test
    public void testGetNightModeBrightnessDefault() {
        TestSharedPreferences prefs = new TestSharedPreferences();
        Settings settings = new Settings(prefs);

        Assert.assertEquals(0.0f, settings.getNightModeBrightness(), 0.001f);
    }

    @Test
    public void testGetNightModeBrightnessFromPrefs() {
        TestSharedPreferences prefs = new TestSharedPreferences();
        prefs.map.put("nightModeBrightness", 0.15f);

        Settings settings = new Settings(prefs);
        Assert.assertEquals(0.15f, settings.getNightModeBrightness(), 0.001f);
    }

    @Test
    public void testSetNightModeBrightness() {
        TestSharedPreferences prefs = new TestSharedPreferences();
        Settings settings = new Settings(prefs);
        settings.setNightModeBrightness(0.2f);

        Assert.assertEquals(0.2f, settings.getNightModeBrightness(), 0.001f);
        Assert.assertEquals(0.2f, prefs.getFloat("nightModeBrightness", 0f), 0.001f);
    }
}
