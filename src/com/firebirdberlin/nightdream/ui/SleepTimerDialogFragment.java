package com.firebirdberlin.nightdream.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.receivers.RadioStreamSleepTimeReceiver;

import java.util.Calendar;

public class SleepTimerDialogFragment extends DialogFragment {

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
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.DialogTheme);
        // Get the layout inflater

        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.sleep_timer_dialog, null);
        minuteTextEdit = (EditText) view.findViewById(R.id.minuteText);

        builder.setTitle(R.string.sleep_time_dialog_title)
                .setIcon(R.drawable.ic_nightmode)
                .setView(view)

                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        int minutes = 0;

                        String minuteText = minuteTextEdit.getText().toString();
                        if (!minuteText.isEmpty()) {
                            minutes += Integer.parseInt(minuteText);
                        }
                        Calendar now = Calendar.getInstance();
                        now.add(Calendar.MINUTE, minutes);
                        long millis = now.getTimeInMillis();
                        RadioStreamSleepTimeReceiver.schedule((Context) mListener, millis);

                        mListener.onSleepTimeSelected(minutes);
                    }
                })
                .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onSleepTimeDismissed();
                    }
                });

        if (RadioStreamSleepTimeReceiver.isSleepTimeSet()) {
            long sleepTimeMillis = RadioStreamSleepTimeReceiver.getSleepTime();
            long now = Calendar.getInstance().getTimeInMillis();

            long diffInMinutes = (sleepTimeMillis - now) / 1000L / 60L;
            if (diffInMinutes > 0L) {
                minuteTextEdit.setText(String.valueOf(diffInMinutes));
            }

            builder.setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    RadioStreamSleepTimeReceiver.cancel((Context) mListener);
                    mListener.onSleepTimeDismissed();
                    SleepTimerDialogFragment.this.getDialog().cancel();
                }
            });
        }

        return builder.create();
    }

    public interface SleepTimerDialogListener {
        void onSleepTimeSelected(int minutes);
        void onSleepTimeDismissed();
    }
}