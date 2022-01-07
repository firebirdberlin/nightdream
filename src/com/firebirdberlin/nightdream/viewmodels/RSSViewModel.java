package com.firebirdberlin.nightdream.viewmodels;
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

import com.firebirdberlin.nightdream.services.RSSParserService;
import com.prof.rssparser.Channel;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

public class RSSViewModel extends ViewModel {
    private static final String TAG = "RSSViewModel";
    private WeakReference<Context> weakContext;

    private final MutableLiveData<Channel> myLiveData = new MutableLiveData<>();

    public void stopWorker(){
        WorkManager.getInstance(weakContext.get()).cancelAllWorkByTag(TAG);
    }

    public void loadDataFromWorker(Context context, LifecycleOwner lifecycleOwner) {
        Log.d(TAG, "loadDataFromWorker");
        weakContext = new WeakReference<>(context);
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest downloadWeatherWork =
                new PeriodicWorkRequest.Builder(RSSParserService.class,
                        15,
                        TimeUnit.MINUTES)
                        .addTag(TAG)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork("DownloadWeather", ExistingPeriodicWorkPolicy.REPLACE, downloadWeatherWork);

        WorkManager.getInstance(context).getWorkInfoByIdLiveData(downloadWeatherWork.getId())
                .observe((LifecycleOwner) context, info -> {
                    RSSParserService.articleListLive.observe(lifecycleOwner,
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

    public MutableLiveData<Channel> getData() {
        return myLiveData;
    }
}