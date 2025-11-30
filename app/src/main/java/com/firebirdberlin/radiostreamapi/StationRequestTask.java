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

package com.firebirdberlin.radiostreamapi;

import android.os.AsyncTask;

import java.util.List;

import com.firebirdberlin.radiostreamapi.DirbleApi;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

public class StationRequestTask extends AsyncTask<String, Void, List<RadioStation> > {

    public interface AsyncResponse {
        public void onRequestFinished(List<RadioStation> stations);
    }

    private AsyncResponse delegate = null;

    public StationRequestTask(AsyncResponse listener) {
        this.delegate = listener;
    }

    @Override
    protected List<RadioStation> doInBackground(String... query) {
        String q = query[0];
        String countryCode = query.length > 1 ? query[1] : null;

        return RadioBrowserApi.fetchStations(q, countryCode);
        //return DirbleApi.fetchStations(q, countryCode);
    }

    @Override
    protected void onPostExecute(List<RadioStation> stations) {
        delegate.onRequestFinished(stations);
    }
}
