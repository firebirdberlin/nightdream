package com.firebirdberlin.nightdream.ui;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;

public class AskForOverlayPermissionDialogFragment extends AppCompatDialogFragment{
    public static String TAG = "NightDreamActivity";
    private Handler handler = new Handler();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        String message = getString(R.string.permission_request_overlays);

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.DialogTheme);
        builder.setTitle(R.string.permission_request)
                .setMessage(message)
                .setIcon(R.drawable.ic_attention)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    Utility.requestPermissionCanDrawOverlays(activity);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> {
                    // User cancelled the dialog
                });
        handler.postDelayed(() -> {
            getDialog().dismiss();
        }, 60000);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.border_dialog);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        setOkButtonEnabled(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG,"onDetach()");
        AlertDialog dialog = (AlertDialog) getDialog();
        if ((dialog != null) && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private void setOkButtonEnabled(boolean enabled) {
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog == null) {
            return;
        }
        Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        button.setEnabled(enabled);
    }
}
