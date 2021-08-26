package com.firebirdberlin.nightdream;


import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;

import androidx.multidex.MultiDex;

import com.firebirdberlin.nightdream.widget.ClockWidgetProvider;

/**
 * A global Application instance which notifies widgets to update its content on orientation
 * change events.
 */
public class NightDreamApplication extends Application {

    private static final String TAG = "NightDreamApplication";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.i(TAG, "onConfigurationChanged");

        notifyClockWidgets();
    }

    /**
     * Notify widget that a configuration change event occurred.
     */
    private void notifyClockWidgets() {
        // update all widget instances via intent
        ClockWidgetProvider.updateAllWidgets(this);
    }
}
