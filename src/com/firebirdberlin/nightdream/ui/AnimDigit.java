package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;

import androidx.core.content.res.ResourcesCompat;

import com.firebirdberlin.nightdream.ui.background.ImageViewExtended;

public class AnimDigit extends ImageViewExtended {
    private static final String TAG = "AnimDigit";
    private Paint mNumberPaint = new Paint();
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
            Resources res = getResources();
            int resourceId = res.getIdentifier(
                    "vd_pathmorph_digits_" + index, "drawable",
                    getContext().getPackageName()
            );
            Drawable drawable = ResourcesCompat.getDrawable(
                    getResources(), resourceId, getContext().getTheme()
            );
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
        Resources res = getResources();
        int resourceId = res.getIdentifier(
                "avd_pathmorph_digits_" + number + "_to_" + index, "drawable",
                getContext().getPackageName()
        );
        if (resourceId != 0) {
            Drawable drawable = ResourcesCompat.getDrawable(
                    getResources(), resourceId,
                    getContext().getTheme()
            );
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
