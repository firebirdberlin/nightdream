package com.firebirdberlin.nightdream.NotificationList;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationAppList {

    private final List<NotificationApp> notificationApps = new ArrayList<>();

    public NotificationAppList(List<NotificationApp> notificationAppList) {
        this.notificationApps.addAll(notificationAppList);
    }

    public List<NotificationApp> get() {
        return this.notificationApps;
    }

    public void clear() {
        notificationApps.clear();
    }

    public void replace(List<NotificationApp> notificationAppList) {
        clear();

        notificationApps.addAll(notificationAppList);
        Collections.sort(notificationApps, NotificationApp.comparator);
    }
}

