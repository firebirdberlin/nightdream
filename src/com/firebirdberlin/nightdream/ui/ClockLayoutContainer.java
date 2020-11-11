package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class ClockLayoutContainer extends FrameLayout {
    private ClockLayout clockLayout;

    public ClockLayoutContainer(@NonNull Context context) {
        super(context);
    }

    public ClockLayoutContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ClockLayoutContainer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setClockLayout(ClockLayout clockLayout) {
        this.clockLayout = clockLayout;
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev);
    }

    public void applyScaleFactor(float factor) {
        int width = getWidth();
        int height = getHeight();
        factor *= clockLayout.getAbsScaleFactor();
        int new_width = (int) (clockLayout.getWidth() * factor);
        int new_height = (int) (clockLayout.getHeight() * factor);
        if (factor > 0.5f && new_width < width && new_height < height) {
            clockLayout.setScaleFactor(factor);
            keepClockWithinContainer(new_width, new_height, width, height);
        }
    }

    /**
     * make sure the clock does not run over clockLayoutContainer edges after scaling
     */
    private void keepClockWithinContainer(int newClockWidth, int newClockHeight, int containerWidth, int containerHeight) {

        final float distanceX = containerWidth - newClockWidth - Math.abs(clockLayout.getTranslationX()) * 2f;
        final float distanceY = containerHeight - newClockHeight - Math.abs(clockLayout.getTranslationY()) * 2f;

        if (distanceX < 0 || distanceY < 0) {

            // stop animation, otherwise it gets out of screen while animation is in progress
            clockLayout.animate().cancel();

            if (distanceX < 0) {
                // move clock to left or right screen edge
                final float correctionX = (newClockWidth - containerWidth) * 0.5f * (clockLayout.getTranslationX() < 0 ? 1f : -1f);
                clockLayout.setTranslationX(correctionX);
            }

            if (distanceY < 0) {
                // move clock to top or bottom screen edge
                final float correctionY = (newClockHeight - containerHeight) * 0.5f * (clockLayout.getTranslationY() < 0 ? 1f : -1f);
                clockLayout.setTranslationY(correctionY);
            }
        }
    }
}
