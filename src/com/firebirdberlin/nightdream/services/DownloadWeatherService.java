package com.firebirdberlin.nightdream.services;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.openweathermapapi.OpenWeatherMapApi;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class DownloadWeatherService extends IntentService {
    private static String TAG = "DownloadWeatherService";

    private Context mContext = null;

    public DownloadWeatherService() {
        super("DownloadWeatherService");
    }

    public DownloadWeatherService(String name) {
        super(name);
    }

    public static void start(Context context, Location location) {
        Intent i = new Intent(context, DownloadWeatherService.class);
        i.putExtra("lat", (float) location.getLatitude());
        i.putExtra("lon", (float) location.getLongitude());
        context.startService(i);
    }

    public static void start(Context context, String cityID) {
        Intent i = new Intent(context, DownloadWeatherService.class);
        i.putExtra("cityID", cityID);
        context.startService(i);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
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
            broadcastResult();
        } else {
            Log.w(TAG, "entry.timestamp is INVALID!");

        }

        Log.d(TAG, "Download finished.");

    }

    private void broadcastResult() {
        Intent i = new Intent(OpenWeatherMapApi.ACTION_WEATHER_DATA_UPDATED);
        sendBroadcast(i);
    }
}
