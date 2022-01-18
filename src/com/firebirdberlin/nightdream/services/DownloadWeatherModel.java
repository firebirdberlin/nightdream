package com.firebirdberlin.nightdream.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
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

    private void loadDataFromWorker(Context context, LifecycleOwner lifecycleOwner) {
        Log.d(TAG, "loadDataFromWorker");
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest downloadWeatherWork =
                new PeriodicWorkRequest.Builder(
                        DownloadWeatherService.class, 60, TimeUnit.MINUTES
                ).addTag(TAG).setConstraints(constraints).build();

        WorkManager manager = WorkManager.getInstance(context);
        manager.enqueueUniquePeriodicWork("DownloadWeather", ExistingPeriodicWorkPolicy.REPLACE, downloadWeatherWork);

        manager.getWorkInfoByIdLiveData(downloadWeatherWork.getId())
                .observe((LifecycleOwner) context, info -> DownloadWeatherService.outputObservable.observe(
                        lifecycleOwner,
                        entry -> {
                            //EDIT: Remove the observer of the worker otherwise
                            //before execution of your below code, the observation might switch
                            Log.d(TAG, "onChanged");
                            manager.getWorkInfoByIdLiveData(downloadWeatherWork.getId()).removeObservers(lifecycleOwner);
                            myLiveData.setValue(entry);
                        }
                ));
    }

    private MutableLiveData<WeatherEntry> getData() {
        return myLiveData;
    }

    public static void observe(Context context, @NonNull Observer<WeatherEntry> observer) {
        DownloadWeatherModel model = new ViewModelProvider((ViewModelStoreOwner) context).get(DownloadWeatherModel.class);
        model.loadDataFromWorker(context, (LifecycleOwner) context);
        model.getData().observe((LifecycleOwner) context, observer);
    }
}
