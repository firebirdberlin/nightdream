package com.firebirdberlin.nightdream.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.receivers.LocationUpdateReceiver;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

public class WeatherService extends Service
                            implements LocationUpdateReceiver.AsyncResponse {
    private static String TAG = "NightDream.WeatherService";
    private static LocationUpdateReceiver locationReceiver = null;
    private static long lastLocationRequest = 0L;
    private Context mContext = null;
    private boolean running = false;

    public static void start(Context context) {
        Intent i = new Intent(context, WeatherService.class);
        context.startService(i);
    }

    public static void start(Context context, String cityID) {
        if (!cityID.isEmpty()) {
            DownloadWeatherService.start(context, cityID);
            return;
        }

        Intent i = new Intent(context, WeatherService.class);
        context.startService(i);
    }

    public static boolean shallUpdateWeatherData(Settings settings) {
        if (!settings.showWeather) return false;

        WeatherEntry entry = settings.weatherEntry;
        long requestAge = System.currentTimeMillis() - lastLocationRequest;
        long diff = entry.ageMillis();
        final int maxDiff = 90 * 60 * 1000;
        final int maxRequestAge = 15 * 60 * 1000;
        final String cityID = String.valueOf(entry.cityID);
        Log.d(TAG, String.format("Weather: data age %d => %b", diff, diff > maxDiff));
        Log.d(TAG, String.format("Time since last request %d => %b", requestAge, requestAge > maxRequestAge));
        Log.d(TAG, String.format("City ID changed => %b (%s =?= %s)",
                (!settings.weatherCityID.isEmpty() && !settings.weatherCityID.equals(cityID)),
                settings.weatherCityID, cityID));
        boolean result = (
                diff < 0L
                        || (!settings.weatherCityID.isEmpty() && !settings.weatherCityID.equals(cityID))
                        || (diff > maxDiff && requestAge > maxRequestAge)
        );
        if (result) {
            lastLocationRequest = System.currentTimeMillis();
        }
        return result;
    }

    @Override
    public void onCreate(){
        locationReceiver = LocationUpdateReceiver.register(this, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if ( running ) {
            Log.i(TAG, "WeatherService starts NOT");
            return Service.START_REDELIVER_INTENT;
        }
        Log.i(TAG, "WeatherService starts");
        running = true;
        mContext = this;

        LocationService.start(this);

        return Service.START_REDELIVER_INTENT;
    }

    public void onLocationUpdated() {
        Log.i(TAG, "location updated");
        final Settings settings = new Settings(mContext);
        DownloadWeatherService.start(mContext, settings.getLocation());
        stopSelf();
    }

    public void onLocationFailure() {
        Log.i(TAG, "location failure");
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy(){
        running = false;
        LocationUpdateReceiver.unregister(this, locationReceiver);
    }
}
