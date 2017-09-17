package com.firebirdberlin.nightdream.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.firebirdberlin.nightdream.R;

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

                        mListener.onSleepTimeSelected(minutes);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SleepTimerDialogFragment.this.getDialog().cancel();
                        mListener.onSleepTimeDismissed();
                    }
                });

        return builder.create();
    }

    public interface SleepTimerDialogListener {
        void onSleepTimeSelected(int minutes);
        void onSleepTimeDismissed();
    }
}