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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.services.RadioStreamService;

import java.util.Calendar;

public class SleepTimerDialogFragment extends AppCompatDialogFragment {

    // Use this instance of the interface to deliver action events
    SleepTimerDialogListener mListener;
    EditText minuteTextEdit;

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (SleepTimerDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement SleepTimerDialogListener");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (SleepTimerDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement SleepTimerDialogListener");
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mListener != null) {
            mListener.onSleepTimeDismissed();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.DialogTheme);
        // Get the layout inflater

        // Warning: must use context of AlertDialog.Builder here so that the changed theme is applied by LayoutInflater in RadioStreamDialog!
        // (AlertDialog.Builder uses a ContextThemeWrapper internally to change the theme for this DialogFragment)
        LayoutInflater inflater = (LayoutInflater)
                builder.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);


        View view = inflater.inflate(R.layout.sleep_timer_dialog, null);
        minuteTextEdit = view.findViewById(R.id.minuteText);

        final boolean sleepTimeisAlreadySet = RadioStreamService.isSleepTimeSet();
        builder.setTitle(R.string.sleep_time_dialog_title)
                .setIcon(R.drawable.ic_nightmode)
                .setView(view)

                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        int minutes = 0;

                        Settings settings = new Settings((Context) mListener);
                        String minuteText = minuteTextEdit.getText().toString();
                        if (!minuteText.isEmpty()) {
                            minutes += Integer.parseInt(minuteText);
                            if (!sleepTimeisAlreadySet) {
                                settings.setSleepTimeInMinutesDefaultValue(minutes);
                            }
                        }
                        Calendar now = Calendar.getInstance();
                        now.add(Calendar.MINUTE, minutes);
                        long millis = now.getTimeInMillis();
                        settings.setSleepTimeInMillis(millis);

                        mListener.onSleepTimeSelected(minutes);
                    }
                })
                .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onSleepTimeDismissed();
                    }
                });

        Settings settings = new Settings((Context) mListener);
        if (sleepTimeisAlreadySet) {
            long sleepTimeMillis = settings.sleepTimeInMillis;
            long now = Calendar.getInstance().getTimeInMillis();

            long diffInMinutes = (sleepTimeMillis - now) / 1000L / 60L;
            if (diffInMinutes > 0L) {
                minuteTextEdit.setText(String.valueOf(diffInMinutes));
            }

            builder.setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    Settings settings = new Settings(((Context) mListener).getApplicationContext());
                    settings.setSleepTimeInMillis(0L);
                    mListener.onSleepTimeDismissed();
                    SleepTimerDialogFragment.this.getDialog().cancel();
                }
            });
        } else {
            if (settings.sleepTimeInMinutesDefaultValue > -1) {
                minuteTextEdit.setText(String.valueOf(settings.sleepTimeInMinutesDefaultValue));
            }
        }

        return builder.create();
    }

    public interface SleepTimerDialogListener {
        void onSleepTimeSelected(int minutes);

        void onSleepTimeDismissed();
    }
}