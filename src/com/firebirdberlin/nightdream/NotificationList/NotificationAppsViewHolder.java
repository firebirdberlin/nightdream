package com.firebirdberlin.nightdream.NotificationList;


import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebirdberlin.nightdream.R;


public class NotificationAppsViewHolder extends RecyclerView.ViewHolder {

    public ImageView appicon;
    public LinearLayout item;
    public TextView name;
    public TextView time;

    public NotificationAppsViewHolder(@NonNull View itemView) {
        super(itemView);
        appicon = itemView.findViewById(R.id.icon);
        item = itemView.findViewById(R.id.item);
        name = itemView.findViewById(R.id.name);
        time = itemView.findViewById(R.id.text);
    }
}
