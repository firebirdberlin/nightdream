package com.firebirdberlin.nightdream.services;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.util.concurrent.TimeUnit;

public class DownloadWeatherModel extends ViewModel {
    private static final String TAG = "DownloadWeatherModel";

    private final MutableLiveData<WeatherEntry> myLiveData = new MutableLiveData<>();

    public void loadDataFromWorker(Context context, LifecycleOwner lifecycleOwner) {
        Log.d(TAG, "loadDataFromWorker");
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest downloadWeatherWork =
                new PeriodicWorkRequest.Builder(DownloadWeatherService.class,
                        60,
                        TimeUnit.MINUTES)
                        .addTag(TAG)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork("DownloadWeather", ExistingPeriodicWorkPolicy.REPLACE, downloadWeatherWork);

        WorkManager.getInstance(context).getWorkInfoByIdLiveData(downloadWeatherWork.getId())
                .observe((LifecycleOwner) context, info -> {
                    DownloadWeatherService.outputObservable.observe(lifecycleOwner,
                            entry -> {
                                //EDIT: Remove the observer of the worker otherwise
                                //before execution of your below code, the observation might switch
                                Log.d(TAG, "onChanged");
                                WorkManager.getInstance(context).getWorkInfoByIdLiveData(downloadWeatherWork.getId()).removeObservers(lifecycleOwner);
                                myLiveData.setValue(entry);
                            }
                    );
                });
    }

    public MutableLiveData<WeatherEntry> getData() {
        return myLiveData;
    }
}
