package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;


public class AutoAdjustTextView extends TextView {
    private static final String TAG = "AutoAdjustTextView";
    Context context = null;
    private int maxWidth = -1;
    private int maxHeight = -1;

    private int maxFontSizeSp = -1;
    private int minFontSizeSp = -1;

    private String fontPath = null;
    private String sampleText = null;


    public AutoAdjustTextView(Context context) {
        super(context);
        this.context = context;
        init(context);
    }

    public AutoAdjustTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AutoAdjustTextView);

        fontPath = a.getString(R.styleable.AutoAdjustTextView_fontPath);
        minFontSizeSp = a.getInt(R.styleable.AutoAdjustTextView_minFontSizeSp, 8);
        maxFontSizeSp = a.getInt(R.styleable.AutoAdjustTextView_maxFontSizeSp, 30);

        a.recycle();

        init(context);
    }

    private void init(Context context) {
        if (fontPath != null) {
            Typeface typeface = Typeface.createFromAsset(context.getAssets(), fontPath);
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
            adjustTextSize();
        } catch (NullPointerException e) {
            Log.e(TAG, "Could not adjust the text size");
            e.printStackTrace();
        }
        super.invalidate();
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

    private void adjustTextSize() {
        if (Build.VERSION.SDK_INT < 14) return;
        if ( maxWidth == -1) return;
        if ( maxFontSizeSp == -1 || minFontSizeSp == -1) return;
        Paint paint = getPaint();
        for(int size = minFontSizeSp; size <= maxFontSizeSp; size++) {
            paint.setTextSize(Utility.spToPx(context, size));
            if (measureText(paint) > maxWidth) {
                paint.setTextSize(Utility.spToPx(context, size - 1));
                break;
            } else if (maxHeight > -1 && measureTextHeight(paint) > maxHeight) {
                paint.setTextSize(Utility.spToPx(context, size - 1));
                break;
            }
        }
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
        Rect bounds = new Rect(); // TODO make member field
        String text = (sampleText != null ) ? sampleText : getText().toString();
        paint.getTextBounds(text, 0, text.length(), bounds);
        return bounds.height();
    }
}
