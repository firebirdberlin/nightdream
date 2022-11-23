package com.firebirdberlin.nightdream.viewmodels;

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
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.services.RSSParserService;
import com.prof.rssparser.Channel;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

public class RSSViewModel extends ViewModel {
    private static final String TAG = "RSSViewModel";
    private static WeakReference<Context> weakContext;

    private static final MutableLiveData<Channel> myLiveData = new MutableLiveData<>();
    private static final MutableLiveData<Long> tickerAnimationSpeed = new MutableLiveData<>();
    private static final MutableLiveData<Integer> intervalMode = new MutableLiveData<>();
    private static final MutableLiveData<Float> textSize = new MutableLiveData<>();

    public static void stopWorker() {
        Log.d(TAG, "stopWorker");
        if (weakContext.get() != null) {
            WorkManager.getInstance(weakContext.get()).cancelAllWorkByTag(TAG);
        }
    }

    public static void setTickerSpeed(Long speed) {
        tickerAnimationSpeed.setValue(speed);
    }

    public static void setIntervalMode(int interval) {
        intervalMode.setValue(interval);
    }

    public static void setTextSize(float interval) {
        textSize.setValue(interval);
    }

    public static void refreshDataFromWorker(Context context) {
        Log.d(TAG, "loadDataFromWorker");
        weakContext = new WeakReference<>(context);
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest downloadRSSWork =
                new OneTimeWorkRequest.Builder(RSSParserService.class)
                        .addTag(TAG)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context).enqueue(downloadRSSWork);
    }

    public static void loadDataPeriodicFromWorker(Context context, LifecycleOwner lifecycleOwner) {
        Log.d(TAG, "loadDataPeriodicFromWorker");
        weakContext = new WeakReference<>(context);
        Settings settings = new Settings(context);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest downloadRSSWork =
                new PeriodicWorkRequest.Builder(RSSParserService.class,
                        settings.rssIntervalMode,
                        TimeUnit.MINUTES)
                        .addTag(TAG)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, downloadRSSWork);

        WorkManager.getInstance(context).getWorkInfoByIdLiveData(downloadRSSWork.getId())
                .observe((LifecycleOwner) context, info -> {
                    RSSParserService.articleListLive.observe(lifecycleOwner,
                            entry -> {
                                //EDIT: Remove the observer of the worker otherwise
                                //before execution of your below code, the observation might switch
                                Log.d(TAG, "onChanged");
                                WorkManager.getInstance(context).getWorkInfoByIdLiveData(downloadRSSWork.getId()).removeObservers(lifecycleOwner);
                                myLiveData.setValue(entry);
                            }
                    );
                });
    }

    public MutableLiveData<Channel> getData() {
        return myLiveData;
    }

    private MutableLiveData<Long> getDataSpeed() {
        return tickerAnimationSpeed;
    }

    private MutableLiveData<Integer> getDataInterval() {
        return intervalMode;
    }

    private MutableLiveData<Float> getDataTextSize() {
        return textSize;
    }

    public static void observe(Context context, @NonNull Observer<Channel> observer) {
        RSSViewModel model = new ViewModelProvider((ViewModelStoreOwner) context).get(RSSViewModel.class);
        RSSViewModel.loadDataPeriodicFromWorker(context, (LifecycleOwner) context);
        model.getData().observe((LifecycleOwner) context, observer);
    }

    public static void observeSpeed(Context context, @NonNull Observer<Long> observer) {
        RSSViewModel model = new ViewModelProvider((ViewModelStoreOwner) context).get(RSSViewModel.class);
        model.getDataSpeed().observe((LifecycleOwner) context, observer);
    }

    public static void observeInterval(Context context, @NonNull Observer<Integer> observer) {
        RSSViewModel model = new ViewModelProvider((ViewModelStoreOwner) context).get(RSSViewModel.class);
        model.getDataInterval().observe((LifecycleOwner) context, observer);
    }

    public static void observeTextSize(Context context, @NonNull Observer<Float> observer) {
        RSSViewModel model = new ViewModelProvider((ViewModelStoreOwner) context).get(RSSViewModel.class);
        model.getDataTextSize().observe((LifecycleOwner) context, observer);
    }
}