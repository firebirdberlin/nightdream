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

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.radiostreamapi.models.FavoriteRadioStations;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

import java.io.File;
import java.util.ArrayList;


@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class SelectRadioStationSlotDialogFragment extends DialogFragment {
    final static String TAG = "ManageAlarmSoundsDialog";
    private FavoriteRadioStations radioStations = null;
    ArrayList<String> stationNames = new ArrayList<>();
    // Use this instance of the interface to deliver action events
    SelectRadioStationSlotDialogListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        stationNames.clear();
        stationNames.add(getResources().getString(R.string.radio_station_none));
        String radioStation = getResources().getString(R.string.radio_station);
        for (int i=1; i<7;i++) {
            stationNames.add(String.format("%s #%d", radioStation, i));
        }

        final CharSequence[] stationsSeq = stationNames.toArray(new CharSequence[stationNames.size()]);

        for (int i=0; i < FavoriteRadioStations.MAX_NUM_ENTRIES; i++) {
            RadioStation station = radioStations.get(i);
            if (station == null) continue;
            stationsSeq[i + 1] = station.name;
        }

        builder.setTitle(getResources().getString(R.string.radio_station_dialog_title))
               .setItems(stationsSeq, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        mListener.onStationSlotSelected(which, stationsSeq[which].toString());
                    }
                });
        return builder.create();
    }

    public void setRadioStations(FavoriteRadioStations radioStations) {
        this.radioStations = radioStations;
    }

    public void setOnStationSlotSelectedListener(SelectRadioStationSlotDialogListener listener) {
        this.mListener = listener;
    }

    public interface SelectRadioStationSlotDialogListener {
        void onStationSlotSelected(int index, String name);
    }
}