package com.firebirdberlin.nightdream.NotificationList;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

public class NotificationDiffCallback extends DiffUtil.Callback {
    public static String TAG = "NotificationDiffCallback";
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
        return oldNotificationList.getNotifications().get(oldItemPosition).getNotificationID() ==
                newNotificationList.getNotifications().get(newItemPosition).getNotificationID();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Log.d(TAG, "areContentsTheSame");
        final Notification oldNotification = oldNotificationList.get(oldItemPosition);
        final Notification newNotification = newNotificationList.get(newItemPosition);

        return (oldNotification.getNotificationKey().equals(newNotification.getNotificationKey()) &&
                oldNotification.getPostTimestamp() == newNotification.getPostTimestamp() &&
                oldNotificationList.isSelected(oldItemPosition) == newNotificationList.isSelected(newItemPosition));
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        // Implement method if you're going to use ItemAnimator
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}
