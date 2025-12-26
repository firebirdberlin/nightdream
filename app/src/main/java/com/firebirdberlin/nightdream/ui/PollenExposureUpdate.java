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

package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;

import com.firebirdberlin.dwd.PollenExposureRequestTask;
import com.firebirdberlin.nightdream.PollenExposure;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.databinding.PollenExposureBinding;
import com.firebirdberlin.openweathermapapi.GeocoderApi;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.lang.ref.WeakReference;

public class PollenExposureUpdate implements PollenExposureRequestTask.AsyncResponse {

    private final static String TAG = "PollenCount";
    private static PollenExposureRequestTask requestTask;
    private final WeakReference<Context> mContext;
    private final WeakReference<ConstraintLayout> pollenContainer;

    public PollenExposureUpdate(Context mContext, ConstraintLayout pollenContainer) {
        this.mContext = new WeakReference<>(mContext);
        this.pollenContainer = new WeakReference<>(pollenContainer);
    }

    public static void cancelUpdate() {
        if (requestTask != null) {
            requestTask = null;
        }
    }

    public void execute(WeatherEntry weatherEntry) {
        cancelUpdate();

        if (weatherEntry == null || !weatherEntry.isValid()) {
            return;
        }
        City city = GeocoderApi.findCityByCoordinates(mContext.get(), weatherEntry.lat, weatherEntry.lon);
        if (city == null || !"DE".equals(city.countryCode) || Utility.isEmpty(city.postalCode)) {
            clear();
            return;
        }

        requestTask = new PollenExposureRequestTask(this, mContext.get());
        requestTask.execute(city.postalCode);
    }

    @Override
    public void onRequestFinished(PollenExposure pollen) {
        ConstraintLayout container = pollenContainer.get();
        if (container == null || requestTask == null) {
            clear();
            return;
        }
        View boundView = container.getChildAt(0);
        PollenExposureBinding pollenExposureBinding = DataBindingUtil.getBinding(boundView);

        if (pollen.getPollenList() != null) {
            if (pollenExposureBinding != null) {
                pollenExposureBinding.getModel().setupFromObject(mContext.get(), pollen);
                pollenExposureBinding.invalidateAll();
            } else {
                clear();
                PollenExposureLayout pollenExposureLayout = new PollenExposureLayout(container);
                container.addView(pollenExposureLayout.getView());
                pollenExposureLayout.setupFromObject(mContext.get(), pollen);
            }
        }
    }

    @Override
    public void onRequestError(Exception exception) {
        Log.e(TAG, "Request error: " + exception.getMessage());
    }

    private void clear() {
        ConstraintLayout container = pollenContainer.get();
        if (container != null) {
            container.removeAllViews();
        }
    }
}
