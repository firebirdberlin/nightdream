/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.firebirdberlin.nightdream;


import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;

import androidx.multidex.MultiDex;

import com.firebirdberlin.nightdream.widget.ClockWidgetProvider;
import com.firebirdberlin.openweathermapapi.CityRequestManager;
import com.firebirdberlin.openweathermapapi.ForecastRequestTask;
import com.firebirdberlin.openweathermapapi.ForecastRequestTaskToday;

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

     @Override
     public void onTerminate() {
         super.onTerminate();
         CityRequestManager.shutdownExecutor();
         ForecastRequestTask.shutdownExecutor();
         ForecastRequestTaskToday.shutdownExecutor();
     }
}
