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
    TextView[] notificationActionText;

    public NotificationViewHolder(@NonNull View itemView) {
        super(itemView);

        this.notificationpostTimeView = itemView.findViewById(R.id.notify_timestamp);
        this.notificationRemoteView = itemView.findViewById(R.id.notify_remoteview);
        this.itemView = itemView;
        this.notificationMessagebitmap = itemView.findViewById(R.id.notify_smallicon);
        this.notificationMessageLargeIcon = itemView.findViewById(R.id.notify_largeicon);
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