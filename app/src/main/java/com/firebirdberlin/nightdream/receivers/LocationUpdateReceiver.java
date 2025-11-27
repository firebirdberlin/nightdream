package com.firebirdberlin.nightdream.receivers;

import java.lang.String;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

public class LocationUpdateReceiver extends BroadcastReceiver {
    private static final String TAG = "LocationUpdateReceiver";
    public static final String ACTION_LOCATION_UPDATED = "com.firebirdberlin.nightdream.ACTION_LOCATION_UPDATED";
    public static final String ACTION_LOCATION_FAILURE = "com.firebirdberlin.nightdream.ACTION_LOCATION_FAILURE";

    protected AsyncResponse delegate;
    public interface AsyncResponse {
        void onLocationUpdated();
        void onLocationFailure();
    }

    public static LocationUpdateReceiver register(Context ctx, AsyncResponse listener) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_LOCATION_UPDATED);
        filter.addAction(ACTION_LOCATION_FAILURE);
        LocationUpdateReceiver receiver = new LocationUpdateReceiver();
        receiver.delegate = listener;

        LocalBroadcastManager.getInstance(ctx).registerReceiver(receiver, filter);
        return receiver;
    }

    public static void unregister(Context ctx, BroadcastReceiver receiver) {
        if (receiver != null) {
            LocalBroadcastManager.getInstance(ctx).unregisterReceiver(receiver);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_LOCATION_UPDATED.equals(intent.getAction())) {
            delegate.onLocationUpdated();
        } else
        if (ACTION_LOCATION_FAILURE.equals(intent.getAction())) {
            delegate.onLocationFailure();
        }
    }

    public static void send(Context ctx) {
        LocalBroadcastManager.getInstance(ctx).sendBroadcast( new Intent(ACTION_LOCATION_UPDATED) );
    }

    public static void send(Context ctx, String action) {
        Log.i(TAG, action);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast( new Intent(action) );
    }
}

