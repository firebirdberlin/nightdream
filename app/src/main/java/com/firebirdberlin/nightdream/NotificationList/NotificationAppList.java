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

