package com.firebirdberlin.openweathermapapi;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.util.List;

public class ForecastRequestTask extends AsyncTask<String, Void, List<WeatherEntry>> {

    private AsyncResponse delegate;
    final static String TAG = "ForecastRequestTask";
    Settings.WeatherProvider weatherProvider;
    Context context;

    public ForecastRequestTask(AsyncResponse listener, Settings.WeatherProvider weatherProvider, Context mContext) {
        this.delegate = listener;
        this.weatherProvider = weatherProvider;
        this.context = mContext;
    }

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
                if (context == null) {
                    return OpenWeatherMapApi.fetchWeatherForecast((Context) delegate, city);
                }
                else{
                    return OpenWeatherMapApi.fetchWeatherForecast(context, city);
                }
            case DARK_SKY:
                if (context == null) {
                    return DarkSkyApi.fetchHourlyWeatherData((Context) delegate, city);
                }
                else {
                    return DarkSkyApi.fetchHourlyWeatherData(context, city);
                }
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
