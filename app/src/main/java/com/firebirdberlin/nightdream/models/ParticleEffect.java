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

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public enum ParticleEffect {
    SNOW(1),
    GOLDEN_STARS(2);

    private final int value;

    ParticleEffect(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @NonNull
    public static String toPreferenceValue(@Nullable ParticleEffect effect) {
        return String.valueOf((effect != null ? effect : SNOW).getValue());
    }

    @NonNull
    public static ParticleEffect fromValue(int value) {
        return fromValue(value, SNOW);
    }

    @NonNull
    public static ParticleEffect fromValue(int value, @Nullable ParticleEffect defaultValue) {
        if (value <= 0) {
            return SNOW;
        }
        ParticleEffect fallback = defaultValue != null ? defaultValue : SNOW;
        for (ParticleEffect effect : values()) {
            if (effect.value == value) {
                return effect;
            }
        }
        return fallback;
    }

    @NonNull
    public static ParticleEffect fromValue(@Nullable String valueStr,
                                            @Nullable ParticleEffect defaultValue) {
        ParticleEffect fallback = defaultValue != null ? defaultValue : SNOW;
        if (valueStr != null) {
            if ("0".equals(valueStr) || "none".equalsIgnoreCase(valueStr)) {
                return SNOW;
            }
            try {
                int val = Integer.parseInt(valueStr);
                return fromValue(val, fallback);
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    @NonNull
    public static ParticleEffect fromPreference(@Nullable SharedPreferences prefs,
                                                @NonNull String key,
                                                @Nullable ParticleEffect defaultValue) {
        ParticleEffect fallback = defaultValue != null ? defaultValue : SNOW;
        if (prefs == null) return fallback;

        String str = null;
        try {
            str = prefs.getString(key, null);
        } catch (ClassCastException e) {
            try {
                int val = prefs.getInt(key, -1);
                if (val != -1) {
                    str = String.valueOf(val);
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        }

        if (str != null) {
            ParticleEffect effect = fromValue(str, fallback);
            if (isLegacyOrInvalid(str)) {
                try {
                    prefs.edit().putString(key, toPreferenceValue(SNOW)).apply();
                } catch (Exception ignored) {
                }
                return SNOW;
            }
            return effect;
        }

        return fallback;
    }

    public static boolean isLegacyOrInvalid(@Nullable String valueStr) {
        if (valueStr == null) return false;
        if ("0".equals(valueStr) || "none".equalsIgnoreCase(valueStr)) {
            return true;
        }
        try {
            int val = Integer.parseInt(valueStr);
            for (ParticleEffect effect : values()) {
                if (effect.value == val) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException ignored) {
        }
        return true;
    }
}
