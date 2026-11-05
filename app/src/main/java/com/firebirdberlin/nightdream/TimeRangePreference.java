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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;

import java.util.Calendar;
import java.util.Date;

public class TimeRangePreference extends Preference {

    private static final String TIMERANGE = "timerange";

    static class SimpleTime {
        public int hour = 0;
        public int min = 0;

        SimpleTime(long millis) {
            if (millis < 0L) return;
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(millis);
            hour = calendar.get(Calendar.HOUR_OF_DAY);
            min = calendar.get(Calendar.MINUTE);
        }

        SimpleTime(int minutes) {
            this.hour = minutes / 60;
            this.min = minutes % 60;
        }

        int toMinutes() {
            return this.hour * 60 + this.min;
        }

        long getMillis() {
            return getCalendar().getTimeInMillis();
        }

        public Calendar getCalendar() {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, min);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            Calendar now = Calendar.getInstance();
            if (cal.before(now)) {
                cal.add(Calendar.DATE, 1);
            }

            return cal;
        }
    }

    private final Context mContext;
    private SimpleTime startTime;
    private SimpleTime endTime;
    private String keyStart;
    private String keyEnd;
    private String keyStartInMinutes;
    private String keyEndInMinutes;

    public TimeRangePreference(Context context) {
        super(context, null);
        mContext = context;
    }

    public TimeRangePreference(Context context, AttributeSet attrs) {
        super(context, attrs, android.R.attr.dialogPreferenceStyle);
        mContext = context;
        setValuesFromXml(attrs);
    }

    public TimeRangePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        setValuesFromXml(attrs);
    }

    private void setValuesFromXml(AttributeSet attrs) {
        String key_suffix_start = getAttributeStringValue(attrs, "key_suffix_start", "_start");
        String key_suffix_end = getAttributeStringValue(attrs, "key_suffix_end", "_end");

        keyStart = getKey() + key_suffix_start;
        keyEnd = getKey() + key_suffix_end;

        keyStartInMinutes = getKey() + key_suffix_start + "_minutes";
        keyEndInMinutes = getKey() + key_suffix_end + "_minutes";
        setIconSpaceReserved(false);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View view = holder.itemView;
        if (view instanceof ViewGroup vg) {
            for (int i = 0; i < vg.getChildCount(); i++) {
                View currView = vg.getChildAt(i);
                if (currView instanceof LinearLayout) {
                    currView.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    protected void onAttachedToHierarchy(@NonNull PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        setSummary(getSummary());
        registerResultListener();
    }

    private String getRequestKey() {
        return "TimeRangePickerDialogFragment.result." + getKey();
    }

    private void registerResultListener() {
        if (getContext() instanceof AppCompatActivity activity && getKey() != null) {
            activity.getSupportFragmentManager().setFragmentResultListener(
                    getRequestKey(),
                    activity,
                    (requestKey, result) -> {
                        int hStart = result.getInt(TimeRangePickerDialogFragment.KEY_HOUR_START);
                        int mStart = result.getInt(TimeRangePickerDialogFragment.KEY_MINUTE_START);
                        int hEnd = result.getInt(TimeRangePickerDialogFragment.KEY_HOUR_END);
                        int mEnd = result.getInt(TimeRangePickerDialogFragment.KEY_MINUTE_END);
                        onTimeRangeSet(hStart, mStart, hEnd, mEnd);
                    }
            );
        }
    }

    private static String getAttributeStringValue(AttributeSet attrs, String name,
                                                  String defaultValue) {
        String value = attrs.getAttributeValue(TIMERANGE, name);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    @Override
    public void onClick() {
        showTimeRangePickerDialog();
    }

    private void showTimeRangePickerDialog() {
        if (!(getContext() instanceof AppCompatActivity)) {
            return;
        }
        registerResultListener();
        FragmentManager fm = ((AppCompatActivity) getContext()).getSupportFragmentManager();
        int startH = startTime != null ? startTime.hour : 0;
        int startM = startTime != null ? startTime.min : 0;
        int endH = endTime != null ? endTime.hour : 0;
        int endM = endTime != null ? endTime.min : 0;

        TimeRangePickerDialogFragment dialog =
                TimeRangePickerDialogFragment.newInstance(getRequestKey(), startH, startM, endH, endM);
        dialog.show(fm, "time_range_picker_" + getKey());
    }

    public void onTimeRangeSet(int hourStart, int minuteStart, int hourEnd, int minuteEnd) {
        if (startTime == null) {
            startTime = new SimpleTime(0);
        }
        if (endTime == null) {
            endTime = new SimpleTime(0);
        }
        updateTime(startTime, hourStart, minuteStart);
        updateTime(endTime, hourEnd, minuteEnd);
    }

    private void updateTime(SimpleTime time, int hour, int min) {
        time.hour = hour;
        time.min = min;
        setSummary(getSummary());
        if (callChangeListener(time.getMillis())) {
            SharedPreferences sharedPreferences = getSharedPreferences();
            if (sharedPreferences != null) {
                SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                prefEditor.putInt(keyStartInMinutes, startTime.toMinutes());
                prefEditor.putInt(keyEndInMinutes, endTime.toMinutes());
                prefEditor.apply();
                notifyChanged();
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        SharedPreferences sharedPreferences = getSharedPreferences();
        if (sharedPreferences != null) {
            int startInMinutes = sharedPreferences.getInt(keyStartInMinutes, -1);
            int endInMinutes = sharedPreferences.getInt(keyEndInMinutes, -1);

            if (startInMinutes > -1 && endInMinutes > -1) {
                startTime = new SimpleTime(startInMinutes);
                endTime = new SimpleTime(endInMinutes);
            } else {
                long startInMillis = sharedPreferences.getLong(keyStart, -1L);
                long endInMillis = sharedPreferences.getLong(keyEnd, -1L);

                startTime = new SimpleTime(startInMillis);
                endTime = new SimpleTime(endInMillis);
            }
        }
        setSummary(getSummary());
    }

    @Override
    public CharSequence getSummary() {
        if (startTime == null || endTime == null) {
            return null;
        }

        long start = startTime.getCalendar().getTimeInMillis();
        long end = endTime.getCalendar().getTimeInMillis();

        return DateFormat.getTimeFormat(mContext).format(new Date(start)) + " -- " +
               DateFormat.getTimeFormat(mContext).format(new Date(end));
    }
}
