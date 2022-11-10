package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.preference.DialogPreference;

import com.firebirdberlin.nightdream.R;

public class HueConnectPreference extends DialogPreference {
    private static final String TAG = "HueConnectPreference";

    public HueConnectPreference(Context context) {
        this(context, null);
    }

    public HueConnectPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.dialogPreferenceStyle);
    }

    public HueConnectPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, defStyleAttr);
    }

    public HueConnectPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        Log.d(TAG, "HueConnectPreference()");

        // Do custom stuff here, read attributes etc.
        setPositiveButtonText(null);
        setNegativeButtonText(android.R.string.cancel);
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.hue_connect_preferences;
    }
}
