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
    final static int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    protected File DIRECTORY = null;
    // Use this instance of the interface to deliver action events
    SelectRadioStationSlotDialogListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        ArrayList<String> stations = new ArrayList<String>();
        for (int i=1; i<7;i++) {
            stations.add(String.format("Radio station %d", i));
        }

        final CharSequence[] stationsSeq = stations.toArray(new CharSequence[stations.size()]);

        builder.setTitle("Select a radio station")
               .setItems(stationsSeq, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        mListener.onStationSlotSelected(which, "Selected station name");
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