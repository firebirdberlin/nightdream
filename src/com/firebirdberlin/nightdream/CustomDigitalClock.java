package com.firebirdberlin.nightdream;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;

import com.firebirdberlin.nightdream.ui.AutoAdjustTextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CustomDigitalClock extends AutoAdjustTextView {

    Context context = null;
    TimeReceiver timeReceiver;
    Calendar mCalendar;
    String mFormat;
    private String m12 = "h:mm aa";
    private String m24 = "HH:mm";
    private boolean capitalize = false;
    private SimpleDateFormat simpleDateFormat;
    private FormatChangeObserver mFormatChangeObserver;


    public CustomDigitalClock(Context context) {
        super(context);
        this.context = context;
        initClock();
    }

    public CustomDigitalClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomDigitalClock);

        m12 = a.getString(R.styleable.CustomDigitalClock_format12Hr);
        m24 = a.getString(R.styleable.CustomDigitalClock_format24Hr);
        capitalize = a.getBoolean(R.styleable.CustomDigitalClock_capitalize, false);

        a.recycle();

        initClock();
    }

    private void initClock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Due to an issue with hardware acceleration the text disappears if the font
            // size gets too large. So we disable hardware acceleration.
            // https://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        setFormat();
        updateTextView();
    }

    protected void updateTextView() {
        if (mCalendar == null) {
            mCalendar = Calendar.getInstance();
        }
        mCalendar.setTimeInMillis(System.currentTimeMillis());

        String text = simpleDateFormat.format(mCalendar.getTime());
        if (text != getText() ) {
            setText(text);
            invalidate();
        }
    }

    @Override
    public void onAttachedToWindow(){
        super.onAttachedToWindow();
        setTimeTick();

        mFormatChangeObserver = new FormatChangeObserver();
        getContext().getContentResolver().registerContentObserver(
                Settings.System.CONTENT_URI, true, mFormatChangeObserver);

    }

    @Override
    public void onDetachedFromWindow(){
        super.onDetachedFromWindow();
        if (timeReceiver != null) {
            try {
                context.unregisterReceiver(timeReceiver);
            } catch (IllegalArgumentException e) {
                // receiver was not registered,
            }
            timeReceiver = null;
        }
        if (mFormatChangeObserver != null) {
            ContentResolver cr = getContext().getContentResolver();
            cr.unregisterContentObserver(mFormatChangeObserver);
        }
    }

    void setTimeTick() {
        timeReceiver = new TimeReceiver();
        context.registerReceiver(timeReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    /**
     * Pulls 12/24 mode from system settings
     */
    private boolean get24HourMode() {
        return android.text.format.DateFormat.is24HourFormat(getContext());
    }

    private void setFormat() {
        if (get24HourMode()) {
            mFormat = m24;
        } else {
            mFormat = m12;
        }
        simpleDateFormat = new SimpleDateFormat(mFormat);
    }

    public void setFormat12Hour(String format) {
        this.m12 = format;
        setFormat();
        updateTextView();
    }

    public void setFormat24Hour(String format) {
        this.m24 = format;
        setFormat();
        updateTextView();
    }

    class TimeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent arg1) {
            updateTextView();
        }
    }

    private class FormatChangeObserver extends ContentObserver {
        public FormatChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            setFormat();
        }
    }

    @Override
    public void invalidate() {
        if (capitalize) {
            String text = capitalize(getText().toString());
            setText(text);
        }
        super.invalidate();
    }

    private String capitalize(String input) {
        if (input == null) {
            return null;
        }
        if (input.length() > 1 ) {
            return input.substring(0, 1).toUpperCase() + input.substring(1);
        } else
        if (input.length() > 0 ) {
            return input.substring(0, 1).toUpperCase();
        }

        return input;
    }
}
