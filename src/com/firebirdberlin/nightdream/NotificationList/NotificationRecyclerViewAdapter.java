package com.firebirdberlin.nightdream.NotificationList;

import android.app.PendingIntent;
import android.content.IntentSender;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.firebirdberlin.nightdream.R;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationRecyclerViewAdapter extends RecyclerView.Adapter<NotificationViewHolder> {
    public static String TAG = "CustomRecyclerViewAdapter";
    private final int[] colorResId = new int[]{
            R.color.white,
            R.color.material_light_blue_grey
    };
    NotificationList notificationList;

    public NotificationRecyclerViewAdapter(NotificationList notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        Log.d(TAG, "BrowseNotification started");

        final View recyclerViewItem = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.notification_list_item_view, parent, false
        );

        //Interface definition for a callback to be invoked when a view is clicked.
        recyclerViewItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    handleRecyclerItemClick((RecyclerView) parent, v);
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
                cardView.setBackgroundResource(colorResId[isSelected ? 0 : 1]);
                notificationList.setSelected(index, !isSelected);

                return true;
            }
        });
        return new NotificationViewHolder(recyclerViewItem);
    }

    public void removeNotification(int position, NotificationList oldNotificationList) {
        Log.d(TAG, "removeNotification");
        this.notificationList.remove(position);
        //Notifies the attached observers that the underlying data has been changed and any View reflecting the data set should refresh itself.
        //notifyDataSetChanged();

        updateDataSet(oldNotificationList, this.notificationList);
    }

    public void updateDataSet(NotificationList oldNotificationList, NotificationList newNotificationList) {
        final NotificationDiffCallback diffCallback = new NotificationDiffCallback(oldNotificationList, newNotificationList);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        diffResult.dispatchUpdatesTo(this);
    }

    //Called by RecyclerView to display the data at the specified position.
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onBindViewHolder(NotificationViewHolder holder, int position) {

        final Notification notification = this.notificationList.get(position);
        holder.itemView.setTag("" + position);

        // Bind data to viewholder
        holder.notificationLargePicture.setVisibility(View.GONE);

        //Layout without style / Remoteview supported by Notification?

        holder.notificationRemoteView.removeAllViews();
        holder.itemView.findViewById(R.id.notify).setVisibility(View.VISIBLE);

        View cardView = holder.itemView.findViewById(R.id.notify);
        boolean isSelected = notificationList.isSelected(position);
        cardView.setBackgroundResource(colorResId[isSelected ? 1 : 0]);

        holder.notificationMessagebitmap.setImageBitmap(notification.getSmallIconBitmap());

        if (notification.getColor() != 0) {
            holder.notificationMessagebitmap.setColorFilter(notification.getColor(), PorterDuff.Mode.SRC_ATOP);
        } else {
            holder.notificationMessagebitmap.setColorFilter(Color.parseColor("#000000"), PorterDuff.Mode.SRC_ATOP);
        }

        if (notification.getBitmapLargeIcon() != null) {
            holder.notificationMessageLargeIcon.setVisibility(View.VISIBLE);
            holder.notificationMessageLargeIcon.setImageBitmap(notification.getBitmapLargeIcon());
        } else {
            holder.notificationMessageLargeIcon.setVisibility(View.GONE);
        }

        holder.notificationAppname.setText(notification.getApplicationName());
        holder.notificationpostTimeView.setText(notification.getPostTime());
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
                        holder.notificationActionText[positionAction].setTextColor(Color.rgb(0, 0, 200));

                        holder.notificationActionText[positionAction].setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                try {
                                    action.actionIntent.send();
                                } catch (Exception ex) {
                                    Log.e(TAG, ex.toString());
                                }
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

        switch (match) {
            case "DecoratedCustomViewStyle":
                try {
                    View bigCardView = notification.getBigCardView();
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
    private void handleRecyclerItemClick(RecyclerView recyclerView, View itemView) throws IntentSender.SendIntentException {
        int itemPosition = recyclerView.getChildLayoutPosition(itemView);
        Notification notification = this.notificationList.get(itemPosition);
        try {
            if (notification.getPendingIntent() != null) {
                notification.getPendingIntent().send();
            }
        } catch (PendingIntent.CanceledException e) {
            throw new IntentSender.SendIntentException(e);
        }
    }

    public ArrayList<Notification> getSelectedNotifications() {
        ArrayList<Notification> notifications = new ArrayList<>();
        for (int index = 0; index < notificationList.size(); index++) {
            if (notificationList.isSelected(index)) {
                notifications.add(notificationList.get(index));
            }
        }
        return notifications;
    }
}