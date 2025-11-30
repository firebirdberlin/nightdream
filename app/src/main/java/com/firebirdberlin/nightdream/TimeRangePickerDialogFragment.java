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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.text.DateFormatSymbols;
import java.util.Locale;

public class TimeRangePickerDialogFragment extends DialogFragment {
    private static String TAG = "TimeRangePickerDialogFragment";

    private EditText editTextHourStart, editTextHourEnd;
    private EditText editTextMinuteStart, editTextMinuteEnd;
    private ToggleButton toggleAmPmStart, toggleAmPmEnd;
    private Result delegate;

    private String amString = "am";
    private String pmString = "pm";
    private int maxHourValue = 23;
    private int minHourValue = 0;
    private int hourStart, hourEnd = 0;
    private int minuteStart, minuteEnd = 0;

    private TextWatcher textWatcherHour = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            try {
                int number = Integer.parseInt(s.toString());
                setOkButtonEnabled(number >= minHourValue && number <= maxHourValue);
            } catch (NumberFormatException e) {
                setOkButtonEnabled(false);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };
    private TextWatcher textWatcherMinute = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            try {
                int number = Integer.parseInt(s.toString());
                setOkButtonEnabled(number >= 0 && number < 60);
            } catch (NumberFormatException e) {
                setOkButtonEnabled(false);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };

    TimeRangePickerDialogFragment(Result listener) {
        this.delegate = listener;
    }

    public interface Result {
        void onTimeRangeSet(int hourStart, int minuteStart, int hourEnd, int minuteEnd);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setRetainInstance(true);
        getAmPmStrings();

        Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        View v = inflater.inflate(R.layout.time_range_dialog, null);

        editTextHourStart = v.findViewById(R.id.editTextHourStart);
        editTextHourEnd = v.findViewById(R.id.editTextHourEnd);
        editTextMinuteStart = v.findViewById(R.id.editTextMinuteStart);
        editTextMinuteEnd = v.findViewById(R.id.editTextMinuteEnd);

        toggleAmPmStart = v.findViewById(R.id.toggleAmPmStart);
        toggleAmPmEnd = v.findViewById(R.id.toggleAmPmEnd);

        toggleAmPmStart.setTextOff(amString);
        toggleAmPmStart.setTextOn(pmString);
        toggleAmPmEnd.setTextOff(amString);
        toggleAmPmEnd.setTextOn(pmString);

        editTextHourStart.addTextChangedListener(textWatcherHour);
        editTextHourEnd.addTextChangedListener(textWatcherHour);
        editTextMinuteStart.addTextChangedListener(textWatcherMinute);
        editTextMinuteEnd.addTextChangedListener(textWatcherMinute);

        boolean is24Hour = is24HourFormat(getContext());
        maxHourValue = (is24Hour) ? 23 : 12;
        minHourValue = (is24Hour) ? 0 : 1;
        toggleAmPmStart.setChecked(hourStart > 11);
        toggleAmPmEnd.setChecked(hourEnd > 11);
        toggleAmPmStart.setVisibility(is24Hour ? View.GONE : View.VISIBLE);
        toggleAmPmEnd.setVisibility(is24Hour ? View.GONE : View.VISIBLE);

        if (is24Hour) {
            editTextHourStart.setText(String.format("%02d", hourStart));
            editTextHourEnd.setText(String.format("%02d", hourEnd));
        } else {
            editTextHourStart.setText(String.format("%02d", convertTo12Hour(hourStart)));
            editTextHourEnd.setText(String.format("%02d", convertTo12Hour(hourEnd)));
        }
        editTextMinuteStart.setText(String.format("%02d", minuteStart));
        editTextMinuteEnd.setText(String.format("%02d", minuteEnd));

        builder.setView(v)
               .setPositiveButton(android.R.string.ok, positiveButtonClickListener)
               .setNegativeButton(android.R.string.cancel, null);

        return builder.create();
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
        if (hour == 0) return 12;
        else if (hour > 12) return hour - 12;
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

    void setStartTime(int hour, int min) {
        minuteStart = min;
        hourStart = hour;
    }

    void setEndTime(int hour, int min) {
        minuteEnd = min;
        hourEnd = hour;
    }

    @Override
    public void onDestroyView() {
      if (getDialog() != null && getRetainInstance())
        getDialog().setDismissMessage(null);
      super.onDestroyView();
    }

    private boolean is24HourFormat(Context context) {
        return android.text.format.DateFormat.is24HourFormat(context);
    }

    private DialogInterface.OnClickListener positiveButtonClickListener =
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                int hourStart, hourEnd, minuteStart, minuteEnd;

                try {
                    hourStart = Integer.parseInt(editTextHourStart.getText().toString());
                } catch (NumberFormatException e) {
                    hourStart = 0;
                }
                try {
                    hourEnd = Integer.parseInt(editTextHourEnd.getText().toString());
                } catch (NumberFormatException e) {
                    hourEnd = 0;
                }
                try {
                    minuteStart = Integer.parseInt(editTextMinuteStart.getText().toString());
                } catch (NumberFormatException e) {
                    minuteStart = 0;
                }
                try {
                    minuteEnd = Integer.parseInt(editTextMinuteEnd.getText().toString());
                } catch (NumberFormatException e) {
                    minuteEnd = 0;
                }
                boolean is24Hour = is24HourFormat(getContext());
                if (!is24Hour) {
                    hourStart = convertTo24Hour(hourStart, toggleAmPmStart.isChecked());
                    hourEnd = convertTo24Hour(hourEnd, toggleAmPmEnd.isChecked());
                }
                delegate.onTimeRangeSet(hourStart, minuteStart, hourEnd, minuteEnd);
            }
        };

    private void setOkButtonEnabled(boolean enabled) {
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog == null) {
            return;
        }
        Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        button.setEnabled(enabled);
    }
}
