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

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;

import java.util.ArrayList;

public class NotificationRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = NotificationRecyclerViewAdapter.class.getSimpleName();
    public static final int VIEW_TYPE_NOTIFICATION = 0;
    public static final int VIEW_TYPE_HEADER = 1;

    private final int[] colorResId = new int[]{
            R.color.grey_bg,
            R.color.material_blue_grey
    };
    NotificationList notificationList;

    public NotificationRecyclerViewAdapter(NotificationList notificationList) {
        this.notificationList = notificationList;
    }

    @Override
    public int getItemViewType(int position) {
        if (notificationList.isHeader(position)) {
            return VIEW_TYPE_HEADER;
        }
        return VIEW_TYPE_NOTIFICATION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View headerView = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.notification_list_group_header, parent, false
            );
            headerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        int itemPosition = ((RecyclerView) parent).getChildLayoutPosition(v);
                        if (itemPosition != RecyclerView.NO_POSITION) {
                            NotificationList.Entry entry = notificationList.getEntry(itemPosition);
                            NotificationList oldList = new NotificationList(notificationList);
                            notificationList.stack(entry.getPackageName());
                            updateDataSet(oldList, notificationList);
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "header onClick", ex);
                    }
                }
            });
            return new HeaderViewHolder(headerView);
        }

        Log.d(TAG, "BrowseNotification started");

        final View recyclerViewItem = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.notification_list_item_view, parent, false
        );

        //Interface definition for a callback to be invoked when a view is clicked.
        recyclerViewItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int itemPosition = ((RecyclerView) parent).getChildLayoutPosition(v);
                    if (itemPosition != RecyclerView.NO_POSITION) {
                        NotificationList.Entry entry = notificationList.getEntry(itemPosition);
                        if (entry.isStack()) {
                            NotificationList oldList = new NotificationList(notificationList);
                            notificationList.unstack(entry.getPackageName());
                            updateDataSet(oldList, notificationList);
                        } else {
                            handleRecyclerItemClick((RecyclerView) parent, v);
                        }
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "setOnClickListener", ex);
                }
            }
        });

        recyclerViewItem.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                int index = Integer.parseInt((String) view.getTag());
                boolean isSelected = notificationList.isSelected(index);
                View cardView = recyclerViewItem.findViewById(R.id.notify);
                if (cardView != null) {
                    cardView.setBackgroundResource(colorResId[isSelected ? 0 : 1]);
                }
                notificationList.setSelected(index, !isSelected);

                return true;
            }
        });
        return new NotificationViewHolder(recyclerViewItem);
    }

    public void removeNotification(int position, NotificationList oldNotificationList) {
        Log.d(TAG, "removeNotification");
        this.notificationList.remove(position);
        updateDataSet(oldNotificationList, this.notificationList);
    }

    public void updateDataSet(NotificationList oldNotificationList, NotificationList newNotificationList) {
        final NotificationDiffCallback diffCallback = new NotificationDiffCallback(oldNotificationList, newNotificationList);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        diffResult.dispatchUpdatesTo(this);
    }

    //Called by RecyclerView to display the data at the specified position.
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder rawHolder, int position) {
        final NotificationList.Entry entry = this.notificationList.getEntry(position);
        rawHolder.itemView.setTag("" + position);

        if (getItemViewType(position) == VIEW_TYPE_HEADER) {
            HeaderViewHolder holder = (HeaderViewHolder) rawHolder;
            Notification primaryNotification = entry.getPrimaryNotification();
            if (primaryNotification != null) {
                holder.headerAppname.setText(primaryNotification.getApplicationName());
                if (primaryNotification.getSmallIconBitmap() != null) {
                    holder.headerIcon.setImageBitmap(primaryNotification.getSmallIconBitmap());
                }
            }
            holder.headerCount.setText(String.valueOf(entry.getNotifications().size()));
            return;
        }

        NotificationViewHolder holder = (NotificationViewHolder) rawHolder;
        final Notification notification = entry.getPrimaryNotification();

        // Bind data to viewholder
        holder.notificationLargePicture.setVisibility(View.GONE);

        //Layout without style / Remoteview supported by Notification?

        holder.notificationRemoteView.removeAllViews();
        holder.itemView.findViewById(R.id.notify).setVisibility(View.VISIBLE);

        View cardView = holder.itemView.findViewById(R.id.notify);
        boolean isSelected = notificationList.isSelected(position);
        cardView.setBackgroundResource(colorResId[isSelected ? 1 : 0]);

        holder.notificationMessagebitmap.setImageBitmap(notification.getSmallIconBitmap());
        holder.notificationMessagebitmap.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_ATOP));

        if (notification.getBitmapLargeIcon() != null) {
            holder.notificationMessageLargeIcon.setVisibility(View.VISIBLE);
            holder.notificationMessageLargeIcon.setImageBitmap(notification.getBitmapLargeIcon());
        } else {
            holder.notificationMessageLargeIcon.setVisibility(View.GONE);
        }

        holder.notificationAppname.setText(notification.getApplicationName());
        holder.notificationpostTimeView.setText(notification.getPostTime());

        if (entry.isStack()) {
            int stackSize = entry.getNotifications().size();
            if (holder.notifyStackCount != null) {
                holder.notifyStackCount.setText(String.valueOf(stackSize));
                holder.notifyStackCount.setVisibility(View.VISIBLE);
            }
            if (holder.stackBg1 != null) {
                holder.stackBg1.setVisibility(stackSize > 1 ? View.VISIBLE : View.GONE);
            }
            if (holder.stackBg2 != null) {
                holder.stackBg2.setVisibility(stackSize > 2 ? View.VISIBLE : View.GONE);
            }
        } else {
            if (holder.notifyStackCount != null) {
                holder.notifyStackCount.setVisibility(View.GONE);
            }
            if (holder.stackBg1 != null) {
                holder.stackBg1.setVisibility(View.GONE);
            }
            if (holder.stackBg2 != null) {
                holder.stackBg2.setVisibility(View.GONE);
            }
        }
        holder.notificationTitle.setText(notification.getTitle());
        holder.notificationText.setText(notification.getText());

        //get actions
        for (TextView ActionText : holder.notificationActionText) {
            ActionText.setVisibility(View.GONE);
        }

        try {
            int positionAction = 0;

            android.app.Notification.Action[] actions = notification.getActions();
            if (actions != null) {
                for (final android.app.Notification.Action action : actions) {
                    try {
                        Log.d(TAG, "ShowActionText (" + positionAction + "): " + action.title.toString().toUpperCase());
                        holder.notificationActionText[positionAction].setVisibility(View.VISIBLE);
                        holder.notificationActionText[positionAction].setText(action.title.toString().toUpperCase());
                        holder.notificationActionText[positionAction].setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.material_amber));

                        holder.notificationActionText[positionAction].setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                Utility.sendPendingIntent(v.getContext(), action.actionIntent);
                            }
                        });
                        positionAction++;
                    } catch (Exception ex) {
                        Log.e(TAG, "ShowActionText: " + ex);
                        for (TextView ActionText : holder.notificationActionText) {
                            ActionText.setVisibility(View.GONE);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
        }
        //End Actions

        //get template style
        String[] templates = {"BigPictureStyle", "BigTextStyle", "DecoratedCustomViewStyle", "InboxStyle", "MessagingStyle"};

        String match = "default";
        for (String s : templates) {
            String temp = notification.getTemplate();
            if (temp != null && temp.contains(s)) {
                match = s;
                break;
            }
        }
        Log.d(TAG, notification.getApplicationName() + ": " + match);
        switch (match) {
            case "DecoratedCustomViewStyle":
                try {
                    View bigCardView = notification.getBigCardView();
                    if (bigCardView == null) {
                        bigCardView = notification.getCardView();
                    }
                    if (bigCardView != null) {
                        if (bigCardView.getParent() != null) {
                            ((ViewGroup) bigCardView.getParent()).removeView(bigCardView);
                        }
                        holder.notificationRemoteView.removeAllViews();
                        holder.itemView.findViewById(R.id.notify).setVisibility(View.GONE);
                        if (holder.notificationRemoteView.getChildCount() == 0) {
                            holder.notificationRemoteView.addView(bigCardView);
                        }
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "DecoratedCustomViewStyle", ex);
                }
                break;
            case "InboxStyle":
                holder.notificationText.setText(notification.getTextLines());
                break;
            case "MessagingStyle":
                holder.notificationText.setText(notification.getMessages());
                break;
            case "BigTextStyle":
                if (!notification.getTitleBig().equals("")) {
                    holder.notificationTitle.setText(notification.getTitleBig());
                } else {
                    holder.notificationTitle.setText(notification.getTitle());
                }
                if (!notification.getTextBig().isEmpty()) {
                    holder.notificationText.setText(notification.getTextBig());
                } else {
                    holder.notificationText.setText(notification.getText());
                }
                break;
            case "BigPictureStyle":
                if (!notification.getTitleBig().isEmpty()) {
                    holder.notificationTitle.setText(notification.getTitleBig());
                } else {
                    holder.notificationTitle.setText(notification.getTitle());
                }
                if (!notification.getSummaryText().isEmpty()) {
                    holder.notificationText.setText(notification.getSummaryText());
                } else {
                    holder.notificationText.setText(notification.getText());
                }

                if (notification.getBitmapLargeIcon() == null) {
                    holder.notificationMessageLargeIcon.setVisibility(View.VISIBLE);
                    holder.notificationMessageLargeIcon.setImageBitmap(notification.getBigPicture());
                }

                holder.notificationLargePicture.setVisibility(View.VISIBLE);
                holder.notificationLargePicture.setImageBitmap(notification.getBigPicture());
                holder.notificationLargePicture.bringToFront();
                break;
            case "default":
                //try to set a custom view without style
                holder.itemView.findViewById(R.id.notify_remoteview);

                for (int index = 0; index < holder.notificationRemoteView.getChildCount(); index++) {
                    View nextChild = holder.notificationRemoteView.getChildAt(index);
                    holder.notificationRemoteView.removeView(nextChild);
                }

                try {
                    View notificationCardView = notification.getCardView();
                    if (notificationCardView != null) {
                        if (notificationCardView.getParent() != null) {
                            ((ViewGroup) notificationCardView.getParent()).removeView(notificationCardView);
                        }
                        holder.notificationRemoteView.removeAllViews();
                        holder.notificationRemoteView.addView(notificationCardView);

                        holder.itemView.findViewById(R.id.notify).setVisibility(View.GONE);
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "defaultstyle: ", ex);
                }
                break;
        }//End of switch
    }

    //Returns the total number of items in the data set held by the adapter.
    @Override
    public int getItemCount() {
        return this.notificationList.size();
    }

    // Interface definition for a callback to be invoked when a view is clicked.
    private void handleRecyclerItemClick(RecyclerView recyclerView, View itemView) {
        int itemPosition = recyclerView.getChildLayoutPosition(itemView);
        Notification notification = this.notificationList.get(itemPosition);
        if (notification.getPendingIntent() != null) {
            Utility.sendPendingIntent(recyclerView.getContext(), notification.getPendingIntent());
        }
    }

    public ArrayList<Notification> getSelectedNotifications() {
        ArrayList<Notification> notifications = new ArrayList<>();
        for (int index = 0; index < notificationList.size(); index++) {
            if (notificationList.isSelected(index)) {
                notifications.addAll(notificationList.getEntry(index).getNotifications());
            }
        }
        return notifications;
    }
}
