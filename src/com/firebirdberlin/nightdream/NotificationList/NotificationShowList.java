package com.firebirdberlin.nightdream.NotificationList;

import android.content.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationShowList {
    private final List<Notification> notifications = new ArrayList<>();

    public NotificationShowList(List<Notification> notifications, Context context) {
        this.notifications.addAll(notifications);
    }

    public List<Notification> get() {
        return this.notifications;
    }

    public void clear() {
        notifications.clear();
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

    private boolean shallShowNotification(Notification notification, String packageName) {
        return (
                notification.isClearable()
                        && notification.getPackageName().equals(packageName)
        );
    }
}
