package com.firebirdberlin.nightdream.NotificationList;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationShowList {
    private final List<Notification> notifications = new ArrayList<>();

    private final SharedPreferences sharedPreferences;

    public NotificationShowList(List<Notification> notifications, Context context) {
        this.notifications.addAll(notifications);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public List<Notification> get() {
        return this.notifications;
    }

    public void clear() {
        notifications.clear();
    }

    private boolean checkShowClearable(Notification notification) {
        if (sharedPreferences.getBoolean("shownotclearable", false)) {
            return true;
        } else {
            return !notification.isClearable() == sharedPreferences.getBoolean("shownotclearable", false);
        }
    }

    private boolean checkPackageName(Notification notification, String packageName) {
        if (packageName.equals("")) {
            return true;
        }

        return notification.getPackageName().equals(packageName);
    }

    public void replace(List<Notification> notifications, String packageName) {

        clear();

        for (int index = 0; index < notifications.size(); index++) {
            Notification n = notifications.get(index);
            if (checkShowClearable(n) && checkPackageName(n, packageName)) {
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
                if ( t1 == t2 ) {
                    return 0;
                } else
                if ( t1 > t2 ) {
                    return -1;
                };
                return 1;
            }
        });
    }
}
