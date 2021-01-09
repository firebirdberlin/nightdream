package com.firebirdberlin.nightdream.NotificationList;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
    TextView [] notificationActionText;

    public NotificationViewHolder(@NonNull View itemView) {
        super(itemView);

        this.notificationpostTimeView = itemView.findViewById(R.id.notify_timestamp);
        this.notificationRemoteView = itemView.findViewById(R.id.notify_remoteview);
        this.itemView = itemView;
        this.notificationMessagebitmap = itemView.findViewById(R.id.notify_smallicon);
        this.notificationMessageLargeIcon= itemView.findViewById(R.id.notify_largeicon);
        this.notificationLargePicture = itemView.findViewById(R.id.notify_largepicture);
        this.notificationAppname = itemView.findViewById(R.id.notify_appname);
        this.notificationTitle = itemView.findViewById(R.id.notify_title);
        this.notificationText = itemView.findViewById(R.id.notify_text);

        this.notificationActions = itemView.findViewById(R.id.notify_actions_images);

        this.notificationActionText = new TextView[3];
        this.notificationActionText[0] = itemView.findViewById(R.id.notify_actiontext1);
        this.notificationActionText[1] = itemView.findViewById(R.id.notify_actiontext2);
        this.notificationActionText[2] = itemView.findViewById(R.id.notify_actiontext3);
    }
}