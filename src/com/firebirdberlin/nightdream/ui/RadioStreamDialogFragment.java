package com.firebirdberlin.nightdream.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

import java.util.ArrayList;
import java.util.List;

import static android.R.attr.height;

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

    public static void showDialog(Activity parentActivity, int stationIndex, RadioStation radioStation, RadioStreamDialogListener listener) {
        RadioStreamDialogFragment dialog = new RadioStreamDialogFragment(listener, radioStation, stationIndex);
        //todo: show favoriteIndex in dialog title?
        dialog.show(parentActivity.getFragmentManager(), "radio_stream_dialog");
    }
}
