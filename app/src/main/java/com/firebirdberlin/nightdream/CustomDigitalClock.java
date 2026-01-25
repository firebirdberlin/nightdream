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

package com.firebirdberlin.nightdream;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan; // Add this import
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import com.firebirdberlin.nightdream.ui.AutoAdjustTextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CustomDigitalClock extends AutoAdjustTextView {

    Context context;
    TimeReceiver timeReceiver;
    Calendar mCalendar;
    String mFormat;
    private String m12 = "h:mm aa";
    private String m24 = "HH:mm";
    private String mCustom = null;
    private boolean capitalize = false;
    private SimpleDateFormat simpleDateFormat;
    private FormatChangeObserver mFormatChangeObserver;
    private Handler handler;

    // New color variables
    private int primaryColor = -1;
    private int hourColor = -1;
    private int minuteColor = -1;
    private int secondColor = -1;

    private final Runnable update = this::updateTextView;

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

        // Read new color attributes
        hourColor = a.getColor(R.styleable.CustomDigitalClock_hourColor, -1);
        minuteColor = a.getColor(R.styleable.CustomDigitalClock_minuteColor, -1);
        secondColor = a.getColor(R.styleable.CustomDigitalClock_secondColor, -1);

        a.recycle();

        initClock();
    }

    private void initClock() {
        // Due to an issue with hardware acceleration the text disappears if the font
        // size gets too large. So we disable hardware acceleration.
        // https://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        setFormat();
        setClickable(false);
    }

    /**
     * Splits a time string into segments based on the colon delimiter.
     * For example, "12:34:56" would become ["12", "34", "56"].
     * "12:34 PM" would become ["12", "34 PM"].
     *
     * @param timeString The time string to split.
     * @return An array of strings, where each string is a segment of the time string.
     */
    public String[] splitTimeStringByColon(String timeString) {
        if (timeString == null) {
            return new String[0]; // Return an empty array for null input
        }
        // The split method takes a regular expression. ":" is not a special regex character here.
        return timeString.split(":");
    }

    protected void updateTextView() {
        if (mCalendar == null) {
            mCalendar = Calendar.getInstance();
        }
        mCalendar.setTimeInMillis(System.currentTimeMillis());

        if ("a".equals(mFormat)) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) getLayoutParams();
            if (params != null) {
                int hour = mCalendar.get(Calendar.HOUR_OF_DAY);
                params.gravity = (hour < 12) ? Gravity.BOTTOM : Gravity.TOP;
            }
        }

        String currentTimeString = simpleDateFormat.format(mCalendar.getTime());
        if (currentTimeString.contains(":")) {
            hourColor = (hourColor == -1) ? primaryColor : hourColor;
            minuteColor = (minuteColor == -1) ? primaryColor : minuteColor;
            secondColor = (secondColor == -1) ? primaryColor : secondColor;

            String hourStr = "";
            String minuteStr = "";
            String secondStr = "";

            // Split the current time string into segments
            String[] timeSegments = splitTimeStringByColon(currentTimeString);
            if (timeSegments.length > 0) {
                hourStr = timeSegments[0];
            }
            if (timeSegments.length > 1) {
                minuteStr = timeSegments[1];
            }
            if (timeSegments.length > 2) {
                secondStr = " " + timeSegments[2];
            }
            String divider = ":";
            SpannableString spannableString = new SpannableString(
                    hourStr + divider + minuteStr + secondStr
            );

            // Calculate span start and end for coloring based on the assigned strings
            int hourStart = -1, hourEnd = -1;
            if (!hourStr.isEmpty()) {
                hourStart = 0;
                hourEnd = hourStart + hourStr.length();
            }

            int minuteStart = -1, minuteEnd = -1;
            // Search for minute after the hour segment, or from the beginning if hour wasn't found
            if (!minuteStr.isEmpty()) {
                minuteStart = hourEnd + divider.length();
                minuteEnd = minuteStart + minuteStr.length();
            }
            int secondStart = -1, secondEnd = -1;
            // Only search for seconds if the format includes ":ss" and secondStr is not empty.
            // Search after the minute segment, or hour, or from the beginning.
            if (mFormat.contains(":ss") && !secondStr.isEmpty()) {
                secondStart = minuteEnd;
                secondEnd = secondStart + secondStr.length();
            }
            // Apply colors if they are set and the component is present
            if (hourColor != -1 && hourStart != -1) {
                spannableString.setSpan(new ForegroundColorSpan(hourColor), hourStart, hourEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (minuteColor != -1 && minuteStart != -1) {
                spannableString.setSpan(new ForegroundColorSpan(minuteColor), minuteStart, minuteEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (secondColor != -1 && secondStart != -1) {
                spannableString.setSpan(new ForegroundColorSpan(secondColor), secondStart, secondEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableString.setSpan(new RelativeSizeSpan(0.5f), secondStart, secondEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            if (!spannableString.equals(getText())){
                setText(spannableString);
            }
        } else {
            if (getText() == null || !getText().toString().equals(currentTimeString)){
                setText(currentTimeString);
            }
        }

        // retrigger the update in 1s if seconds are visible
        if (mFormat.contains(":ss")) {
            if (handler == null) {
                handler = new Handler();
            }
            handler.removeCallbacks(update);
            long now = System.currentTimeMillis();
            long delta = 1000 - (now % 1000); // Correct delta calculation for seconds
            handler.postDelayed(update, delta);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setTimeTick();

        mFormatChangeObserver = new FormatChangeObserver();
        getContext().getContentResolver().registerContentObserver(
                Settings.System.CONTENT_URI, true, mFormatChangeObserver);

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
        if (handler != null) {
            handler.removeCallbacks(update);
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
        // Use android.text.format.DateFormat for reliable 24-hour format check
        return DateFormat.is24HourFormat(getContext());
    }

    private void setFormat() {
        if (mCustom != null) {
            mFormat = mCustom;
        } else if (get24HourMode()) {
            mFormat = m24;
        } else {
            mFormat = m12;
        }
        simpleDateFormat = new SimpleDateFormat(mFormat);

        setSampleTime();
        updateTextView();
    }

    public void setSampleTime() {
        String text = getSampleText();
        setSampleText(text);
    }

    public String getSampleText() {
        if (!Utility.containsAny(mFormat, "m", "h", "H")) {
            return null;
        }

        Paint paint = getPaint();
        float width = -1.f;
        float tmp;

        int h_tens_digit = 0;
        for (int i = 0; i < 3; i++) {
            tmp = paint.measureText(String.valueOf(i));
            if (tmp > width) {
                h_tens_digit = i;
                width = tmp;
            }
        }
        int m_tens_digit = h_tens_digit;
        for (int i = 3; i < 7; i++) {
            tmp = paint.measureText(String.valueOf(i));
            if (tmp > width) {
                m_tens_digit = i;
                width = tmp;
            }
        }
        int ones_digit = m_tens_digit;
        for (int i = 7; i < 10; i++) {
            tmp = paint.measureText(String.valueOf(i));
            if (tmp > width) {
                ones_digit = i;
                width = tmp;
            }
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, h_tens_digit * 10 + ones_digit);
        cal.set(Calendar.MINUTE, m_tens_digit * 10 + ones_digit);
        cal.set(Calendar.SECOND, m_tens_digit * 10 + ones_digit);
        String text = simpleDateFormat.format(cal.getTime());
//        Log.i("CustomDigitalClock", "sample text: " + text);
        return text;
    }

    public void setCustomFormat(String format) {
        this.mCustom = format;
        setFormat();
    }

    public void setFormat12Hour(String format) {
        this.m12 = format;
        setFormat();
    }

    public void setFormat24Hour(String format) {
        this.m24 = format;
        setFormat();
    }

    @Override
    public void setTextColor(int color) {
        Log.d("CustomDigitalClock", "setTextColor called with color: " + color);
        this.primaryColor = color;
        super.setTextColor(color);
    }

    // Setters for colors
    public void setHourColor(int color) {
        this.hourColor = color;
        updateTextView();
    }

    public void setMinuteColor(int color) {
        this.minuteColor = color;
        updateTextView();
    }

    public void setSecondColor(int color) {
        this.secondColor = color;
        updateTextView();
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
        if (input.length() > 1) {
            return input.substring(0, 1).toUpperCase() + input.substring(1);
        } else if (!input.isEmpty()) {
            return input.substring(0, 1).toUpperCase();
        }

        return input;
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
}
