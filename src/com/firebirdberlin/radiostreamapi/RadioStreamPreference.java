package com.firebirdberlin.radiostreamapi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.ui.RadioStreamDialog;
import com.firebirdberlin.nightdream.ui.RadioStreamDialogListener;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

import org.json.JSONException;


public class RadioStreamPreference extends DialogPreference {
    private final static String TAG = "RadioStreamPreference";

    private RadioStreamDialog radioStreamDialog;

    public RadioStreamPreference(Context ctx) {
        this(ctx, null);

    }

    public RadioStreamPreference(Context ctx, AttributeSet attrs) {
        this(ctx, attrs, android.R.attr.dialogPreferenceStyle);
    }

    public RadioStreamPreference(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
    }


    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setNeutralButton(android.R.string.cancel, null);
        builder.setPositiveButton(null, null);

        // provide delete button if a station was already configured before
        RadioStation station = getPersistedRadioStation();
        if (station == null) {
            return;
        }
        builder.setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteRadioStation();
                notifyChanged();
                setSummary("");
            }
        });

    }

    @Override
    protected View onCreateDialogView() {

        RadioStation station = getPersistedRadioStation();

        radioStreamDialog = new RadioStreamDialog(getContext(), station);

        return radioStreamDialog.createDialogView(new RadioStreamDialogResultListener());

    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        RadioStation station = getPersistedRadioStation();
        if (station != null && station.isUserDefinedStreamUrl) {
            radioStreamDialog.showManualInputDialog(new RadioStreamDialogResultListener());
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        //this.stations.clear();
        radioStreamDialog.clearLastSearchResult();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }

    @Override
    public CharSequence getSummary () {

        RadioStation station = getPersistedRadioStation();
        if (station != null && station.name != null && !station.name.isEmpty()) {

            final boolean hasCountry = (station.countryCode != null && !station.countryCode.isEmpty());
            final boolean hasBitrate = (station.bitrate > 0);
            String summary;
            if (hasCountry && hasBitrate) {
                summary = String.format("%s (%s, %d kbit/s)", station.name, station.countryCode, station.bitrate);
            } else if (hasBitrate) {
                summary = String.format("%s (%d kbit/s)", station.name, station.bitrate);
            } else if (hasCountry){
                summary = String.format("%s (%s)", station.name, station.countryCode);
            } else {
                summary = String.format("%s", station.name);
            }

            return summary;
        } else {
            return super.getSummary();
        }
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setTitle(getTitle());
        setSummary(getPersistedString((String) defaultValue));
    }

    private String jsonKey() {
        return String.format("%s_json", getKey());
    }

    private void persistRadioStation(RadioStation station) {
        persistString(station.stream);
        //save complete station as json string
        try {
            String json = station.toJson();
            SharedPreferences prefs = getSharedPreferences();
            SharedPreferences.Editor prefEditor = prefs.edit();
            prefEditor.putString(jsonKey(), json);
            prefEditor.commit();
        } catch (JSONException e) {
            Log.e(TAG, "error converting station to json", e);
        }
    }

    private void deleteRadioStation() {
        persistString(null);
        SharedPreferences prefs = getSharedPreferences();
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.remove(jsonKey());
        prefEditor.commit();
    }

    private RadioStation getPersistedRadioStation() {
        SharedPreferences prefs = getSharedPreferences();
        String json = prefs.getString(jsonKey(),  null);
        if (json != null) {
            try {
                return RadioStation.fromJson(json);
            } catch (JSONException e) {
                Log.e(TAG, "error converting json to station", e);
            }
        }
        return null;
    }

    private class RadioStreamDialogResultListener implements RadioStreamDialogListener {
        public void onRadioStreamSelected(RadioStation station) {
            persistRadioStation(station);
            notifyChanged();
            getDialog().dismiss();
        }
        @Override
        public void onCancel() {
        }

        @Override
        public void onDelete(int stationIndex) {
            setSummary("");
        }
    }

}
