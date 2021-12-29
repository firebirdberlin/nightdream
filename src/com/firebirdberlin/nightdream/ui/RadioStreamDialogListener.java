package com.firebirdberlin.nightdream.ui;

import com.firebirdberlin.radiostreamapi.models.RadioStation;

public interface RadioStreamDialogListener {
    void onRadioStreamSelected(RadioStation station);

    void onCancel();

    void onDelete(int stationIndex);
}
