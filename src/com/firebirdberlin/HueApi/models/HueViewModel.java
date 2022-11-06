package com.firebirdberlin.HueApi.models;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.HueApi.HueBridgeSearch;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.zeroone3010.yahueapi.Hue;
import io.github.zeroone3010.yahueapi.TrustEverythingManager;


public class HueViewModel extends ViewModel {
    private static final String TAG = "HueViewModel";

    private final MutableLiveData<String> hueIPLiveData = new MutableLiveData<>();
    private static MutableLiveData<String> hueKeyLiveData = new MutableLiveData<>();
    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    private void findHueBridge(Context context, LifecycleOwner lifecycleOwner) {
        Log.d(TAG, "findHueBridge()");

        //return no LAN
        if (!Utility.hasNetworkConnection(context)) {
            Log.e(TAG, "findHueBridge() no Network Connection");
            hueIPLiveData.postValue("Error:No Network connection");
            return;
        }

        //return IP already found
        if (hueIPLiveData.getValue() != null && !hueIPLiveData.getValue().isEmpty() && !hueIPLiveData.getValue().contains("Error")) {
            return;
        }

        Constraints constraints =
                new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

        OneTimeWorkRequest getHueBridgeIP =
                new OneTimeWorkRequest.Builder(
                        HueBridgeSearch.class)
                        .addTag(TAG)
                        .setConstraints(constraints)
                        .build();

        WorkManager manager = WorkManager.getInstance(context);
        manager.enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, getHueBridgeIP);

        WorkManager.getInstance(context).getWorkInfoByIdLiveData(getHueBridgeIP.getId())
                .observe(lifecycleOwner, info -> {
                    if (info != null && info.getState().isFinished()) {
                        Log.d(TAG, "findHueBridge() onChanged");
                        manager.getWorkInfoByIdLiveData(getHueBridgeIP.getId()).removeObservers(lifecycleOwner);
                        String myResult = info.getOutputData().getString("bridgeIP");
                        if (myResult == null){
                            hueIPLiveData.setValue("");
                        }else {
                            hueIPLiveData.setValue(myResult);
                        }
                    }
                });
    }

    private void findHueKey(Context context, String bridgeIP) {
        Log.d(TAG, "findHueKey() bridgeIP: " + bridgeIP);

        if (!Utility.hasNetworkConnection(context)) {
            Log.e(TAG, "findHueKey() no Network Connection");
            hueKeyLiveData.setValue("Error:No Network connection");
        } else {
            if (executor.isShutdown()) {
                executor = Executors.newSingleThreadExecutor();
            }

            executor.execute(() -> { //background thread
                Log.d(TAG, "findHueKey() executor");
                final String appName = "nightdream"; // Fill in the name of your application
                final CompletableFuture<String> apiKey = Hue.hueBridgeConnectionBuilder(bridgeIP).initializeApiConnection(appName);
                String key;

                // Push the button on your Hue Bridge to resolve the apiKey future:
                try {
                    key = apiKey.get();
                    Log.d(TAG, "findHueKey() Store this API key for future use: " + key);
                    String finalKey = key;
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() -> { //like onPostExecute()
                        if (!finalKey.isEmpty()) {
                            hueKeyLiveData.postValue(finalKey);
                            Log.d(TAG, "findHueKey() executor after: " + Hue.hueBridgeConnectionBuilder(bridgeIP).initializeApiConnection(appName).isDone());
                            testHueConnection(bridgeIP, finalKey);
                        }
                        apiKey.cancel(true);
                        executor.shutdown();
                    });
                } catch (ExecutionException e) {
                    Log.e(TAG, "findHueKey() ExecutionException: " + e.getMessage());
                    handleHueApiException(e.getMessage());
                    apiKey.cancel(true);
                    executor.shutdown();
                } catch (InterruptedException e) {
                    Log.e(TAG, "findHueKey() InterruptedException: " + e.toString());
                    handleHueApiException(e.getMessage());
                    apiKey.cancel(true);
                    executor.shutdown();
                }
            });
        }
    }

    private void handleHueApiException(String exception) {
        if (exception != null) {
            String[] splitStr = exception.split(":");
            Log.e(TAG, "handleHueApiException() Error: " + splitStr[splitStr.length - 1].trim());
            hueKeyLiveData.postValue("Error:" + splitStr[splitStr.length - 1].trim());
        } else {
            hueKeyLiveData.postValue("Error:unknown");
        }
    }

    private MutableLiveData<String> getHueIP() {
        return hueIPLiveData;
    }

    private MutableLiveData<String> getHueKey() {
        return hueKeyLiveData;
    }

    public static void stopExecutor() {
        Log.d(TAG, "stopExecutor()");
        executor.shutdownNow();
    }

    public static void observeIP(Context context, LifecycleOwner lifecycleOwner, @NonNull Observer<String> observer) {
        HueViewModel model = new ViewModelProvider((ViewModelStoreOwner) context).get(HueViewModel.class);
        model.findHueBridge(context, lifecycleOwner);
        model.getHueIP().observe(lifecycleOwner, observer);
    }

    public static void observeKey(Context context, LifecycleOwner lifecycleOwner, String BridgeIP, @NonNull Observer<String> observer) {
        hueKeyLiveData = new MutableLiveData<>();
        HueViewModel model = new ViewModelProvider((ViewModelStoreOwner) context).get(HueViewModel.class);
        model.findHueKey(context, BridgeIP);
        LiveDataUtil.observeOnce(model.getHueKey(), observer);
    }

    public static boolean testHueIP(String bridgeIP) {
        Log.d(TAG, "testHueIP (" + bridgeIP + ")");
        final AtomicBoolean b = new AtomicBoolean(false);

        Thread thread = new Thread(() -> {
            try {
                TrustEverythingManager.trustAllSslConnectionsByDisablingCertificateVerification();
                HttpURLConnection urlConnection = (HttpURLConnection) new URL("https://" + bridgeIP + "/api/config").openConnection();
                int responseCode = urlConnection.getResponseCode();
                Log.d(TAG, "testHueIP(): " + responseCode);
                if (HttpURLConnection.HTTP_OK == responseCode){
                    b.set(true);
                }
            } catch (IOException e) {
                Log.e(TAG, "testHueIP(): " + e.toString());
                b.set(false);
            }
        });

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return b.get();
    }

    public static boolean testHueConnection(String bridgeIP, String apiKey) {
        Log.d(TAG, "testHueConnection (" + bridgeIP + "," + apiKey + ")");
        final AtomicBoolean b = new AtomicBoolean(false);
        final Hue hue = new Hue(bridgeIP, apiKey);

        Thread thread = new Thread(() -> {
            try {
                try {
                    hue.getAllLights();
                    b.set(true);
                    Log.d(TAG, "testHueConnection() true");
                } catch (NetworkOnMainThreadException e) {
                    Log.d(TAG, "testHueConnection() failed: " + e);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return b.get();
    }

    private static class LiveDataUtil {
        public static <T> void observeOnce(final LiveData<T> liveData, final Observer<T> observer) {
            liveData.observeForever(new Observer<T>() {
                @Override
                public void onChanged(T t) {
                    liveData.removeObserver(this);
                    observer.onChanged(t);
                }
            });
        }
    }

}
