package com.firebirdberlin.nightdream.NotificationList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.mNotificationListener;

import java.util.ArrayList;
import java.util.List;


public class NotificationActivity extends AppCompatActivity {

    public static String TAG = "NotificationListActivity";
    List<Notification> notifications = new ArrayList<>();
    NotificationShowList notificationshowlist;
    String packageName;
    private NotificationRecyclerViewAdapter adapter;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "NotActivity - Broadcastreceiver ");

            if (intent.hasExtra("notifications")) {
                notifications = intent.getParcelableArrayListExtra("notifications");
                notificationshowlist.replace(notifications, packageName);

                adapter.updateData(notificationshowlist.get());
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null) {
            packageName = intent.getStringExtra("packageName");
            String name = intent.getStringExtra("name");
            ActionBar ab = getSupportActionBar();
            if (ab != null && name != null) {
                ab.setTitle(name);
                //ab.setSubtitle(subtitle);
            }
        }

        Log.d(TAG, "NotificationActivity started");
        notificationshowlist = new NotificationShowList(notifications, getApplicationContext());

        setContentView(R.layout.notification_list_layout);

        adapter = new NotificationRecyclerViewAdapter(notifications);
        RecyclerView recyclerView = this.findViewById(R.id.recyclerView);
        recyclerView.setAdapter(adapter);

        // RecyclerView scroll vertical
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false
        );
        recyclerView.setLayoutManager(linearLayoutManager);

        LocalBroadcastManager.getInstance(this).registerReceiver(
                broadcastReceiver, new IntentFilter("Notification.Action.notificationList")
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mNotificationListener.requestNotificationList(getApplicationContext());
        }
    }

    @Override
    public void onDestroy() {
        try {
            if (broadcastReceiver != null) {
                unregisterReceiver(broadcastReceiver);
            }
        } catch (IllegalArgumentException ignored) {
        }

        super.onDestroy();
    }

}
