package com.firebirdberlin.nightdream.services;

import java.lang.String;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import com.firebirdberlin.nightdream.receivers.LocationUpdateReceiver;
import com.firebirdberlin.nightdream.Settings;

public class WeatherService extends Service
                            implements LocationUpdateReceiver.AsyncResponse {
    private static String TAG = "NightDream.WeatherService";

    private Context mContext = null;
    private boolean running = false;
    private static LocationUpdateReceiver locationReceiver = null;

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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy(){
        running = false;
        LocationUpdateReceiver.unregister(this, locationReceiver);
    }

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
}
