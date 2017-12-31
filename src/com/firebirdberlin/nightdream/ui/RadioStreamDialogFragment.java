package com.firebirdberlin.nightdream.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.radiostreamapi.RadioStreamPreference;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

public class RadioStreamDialogFragment extends DialogFragment {

    private RadioStreamDialog radioStreamDialog;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        /*
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Hallo")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // FIRE ZE MISSILES!
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
        */

        // TODO get current station
        RadioStation station = null;

        final RadioStreamDialogListener dialogListener = new RadioStreamDialogListener() {
            public void onRadioStreamSelected(RadioStation station) {
                //TODO save station to some preset (json)
            }
        };

        radioStreamDialog = new RadioStreamDialog(getActivity(), station);
        View view = radioStreamDialog.createDialogView(dialogListener);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.DialogTheme);

        builder.setTitle(R.string.radio_stream)
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // FIRE ZE MISSILES!
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();

    }

}
