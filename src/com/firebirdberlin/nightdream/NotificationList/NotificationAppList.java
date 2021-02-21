package com.firebirdberlin.nightdream.NotificationList;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

        //sorting time descending order
        Collections.sort(notificationApps, new Comparator<NotificationApp>() {
            @Override
            public int compare(NotificationApp obj1, NotificationApp obj2) {
                return obj2.getPostTime().compareToIgnoreCase(obj1.getPostTime());
            }
        });
    }
}

