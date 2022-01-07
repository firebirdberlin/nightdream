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

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.prof.rssparser.Article;
import com.prof.rssparser.Channel;
import com.prof.rssparser.OnTaskCompleted;
import com.prof.rssparser.Parser;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RSSParserService extends Worker {
    private static final String TAG = "RSSParserService";
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

        Settings settings = new Settings(getApplicationContext());
        String urlString = settings.rssURL;

        List<Article> articles = new ArrayList<Article>();

        Log.d(TAG, "Url: " + urlString);

        if (!urlString.isEmpty()) {
            Parser parser = new Parser.Builder()
                    // To provide a custom charset (the default is utf-8):
                    .charset(Charset.forName(settings.rssCharSet))
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
                    Article article = new Article("",getApplicationContext().getResources().getString(R.string.rss_data_error),"","","","","","","","","","",new ArrayList<String>(),null);
                    articles.add(article);
                    articleListLive.postValue(new Channel(null, null, null, null, null, null, articles, null));
                    e.printStackTrace();
                    Log.d(TAG, "An error has occurred. Please try again");
                }
            });
            parser.execute(urlString);
        }
        else {
            Article article = new Article("",getApplicationContext().getResources().getString(R.string.rss_url_error),"","","","","","","","","","",new ArrayList<String>(),null);
            articles.add(article);
            articleListLive.postValue(new Channel(null, null, null, null, null, null, articles, null));
        }

        return Result.success();
    }

}