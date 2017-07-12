package com.firebirdberlin.openweathermapapi;

import android.os.AsyncTask;

import java.util.List;

import com.firebirdberlin.openweathermapapi.OpenWeatherMapApi;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

public class ForecastRequestTask extends AsyncTask<String, Void, List<WeatherEntry>> {

    public interface AsyncResponse {
        public void onRequestFinished(List<WeatherEntry> entries);
    }

    private AsyncResponse delegate = null;

    public ForecastRequestTask(AsyncResponse listener) {
        this.delegate = listener;
    }

    @Override
    protected List<WeatherEntry> doInBackground(String... query) {
        String q = query[0];
        return OpenWeatherMapApi.fetchWeatherForecast(q, 0.f, 0.f);
    }

    @Override
    protected void onPostExecute(List<WeatherEntry> entries) {
        delegate.onRequestFinished(entries);
    }
}
