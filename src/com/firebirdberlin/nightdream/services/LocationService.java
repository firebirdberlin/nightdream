package com.firebirdberlin.nightdream.services;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.receivers.LocationUpdateReceiver;

import java.util.List;

public class LocationService extends Service {
    private static String TAG = "NightDream.LocationService";
    private static long MAX_AGE_IN_MILLIS = 30 * 60 * 1000;
    private LocationManager locationManager = null;

    private Context mContext = null;
    private LocationListener locationListener = null;
    private boolean running = false;
    private Location knownLocation = null;

    @Override
    public void onCreate(){
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    }

    Runnable gpsTimeout = new Runnable() {
        public void run() {
            Log.i(TAG, "gpsTimeout.run()");
            stopWithSuccess(knownLocation);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if ( running ) {
            return Service.START_REDELIVER_INTENT;
        }
        running = true;
        mContext = this;

        Notification note = Utility.getForegroundServiceNotification(this);
        startForeground(Config.NOTIFICATION_ID_FOREGROUND_SERVICES, note);

        final Settings settings = new Settings(mContext);
        if (!settings.hasPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ) {
            Log.w(TAG, "No location permissions granted !");
            stopWithFailure();
            return Service.START_REDELIVER_INTENT;
        }

        knownLocation = getLastKnownLocation(settings);
        long now = System.currentTimeMillis();
        if ( knownLocation != null && now - knownLocation.getTime() < MAX_AGE_IN_MILLIS ) {
            stopWithSuccess(knownLocation);
        } else {
            locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    stopWithSuccess(location);
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {}

                public void onProviderEnabled(String provider) {}

                public void onProviderDisabled(String provider) {}
            };

            if ( isLocationEnabled() ) {
                Log.i(TAG, "Requesting network locations");
                try {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                    setTimeout(60000);
                } catch (IllegalArgumentException e) {
                    //java.lang.IllegalArgumentException: provider doesn't exist: network
                    stopWithFailure();
                }
            } else {
                stopWithFailure();
            }

        }
        return Service.START_REDELIVER_INTENT;
    }

    private void stopWithSuccess(Location location) {
        storeLocation(location);
        stopSelf();
    }

    public void setTimeout(long time) {
        new Handler().postDelayed(gpsTimeout, time);
    }

    public void removeTimeout() {
        new Handler().removeCallbacks(gpsTimeout);
    }

    private void stopWithFailure() {
        LocationUpdateReceiver.send(this, LocationUpdateReceiver.ACTION_LOCATION_FAILURE);
        stopSelf();
    }

    private void storeLocation(Location location) {
        Log.w(TAG, "storeLocation()");
        if (location == null) {
            Log.w(TAG, "location is NULL !");
            LocationUpdateReceiver.send(this, LocationUpdateReceiver.ACTION_LOCATION_FAILURE);
            return;
        }

        final Settings settings = new Settings(mContext);
        float lon = (float) location.getLongitude();
        float lat = (float) location.getLatitude();
        long time = location.getTime();

        Log.i(TAG, "storing location: " + String.valueOf(lon) + ", " + String.valueOf(lat));
        settings.setLocation(location);
        LocationUpdateReceiver.send(this);
    }

    private Location getLastKnownLocation(Settings settings) {
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        Location lastLocation = settings.getLocation();
        if (lastLocation.getTime() > -1L ) bestLocation = lastLocation;

        for (String provider : providers) {
            if ( LocationManager.GPS_PROVIDER.equals(provider) ) {
                continue;
            }
            Location l = locationManager.getLastKnownLocation(provider);
            if (l != null && bestLocation != null && l.getTime() > bestLocation.getTime()) {
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy(){
        Log.i(TAG, "onDestroy()");
        release();
    }

    private void release() {
        running = false;
        new Handler().removeCallbacksAndMessages(null);
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    private boolean isLocationEnabled() {
        int locationMode = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Secure.getInt(mContext.getContentResolver(), Secure.LOCATION_MODE);

            } catch (SettingNotFoundException e) {
                e.printStackTrace();
            }

            return locationMode != Secure.LOCATION_MODE_OFF;

        } else {
            return isLocationEnabled_Deprecated();
        }
    }

    @SuppressWarnings("deprecation")
    private boolean isLocationEnabled_Deprecated() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT){
            String locationProviders = Secure.getString(mContext.getContentResolver(),
                    Secure.LOCATION_PROVIDERS_ALLOWED);
            return !locationProviders.isEmpty();
        }
        return false;
    }

    public static void start(Context context) {
        Intent i = new Intent(context, LocationService.class);
        //i.putExtra("start alarm", true);
        Utility.startForegroundService(context, i);
    }
}
