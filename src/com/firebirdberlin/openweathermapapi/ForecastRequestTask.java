package com.firebirdberlin.openweathermapapi;

import android.content.Context;
import android.os.AsyncTask;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.lang.ref.WeakReference;
import java.util.List;

public class ForecastRequestTask extends AsyncTask<String, Void, List<WeatherEntry>> {

    final static String TAG = "ForecastRequestTask";
    private final AsyncResponse delegate;
    private final WeakReference<Context> context;
    Settings.WeatherProvider weatherProvider;

    public ForecastRequestTask(
            AsyncResponse listener, Settings.WeatherProvider weatherProvider, Context mContext
    ) {
        this.delegate = listener;
        this.weatherProvider = weatherProvider;
        this.context = new WeakReference<>(mContext);
    }

    @Override
    protected List<WeatherEntry> doInBackground(String... query) {
        City city = null;
        String cityJson = query[0];

        if (!Utility.isEmpty(cityJson)) {
            city = City.fromJson(cityJson);
        }

        if (city == null) {
            return null;
        }

        switch (weatherProvider) {
            case OPEN_WEATHER_MAP:
            default:
                return OpenWeatherMapApi.fetchWeatherForecast(context.get(), city);
            case DARK_SKY:
                return DarkSkyApi.fetchHourlyWeatherData(context.get(), city);
            case BRIGHT_SKY:
                return BrightSkyApi.fetchHourlyWeatherData(context.get(), (float) city.lat, (float) city.lon);
            case Met_No:
                return MetNoApi.fetchHourlyWeatherData(context.get(), (float) city.lat, (float) city.lon);
        }
    }

    @Override
    protected void onPostExecute(List<WeatherEntry> entries) {
        delegate.onRequestFinished(entries);
    }

    public interface AsyncResponse {
        void onRequestFinished(List<WeatherEntry> entries);
    }
}
