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


import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Color;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.xenione.digit.TabDigit;

import java.util.Calendar;

public class CustomDigitalFlipClock extends LinearLayout {

    private static final String TAG = "CustomDigitalFlipClock";
    private static final char[] HOURS = new char[]{'0', '1', '2'};
    private static final char[] HOURS_BLANK = new char[]{' ', '1', '2'};
    private static final char[] HOURS12 = new char[]{'0', '1'};
    private static final char[] HOURS12_BLANK = new char[]{' ', '1'};
    private static final char[] LOWHOURS24 = new char[]{
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3'};
    private static final char[] LOWHOURS12 = new char[]{'2', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1'};
    private static final char[] SEXAGISIMAL = new char[]{'0', '1', '2', '3', '4', '5'};
    private final Context context;
    TimeReceiver timeReceiver;
    private FormatChangeObserver mFormatChangeObserver;
    private Boolean customIs24Hour = null;
    private String customFormat = null;
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

        mCharHighMinute.setDividerColor(Color.BLACK);
        mCharLowMinute.setDividerColor(Color.BLACK);
        mCharHighHour.setDividerColor(Color.BLACK);
        mCharLowHour.setDividerColor(Color.BLACK);

        mCharHighMinute.setDividerThickness(3);
        mCharLowMinute.setDividerThickness(3);
        mCharHighHour.setDividerThickness(3);
        mCharLowHour.setDividerThickness(3);
    }

    private void init() {
        mCharHighMinute.setChars(SEXAGISIMAL);
        if (get24HourMode()) {
            if (customFormat != null && customFormat.startsWith("H:") ) {
                mCharHighHour.setChars(HOURS_BLANK);
            } else {
                mCharHighHour.setChars(HOURS);
            }
        } else {
            if (customFormat != null && customFormat.startsWith("h:") ) {
                mCharHighHour.setChars(HOURS12_BLANK);
            } else {
                mCharHighHour.setChars(HOURS12);
            }
        }
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
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setTimeTick();
        mFormatChangeObserver = new FormatChangeObserver();
        getContext().getContentResolver().registerContentObserver(
                Settings.System.CONTENT_URI, true, mFormatChangeObserver);
        resume();
    }

    @Override
    public void onDetachedFromWindow() {
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

    public void setCustomFormat(String format) {
        this.customFormat = format;
        this.customIs24Hour = format.startsWith("H");
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
            colon.setText(":");
            colon.setTextColor(color);
        }
        invalidate();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        mCharLowMinute.sync();
        mCharHighMinute.sync();
        mCharLowHour.sync();
        mCharHighHour.sync();
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
