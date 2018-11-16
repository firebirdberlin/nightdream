package com.firebirdberlin.nightdream.ui;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;


@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class SelectRadioStationSlotDialogFragment extends DialogFragment {
    final static String TAG = "ManageAlarmSoundsDialog";
    protected File DIRECTORY = null;
    ArrayList<String> stationNames = new ArrayList<>();
    // Use this instance of the interface to deliver action events
    SelectRadioStationSlotDialogListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        stationNames.clear();
        stationNames.add("No radio station");
        for (int i=1; i<7;i++) {
            stationNames.add(String.format("Radio station %d", i));
        }

        final CharSequence[] stationsSeq = stationNames.toArray(new CharSequence[stationNames.size()]);

        builder.setTitle("Select a radio station")
               .setItems(stationsSeq, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        mListener.onStationSlotSelected(which, stationNames.get(which));
                    }
                });
        return builder.create();
    }

    public void setOnStationSlotSelectedListener(SelectRadioStationSlotDialogListener listener) {
        this.mListener = listener;
    }

    public interface SelectRadioStationSlotDialogListener {
        void onStationSlotSelected(int index, String name);
    }
}