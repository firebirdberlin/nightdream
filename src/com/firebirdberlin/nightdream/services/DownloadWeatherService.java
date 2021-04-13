package com.firebirdberlin.nightdream.services;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.core.app.JobIntentService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.openweathermapapi.BrightSkyApi;
import com.firebirdberlin.openweathermapapi.DarkSkyApi;
import com.firebirdberlin.openweathermapapi.OpenWeatherMapApi;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class DownloadWeatherService extends JobIntentService {
    private static String TAG = "DownloadWeatherService";

    private Context mContext = null;


    public static void start(Context context) {
        Log.d(TAG, "start() -> enqueueWork");
        Intent i = new Intent(context, DownloadWeatherService.class);
        enqueueWork(context, DownloadWeatherService.class, Config.JOB_ID_FETCH_WEATHER_DATA, i);
    }

    @Override
    protected void onHandleWork(Intent intent) {
        mContext = this;
        Log.i(TAG, "onHandleWork");
        Utility.logIntent(TAG, "onHandleWork", intent);
        Settings settings = new Settings(this);
        if (! WeatherService.shallUpdateWeatherData(this, settings)) {
           return;
        }
        if (! Utility.hasNetworkConnection(this)) {
            return;
        }
        City city = settings.getCityForWeather();
        Location location = settings.getLocation();
        String cityID = settings.weatherCityID;
        Settings.WeatherProvider weatherProvider = settings.getWeatherProvider();
        WeatherEntry entry;
        Log.d(TAG, "fetchWeatherData");
        switch (weatherProvider) {
            case OPEN_WEATHER_MAP:
            default:
                entry = OpenWeatherMapApi.fetchWeatherData(
                        this,
                        cityID,
                        (float) location.getLatitude(),
                        (float) location.getLongitude()
                );
                break;
            case DARK_SKY:
                entry = DarkSkyApi.fetchCurrentWeatherData(
                        this,
                        city,
                        (float) location.getLatitude(),
                        (float) location.getLongitude()
                );
                break;
            case BRIGHT_SKY:
                if (city != null) {
                    entry = BrightSkyApi.fetchCurrentWeatherData(
                            this,
                            (float) city.lat,
                            (float) city.lon
                    );
                } else {
                    entry = BrightSkyApi.fetchCurrentWeatherData(
                            this,
                            (float) location.getLatitude(),
                            (float) location.getLongitude()
                    );
                }
                break;
        }
        onPostExecute(entry);
    }

    protected void onPostExecute(WeatherEntry entry) {

        if (entry == null){
            return;
        }
        if ( entry.timestamp > WeatherEntry.INVALID ) {
            Settings settings = new Settings(mContext);
            settings.setWeatherEntry(entry);
            broadcastResult(settings, entry);
        } else {
            Log.w(TAG, "entry.timestamp is INVALID!");

        }
        Log.d(TAG, "Download finished.");
    }

    private void broadcastResult(Settings settings, WeatherEntry entry) {
        Intent intent = new Intent(OpenWeatherMapApi.ACTION_WEATHER_DATA_UPDATED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        ScreenWatcherService.updateNotification(mContext, entry, settings.temperatureUnit);
    }
}
