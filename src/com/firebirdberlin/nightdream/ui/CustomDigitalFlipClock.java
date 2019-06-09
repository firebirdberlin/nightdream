package com.firebirdberlin.nightdream.ui;


import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.xenione.digit.TabDigit;

import java.util.Calendar;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class CustomDigitalFlipClock extends LinearLayout {

    private static final String TAG = "CustomDigitalFlipClock";
    private static final char[] HOURS = new char[]{'0', '1', '2'};
    private static final char[] HOURS12 = new char[]{'0', '1'};
    private static final char[] LOWHOURS24 = new char[]{
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3'};
    private static final char[] LOWHOURS12 = new char[]{'2', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1'};
    private static final char[] SEXAGISIMAL = new char[]{'0', '1', '2', '3', '4', '5'};

    TimeReceiver timeReceiver;
    private Context context;
    private FormatChangeObserver mFormatChangeObserver;
    private Boolean customIs24Hour = null;
    private TabDigit mCharHighMinute;
    private TabDigit mCharLowMinute;
    private TabDigit mCharHighHour;
    private TabDigit mCharLowHour;
    private int currentHourHigh = -1;
    private int currentHourLow = -1;
    private int currentMinuteHigh = -1;
    private int currentMinuteLow = -1;

    public CustomDigitalFlipClock(Context context) {
        super(context);
        this.context = context;
        initLayout();
        init();
    }

    public CustomDigitalFlipClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initLayout();
        init();
    }


    private void initLayout() {
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View child = inflater.inflate(R.layout.custom_digital_flip_clock, null);

        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(child, lp);
        mCharHighMinute = findViewById(R.id.charHighMinute);
        mCharLowMinute = findViewById(R.id.charLowMinute);
        mCharHighHour = findViewById(R.id.charHighHour);
        mCharLowHour = findViewById(R.id.charLowHour);

    }

    private void init() {
        mCharHighMinute.setChars(SEXAGISIMAL);
        mCharHighHour.setChars(get24HourMode() ? HOURS : HOURS12);
        mCharLowHour.setChars(get24HourMode() ? LOWHOURS24 : LOWHOURS12);
        resume();
    }

    private void resume() {
        Calendar time = Calendar.getInstance();
        int hour = time.get(get24HourMode() ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        int highHour = hour / 10;
        int lowHour = hour;
        if (!get24HourMode()) {
            highHour = (hour == 0) ? 1 : highHour;
        }
        int minutes = time.get(Calendar.MINUTE);
        int highMinute = minutes / 10;
        int lowMinute = (minutes - highMinute * 10);

        mCharHighHour.setChar(highHour);
        mCharLowHour.setChar(lowHour);
        mCharHighMinute.setChar(highMinute);
        mCharLowMinute.setChar(lowMinute);
        currentHourHigh = highHour;
        currentHourLow = lowHour;
        currentMinuteHigh = highMinute;
        currentMinuteLow = lowMinute;
    }

    @Override
    public void onAttachedToWindow(){
        super.onAttachedToWindow();
        setTimeTick();
        mFormatChangeObserver = new FormatChangeObserver();
        getContext().getContentResolver().registerContentObserver(
                Settings.System.CONTENT_URI, true, mFormatChangeObserver);
        resume();
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
        if (customIs24Hour != null) {
            return customIs24Hour;
        }
        return android.text.format.DateFormat.is24HourFormat(getContext());
    }

    public void setCustomIs24Hour(boolean is24Hour) {
        Log.i(TAG, "Settings custom 24 hour format: " + is24Hour);
        this.customIs24Hour = is24Hour;
        init();
        invalidate();
    }

    public void setPrimaryColor(int color) {
        mCharHighHour.setTextColor(color);
        mCharLowHour.setTextColor(color);
        mCharHighMinute.setTextColor(color);
        mCharLowMinute.setTextColor(color);
        invalidate();
    }

    public void setSecondaryColor(int color) {
        TextView colon = findViewById(R.id.colon);
        if (colon != null) {
            colon.setTextColor(color);
        }
        invalidate();
    }

    class TimeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent arg1) {
            Calendar time = Calendar.getInstance();
            int hour = time.get(get24HourMode() ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
            int highHour = hour / 10;
            int lowHour = hour;
            if (!get24HourMode()) {
                highHour = (hour == 0) ? 1 : highHour;
            }
            int minutes = time.get(Calendar.MINUTE);
            int highMinute = minutes / 10;
            int lowMinute = (minutes - highMinute * 10);

            if (currentHourHigh != highHour) {
                mCharHighHour.start();
            }
            if (currentHourLow != lowHour) {
                mCharLowHour.start();
            }
            if (currentMinuteHigh != highMinute) {
                mCharHighMinute.start();
            }
            if (currentMinuteLow != lowMinute) {
                mCharLowMinute.start();
            }
            currentHourHigh = highHour;
            currentHourLow = lowHour;
            currentMinuteHigh = highMinute;
            currentMinuteLow = lowMinute;
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        mCharLowMinute.sync();
        mCharHighMinute.sync();
        mCharLowHour.sync();
        mCharHighHour.sync();
    }

    private class FormatChangeObserver extends ContentObserver {
        public FormatChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            init();
        }
    }
}
