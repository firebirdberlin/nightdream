package com.firebirdberlin.nightdream.models;

public class RssFeedItem {
    private final String title;
    private final String link;
    private final String pubDate;

    public RssFeedItem(String title, String link, String pubDate) {
        this.title = title;
        this.link = link;
        this.pubDate = pubDate;
    }

    public String getTitle() {
        return title;
    }

    public String getLink() {
        return link;
    }

    public String getPubDate() {
        return pubDate;
    }
}
