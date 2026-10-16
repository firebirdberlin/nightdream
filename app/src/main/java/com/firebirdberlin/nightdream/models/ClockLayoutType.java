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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public enum ClockLayoutType {
    DIGITAL(0),
    ANALOG(1),
    ANALOG2(2),
    ANALOG3(3),
    ANALOG4(4),
    DIGITAL_FLIP(5),
    CALENDAR(6),
    DIGITAL2(7),
    DIGITAL3(8),
    DIGITAL_ANIMATED(9);

    private final int value;

    ClockLayoutType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @NonNull
    public static ClockLayoutType fromValue(int value) {
        return fromValue(value, DIGITAL);
    }

    @NonNull
    public static ClockLayoutType fromValue(int value, @NonNull ClockLayoutType defaultValue) {
        for (ClockLayoutType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return defaultValue;
    }

    @NonNull
    public static ClockLayoutType fromValue(@Nullable String valueStr, @NonNull ClockLayoutType defaultValue) {
        if (valueStr != null) {
            try {
                int val = Integer.parseInt(valueStr);
                return fromValue(val, defaultValue);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }
}
