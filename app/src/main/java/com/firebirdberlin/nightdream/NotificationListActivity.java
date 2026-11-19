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

package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebirdberlin.nightdream.NotificationList.Notification;
import com.firebirdberlin.nightdream.NotificationList.NotificationList;
import com.firebirdberlin.nightdream.NotificationList.NotificationRecyclerViewAdapter;
import com.firebirdberlin.nightdream.events.AppEventBus;
import com.firebirdberlin.nightdream.events.OnNotificationsUpdated;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NotificationListActivity extends AppCompatActivity {

    private static final String TAG = NotificationListActivity.class.getSimpleName();

    private final NotificationList notificationList = new NotificationList();
    private NotificationRecyclerViewAdapter adapter;

    private final Consumer<Object> eventListener = event -> {
        if (event instanceof OnNotificationsUpdated updatedEvent) {
            List<Notification> notifications = updatedEvent.notifications;
            Log.d(TAG, "Received notifications size: " + (notifications != null ? notifications.size() : "null"));

            NotificationList oldNotificationList = new NotificationList(
                    notificationList.getNotifications()
            );

            if (notifications != null) {
                notificationList.replace(notifications, null);
            }
            Log.d(TAG, "Filtered notificationList size: " + notificationList.size());
            adapter.updateDataSet(oldNotificationList, notificationList);
        }
    };

    private final ItemTouchHelper.SimpleCallback simpleItemTouchCallback =
            new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

        @Override
        public boolean onMove(
                @NonNull RecyclerView recyclerView,
                @NonNull RecyclerView.ViewHolder viewHolder,
                @NonNull RecyclerView.ViewHolder target
        ) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int swipeDir) {
            int position = viewHolder.getBindingAdapterPosition();
            deleteItem(position);
        }
    };

    public static void start(Context context) {
        Intent intent = new Intent(context, NotificationListActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);

        Log.d(TAG, "NotificationListActivity started");
        setContentView(R.layout.notification_list_layout);

        adapter = new NotificationRecyclerViewAdapter(notificationList);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setAdapter(adapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false
        );
        recyclerView.setLayoutManager(linearLayoutManager);

        AppEventBus.addListener(eventListener);

        MNotificationListener.requestNotificationList(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        AppEventBus.removeListener(eventListener);
        super.onDestroy();
    }

    private static final int MENU_ITEM_PREFERENCES = 3000;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.notification_list_menu, menu);
        menu.add(Menu.NONE, MENU_ITEM_PREFERENCES, Menu.NONE, R.string.preferences)
                .setIcon(R.drawable.ic_settings)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == MENU_ITEM_PREFERENCES) {
            NotificationPreferencesActivity.start(this);
            return true;
        } else if (id == R.id.menuItem_markall) {
            if (notificationList != null) {
                NotificationList oldNotificationList = new NotificationList(
                        notificationList.getNotifications()
                );
                for (int index = 0; index < notificationList.size(); index++) {
                    oldNotificationList.setSelected(index, notificationList.isSelected(index));
                    notificationList.setSelected(index, true);
                }
                adapter.updateDataSet(oldNotificationList, notificationList);
            }
            return true;
        } else if (id == R.id.menuItem_delete) {
            clearSelectedNotifications();
            return true;
        } else if (id == R.id.menuItem_delete_all) {
            MNotificationListener.clearAllNotifications();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void clearSelectedNotifications() {
        if (notificationList == null) return;

        ArrayList<String> notificationsKeys = new ArrayList<>();
        ArrayList<Notification> selectedNotifications = new ArrayList<>(adapter.getSelectedNotifications());
        for (Notification notification : selectedNotifications) {
            notificationsKeys.add(notification.getNotificationKey());
        }

        MNotificationListener.clearNotifications(notificationsKeys);
    }

    private void deleteItem(int position) {
        if (notificationList == null || position < 0) return;

        ArrayList<String> notificationKeys = new ArrayList<>();
        Notification notification = notificationList.get(position);
        Log.d(TAG, "remove swipe: " + position);
        notificationKeys.add(notification.getNotificationKey());

        NotificationList oldNotificationList = new NotificationList(
                notificationList.getNotifications()
        );
        adapter.removeNotification(position, oldNotificationList);

        MNotificationListener.clearNotifications(notificationKeys);
    }
}
