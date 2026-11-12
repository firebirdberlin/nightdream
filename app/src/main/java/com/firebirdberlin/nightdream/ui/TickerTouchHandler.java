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

package com.firebirdberlin.nightdream.ui;

import android.view.ViewConfiguration;

public class TickerTouchHandler {
    private static final String TAG = TickerTouchHandler.class.getSimpleName();

    private float downX;
    private float downY;
    private float lastX;
    private long downTime;
    private boolean isClickValid = false;
    private boolean isDragging = false;
    private String pendingUrl = null;
    private int touchSlop;
    private int longPressTimeout;

    public TickerTouchHandler(ViewConfiguration vc) {
        if (vc != null) {
            touchSlop = vc.getScaledTouchSlop();
            longPressTimeout = ViewConfiguration.getLongPressTimeout();
        } else {
            touchSlop = 8;
            longPressTimeout = 500;
        }
    }

    public static boolean isDragExceeded(float downX, float downY, float currentX, float currentY, int touchSlop) {
        float totalDx = Math.abs(currentX - downX);
        float totalDy = Math.abs(currentY - downY);
        return totalDx > touchSlop || totalDy > touchSlop;
    }

    public static boolean isValidTap(boolean isClickValid, long duration, int longPressTimeout) {
        return isClickValid && duration <= longPressTimeout;
    }

    public float getDownX() {
        return downX;
    }

    public void setDownX(float downX) {
        this.downX = downX;
    }

    public float getDownY() {
        return downY;
    }

    public void setDownY(float downY) {
        this.downY = downY;
    }

    public float getLastX() {
        return lastX;
    }

    public void setLastX(float lastX) {
        this.lastX = lastX;
    }

    public long getDownTime() {
        return downTime;
    }

    public void setDownTime(long downTime) {
        this.downTime = downTime;
    }

    public boolean isClickValid() {
        return isClickValid;
    }

    public void setClickValid(boolean clickValid) {
        isClickValid = clickValid;
    }

    public boolean isDragging() {
        return isDragging;
    }

    public void setDragging(boolean dragging) {
        isDragging = dragging;
    }

    public String getPendingUrl() {
        return pendingUrl;
    }

    public void setPendingUrl(String pendingUrl) {
        this.pendingUrl = pendingUrl;
    }

    public int getTouchSlop() {
        return touchSlop;
    }

    public int getLongPressTimeout() {
        return longPressTimeout;
    }

    public void reset() {
        isClickValid = false;
        isDragging = false;
        pendingUrl = null;
    }
}
