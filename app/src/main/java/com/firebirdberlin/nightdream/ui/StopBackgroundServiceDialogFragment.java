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
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.services.ScreenWatcherService;

public class StopBackgroundServiceDialogFragment extends AppCompatDialogFragment{
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //return super.onCreateDialog(savedInstanceState);

        String message = getString(R.string.backgroundServiceDialogMessage);

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.DialogTheme);
        builder.setTitle(R.string.backgroundServiceDialogTitle)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    Settings settings = new Settings(getActivity());
                    settings.disableSettingsNeedingBackgroundService();
                    ScreenWatcherService.stop(getActivity());
                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> {
                    // User cancelled the dialog
                });
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        setOkButtonEnabled(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
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
