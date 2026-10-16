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
    NONE(0),
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
    public static ParticleEffect fromValue(int value) {
        return fromValue(value, NONE);
    }

    @NonNull
    public static ParticleEffect fromValue(int value, @NonNull ParticleEffect defaultValue) {
        for (ParticleEffect effect : values()) {
            if (effect.value == value) {
                return effect;
            }
        }
        return defaultValue;
    }

    @NonNull
    public static ParticleEffect fromValue(@Nullable String valueStr, @NonNull ParticleEffect defaultValue) {
        if (valueStr != null) {
            try {
                int val = Integer.parseInt(valueStr);
                return fromValue(val, defaultValue);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    @NonNull
    public static ParticleEffect fromPreference(@Nullable SharedPreferences prefs, @NonNull String key, @NonNull ParticleEffect defaultValue) {
        if (prefs == null) return defaultValue;
        try {
            String str = prefs.getString(key, null);
            if (str != null) {
                return fromValue(str, defaultValue);
            }
        } catch (ClassCastException e) {
            try {
                int val = prefs.getInt(key, defaultValue.getValue());
                return fromValue(val, defaultValue);
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }
}
