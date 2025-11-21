package com.firebirdberlin.nightdream.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.models.RssFeedItem;
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
    public static MutableLiveData<List<RssFeedItem>> articleListLive = new MutableLiveData<>();

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
            List<RssFeedItem> rssItems = new ArrayList<>();
            RssFeedItem rssItem = new RssFeedItem(
                    getApplicationContext().getResources().getString(R.string.rss_url_error),
                    "",
                    ""
            );
            rssItems.add(rssItem);
            articleListLive.postValue(rssItems);
            return Result.success();
        }

        articleListLive.postValue(fetchRssItems(url));

        return Result.success();
    }

    private List<RssFeedItem> fetchRssItems(String url) {
        RssParser parser = new RssParserBuilder().build();
        CompletableFuture<RssChannel> future = CoroutineBridge.INSTANCE.parseFeed(parser, url);

        try {
            RssChannel channel = future.get();
            if (channel != null) {
                List<RssFeedItem> feedItems = new ArrayList<>();
                for (RssItem item : channel.getItems()) {
                    feedItems.add(new RssFeedItem(item.getTitle(), item.getLink(), item.getPubDate()));
                }
                return feedItems;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing RSS feed", e);
            List<RssFeedItem> rssItems = new ArrayList<>();
            RssFeedItem rssItem = new RssFeedItem(
                    getApplicationContext().getResources().getString(R.string.rss_data_error),
                    "",
                    ""
            );
            rssItems.add(rssItem);
            return rssItems;
        }
        return new ArrayList<>();
    }
}
