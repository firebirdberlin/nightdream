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
    private int maxFontSizePx = -1;
    private int minFontSizePx = -1;
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

        //m12 = a.getString(R.styleable.CustomDigitalClock_format12Hr);
        //m24 = a.getString(R.styleable.CustomDigitalClock_format24Hr);

        a.recycle();

        init(context);
    }

    private void init(Context context) {
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

    public void setMaxFontSizesInPx(float minSize, float maxSize) {
        this.minFontSizePx = (int) minSize;
        this.maxFontSizePx = (int) maxSize;
    }

    private void adjustTextSize() {
        if ( maxWidth == -1) return;
        if ( maxFontSizePx == -1 || minFontSizePx == -1) return;
        for(int size = minFontSizePx; size <= maxFontSizePx; size++) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
            if ( measureText() > maxWidth ) {
                setTextSize(TypedValue.COMPLEX_UNIT_PX, size - 1);
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
