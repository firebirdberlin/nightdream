/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
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
import android.graphics.Point;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.firebirdberlin.nightdream.Utility;

public class BackgroundBurnInProtectionHelper {
    private static final String TAG = BackgroundBurnInProtectionHelper.class.getSimpleName();

//    private static final long INTERVAL_MS = 180000L; // 3 minutes
    private static final long INTERVAL_MS = 30000L; // 30 seconds
    private static final long ANIMATION_DURATION_MS = 1500L; // 1.5 seconds
    private static final int MAX_DISPLACEMENT_DP = 8;
    private static final float DEFAULT_ZOOM = 1.03f;

    // 8-point orbit pattern in dp: (x, y)
    private static final int[][] ORBIT_DP = {
            {0, 0},
            {8, 4},
            {-4, 8},
            {-8, -4},
            {4, -8},
            {8, -8},
            {-8, 8},
            {4, 4}
    };

    private final Context context;
    private final Handler handler;
    private final Runnable shiftRunnable = this::shiftNext;

    private int currentStep = 0;
    private boolean isRunning = false;
    private View currentTargetView = null;

    public BackgroundBurnInProtectionHelper(@NonNull Context context, @NonNull Handler handler) {
        this.context = context.getApplicationContext();
        this.handler = handler;
    }

    public void start(@Nullable View targetView, boolean isZoomInActive) {
        Log.d(TAG, "start() targetView=" + targetView + ", isZoomInActive=" + isZoomInActive);
        stop();
        if (targetView == null || isZoomInActive) {
            Log.d(TAG, "start() skipped: targetView is null or slideshow zoom-in is active");
            return;
        }

        currentTargetView = targetView;
        isRunning = true;

        applyZoomAndPosition(targetView, false);
        handler.postDelayed(shiftRunnable, INTERVAL_MS);
        Log.d(TAG, "start() scheduled next shift in " + (INTERVAL_MS / 1000) + "s");
    }

    public void stop() {
        Log.d(TAG, "stop() isRunning=" + isRunning);
        handler.removeCallbacks(shiftRunnable);
        if (currentTargetView != null) {
            currentTargetView.animate().cancel();
        }
        isRunning = false;
    }

    public void reset(@Nullable View targetView) {
        Log.d(TAG, "reset() targetView=" + targetView);
        stop();
        currentStep = 0;
        if (targetView != null) {
            targetView.animate().cancel();
            targetView.setScaleX(1.0f);
            targetView.setScaleY(1.0f);
            targetView.setTranslationX(0.0f);
            targetView.setTranslationY(0.0f);
        }
    }

    public void applyZoomAndPosition(@Nullable View targetView, boolean isZoomInActive) {
        Log.d(TAG, "applyZoomAndPosition() step=" + currentStep
                + ", isZoomInActive=" + isZoomInActive);
        if (targetView == null) return;

        if (isZoomInActive) {
            Log.d(TAG, "applyZoomAndPosition() resetting transforms due to isZoomInActive");
            targetView.setScaleX(1.0f);
            targetView.setScaleY(1.0f);
            targetView.setTranslationX(0.0f);
            targetView.setTranslationY(0.0f);
            return;
        }

        float zoomScale = calculateZoomScale();
        targetView.setScaleX(zoomScale);
        targetView.setScaleY(zoomScale);

        int[] dpOffset = ORBIT_DP[currentStep];
        float targetX = Utility.dpToPx(context, dpOffset[0]);
        float targetY = Utility.dpToPx(context, dpOffset[1]);
        targetView.setTranslationX(targetX);
        targetView.setTranslationY(targetY);

        Log.d(TAG, "applyZoomAndPosition() applied zoomScale=" + zoomScale
                + ", offset=(" + dpOffset[0] + "dp, " + dpOffset[1] + "dp -> "
                + targetX + "px, " + targetY + "px)");
    }

    public void onConfigurationChanged(@Nullable View targetView, boolean isZoomInActive) {
        Log.d(TAG, "onConfigurationChanged() isRunning=" + isRunning
                + ", isZoomInActive=" + isZoomInActive);
        if (!isRunning || targetView == null || isZoomInActive) return;

        currentTargetView = targetView;
        applyZoomAndPosition(targetView, false);
    }

    private float calculateZoomScale() {
        float maxShiftPx = Utility.dpToPx(context, MAX_DISPLACEMENT_DP);
        Point displaySize = Utility.getDisplaySize(context);
        int minDimension = Math.min(displaySize.x, displaySize.y);
        if (minDimension <= 0) return DEFAULT_ZOOM;

        float requiredScale = 1.0f + (2.0f * maxShiftPx / (float) minDimension);
        float finalZoom = Math.max(DEFAULT_ZOOM, requiredScale);
        Log.d(TAG, "calculateZoomScale() minDimension=" + minDimension
                + ", maxShiftPx=" + maxShiftPx + ", finalZoom=" + finalZoom);
        return finalZoom;
    }

    private void shiftNext() {
        if (!isRunning || currentTargetView == null) {
            Log.d(TAG, "shiftNext() skipped: isRunning=" + isRunning
                    + ", currentTargetView=" + currentTargetView);
            return;
        }

        currentStep = (currentStep + 1) % ORBIT_DP.length;
        int[] dpOffset = ORBIT_DP[currentStep];
        float targetX = Utility.dpToPx(context, dpOffset[0]);
        float targetY = Utility.dpToPx(context, dpOffset[1]);

        Log.d(TAG, "shiftNext() step " + currentStep + " (" + dpOffset[0] + "dp, "
                + dpOffset[1] + "dp -> " + targetX + "px, " + targetY + "px)");

        currentTargetView.animate()
                .translationX(targetX)
                .translationY(targetY)
                .setDuration(ANIMATION_DURATION_MS)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        handler.postDelayed(shiftRunnable, INTERVAL_MS);
    }
}
