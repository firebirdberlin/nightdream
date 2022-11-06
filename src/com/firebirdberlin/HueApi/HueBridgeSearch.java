package com.firebirdberlin.HueApi;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import io.github.zeroone3010.yahueapi.discovery.HueBridgeDiscoveryService;

public class HueBridgeSearch extends Worker {
    private static final String TAG = "HueBridgeSearch";

    public HueBridgeSearch(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "HueBridgeSearch doWork()");

        Future<List<io.github.zeroone3010.yahueapi.HueBridge>> bridgesFuture = new HueBridgeDiscoveryService()
                .discoverBridges(bridge -> Log.d(TAG, "ppt Bridge found: " + bridge));
        final List<io.github.zeroone3010.yahueapi.HueBridge> bridges;
        try {
            bridges = bridgesFuture.get();
            if (!bridges.isEmpty()) {
                final String bridgeIp = bridges.get(0).getIp();
                Log.d(TAG, "Bridge found at " + bridgeIp);
                // Then follow the code snippets below under the "Once you have a Bridge IP address" header
                Data myData = new Data.Builder()
                        .putString("bridgeIP", bridgeIp)
                        .build();
                return Result.success(myData);
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
            return Result.failure();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Result.failure();
        }
        return Result.success();
    }
}
