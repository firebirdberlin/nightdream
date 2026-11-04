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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.text.DateFormatSymbols;
import java.util.Locale;

public class TimeRangePickerDialogFragment extends DialogFragment {
    private static final String TAG = TimeRangePickerDialogFragment.class.getSimpleName();

    public static final String REQUEST_KEY = "TimeRangePickerDialogFragment.result";
    public static final String KEY_REQUEST_KEY = "requestKey";
    public static final String KEY_HOUR_START = "hourStart";
    public static final String KEY_MINUTE_START = "minuteStart";
    public static final String KEY_HOUR_END = "hourEnd";
    public static final String KEY_MINUTE_END = "minuteEnd";

    private NumberPicker numberPickerHourStart, numberPickerHourEnd;
    private NumberPicker numberPickerMinuteStart, numberPickerMinuteEnd;
    private ToggleButton toggleAmPmStart, toggleAmPmEnd;

    private String requestKey = REQUEST_KEY;
    private String amString = "am";
    private String pmString = "pm";
    private int maxHourValue = 23;
    private int minHourValue = 0;
    private int hourStart, hourEnd = 0;
    private int minuteStart, minuteEnd = 0;

    public TimeRangePickerDialogFragment() {
        // Public no-arg constructor required for fragment recreation
    }

    public static TimeRangePickerDialogFragment newInstance(int hourStart, int minuteStart,
                                                           int hourEnd, int minuteEnd) {
        return newInstance(REQUEST_KEY, hourStart, minuteStart, hourEnd, minuteEnd);
    }

    public static TimeRangePickerDialogFragment newInstance(String requestKey, int hourStart,
                                                           int minuteStart, int hourEnd,
                                                           int minuteEnd) {
        TimeRangePickerDialogFragment fragment = new TimeRangePickerDialogFragment();
        Bundle args = new Bundle();
        args.putString(KEY_REQUEST_KEY, requestKey);
        args.putInt(KEY_HOUR_START, hourStart);
        args.putInt(KEY_MINUTE_START, minuteStart);
        args.putInt(KEY_HOUR_END, hourEnd);
        args.putInt(KEY_MINUTE_END, minuteEnd);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            requestKey = savedInstanceState.getString(KEY_REQUEST_KEY, REQUEST_KEY);
            hourStart = savedInstanceState.getInt(KEY_HOUR_START, 0);
            minuteStart = savedInstanceState.getInt(KEY_MINUTE_START, 0);
            hourEnd = savedInstanceState.getInt(KEY_HOUR_END, 0);
            minuteEnd = savedInstanceState.getInt(KEY_MINUTE_END, 0);
        } else if (getArguments() != null) {
            requestKey = getArguments().getString(KEY_REQUEST_KEY, REQUEST_KEY);
            hourStart = getArguments().getInt(KEY_HOUR_START, 0);
            minuteStart = getArguments().getInt(KEY_MINUTE_START, 0);
            hourEnd = getArguments().getInt(KEY_HOUR_END, 0);
            minuteEnd = getArguments().getInt(KEY_MINUTE_END, 0);
        }

        getAmPmStrings();

