package com.firebirdberlin.nightdream.ui;


import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.firebirdberlin.nightdream.models.BatteryValue;

public class BatteryView extends TextView {
    private static String TAG ="BatteryView";
    private static float VALUE_FULLY_CHARGED = 95.f;

    public BatteryView(Context context) {
        super(context);
    }

    public BatteryView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void update(BatteryValue batteryValue, BatteryValue reference) {
        float percentage = batteryValue.getPercentage();
        if (batteryValue.isCharging) {
            if (percentage < VALUE_FULLY_CHARGED){
                long est = batteryValue.getEstimateMillis(reference)/1000; // estimated seconds
                formatBatteryEstimate(percentage, est);
            }  else {
                setText(String.format("%3d %%", (int) percentage));
            }
        } else { // not charging
            long est = batteryValue.getDischargingEstimateMillis(reference)/1000; // estimated seconds
            formatBatteryEstimate(percentage, est);
        }
        setVisibility(View.VISIBLE);
    }

    private void formatBatteryEstimate(float percentage, long est) {
        Log.i(TAG, String.valueOf(est));
        if (est > 0){
            long h = est / 3600;
            long m  = ( est % 3600 ) / 60;
            setText(String.format("% 3d %% -- %02d:%02d",
                                              (int) percentage, (int) h, (int) m));
        } else {
            setText(String.format("%3d %%", (int) percentage));
        }
    }

    public boolean isVisible() {
        if (Build.VERSION.SDK_INT < 11) return true;

        return ( getAlpha() > 0.f );

    }

    public boolean shallBeVisible(BatteryValue batteryValue) {
        if (! batteryValue.isCharging ) return false;
        if ( batteryValue.getPercentage() >= VALUE_FULLY_CHARGED ) return false;

        return true;
    }
}
