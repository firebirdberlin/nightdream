package com.firebirdberlin.openweathermapapi;


import android.content.Context;
import android.os.AsyncTask;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;


public class ForecastRequestTaskToday extends AsyncTask<String, Void, WeatherEntry> {

    final static String TAG = "ForecastRequestTask";
    Settings.WeatherProvider weatherProvider;
    Context context;
    private final AsyncResponse delegate;

    public ForecastRequestTaskToday(AsyncResponse listener, Settings.WeatherProvider weatherProvider, Context mContext) {
        this.delegate = listener;
        this.weatherProvider = weatherProvider;
        this.context = mContext;
    }

    @Override
    protected WeatherEntry doInBackground(String... query) {
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
                return OpenWeatherMapApi.fetchWeatherData(context, String.valueOf(city.id), (float) city.lat, (float) city.lon);
            case DARK_SKY:
                return DarkSkyApi.fetchCurrentWeatherData(context, city, (float) city.lat, (float) city.lon);
            case BRIGHT_SKY:
                return BrightSkyApi.fetchCurrentWeatherData(context, (float) city.lat, (float) city.lon);
            case MET_NO:
                return MetNoApi.fetchCurrentWeatherData(context, (float) city.lat, (float) city.lon);
        }
    }

    @Override
    protected void onPostExecute(WeatherEntry entries) {
        delegate.onRequestFinished(entries);
    }

    public interface AsyncResponse {
        void onRequestFinished(WeatherEntry entries);
    }
}

