/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
