/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.firebirdberlin.nightdream.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.radiostreamapi.models.FavoriteRadioStations;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

import java.util.ArrayList;


public class SelectRadioStationSlotDialogFragment extends AppCompatDialogFragment {
    final static String TAG = "ManageAlarmSoundsDialog";
    private FavoriteRadioStations radioStations = null;
    private ArrayList<String> stationNames = new ArrayList<>();
    // Use this instance of the interface to deliver action events
    private SelectRadioStationSlotDialogListener mListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        stationNames.clear();
        stationNames.add(getResources().getString(R.string.radio_station_none));
        String radioStation = getResources().getString(R.string.radio_station);
        for (int i = 1; i<7; i++) {
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
                        Settings.setDefaultAlarmRadioStation(getContext(), which - 1);
                        mListener.onStationSlotSelected(which, stationsSeq[which].toString());
                    }
                });
        Dialog dialog = (Dialog) builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.border_dialog);
        return dialog;
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