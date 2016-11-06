package com.firebirdberlin.nightdream.services;

import java.lang.Runnable;
import java.lang.String;
import java.util.List;

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

import de.greenrobot.event.EventBus;
import com.firebirdberlin.nightdream.events.OnLocationUpdated;
import com.firebirdberlin.nightdream.Settings;

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if ( running ) {
            return Service.START_REDELIVER_INTENT;
        }
        running = true;

        mContext = this;

        final Settings settings = new Settings(mContext);
        if (!settings.hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ) {
            Log.w(TAG, "No location permissions granted !");
            stopSelf();
            return Service.START_REDELIVER_INTENT;
        }

        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        knownLocation = getLastKnownLocation(settings);
        long now = System.currentTimeMillis();
        if ( knownLocation != null && now - knownLocation.getTime() < MAX_AGE_IN_MILLIS ) {
            storeLocation(knownLocation);
            stopSelf();
        } else {
            locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    storeLocation(location);
                    stopSelf();
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {}

                public void onProviderEnabled(String provider) {}

                public void onProviderDisabled(String provider) {}
            };

            if (isNetworkEnabled) {
                Log.i(TAG, "Requesting network locations");
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            }
            if (isGPSEnabled) {
                Log.i(TAG, "Requesting GPS locations");
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }

            if (!isGPSEnabled && !isNetworkEnabled) {
                stopSelf();
            }
            else
            {
                setTimeout(60000);
            }

        }
        return Service.START_REDELIVER_INTENT;
    }

    public void setTimeout(long time) {
        new Handler().postDelayed(gpsTimeout, time);
    }

    public void removeTimeout() {
        new Handler().removeCallbacks(gpsTimeout);
    }

    Runnable gpsTimeout = new Runnable() {
        public void run() {
            storeLocation(knownLocation);
            EventBus.getDefault().post(new OnLocationUpdated(knownLocation));
            stopSelf();
        }
    };

    private void storeLocation(Location location) {
        if (location == null) return;
        final Settings settings = new Settings(mContext);
        float lon = (float) location.getLongitude();
        float lat = (float) location.getLatitude();
        long time = location.getTime();

        Log.i(TAG, "storing location: " + String.valueOf(lon) + ", " + String.valueOf(lat));
        settings.setLocation(location);
        EventBus.getDefault().post(new OnLocationUpdated(location));
    }

    private Location getLastKnownLocation(Settings settings) {
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        Location lastLocation = settings.getLocation();
        if (lastLocation.getTime() > -1L ) bestLocation = lastLocation;

        for (String provider : providers) {
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
        context.startService(i);
    }
}
