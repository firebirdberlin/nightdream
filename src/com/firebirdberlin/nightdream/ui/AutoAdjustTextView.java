package com.firebirdberlin.nightdream.ui;

import java.util.Calendar;
import java.text.SimpleDateFormat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;
import com.firebirdberlin.nightdream.R;


public class AutoAdjustTextView extends TextView {
    private static final String TAG = "NightDream.AutoAdjustTextView";
    Context context = null;
    private int maxWidth = -1;

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
        adjustTextSize();
        super.invalidate();
    }

    public void setMaxWidth(int width) {
        this.maxWidth = width;
    }

    public void setMaxFontSizesInSp(float minSize, float maxSize) {
        this.minFontSizeSp = (int) minSize;
        this.maxFontSizeSp = (int) maxSize;
    }

    private void adjustTextSize() {
        if (Build.VERSION.SDK_INT < 14) return;
        if ( maxWidth == -1) return;
        if ( maxFontSizeSp == -1 || minFontSizeSp == -1) return;
        for(int size = minFontSizeSp; size <= maxFontSizeSp; size++) {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
            if ( measureText() > maxWidth ) {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, size - 1);
                break;
            }
        }
    }

    // this text may be used as a sample to determine the font size
    public void setSampleText(String sample) {
        this.sampleText = sample;
    }

    private float measureText() {
        String text = (sampleText != null ) ? sampleText : getText().toString();
        return getPaint().measureText(text);
    }
}
