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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.mNotificationListener;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    public static String TAG = "NotificationActivity";
    NotificationList notificationList = new NotificationList();
    String packageName;
    private NotificationRecyclerViewAdapter adapter;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive");

            if (intent.hasExtra("notifications")) {
                List<Notification> notifications = intent.getParcelableArrayListExtra("notifications");

                NotificationList oldNotificationList = new NotificationList(
                        notificationList.getNotifications()
                );

                if (notifications != null) {
                    notificationList.replace(notifications, packageName);
                }
                adapter.updateDataSet(oldNotificationList, notificationList);
            }
        }
    };

    ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

        @Override
        public boolean onMove(
                @NonNull RecyclerView recyclerView,
                @NonNull RecyclerView.ViewHolder viewHolder,
                @NonNull RecyclerView.ViewHolder target
        ) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
            int position = viewHolder.getBindingAdapterPosition();
            deleteItem(position);
        }
    };

    private void setScreenOrientation(int orientation) {
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        } else {
            setRequestedOrientation(orientation);
        }
    }

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
        setContentView(R.layout.notification_list_layout);

        adapter = new NotificationRecyclerViewAdapter(notificationList);
        RecyclerView recyclerView = this.findViewById(R.id.recyclerView);
        recyclerView.setAdapter(adapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false
        );
        recyclerView.setLayoutManager(linearLayoutManager);

        LocalBroadcastManager.getInstance(this).registerReceiver(
                broadcastReceiver, new IntentFilter("Notification.Action.notificationList")
        );

        mNotificationListener.requestNotificationList(getApplicationContext());
        Settings settings = new Settings(getApplicationContext());
        setScreenOrientation(settings.screenOrientation);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.notification_list_submenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.menuItem_markall:

                if (notificationList != null) {
                    NotificationList oldNotificationList = new NotificationList(
                            notificationList.getNotifications()
                    );
                    for (int index = 0; index < notificationList.size(); index++) {
                        oldNotificationList.setSelected(index, notificationList.isSelected(index));
                        notificationList.setSelected(index, true);
                    }
                    //adapter.notifyDataSetChanged();
                    adapter.updateDataSet(oldNotificationList, notificationList);
                }
                break;
            case R.id.menuItem_delete:
                clearSelectedNotifications();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void clearSelectedNotifications() {
        if (notificationList == null) {
            return;
        }
        ArrayList<String> notificationsKeys = new ArrayList<>();
        ArrayList<Notification> selectedNotifications = new ArrayList<>(adapter.getSelectedNotifications());
        for (Notification notification : selectedNotifications) {
            notificationsKeys.add(notification.getNotificationKey());
        }

        Intent i = new Intent(Config.ACTION_NOTIFICATION_LISTENER);
        i.putExtra("command", "clear");
        i.putExtra("notificationKeys", notificationsKeys);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
    }

    private void deleteItem(int position) {
        if (notificationList == null || position < 0) {
            return;
        }
        ArrayList<String> notificationKeys = new ArrayList<>();

        Notification notification = notificationList.get(position);
        Log.d(TAG, "remove swipe: " + position);
        notificationKeys.add(notification.getNotificationKey());
        NotificationList oldNotificationList = new NotificationList(
                notificationList.getNotifications()
        );
        adapter.removeNotification(position, oldNotificationList);

        Intent i = new Intent(Config.ACTION_NOTIFICATION_LISTENER);
        i.putExtra("command", "clear");
        i.putExtra("notificationKeys", notificationKeys);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
    }
}
