package com.firebirdberlin.nightdream;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.text.DateFormat;

public class TimeRangePickerDialogFragment extends DialogFragment {

    private TimePicker pickerStart = null;
    private TimePicker pickerEnd = null;
    private LinearLayout main_layout = null;
    private Result delegate;
    private int hourStart, hourEnd = 0;
    private int minuteStart, minuteEnd = 0;

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
        // Use the Builder class for convenient dialog construction
        Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        View v = inflater.inflate(R.layout.time_range_dialog, null);
        main_layout = v.findViewById ( R.id.main_layout );
        pickerStart = v.findViewById ( R.id.start );
        pickerEnd = v.findViewById ( R.id.end );

        boolean is24Hour = Utility.is24HourFormat(getContext());
        pickerStart.setIs24HourView(is24Hour);
        pickerEnd.setIs24HourView(is24Hour);
        if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            pickerStart.setCurrentHour(hourStart);
            pickerStart.setCurrentMinute(minuteStart);
            pickerEnd.setCurrentHour(hourEnd);
            pickerEnd.setCurrentMinute(minuteEnd);
        } else {
            pickerStart.setHour(hourStart);
            pickerStart.setMinute(minuteStart);
            pickerEnd.setHour(hourEnd);
            pickerEnd.setMinute(minuteEnd);
        }
        setOrientation();

        builder.setView(v)
               .setPositiveButton(android.R.string.ok, positiveButtonClickListener)
               .setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setOrientation();
    }

    void setStartTime(int hour, int min) {
        minuteStart = min;
        hourStart = hour;
    }

    void setEndTime(int hour, int min) {
        minuteEnd = min;
        hourEnd = hour;
    }

    private void setOrientation() {
        if (main_layout == null) {
            return;
        }
        int orientation = getActivity().getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            main_layout.setOrientation(LinearLayout.HORIZONTAL);
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            main_layout.setOrientation(LinearLayout.VERTICAL);
        }
        main_layout.invalidate();
    }

    @Override
    public void onDestroyView() {
      if (getDialog() != null && getRetainInstance())
        getDialog().setDismissMessage(null);
      super.onDestroyView();
    }


    private DialogInterface.OnClickListener positiveButtonClickListener =
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                int hourStart, hourEnd, minuteStart, minuteEnd;

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    hourStart = pickerStart.getCurrentHour();
                    minuteStart = pickerStart.getCurrentMinute();
                    hourEnd = pickerEnd.getCurrentHour();
                    minuteEnd = pickerEnd.getCurrentMinute();
                } else {
                    hourStart = pickerStart.getHour();
                    minuteStart = pickerStart.getMinute();
                    hourEnd = pickerEnd.getHour();
                    minuteEnd = pickerEnd.getMinute();
                }
                delegate.onTimeRangeSet(hourStart, minuteStart, hourEnd, minuteEnd);
            }
        };
}
