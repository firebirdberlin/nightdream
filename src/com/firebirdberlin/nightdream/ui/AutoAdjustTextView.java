package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.FontCache;


public class AutoAdjustTextView extends TextView {
    private static final String TAG = "AutoAdjustTextView";
    private int maxWidth = -1;
    private int maxHeight = -1;

    private int maxFontSizeSp = -1;
    private int minFontSizeSp = -1;

    private String fontPath = null;
    private String sampleText = null;
    private Rect bounds = null;

    public AutoAdjustTextView(Context context) {
        super(context);
        init();
    }

    public AutoAdjustTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AutoAdjustTextView);

        fontPath = a.getString(R.styleable.AutoAdjustTextView_fontPath);
        minFontSizeSp = a.getInt(R.styleable.AutoAdjustTextView_minFontSizeSp, 8);
        maxFontSizeSp = a.getInt(R.styleable.AutoAdjustTextView_maxFontSizeSp, 30);

        a.recycle();

        init();
    }

    private void init() {
        if (fontPath != null) {
            Typeface typeface = FontCache.get(getContext(), fontPath);
            setTypeface(typeface);
        }
    }

    @Override
    public void onAttachedToWindow(){
        super.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow(){
        super.onDetachedFromWindow();
    }

    @Override
    public void invalidate() {
        try {
            int size = getAdjustedTextSize();
            if (size > 0) {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, size - 1);
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Could not adjust the text size");
            e.printStackTrace();
        }
        super.invalidate();
    }

    private int getAdjustedTextSize() {
        if (Build.VERSION.SDK_INT < 14) return -1;
        if (maxWidth == -1) return -1;
        if (maxFontSizeSp == -1 || minFontSizeSp == -1) return -1;
        Paint paint = getPaint();
        for (int size = minFontSizeSp; size <= maxFontSizeSp; size++) {
            paint.setTextSize(Utility.spToPx(getContext(), size));
            if (measureText(paint) > maxWidth ||
                    (maxHeight > -1 && measureTextHeight(paint) > maxHeight)) {
                return size;
            }
        }
        return maxFontSizeSp;
    }

    public void setMaxWidth(int width) {
        this.maxWidth = width;
    }

    public void setMaxHeight(int height) {
        this.maxHeight = height;
    }

    public void setMaxFontSizesInSp(float minSize, float maxSize) {
        this.minFontSizeSp = (int) minSize;
        this.maxFontSizeSp = (int) maxSize;
    }

    // this text may be used as a sample to determine the font size
    public void setSampleText(String sample) {
        this.sampleText = sample;
    }

    private float measureText(Paint paint) {
        String text = (sampleText != null ) ? sampleText : getText().toString();
        return paint.measureText(text);
    }

    private float measureTextHeight(Paint paint) {
        if (bounds == null) {
            bounds = new Rect();
        }
        String text = (sampleText != null ) ? sampleText : getText().toString();
        paint.getTextBounds(text, 0, text.length(), bounds);
        return bounds.height();
    }
}
