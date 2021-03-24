package com.firebirdberlin.openweathermapapi;

import android.content.Context;
import android.os.AsyncTask;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.util.List;

public class ForecastRequestTask extends AsyncTask<String, Void, List<WeatherEntry>> {

    private AsyncResponse delegate;
    Settings.WeatherProvider weatherProvider;

    public ForecastRequestTask(AsyncResponse listener, Settings.WeatherProvider weatherProvider) {
        this.delegate = listener;
        this.weatherProvider = weatherProvider;
    }

    @Override
    protected List<WeatherEntry> doInBackground(String... query) {
        City city = null;
        String cityJson = query[0];
        if (cityJson != null && !cityJson.isEmpty()) {
            city = City.fromJson(cityJson);
        }
        if (city == null) {
            return null;
        }

        switch (weatherProvider) {
            case OPEN_WEATHER_MAP:
            default:
                return OpenWeatherMapApi.fetchWeatherForecast( (Context) delegate, city);
            case DARK_SKY:
                return DarkSkyApi.fetchHourlyWeatherData((Context) delegate, city);
            case BRIGHT_SKY:
                return BrightSkyApi.fetchHourlyWeatherData((Context) delegate, city);
        }
    }

    public interface AsyncResponse {
        void onRequestFinished(List<WeatherEntry> entries);
    }

    @Override
    protected void onPostExecute(List<WeatherEntry> entries) {
        delegate.onRequestFinished(entries);
    }
}
