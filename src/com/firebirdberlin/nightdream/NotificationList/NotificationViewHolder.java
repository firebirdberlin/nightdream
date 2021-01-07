package com.firebirdberlin.nightdream.NotificationList;


import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebirdberlin.nightdream.R;

public class NotificationViewHolder extends RecyclerView.ViewHolder {

    ImageView notificationMessagebitmap;
    ImageView notificationMessageLargeIcon;
    ImageView notificationLargePicture;
    TextView notificationpostTimeView;
    TextView notificationAppname;
    TextView notificationTitle;
    TextView notificationText;
    FrameLayout notificationRemoteView;
    FrameLayout notificationActions;
    View itemView;
    ImageView [] notificationActionImages;
    TextView [] notificationActionText;

    public NotificationViewHolder(@NonNull View itemView) {
        super(itemView);

        this.notificationpostTimeView = (TextView) itemView.findViewById(R.id.notify_timestamp);
        this.notificationRemoteView =  (FrameLayout) itemView.findViewById(R.id.notify_remoteview);
        this.itemView = (View) itemView;
        this.notificationMessagebitmap = (ImageView) itemView.findViewById(R.id.notify_smallicon);
        this.notificationMessageLargeIcon= (ImageView) itemView.findViewById(R.id.notify_largeicon);
        this.notificationLargePicture = (ImageView) itemView.findViewById(R.id.notify_largepicture);
        this.notificationAppname = (TextView) itemView.findViewById(R.id.notify_appname);
        this.notificationTitle = (TextView) itemView.findViewById(R.id.notify_title);
        this.notificationText = (TextView) itemView.findViewById(R.id.notify_text);

        this.notificationActions = (FrameLayout) itemView.findViewById(R.id.notify_actions_images);

        this.notificationActionText = new TextView[3];
        this.notificationActionText[0] = (TextView) itemView.findViewById(R.id.notify_actiontext1);
        this.notificationActionText[1] = (TextView) itemView.findViewById(R.id.notify_actiontext2);
        this.notificationActionText[2] = (TextView) itemView.findViewById(R.id.notify_actiontext3);
    }
}