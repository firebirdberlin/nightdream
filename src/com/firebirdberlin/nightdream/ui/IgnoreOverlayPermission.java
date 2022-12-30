package com.firebirdberlin.nightdream.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.AttributeSet;

import androidx.preference.Preference;

public class IgnoreOverlayPermission extends Preference{
    public IgnoreOverlayPermission(Context context, AttributeSet attrs) {
        super(context, attrs);

        final Context ctx = context;
        setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Intent intent = new Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION);

                            ((Activity) ctx).startActivityForResult(intent,0);
                            return true;
                        }
                        return false;
                    }
                }
        );
    }

}
