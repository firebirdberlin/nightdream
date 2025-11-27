package com.firebirdberlin.nightdream.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;

import androidx.preference.Preference;

public class IgnoreBatteryOptimizationPreference extends Preference{

    public IgnoreBatteryOptimizationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        final Context ctx = context;
        setOnPreferenceClickListener(
                preference -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        ((Activity) ctx).startActivityForResult(intent,0);
                        return true;
                    }
                    return false;
                }
        );
    }


}
