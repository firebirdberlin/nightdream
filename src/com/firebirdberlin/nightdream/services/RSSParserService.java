package com.firebirdberlin.nightdream.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.prof18.rssparser.RssParser;
import com.prof18.rssparser.RssParserBuilder;
import com.prof18.rssparser.model.RssChannel;
import com.prof18.rssparser.model.RssItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RSSParserService extends Worker {
    private static final String TAG = "RSSParserService";
    private String urlString = "https://www.tagesschau.de/xml/rss2/";
    public static MutableLiveData<List<RssItem>> articleListLive = new MutableLiveData<>();

    public RSSParserService(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork()");

        Settings settings = new Settings(getApplicationContext());
        String url = settings.rssURL;

        if (url.isEmpty()) {
            List<RssItem> rssItems = new ArrayList<>();
            RssItem rssItem = new RssItem(
                    "",
                    getApplicationContext().getResources().getString(R.string.rss_url_error),
                    "", "", "", "", "", "", "", "", "", "",
                    new ArrayList<>(),
                    null, null, null, null
            );
            rssItems.add(rssItem);
            articleListLive.postValue(rssItems);
            return Result.success();
        }

        RssParser parser = new RssParserBuilder()
                .build();

        CompletableFuture<RssChannel> future = CoroutineBridge.INSTANCE.parseFeed(parser, url);

        future.whenComplete((channel, e) -> {
            if (e != null) {
                Log.e(TAG, "Error parsing RSS feed", e);
                List<RssItem> rssItems = new ArrayList<>();
                RssItem rssItem = new RssItem(
                        "",
                        getApplicationContext().getResources().getString(R.string.rss_data_error),
                        "", "", "", "", "", "", "", "", "", "",
                        new ArrayList<String>(),
                        null, null, null, null
                );
                rssItems.add(rssItem);
                articleListLive.postValue(rssItems);
            } else if (channel != null) {
                articleListLive.postValue(channel.getItems());
            }
        });

        // We must return success here, as the parsing happens asynchronously.
        // The WorkManager will complete its work when the future is done, or run indefinitely if it\'s not.
        // However, since we are not observing the completion of the future here, it will just return success.
        // If you need to wait for the future to complete, you would need to use a different approach,
        // possibly involving custom WorkManager logic or a different execution context.
        return Result.success();
    }
}
