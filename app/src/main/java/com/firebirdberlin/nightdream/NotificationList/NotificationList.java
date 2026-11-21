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

import com.firebirdberlin.nightdream.Utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NotificationList {
    private final List<Notification> rawNotifications = new ArrayList<>();
    private final List<Entry> entries = new ArrayList<>();
    private final HashSet<String> selectedNotificationKeys = new HashSet<>();
    private final Set<String> unstackedPackages = new HashSet<>();

    public static class Entry {
        private final String packageName;
        private final List<Notification> notifications;
        private final boolean isStack;
        private final boolean isHeader;

        public Entry(String packageName, List<Notification> notifications, boolean isStack, boolean isHeader) {
            this.packageName = packageName;
            this.notifications = notifications;
            this.isStack = isStack;
            this.isHeader = isHeader;
        }

        public String getPackageName() {
            return packageName;
        }

        public List<Notification> getNotifications() {
            return notifications;
        }

        public boolean isStack() {
            return isStack;
        }

        public boolean isHeader() {
            return isHeader;
        }

        public Notification getPrimaryNotification() {
            if (notifications.isEmpty()) {
                return null;
            }
            return notifications.get(0);
        }
    }

    public NotificationList() {
    }

    public NotificationList(List<Notification> notifications) {
        this.set(notifications);
    }

    public NotificationList(NotificationList other) {
        if (other != null) {
            this.rawNotifications.addAll(other.rawNotifications);
            this.unstackedPackages.addAll(other.unstackedPackages);
            this.selectedNotificationKeys.addAll(other.selectedNotificationKeys);
            rebuildEntries();
        }
    }

    public List<Notification> getNotifications() {
        return this.rawNotifications;
    }

    public List<Entry> getEntries() {
        return this.entries;
    }

    public Entry getEntry(int index) {
        return this.entries.get(index);
    }

    public Notification get(int index) {
        Entry entry = entries.get(index);
        return entry.getPrimaryNotification();
    }

    public boolean isStack(int index) {
        return entries.get(index).isStack();
    }

    public boolean isHeader(int index) {
        return entries.get(index).isHeader();
    }

    public List<String> getAllNotificationKeys(int index) {
        List<String> keys = new ArrayList<>();
        Entry entry = entries.get(index);
        for (Notification n : entry.getNotifications()) {
            if (n.getNotificationKey() != null) {
                keys.add(n.getNotificationKey());
            }
        }
        return keys;
    }

    public int getNotificationCountForPackage(String packageName) {
        if (Utility.isEmpty(packageName)) {
            return 0;
        }
        int count = 0;
        for (Notification n : rawNotifications) {
            if (packageName.equals(n.getPackageName())) {
                count++;
            }
        }
        return count;
    }

    public void remove(int index) {
        if (index < 0 || index >= entries.size()) return;
        Entry entry = entries.get(index);
        List<Notification> toRemove = entry.getNotifications();
        for (Notification n : toRemove) {
            selectedNotificationKeys.remove(n.getNotificationKey());
            rawNotifications.remove(n);
        }
        rebuildEntries();
    }

    public void setSelected(int index, boolean on) {
        Entry entry = entries.get(index);
        for (Notification n : entry.getNotifications()) {
            if (on) {
                selectedNotificationKeys.add(n.getNotificationKey());
            } else {
                selectedNotificationKeys.remove(n.getNotificationKey());
            }
        }
    }

    public boolean isSelected(int index) {
        Entry entry = entries.get(index);
        for (Notification n : entry.getNotifications()) {
            if (!selectedNotificationKeys.contains(n.getNotificationKey())) {
                return false;
            }
        }
        return !entry.getNotifications().isEmpty();
    }

    public int size() {
        return this.entries.size();
    }

    public void clear() {
        rawNotifications.clear();
        entries.clear();
        selectedNotificationKeys.clear();
        unstackedPackages.clear();
    }

    private void set(List<Notification> notifications) {
        clear();
        if (notifications != null) {
            for (int index = 0; index < notifications.size(); index++) {
                Notification n = notifications.get(index);
                n.setChildId(index);
                this.rawNotifications.add(n);
            }
        }
        sortRawNotifications();
        rebuildEntries();
    }

    public void replace(List<Notification> notifications, String packageName) {
        clear();
        if (notifications != null) {
            for (int index = 0; index < notifications.size(); index++) {
                Notification n = notifications.get(index);
                if (shallShowNotification(n, packageName)) {
                    n.setChildId(index);
                    this.rawNotifications.add(n);
                }
            }
        }
        sortRawNotifications();
        rebuildEntries();
    }

    public void unstack(String packageName) {
        if (!Utility.isEmpty(packageName)) {
            unstackedPackages.add(packageName);
            rebuildEntries();
        }
    }

    public void stack(String packageName) {
        if (!Utility.isEmpty(packageName)) {
            unstackedPackages.remove(packageName);
            rebuildEntries();
        }
    }

    private void sortRawNotifications() {
        Collections.sort(this.rawNotifications, new Comparator<Notification>() {
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

    private void rebuildEntries() {
        entries.clear();
        List<String> packageOrder = new ArrayList<>();
        Map<String, List<Notification>> packageMap = new HashMap<>();

        for (Notification n : rawNotifications) {
            String pkg = n.getPackageName();
            if (pkg == null) {
                pkg = "";
            }
            List<Notification> list = packageMap.get(pkg);
            if (list == null) {
                list = new ArrayList<>();
                packageMap.put(pkg, list);
                packageOrder.add(pkg);
            }
            list.add(n);
        }

        for (String pkg : packageOrder) {
            List<Notification> list = packageMap.get(pkg);
            if (list == null || list.isEmpty()) {
                continue;
            }

            if (list.size() > 1 && !unstackedPackages.contains(pkg)) {
                entries.add(new Entry(pkg, list, true, false));
            } else if (list.size() > 1) {
                entries.add(new Entry(pkg, list, false, true));
                for (Notification n : list) {
                    List<Notification> singleList = new ArrayList<>();
                    singleList.add(n);
                    entries.add(new Entry(pkg, singleList, false, false));
                }
            } else {
                for (Notification n : list) {
                    List<Notification> singleList = new ArrayList<>();
                    singleList.add(n);
                    entries.add(new Entry(pkg, singleList, false, false));
                }
            }
        }
    }

    private boolean shallShowNotification(Notification notification, String packageName) {
        if (!notification.isClearable()) {
            return false;
        }
        return Utility.isEmpty(packageName) || notification.getPackageName().equals(packageName);
    }
}
