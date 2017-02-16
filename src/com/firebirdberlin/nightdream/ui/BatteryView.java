package com.firebirdberlin.nightdream.ui;


import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.models.BatteryValue;

public class BatteryView extends TextView {
    private static String TAG ="BatteryView";
    private static float VALUE_FULLY_CHARGED = 95.f;

    private Context context;

    public BatteryView(Context context) {
        super(context);
        this.context = context;
    }

    public BatteryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public void update(BatteryValue batteryValue, BatteryValue reference) {
        float percentage = batteryValue.getPercentage();
        String percentage_string = String.format("%3d %%", (int) percentage);
        String estimate_string = "";
        if (batteryValue.isCharging) {
            if (percentage < VALUE_FULLY_CHARGED){
                long est = batteryValue.getEstimateMillis(reference)/1000; // estimated seconds
                estimate_string = formatEstimate(est);
            }
        } else { // not charging
            long est = batteryValue.getDischargingEstimateMillis(reference)/1000; // estimated seconds
            estimate_string = formatEstimate(est);
        }
        setText(percentage_string + estimate_string);
        setVisibility(View.VISIBLE);
    }

    private String formatEstimate(long est) {
        Log.i(TAG, String.valueOf(est));
        if (est > 0){
            long h = est / 3600;
            long m  = ( est % 3600 ) / 60;
            String hour = "";
            String min = "";
            if ( h > 0L ) {
                hour = String.format("%d %s ", h, context.getString(R.string.hour));
            }
            if ( m > 0L ) {
                min = String.format("%d %s ", m, context.getString(R.string.minute));
            }

            return String.format(" (%s%s%s)", hour, min, context.getString(R.string.remaining));
        }
        return "";
    }

    public boolean shallBeVisible(BatteryValue batteryValue) {
        if (! batteryValue.isCharging ) return false;
        if ( batteryValue.getPercentage() >= VALUE_FULLY_CHARGED ) return false;

        return true;
    }
}
