package com.firebirdberlin.nightdream.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.radiostreamapi.RadioStreamPreference;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

import java.util.ArrayList;
import java.util.List;

import static android.R.attr.height;

public class RadioStreamDialogFragment extends DialogFragment {

    private final static String TAG = "RadioStreamDialogFragment";

    private RadioStreamDialog radioStreamDialog;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction

        /*
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.DialogTheme);

        LayoutInflater inflater = getActivity().getLayoutInflater();

        //View view = inflater.inflate(R.layout.sleep_timer_dialog, null);
        View view = inflater.inflate(R.layout.radio_stream_dialog, null);

        Spinner countrySpinner = (Spinner) view.findViewById(R.id.countrySpinner);
        List<String> data = new ArrayList<>();
        data.add("hallo1");
        data.add("hallo2");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, data);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        countrySpinner.setAdapter(dataAdapter);

        builder.setTitle("Hallo")
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

    /*
    @Override
    public void onResume() {
        getDialog().getWindow().setLayout(200, 200);
        super.onResume();
    }
    */
}
