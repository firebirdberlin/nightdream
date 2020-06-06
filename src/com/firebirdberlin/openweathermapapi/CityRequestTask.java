package com.firebirdberlin.openweathermapapi;

import android.os.AsyncTask;

import java.util.List;

import com.firebirdberlin.openweathermapapi.OpenWeatherMapApi;
import com.firebirdberlin.openweathermapapi.models.City;

public class CityRequestTask extends AsyncTask<String, Void, List<City> > {

    public interface AsyncResponse {
        void onRequestFinished(List<City> cities);
    }

    private AsyncResponse delegate = null;

    public CityRequestTask(AsyncResponse listener) {
        this.delegate = listener;
    }

    @Override
    protected List<City> doInBackground(String... query) {
        String q = query[0];
        return OpenWeatherMapApi.findCity(q);
    }

    @Override
    protected void onPostExecute(List<City> cities) {
        delegate.onRequestFinished(cities);
    }
}
