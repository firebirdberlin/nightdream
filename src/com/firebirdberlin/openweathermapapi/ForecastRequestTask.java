package com.firebirdberlin.openweathermapapi;

import android.content.Context;
import android.os.AsyncTask;

import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.util.List;

public class ForecastRequestTask extends AsyncTask<String, Void, List<WeatherEntry>> {

    private AsyncResponse delegate;

    @Override
    protected List<WeatherEntry> doInBackground(String... query) {
        String q = query[0];
        String cityJson = query[1];
        if (cityJson != null && !cityJson.isEmpty()) {
            City city = City.fromJson(cityJson);
            return DarkSkyApi.fetchHourlyWeatherData(
                    (Context) delegate, city, (float) city.lat, (float) city.lon
            );
        }
        return OpenWeatherMapApi.fetchWeatherForecast((Context) delegate, q, 0.f, 0.f);

    }

    public ForecastRequestTask(AsyncResponse listener) {
        this.delegate = listener;
    }

    public interface AsyncResponse {
        void onRequestFinished(List<WeatherEntry> entries);
    }

    @Override
    protected void onPostExecute(List<WeatherEntry> entries) {
        delegate.onRequestFinished(entries);
    }
}
