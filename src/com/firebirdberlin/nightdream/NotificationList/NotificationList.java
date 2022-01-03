package com.firebirdberlin.nightdream.NotificationList;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class NotificationList {
    private final List<Notification> notifications = new ArrayList<>();
    private final HashSet<String> selectedNotificationKeys = new HashSet<>();

    public NotificationList() {
    }

    public NotificationList(List<Notification> notifications) {
        this.set(notifications);
    }

    public List<Notification> get() {
        return this.notifications;
    }

    public Notification get(int index) {
        return this.notifications.get(index);
    }

    public void remove(int index) {
        Notification notification = notifications.get(index);
        selectedNotificationKeys.remove(notification.getNotificationKey());
        this.notifications.remove(index);
    }

    public void setSelected(int index, boolean on) {
        Notification notification = notifications.get(index);
        if (on) {
            selectedNotificationKeys.add(notification.getNotificationKey());
        } else {
            selectedNotificationKeys.remove(notification.getNotificationKey());
        }
    }

    public boolean isSelected(int index) {
        Notification notification = notifications.get(index);
        String key = notification.getNotificationKey();
        return selectedNotificationKeys.contains(key);
    }

    public int size() {
        return this.notifications.size();
    }

    public List<Notification> getNotifications() {
        return this.notifications;
    }

    public void clear() {
        notifications.clear();
    }


    private void set(List<Notification> notifications) {
        clear();
        for (int index = 0; index < notifications.size(); index++) {
            Notification n = notifications.get(index);
            n.setChildId(index);
            this.notifications.add(n);
        }
    }

    public void replace(List<Notification> notifications, String packageName) {
        clear();
        for (int index = 0; index < notifications.size(); index++) {
            Notification n = notifications.get(index);
            if (shallShowNotification(n, packageName)) {
                n.setChildId(index);
                this.notifications.add(n);
            }
        }

        //sorting time descending order
        Collections.sort(this.notifications, new Comparator<Notification>() {
            @Override
            public int compare(Notification obj1, Notification obj2) {
                long t1 = obj1.getPostTimestamp();
                long t2 = obj2.getPostTimestamp();
                if (t1 == t2) {
                    return 0;
                } else if (t1 > t2) {
                    return -1;
                }
                return 1;
            }
        });
    }

    private boolean shallShowNotification(Notification notification, String packageName) {
        return (
                notification.isClearable()
                        && notification.getPackageName().equals(packageName)
        );
    }
}
