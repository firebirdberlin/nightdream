package com.firebirdberlin.nightdream.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.HueApi.models.HueViewModel;

public class HueConnectPreferenceFragmentCompat extends PreferenceDialogFragmentCompat {
    private static final String TAG = "HueConnectPrefFragmentC";
    private static String bridgeIP;
    private boolean isDismissible = false;

    public static HueConnectPreferenceFragmentCompat newInstance(String key, String bridge_IP) {
        final HueConnectPreferenceFragmentCompat
                fragment = new HueConnectPreferenceFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        bridgeIP = bridge_IP;

        return fragment;
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.d(TAG, "onCreateView()");

        HueViewModel.observeKey(getContext(), getViewLifecycleOwner(), bridgeIP, Key -> {

            Log.d(TAG, "onCreateView() found Key: " + Key);

            if (!Key.isEmpty()) {
                setFragmentResult(Key);
                dismissAllowingStateLoss();
            } else {
                Log.d(TAG, "onCreateView() Key empty");
            }

        });

        return inflater.inflate(R.layout.hue_connect_preferences, container, false);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        Log.d(TAG, "onDialogClosed(): " + positiveResult);
        HueViewModel.stopExecutor();
    }

    @Override
    public void dismiss() {
        Log.d(TAG, "dismiss()");
        try {
            isDismissible = true;
            super.dismiss();
            Log.d(TAG, "dismiss() Dialog dismissed!");
        } catch (IllegalStateException e) {
            Log.d(TAG, "dismiss() error: " + e.toString());
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        Log.d(TAG, "onDismiss()");
        if (isDismissible) {
            super.onDismiss(dialog);
        }
    }

    private void setFragmentResult(String hueKey) {
        if (isAdded()) {
            Bundle result = new Bundle();
            result.putString("hueKey", hueKey);
            getParentFragmentManager().setFragmentResult("hueKey", result);
        }
    }

}
