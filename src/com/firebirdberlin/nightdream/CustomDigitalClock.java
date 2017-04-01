package com.firebirdberlin.nightdream;

import java.util.Calendar;
import java.text.SimpleDateFormat;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.TextView;

import com.firebirdberlin.nightdream.ui.AutoAdjustTextView;

public class CustomDigitalClock extends AutoAdjustTextView {

    Context context = null;
    TimeReceiver timeReceiver;
    Calendar mCalendar;
    String mFormat;
    private String m12 = "h:mm aa";
    private String m24 = "HH:mm";
    private FormatChangeObserver mFormatChangeObserver;


    public CustomDigitalClock(Context context) {
        super(context);
        this.context = context;
        initClock(context);
    }

    public CustomDigitalClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomDigitalClock);

        m12 = a.getString(R.styleable.CustomDigitalClock_format12Hr);
        m24 = a.getString(R.styleable.CustomDigitalClock_format24Hr);

        a.recycle();

        initClock(context);
    }

    private void initClock(Context context) {
        Resources r = context.getResources();

        mFormatChangeObserver = new FormatChangeObserver();
        getContext().getContentResolver().registerContentObserver(
                Settings.System.CONTENT_URI, true, mFormatChangeObserver);

        setFormat();
        updateTextView();
    }

    protected void updateTextView() {
        if (mCalendar == null) {
            mCalendar = Calendar.getInstance();
        }
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat(mFormat);
        String text = sdf.format(mCalendar.getTime());
        if (text != getText() ) {
            setText(text);
            invalidate();
        }
    }

    @Override
    public void onAttachedToWindow(){
        setTimeTick();
    }

    @Override
    public void onDetachedFromWindow(){
        if (timeReceiver != null) {
            context.unregisterReceiver(timeReceiver);
            timeReceiver = null;
        }
    }

    void setTimeTick() {
        timeReceiver = new TimeReceiver();
        context.registerReceiver(timeReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    class TimeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent arg1) {
            updateTextView();
        }
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

}
