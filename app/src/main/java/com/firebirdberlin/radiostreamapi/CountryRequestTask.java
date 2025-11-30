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

import android.content.Context;
import android.os.AsyncTask;

import com.firebirdberlin.radiostreamapi.models.Country;

import java.util.List;

public class CountryRequestTask extends AsyncTask<Void, Void, List<Country> > {

    public interface AsyncResponse {
        public void onCountryRequestFinished(List<Country> countries);
    }

    private CountryRequestTask.AsyncResponse delegate = null;
    private Context context;

    public CountryRequestTask(CountryRequestTask.AsyncResponse listener, Context context) {
        this.delegate = listener;
        this.context = context;
    }

    @Override
    protected List<Country> doInBackground(Void... params) {
        //return DirbleApi.fetchCountries(context.getCacheDir());
        return RadioBrowserApi.fetchCountries(context.getCacheDir());
    }

    @Override
    protected void onPostExecute(List<Country> countries) {
        delegate.onCountryRequestFinished(countries);
    }

}
