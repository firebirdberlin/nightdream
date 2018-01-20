package com.firebirdberlin.nightdream.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

public class RadioStreamDialogFragment extends DialogFragment {

    private final static String TAG = "RadioStreamDialogFragment";

    private RadioStreamDialog radioStreamDialog;

    private RadioStreamDialogListener listener;
    private RadioStation radioStation;
    private int stationIndex;

    public RadioStreamDialogFragment() {
        // empty default constructor
    }

    public static RadioStreamDialogFragment newInstance(RadioStreamDialogListener listener, RadioStation radioStation, int stationIndex) {
        RadioStreamDialogFragment f = new RadioStreamDialogFragment();
        f.setListener(listener);
        f.setRadioStation(radioStation);
        f.setStationIndex(stationIndex);
        return f;
    }

    public static void showDialog(Activity parentActivity, int stationIndex, RadioStation radioStation, RadioStreamDialogListener listener) {
        RadioStreamDialogFragment dialog = RadioStreamDialogFragment.newInstance(listener, radioStation, stationIndex);
        dialog.show(parentActivity.getFragmentManager(), "radio_stream_dialog");
    }

    public void setListener(RadioStreamDialogListener listener) {
        this.listener = listener;
    }

    public void setRadioStation(RadioStation radioStation) {
        this.radioStation = radioStation;
    }

    public void setStationIndex(int stationIndex) {
        this.stationIndex = stationIndex;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        radioStreamDialog = new RadioStreamDialog(getActivity(), radioStation);

        RadioStreamDialogListener dialogDismissListener = new RadioStreamDialogListener() {
            @Override
            public void onRadioStreamSelected(RadioStation station) {
                getDialog().dismiss();
                // delegate to listener
                listener.onRadioStreamSelected(station);
            }
            @Override
            public void onCancel() {
                listener.onCancel();
            }
            @Override
            public void onDelete(int stationIndex) {}
        };

        View view = radioStreamDialog.createDialogView(dialogDismissListener);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.DialogTheme);

        DialogInterface.OnClickListener cancelClickListener = new DialogInterface.OnClickListener() {


            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (listener != null) {
                    listener.onCancel();
                }
            }
        };

        DialogInterface.OnClickListener deleteClickListener = new DialogInterface.OnClickListener() {


            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (listener != null) {
                    listener.onDelete(stationIndex);
                }
            }
        };

        String title = getResources().getString(R.string.radio_stream) + " #" + String.valueOf(stationIndex + 1);
        builder.setTitle(title)
                .setView(view)
                .setPositiveButton(null, null)
                .setNegativeButton(android.R.string.cancel, cancelClickListener);

        if (radioStation != null) {
            builder.setNeutralButton(R.string.delete, deleteClickListener);
        }

        return builder.create();

    }
}
