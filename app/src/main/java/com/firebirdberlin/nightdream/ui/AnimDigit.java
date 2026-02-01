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

package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;

import androidx.core.content.res.ResourcesCompat;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.ui.background.ImageViewExtended;

public class AnimDigit extends ImageViewExtended {
    private static final String TAG = "AnimDigit";
    private final Paint mNumberPaint = new Paint();
    private int number = -1;

    public AnimDigit(Context context) {
        this(context, null);
    }

    public AnimDigit(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnimDigit(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mNumberPaint.setAntiAlias(true);
        mNumberPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mNumberPaint.setColor(Color.BLUE);
    }

    public void setChar(int index) {
        Log.d(TAG, "setChar: " + index);

        this.number = index;

        if (index >= 0 && index <= 9) {
            int resourceId = -1;
            switch (index) {
                case 0:
                    resourceId = R.drawable.vd_pathmorph_digits_0;
                    break;
                case 1:
                    resourceId = R.drawable.vd_pathmorph_digits_1;
                    break;
                case 2:
                    resourceId = R.drawable.vd_pathmorph_digits_2;
                    break;
                case 3:
                    resourceId = R.drawable.vd_pathmorph_digits_3;
                    break;
                case 4:
                    resourceId = R.drawable.vd_pathmorph_digits_4;
                    break;
                case 5:
                    resourceId = R.drawable.vd_pathmorph_digits_5;
                    break;
                case 6:
                    resourceId = R.drawable.vd_pathmorph_digits_6;
                    break;
                case 7:
                    resourceId = R.drawable.vd_pathmorph_digits_7;
                    break;
                case 8:
                    resourceId = R.drawable.vd_pathmorph_digits_8;
                    break;
                case 9:
                    resourceId = R.drawable.vd_pathmorph_digits_9;
                    break;
                default:
                    Log.e(TAG, "Invalid index: " + index);
                    return;
            }

            Drawable drawable = ResourcesCompat.getDrawable(getResources(), resourceId, getContext().getTheme());
            if (drawable != null) {
                drawable.setColorFilter(mNumberPaint.getColor(), PorterDuff.Mode.SRC_ATOP);
                setImageDrawable(drawable);
            }
        }
        invalidate();
    }

    public void setTextColor(int color) {
        mNumberPaint.setColor(color);
        setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        invalidate();
    }

    public void start(int index) {
        int resourceId = -1;
        
        if (number == index) {
            // No transition needed if the numbers are the same
            setChar(index);
            return;
        }

        switch (number) {
            case 0:
                if (index == 1) resourceId = R.drawable.avd_pathmorph_digits_0_to_1;
                else Log.e(TAG, "Invalid transition from " + number + " to " + index);
                break;
            case 1:
                if (index == 2) resourceId = R.drawable.avd_pathmorph_digits_1_to_2;
                else if (index == 0) resourceId = R.drawable.avd_pathmorph_digits_1_to_0;
                else Log.e(TAG, "Invalid transition from " + number + " to " + index);
                break;
            case 2:
                if (index == 3) resourceId = R.drawable.avd_pathmorph_digits_2_to_3;
                else if (index == 0) resourceId = R.drawable.avd_pathmorph_digits_2_to_0;
                else Log.e(TAG, "Invalid transition from " + number + " to " + index);
                break;
            case 3:
                if (index == 4) resourceId = R.drawable.avd_pathmorph_digits_3_to_4;
                else if (index == 0) resourceId = R.drawable.avd_pathmorph_digits_3_to_0;
                else Log.e(TAG, "Invalid transition from " + number + " to " + index);
                break;
            case 4:
                if (index == 5) resourceId = R.drawable.avd_pathmorph_digits_4_to_5;
                else Log.e(TAG, "Invalid transition from " + number + " to " + index);
                break;
            case 5:
                if (index == 6) resourceId = R.drawable.avd_pathmorph_digits_5_to_6;
                else if (index == 0) resourceId = R.drawable.avd_pathmorph_digits_5_to_0;
                else Log.e(TAG, "Invalid transition from " + number + " to " + index);
                break;
            case 6:
                if (index == 7) resourceId = R.drawable.avd_pathmorph_digits_6_to_7;
                else Log.e(TAG, "Invalid transition from " + number + " to " + index);
                break;
            case 7:
                if (index == 8) resourceId = R.drawable.avd_pathmorph_digits_7_to_8;
                else Log.e(TAG, "Invalid transition from " + number + " to " + index);
                break;
            case 8:
                if (index == 9) resourceId = R.drawable.avd_pathmorph_digits_8_to_9;
                else Log.e(TAG, "Invalid transition from " + number + " to " + index);
                break;
            case 9:
                if (index == 0) resourceId = R.drawable.avd_pathmorph_digits_9_to_0;
                else Log.e(TAG, "Invalid transition from " + number + " to " + index);
                break;
            default:
                Log.e(TAG, "Invalid current number: " + number);
                return;
        }

        if (resourceId != -1) {
            Drawable drawable = ResourcesCompat.getDrawable(getResources(), resourceId, getContext().getTheme());
            if (drawable != null) {
                drawable.setColorFilter(mNumberPaint.getColor(), PorterDuff.Mode.SRC_ATOP);
                setImageDrawable(drawable);
            }
            this.number = index;
            startDrawableAnimation();
        } else {
            setChar(index);
        }
        invalidate();
    }

}
