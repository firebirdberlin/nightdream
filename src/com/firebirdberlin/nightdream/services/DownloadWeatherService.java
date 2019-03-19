package com.firebirdberlin.nightdream.services;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.JobIntentService;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.openweathermapapi.OpenWeatherMapApi;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class DownloadWeatherService extends JobIntentService {
    private static String TAG = "DownloadWeatherService";

    private Context mContext = null;


    public static void start(Context context, Location location) {
        Intent i = new Intent(context, DownloadWeatherService.class);
        i.putExtra("lat", (float) location.getLatitude());
        i.putExtra("lon", (float) location.getLongitude());
        enqueueWork(context, DownloadWeatherService.class, Config.JOB_ID_FETCH_WEATHER_DATA, i);
    }

    public static void start(Context context, String cityID) {
        Intent i = new Intent(context, DownloadWeatherService.class);
        i.putExtra("cityID", cityID);
        enqueueWork(context, DownloadWeatherService.class, Config.JOB_ID_FETCH_WEATHER_DATA, i);
    }

    @Override
    protected void onHandleWork(Intent intent) {
        mContext = this;
        Log.d(TAG, TAG + " started");

        Bundle bundle = intent.getExtras();
        float lat = bundle.getFloat("lat");
        float lon = bundle.getFloat("lon");
        String cityID = bundle.getString("cityID", "");
        WeatherEntry entry = OpenWeatherMapApi.fetchWeatherData(cityID, lat, lon);
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
