package com.firebirdberlin.nightdream.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.prof.rssparser.Article;
import com.prof.rssparser.Channel;
import com.prof.rssparser.OnTaskCompleted;
import com.prof.rssparser.Parser;

import java.util.ArrayList;

public class RSSParserService extends Worker {
    private static final String TAG = "RSSParserService";
    private String urlString = "https://www.tagesschau.de/xml/rss2/";
    public static MutableLiveData<Channel> articleListLive = new MutableLiveData<>();

    public RSSParserService(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void start(Context context) {
        Log.d(TAG, "start");

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest rssParserWork = new OneTimeWorkRequest.Builder(
                RSSParserService.class)
                .addTag(TAG)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueue(rssParserWork);
    }

    public void stopWorker(Context context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork()");

        Parser parser = new Parser.Builder()
                // If you want to provide a custom charset (the default is utf-8):
                // .charset(Charset.forName("ISO-8859-7"))
                // .cacheExpirationMillis() and .context() not called because on Java side, caching is NOT supported
                .build();

        parser.onFinish(new OnTaskCompleted() {

            //what to do when the parsing is done
            @Override
            public void onTaskCompleted(@NonNull Channel channel) {
                articleListLive.postValue(channel);
            }

            //what to do in case of error
            @Override
            public void onError(@NonNull Exception e) {
                articleListLive.postValue(new Channel(null, null, null, null, null, null, new ArrayList<Article>(),null));
                e.printStackTrace();
                Log.d(TAG, "An error has occurred. Please try again");
            }
        });
        parser.execute(urlString);

        return Result.success();
    }

}