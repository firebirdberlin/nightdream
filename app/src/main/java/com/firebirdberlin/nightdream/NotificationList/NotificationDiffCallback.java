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

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

public class NotificationDiffCallback extends DiffUtil.Callback {
    private static final String TAG = NotificationDiffCallback.class.getSimpleName();
    private final NotificationList oldNotificationList;
    private final NotificationList newNotificationList;

    public NotificationDiffCallback(NotificationList oldNotificationList, NotificationList newNotificationList) {
        this.oldNotificationList = oldNotificationList;
        this.newNotificationList = newNotificationList;
    }

    @Override
    public int getOldListSize() {
        return oldNotificationList.size();
    }

    @Override
    public int getNewListSize() {
        return newNotificationList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        Log.d(TAG, "areItemsTheSame");
        NotificationList.Entry oldEntry = oldNotificationList.getEntry(oldItemPosition);
        NotificationList.Entry newEntry = newNotificationList.getEntry(newItemPosition);
        if (oldEntry.isStack() && newEntry.isStack()) {
            return oldEntry.getPackageName().equals(newEntry.getPackageName());
        }
        if (oldEntry.isHeader() && newEntry.isHeader()) {
            return oldEntry.getPackageName().equals(newEntry.getPackageName());
        }
        if (!oldEntry.isStack() && !oldEntry.isHeader() && !newEntry.isStack() && !newEntry.isHeader()) {
            return oldEntry.getPrimaryNotification().getNotificationID() ==
                    newEntry.getPrimaryNotification().getNotificationID();
        }
        return false;
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Log.d(TAG, "areContentsTheSame");
        NotificationList.Entry oldEntry = oldNotificationList.getEntry(oldItemPosition);
        NotificationList.Entry newEntry = newNotificationList.getEntry(newItemPosition);
        if (oldEntry.isStack() != newEntry.isStack() || oldEntry.isHeader() != newEntry.isHeader()) {
            return false;
        }
        if (oldEntry.isStack() || oldEntry.isHeader()) {
            return oldEntry.getPackageName().equals(newEntry.getPackageName()) &&
                    oldEntry.getNotifications().size() == newEntry.getNotifications().size() &&
                    oldNotificationList.isSelected(oldItemPosition) == newNotificationList.isSelected(newItemPosition);
        } else {
            final Notification oldNotification = oldEntry.getPrimaryNotification();
            final Notification newNotification = newEntry.getPrimaryNotification();

            return (oldNotification.getNotificationKey().equals(newNotification.getNotificationKey()) &&
                    oldNotification.getPostTimestamp() == newNotification.getPostTimestamp() &&
                    oldNotificationList.isSelected(oldItemPosition) == newNotificationList.isSelected(newItemPosition));
        }
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}