        Context context = requireContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.time_range_dialog, null);

        numberPickerHourStart = v.findViewById(R.id.numberPickerHourStart);
        numberPickerHourEnd = v.findViewById(R.id.numberPickerHourEnd);
        numberPickerMinuteStart = v.findViewById(R.id.numberPickerMinuteStart);
        numberPickerMinuteEnd = v.findViewById(R.id.numberPickerMinuteEnd);

        toggleAmPmStart = v.findViewById(R.id.toggleAmPmStart);
        toggleAmPmEnd = v.findViewById(R.id.toggleAmPmEnd);

        toggleAmPmStart.setTextOff(amString);
        toggleAmPmStart.setTextOn(pmString);
        toggleAmPmEnd.setTextOff(amString);
        toggleAmPmEnd.setTextOn(pmString);

        boolean is24Hour = is24HourFormat(getContext());
        maxHourValue = (is24Hour) ? 23 : 12;
        minHourValue = (is24Hour) ? 0 : 1;
        toggleAmPmStart.setChecked(hourStart > 11);
        toggleAmPmEnd.setChecked(hourEnd > 11);
        toggleAmPmStart.setVisibility(is24Hour ? View.GONE : View.VISIBLE);
        toggleAmPmEnd.setVisibility(is24Hour ? View.GONE : View.VISIBLE);

        NumberPicker.Formatter twoDigitFormatter = value ->
            String.format(Locale.getDefault(), "%02d", value);

        numberPickerHourStart.setMinValue(minHourValue);
        numberPickerHourStart.setMaxValue(maxHourValue);
        numberPickerHourEnd.setMinValue(minHourValue);
        numberPickerHourEnd.setMaxValue(maxHourValue);

        numberPickerMinuteStart.setMinValue(0);
        numberPickerMinuteStart.setMaxValue(59);
        numberPickerMinuteStart.setFormatter(twoDigitFormatter);

        numberPickerMinuteEnd.setMinValue(0);
        numberPickerMinuteEnd.setMaxValue(59);
        numberPickerMinuteEnd.setFormatter(twoDigitFormatter);

        if (is24Hour) {
            numberPickerHourStart.setFormatter(twoDigitFormatter);
            numberPickerHourEnd.setFormatter(twoDigitFormatter);
            numberPickerHourStart.setValue(hourStart);
            numberPickerHourEnd.setValue(hourEnd);
        } else {
            numberPickerHourStart.setValue(convertTo12Hour(hourStart));
            numberPickerHourEnd.setValue(convertTo12Hour(hourEnd));
        }
        numberPickerMinuteStart.setValue(minuteStart);
        numberPickerMinuteEnd.setValue(minuteEnd);

        builder.setView(v)
               .setPositiveButton(android.R.string.ok, positiveButtonClickListener)
               .setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        saveCurrentValuesFromUi();
        outState.putString(KEY_REQUEST_KEY, requestKey);
        outState.putInt(KEY_HOUR_START, hourStart);
        outState.putInt(KEY_MINUTE_START, minuteStart);
        outState.putInt(KEY_HOUR_END, hourEnd);
        outState.putInt(KEY_MINUTE_END, minuteEnd);
    }

    private void saveCurrentValuesFromUi() {
        if (numberPickerHourStart == null) {
            return;
        }
        int rawHStart = numberPickerHourStart.getValue();
        int rawHEnd = numberPickerHourEnd.getValue();
        minuteStart = numberPickerMinuteStart.getValue();
        minuteEnd = numberPickerMinuteEnd.getValue();

        boolean is24Hour = is24HourFormat(getContext());
        if (!is24Hour) {
            hourStart = convertTo24Hour(rawHStart, toggleAmPmStart.isChecked());
            hourEnd = convertTo24Hour(rawHEnd, toggleAmPmEnd.isChecked());
        } else {
            hourStart = rawHStart;
            hourEnd = rawHEnd;
        }
    }

    public void getAmPmStrings() {
        Locale locale = Locale.getDefault();
        DateFormatSymbols symbols = new DateFormatSymbols(locale);
        String[] amPmStrings = symbols.getAmPmStrings();
        if (amPmStrings.length > 1) {
            amString = amPmStrings[0];
            pmString = amPmStrings[1];
        }
    }

    private int convertTo12Hour(int hour) {
        if (hour == 0) {
            return 12;
        } else if (hour > 12) {
            return hour - 12;
        }
        return hour;
    }

    private int convertTo24Hour(int hour, boolean isPm) {
        if (!isPm) {
            if (hour == 12) {
                return 0;
            } else {
                return hour;
            }
        } else {
            if (hour == 12) {
                return 12;
            } else {
                return hour + 12;
            }
        }
    }

    private boolean is24HourFormat(Context context) {
        return android.text.format.DateFormat.is24HourFormat(context);
    }

    private final DialogInterface.OnClickListener positiveButtonClickListener =
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                saveCurrentValuesFromUi();

                Bundle result = new Bundle();
                result.putInt(KEY_HOUR_START, hourStart);
                result.putInt(KEY_MINUTE_START, minuteStart);
                result.putInt(KEY_HOUR_END, hourEnd);
                result.putInt(KEY_MINUTE_END, minuteEnd);
                getParentFragmentManager().setFragmentResult(requestKey, result);
            }
        };
}
