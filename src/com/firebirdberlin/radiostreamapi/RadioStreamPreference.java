package com.firebirdberlin.radiostreamapi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.ui.RadioStreamDialog;
import com.firebirdberlin.nightdream.ui.RadioStreamDialogListener;
import com.firebirdberlin.nightdream.ui.RadioStreamManualInputDialog;
import com.firebirdberlin.radiostreamapi.models.Country;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class RadioStreamPreference extends DialogPreference {
    private final static String TAG = "RadioStreamPreference";

    private RadioStreamDialog radioStreamDialog;

    public RadioStreamPreference(Context ctx) {
        this(ctx, null);

    }

    public RadioStreamPreference(Context ctx, AttributeSet attrs) {
        this(ctx, attrs, android.R.attr.dialogPreferenceStyle);
        init();
    }

    public RadioStreamPreference(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
        init();
    }

    private void init() {
        setNegativeButtonText(android.R.string.cancel);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setPositiveButton(null, null);
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
        SharedPreferences prefs = getSharedPreferences();
        Settings.setPersistentFavoriteRadioStation(prefs, station, 0);

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
    }

}
