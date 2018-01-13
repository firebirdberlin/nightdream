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

    private final RadioStreamDialogListener listener;
    private final RadioStation radioStation;
    private final int stationIndex;

    public RadioStreamDialogFragment(RadioStreamDialogListener listener, RadioStation radioStation, int stationIndex) {
        super();
        this.listener = listener;
        this.radioStation = radioStation;
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
        };

        View view = radioStreamDialog.createDialogView(dialogDismissListener);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.DialogTheme);

        DialogInterface.OnClickListener cancelClickListener = new DialogInterface.OnClickListener() {


            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.onCancel();
            }
        };

        String title = getResources().getString(R.string.radio_stream) + " #" + String.valueOf(stationIndex + 1);
        builder.setTitle(title)
                .setView(view)
                .setPositiveButton(null, null)
                .setNegativeButton(android.R.string.cancel, cancelClickListener);

        return builder.create();

    }

    public static void showDialog(Activity parentActivity, int stationIndex, RadioStation radioStation, RadioStreamDialogListener listener) {
        RadioStreamDialogFragment dialog = new RadioStreamDialogFragment(listener, radioStation, stationIndex);
        //todo: show favoriteIndex in dialog title?
        dialog.show(parentActivity.getFragmentManager(), "radio_stream_dialog");
    }
}
