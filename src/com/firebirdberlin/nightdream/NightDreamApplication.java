package com.firebirdberlin.nightdream;


import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;

import com.firebirdberlin.nightdream.widget.ClockWidgetProvider;

/**
 * A global Application instance which notifies widgets to update its content on orientation change events.
 */
public class NightDreamApplication extends Application {

    private static final String TAG = "NightDreamApplication";

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.i(TAG, "onConfigurationChanged");

        notifyClockWidgets();
    }

    /**
     * Notify widget that a configuration change event occored.
     */
    private void notifyClockWidgets() {
        // update all widget instances via intent
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        final int[] appWidgetIds = ClockWidgetProvider.appWidgetIds(this, appWidgetManager);
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, ClockWidgetProvider.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        sendBroadcast(intent);
    }
}
