package com.firebirdberlin.nightdream.services;

import java.lang.String;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import de.greenrobot.event.EventBus;
import com.firebirdberlin.nightdream.events.OnLocationUpdated;

public class WeatherService extends Service {
    private static String TAG = "NightDream.WeatherService";

    private Context mContext = null;
    private boolean running = false;

    @Override
    public void onCreate(){
        EventBus.getDefault().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if ( running ) {
            return Service.START_REDELIVER_INTENT;
        }
        running = true;
        mContext = this;

        LocationService.start(this);

        return Service.START_REDELIVER_INTENT;
    }

    public void onEvent(OnLocationUpdated event){
        if ( event == null ) return;
        if ( event.entry != null ) {
            DownloadWeatherService.start(mContext, event.entry);
        }
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy(){
        EventBus.getDefault().unregister(this);
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
