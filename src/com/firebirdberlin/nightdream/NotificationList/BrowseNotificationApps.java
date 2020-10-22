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

    private List<NotificationApp> notificationapplist;
    private Activity context;

    public BrowseNotificationApps(Activity context, List<NotificationApp> datas) {
        this.context = context;
        this.notificationapplist = datas;
    }

    @NonNull
    @Override
    public NotificationAppsViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {

        Log.d(TAG, "onCreateViewHolder started");

        // Inflate view from notify_item_layout.xml
        View recyclerViewItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_list_apps, parent, false);

        NotificationAppsViewHolder vh = new NotificationAppsViewHolder(recyclerViewItem);


        vh.item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String packagename = (String) v.getTag();

                if (packagename != null) {
                    Log.d(TAG, "onClick");
                    Intent intent = new Intent(context, NotificationActivity.class);
                    intent.putExtra("packagename", packagename);
                    context.startActivityForResult(intent, 1);
                }
            }
        });

        return new NotificationAppsViewHolder(recyclerViewItem);
    }

    //to update the Data
    public void updateData(List<NotificationApp> viewnotificationapplist) {
        notificationapplist = viewnotificationapplist;

        //Notifies the attached observers that the underlying data has been changed and any View reflecting the data set should refresh itself.
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(NotificationAppsViewHolder holder, int position) {

        // get notification in notificationlist via position
        final NotificationApp notifyapp = this.notificationapplist.get(position);

        // Bind data to viewholder
        holder.item.setTag("" + notifyapp.get_notificationapp_package());

        //get application packageName
        final PackageManager pm = context.getApplicationContext().getPackageManager();
        ApplicationInfo ai;

        try {
            ai = pm.getApplicationInfo(notifyapp.get_notificationapp_package(), 0);
            if(ai != null) {
                holder.appicon.setImageDrawable( pm.getApplicationIcon(ai));
            }
        } catch (PackageManager.NameNotFoundException ex) {
            Log.e(TAG, "Appicon not found. ", ex);
            holder.appicon.setImageResource(R.mipmap.ic_launcher);
        }

        holder.name.setVisibility(View.VISIBLE);
        holder.name.setText(notifyapp.get_notificationapp_name());
        holder.time.setText(notifyapp.get_notificationapp_time());
    }

    @Override
    public int getItemCount() {
        if (this.notificationapplist == null) {
            return 0;
        }
        else {
            return this.notificationapplist.size();
        }

    }

}