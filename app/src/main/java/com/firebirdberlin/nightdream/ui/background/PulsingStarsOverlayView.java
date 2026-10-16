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
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PulsingStarsOverlayView extends View {
    private static final int NUM_STARS = 25;
    private static final long FRAME_DELAY_MS = 65L; // ~15 FPS cap for battery protection

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path starPath = new Path();
    private final List<Star> stars = new ArrayList<>();
    private final Random random = new Random();

    private boolean isStarsEnabled = false;
    private boolean isPausedByPowerSetting = false;
    private boolean isNightModeDimmed = false;
    private float maxYRatio = 0.75f;
    private float starSizeScale = 1.0f;
    private int numStars = NUM_STARS;

    private static class Star {
        float x, y;
        float radius;
        int baseAlpha;
        float pulseAngle;
        float pulseSpeed;
    }

    public PulsingStarsOverlayView(Context context) {
        super(context);
        init();
    }

    public PulsingStarsOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PulsingStarsOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setColor(Color.rgb(229, 193, 88)); // Warm champagne gold
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            initStars(w, h);
        }
    }

    private void initStars(int width, int height) {
        stars.clear();
        for (int i = 0; i < numStars; i++) {
            Star star = new Star();
            star.x = random.nextFloat() * width;
            star.y = random.nextFloat() * (height * maxYRatio);
            star.radius = (4.0f + random.nextFloat() * 4.0f) * starSizeScale; // Size for 4-point sparkle
            star.baseAlpha = 140 + random.nextInt(110);
            star.pulseAngle = random.nextFloat() * (float) Math.PI * 2;
            star.pulseSpeed = 0.03f + random.nextFloat() * 0.04f;
            stars.add(star);
        }
    }

    public void setNumStars(int count) {
        if (count > 0 && this.numStars != count) {
            this.numStars = count;
            if (getWidth() > 0 && getHeight() > 0) {
                initStars(getWidth(), getHeight());
            }
        }
    }

    public void setMaxYRatio(float ratio) {
        if (this.maxYRatio != ratio) {
            this.maxYRatio = ratio;
            if (getWidth() > 0 && getHeight() > 0) {
                initStars(getWidth(), getHeight());
            }
        }
    }

    public void setStarSizeScale(float scale) {
        if (this.starSizeScale != scale) {
            this.starSizeScale = scale;
            if (getWidth() > 0 && getHeight() > 0) {
                initStars(getWidth(), getHeight());
            }
        }
    }

    public void setStarsEnabled(boolean enabled) {
        this.isStarsEnabled = enabled;
        setVisibility(enabled ? VISIBLE : GONE);
        if (enabled) {
            invalidate();
        }
    }

    public void setPausedByPowerSetting(boolean paused) {
        this.isPausedByPowerSetting = paused;
        if (!paused && isStarsEnabled) {
            invalidate();
        }
    }

    public void setNightModeDimmed(boolean dimmed) {
        this.isNightModeDimmed = dimmed;
        if (isStarsEnabled) {
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isStarsEnabled || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        float alphaMultiplier = isNightModeDimmed ? 0.35f : 1.0f;

        for (Star star : stars) {
            float pulseFactor = 0.5f + 0.5f * (float) Math.sin(star.pulseAngle);
            int alpha = (int) (star.baseAlpha * pulseFactor * alphaMultiplier);
            paint.setAlpha(Math.min(255, Math.max(0, alpha)));

            drawSparkleStar(canvas, star.x, star.y, star.radius, paint);

            if (!isPausedByPowerSetting) {
                star.pulseAngle += star.pulseSpeed;
            }
        }

        if (!isPausedByPowerSetting) {
            postInvalidateDelayed(FRAME_DELAY_MS);
        }
    }

    private void drawSparkleStar(Canvas canvas, float cx, float cy, float radius, Paint paint) {
        starPath.reset();
        float innerRadius = radius * 0.3f;
        for (int i = 0; i < 4; i++) {
            double angleOuter = Math.PI / 2 * i - Math.PI / 2;
            double angleInner = angleOuter + Math.PI / 4;
            float xOuter = cx + (float) Math.cos(angleOuter) * radius;
            float yOuter = cy + (float) Math.sin(angleOuter) * radius;
            float xInner = cx + (float) Math.cos(angleInner) * innerRadius;
            float yInner = cy + (float) Math.sin(angleInner) * innerRadius;

            if (i == 0) {
                starPath.moveTo(xOuter, yOuter);
            } else {
                starPath.lineTo(xOuter, yOuter);
            }
            starPath.lineTo(xInner, yInner);
        }
        starPath.close();
        canvas.drawPath(starPath, paint);
    }
}
