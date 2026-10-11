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

public class ThemePreset {
    public static final String PRESET_NONE = "none";
    public static final String PRESET_HALLOWEEN_ANALOG = "halloween_analog";
    public static final String PRESET_HALLOWEEN_DIGITAL = "halloween_digital";
    public static final String PRESET_CHRISTMAS_ANALOG = "christmas_analog";
    public static final String PRESET_CHRISTMAS_DIGITAL = "christmas_digital";

    public final String id;
    public final String title;
    public final ClockLayoutType clockLayoutType;
    public final int primaryColor;
    public final int secondaryColor;
    public final int backgroundMode; // Settings.BACKGROUND_IMAGE
    public final String drawableResName; // "bg_christmas"
    public final int clockBackgroundTransparency;
    public final String fontPath;
    public final ParticleEffect particleEffect;
    public final float starMaxYRatio;
    public final float starSizeScale;
    public final int starCount;

    public ThemePreset(@NonNull String id,
                       @NonNull String title,
                       @NonNull ClockLayoutType clockLayoutType,
                       int primaryColor,
                       int secondaryColor,
                       int backgroundMode,
                       String drawableResName,
                       int clockBackgroundTransparency,
                       String fontPath,
                       @NonNull ParticleEffect particleEffect) {
        this(id, title, clockLayoutType, primaryColor, secondaryColor, backgroundMode,
             drawableResName, clockBackgroundTransparency, fontPath, particleEffect, 0.75f, 1.0f, 25);
    }

    public ThemePreset(@NonNull String id,
                       @NonNull String title,
                       @NonNull ClockLayoutType clockLayoutType,
                       int primaryColor,
                       int secondaryColor,
                       int backgroundMode,
                       String drawableResName,
                       int clockBackgroundTransparency,
                       String fontPath,
                       @NonNull ParticleEffect particleEffect,
                       float starMaxYRatio,
                       float starSizeScale) {
        this(id, title, clockLayoutType, primaryColor, secondaryColor, backgroundMode,
             drawableResName, clockBackgroundTransparency, fontPath, particleEffect, starMaxYRatio, starSizeScale, 25);
    }

    public ThemePreset(@NonNull String id,
                       @NonNull String title,
                       @NonNull ClockLayoutType clockLayoutType,
                       int primaryColor,
                       int secondaryColor,
                       int backgroundMode,
                       String drawableResName,
                       int clockBackgroundTransparency,
                       String fontPath,
                       @NonNull ParticleEffect particleEffect,
                       float starMaxYRatio,
                       float starSizeScale,
                       int starCount) {
        this.id = id;
        this.title = title;
        this.clockLayoutType = clockLayoutType;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.backgroundMode = backgroundMode;
        this.drawableResName = drawableResName;
        this.clockBackgroundTransparency = clockBackgroundTransparency;
        this.fontPath = fontPath;
        this.particleEffect = particleEffect;
        this.starMaxYRatio = starMaxYRatio;
        this.starSizeScale = starSizeScale;
        this.starCount = starCount;
    }

    public ThemePreset(@NonNull String id,
                       @NonNull String title,
                       int clockLayoutType,
                       int primaryColor,
                       int secondaryColor,
                       int backgroundMode,
                       String drawableResName,
                       int clockBackgroundTransparency,
                       String fontPath,
                       int particleEffect) {
        this(id, title, ClockLayoutType.fromValue(clockLayoutType), primaryColor, secondaryColor, backgroundMode,
             drawableResName, clockBackgroundTransparency, fontPath, ParticleEffect.fromValue(particleEffect), 0.75f, 1.0f, 25);
    }

    public ThemePreset(@NonNull String id,
                       @NonNull String title,
                       int clockLayoutType,
                       int primaryColor,
                       int secondaryColor,
                       int backgroundMode,
                       String drawableResName,
                       int clockBackgroundTransparency,
                       String fontPath,
                       int particleEffect,
                       float starMaxYRatio,
                       float starSizeScale) {
        this(id, title, ClockLayoutType.fromValue(clockLayoutType), primaryColor, secondaryColor, backgroundMode,
             drawableResName, clockBackgroundTransparency, fontPath, ParticleEffect.fromValue(particleEffect), starMaxYRatio, starSizeScale, 25);
    }

    public ThemePreset(@NonNull String id,
                       @NonNull String title,
                       int clockLayoutType,
                       int primaryColor,
                       int secondaryColor,
                       int backgroundMode,
                       String drawableResName,
                       int clockBackgroundTransparency,
                       String fontPath,
                       int particleEffect,
                       float starMaxYRatio,
                       float starSizeScale,
                       int starCount) {
        this(id, title, ClockLayoutType.fromValue(clockLayoutType), primaryColor, secondaryColor, backgroundMode,
             drawableResName, clockBackgroundTransparency, fontPath, ParticleEffect.fromValue(particleEffect), starMaxYRatio, starSizeScale, starCount);
    }
}
