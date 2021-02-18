package com.firebirdberlin.openweathermapapi;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;


public class ForcecastRequestTaskToday extends AsyncTask<String, Void, WeatherEntry> {

    final static String TAG = "ForecastRequestTask";
    private AsyncResponse delegate;
    Settings.WeatherProvider weatherProvider;
    Context context;

    public ForcecastRequestTaskToday(AsyncResponse listener, Settings.WeatherProvider weatherProvider, Context mContext) {
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
                if (context == null) {
                    return OpenWeatherMapApi.fetchWeatherData((Context) delegate, String.valueOf(city.id),(float) city.lat,(float)city.lon);
                }
                else{
                    return OpenWeatherMapApi.fetchWeatherData(context, String.valueOf(city.id),(float) city.lat,(float)city.lon);
                }
            case DARK_SKY:
                if (context == null) {
                    return DarkSkyApi.fetchCurrentWeatherData((Context) delegate, city,(float) city.lat,(float)city.lon);
                }
                else {
                    return DarkSkyApi.fetchCurrentWeatherData(context, city,(float) city.lat,(float)city.lon);
                }
        }
    }

    public interface AsyncResponse {
        void onRequestFinished(WeatherEntry entries);
    }

    @Override
    protected void onPostExecute(WeatherEntry entries) {
        delegate.onRequestFinished(entries);
    }
}

