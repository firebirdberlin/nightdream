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

package com.firebirdberlin.nightdream.ui.background;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnowOverlayView extends View {
    private static final int NUM_SNOWFLAKES = 45;
    private static final long FRAME_DELAY_MS = 50L; // ~20 FPS cap for battery safety

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Snowflake> snowflakes = new ArrayList<>();
    private final Random random = new Random();

    private boolean isSnowEnabled = false;
    private boolean isPausedByPowerSetting = false;
    private boolean isNightModeDimmed = false;

    private static class Snowflake {
        float x, y;
        float radius;
        float speedY;
        float speedX;
        int alpha;
        float swayAngle;
        float swaySpeed;
    }

    public SnowOverlayView(Context context) {
        super(context);
        init();
    }

    public SnowOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SnowOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            initSnowflakes(w, h);
        }
    }

    private void initSnowflakes(int width, int height) {
        snowflakes.clear();
        for (int i = 0; i < NUM_SNOWFLAKES; i++) {
            snowflakes.add(createRandomSnowflake(width, height, true));
        }
    }

    private Snowflake createRandomSnowflake(int width, int height, boolean randomY) {
        Snowflake flake = new Snowflake();
        flake.x = random.nextFloat() * width;
        flake.y = randomY ? random.nextFloat() * height : -10f;
        flake.radius = 2.5f + random.nextFloat() * 4.5f;
        flake.speedY = 1.2f + random.nextFloat() * 2.5f;
        flake.speedX = -0.5f + random.nextFloat() * 1.0f;
        flake.alpha = 100 + random.nextInt(130);
        flake.swayAngle = random.nextFloat() * (float) Math.PI * 2;
        flake.swaySpeed = 0.02f + random.nextFloat() * 0.03f;
        return flake;
    }

    public void setSnowEnabled(boolean enabled) {
        this.isSnowEnabled = enabled;
        setVisibility(enabled ? VISIBLE : GONE);
        if (enabled) {
            invalidate();
        }
    }

    public void setPausedByPowerSetting(boolean paused) {
        this.isPausedByPowerSetting = paused;
        if (!paused && isSnowEnabled) {
            invalidate();
        }
    }

    public void setNightModeDimmed(boolean dimmed) {
        this.isNightModeDimmed = dimmed;
        if (isSnowEnabled) {
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isSnowEnabled || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        int width = getWidth();
        int height = getHeight();

        // Calculate opacity scaling for night mode dimming
        float alphaMultiplier = isNightModeDimmed ? 0.35f : 1.0f;

        for (Snowflake flake : snowflakes) {
            paint.setAlpha((int) (flake.alpha * alphaMultiplier));
            canvas.drawCircle(flake.x, flake.y, flake.radius, paint);

            if (!isPausedByPowerSetting) {
                flake.y += flake.speedY;
                flake.swayAngle += flake.swaySpeed;
                flake.x += (float) Math.sin(flake.swayAngle) * 0.8f + flake.speedX;

                if (flake.y > height + 10 || flake.x < -20 || flake.x > width + 20) {
                    Snowflake newFlake = createRandomSnowflake(width, height, false);
                    flake.x = newFlake.x;
                    flake.y = newFlake.y;
                    flake.radius = newFlake.radius;
                    flake.speedY = newFlake.speedY;
                    flake.speedX = newFlake.speedX;
                    flake.alpha = newFlake.alpha;
                }
            }
        }

        if (!isPausedByPowerSetting) {
            postInvalidateDelayed(FRAME_DELAY_MS);
        }
    }
}
