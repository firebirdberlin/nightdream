package com.firebirdberlin.nightdream.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
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

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.DialogTheme);

        // Warning: must use context of AlertDialog.Builder here so that the changed theme is applied by LayoutInflater in RadioStreamDialog!
        // (AlertDialog.Builder uses a ContextThemeWrapper internally to change the theme for this DialogFragment)
        radioStreamDialog = new RadioStreamDialog(builder.getContext(), radioStation);

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
                .setNeutralButton(android.R.string.cancel, cancelClickListener);

        if (radioStation != null) {
            builder.setNegativeButton(R.string.delete, deleteClickListener);
        }

        final Dialog dialog = builder.create();

        // detect back button and also call onCancel listener so systemUI can be hidden afterwards
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey (DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK &&
                        event.getAction() == KeyEvent.ACTION_UP &&
                        !event.isCanceled()) {

                    dialog.cancel();

                    if (listener != null) {
                        listener.onCancel();
                    }

                    return true;
                }
                return false;
            }
        });

        return dialog;

    }
}
