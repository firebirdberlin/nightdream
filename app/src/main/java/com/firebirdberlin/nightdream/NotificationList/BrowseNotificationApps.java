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

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebirdberlin.nightdream.R;

import java.util.List;

public class BrowseNotificationApps extends RecyclerView.Adapter<NotificationAppsViewHolder> {

    public static String TAG = "BrowseNotificationApps";

    private List<NotificationApp> notificationAppList;
    private final Activity context;

    public BrowseNotificationApps(Activity context, List<NotificationApp> notificationAppList) {
        this.context = context;
        this.notificationAppList = notificationAppList;
    }

    @NonNull
    @Override
    public NotificationAppsViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {

        Log.d(TAG, "onCreateViewHolder started");

        // Inflate view from notify_item_layout.xml
        View recyclerViewItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_list_apps, parent, false);

        final NotificationAppsViewHolder vh = new NotificationAppsViewHolder(recyclerViewItem);

        vh.item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String packageName = (String) v.getTag();
                if (packageName != null) {
                    Log.d(TAG, "onClick");
                    Intent intent = new Intent(context, NotificationActivity.class);
                    intent.putExtra("packageName", packageName);
                    intent.putExtra("name", vh.name.getText());
                    context.startActivityForResult(intent, 1);
                }
            }
        });

        return new NotificationAppsViewHolder(recyclerViewItem);
    }

    //to update the Data
    public void updateData(List<NotificationApp> notificationApps) {
        notificationAppList = notificationApps;

        //Notifies the attached observers that the underlying data has been changed and any View reflecting the data set should refresh itself.
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(NotificationAppsViewHolder holder, int position) {

        // get notification in notificationlist via position
        final NotificationApp app = this.notificationAppList.get(position);

        // Bind data to viewholder
        holder.item.setTag(app.getPackageName());

        //get application packageName
        final PackageManager pm = context.getApplicationContext().getPackageManager();
        ApplicationInfo ai;

        try {
            ai = pm.getApplicationInfo(app.getPackageName(), 0);
            if (ai != null) {
                holder.appicon.setImageDrawable(pm.getApplicationIcon(ai));
            }
        } catch (PackageManager.NameNotFoundException ex) {
            Log.e(TAG, "Appicon not found. ", ex);
            holder.appicon.setImageResource(R.mipmap.ic_launcher);
        }

        holder.name.setVisibility(View.VISIBLE);
        holder.name.setText(app.getName());
        holder.time.setText(app.getPostTime());
    }

    @Override
    public int getItemCount() {
        if (this.notificationAppList == null) {
            return 0;
        } else {
            return this.notificationAppList.size();
        }

    }

}