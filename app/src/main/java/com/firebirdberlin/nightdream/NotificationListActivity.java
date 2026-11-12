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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebirdberlin.nightdream.NotificationList.BrowseNotificationApps;
import com.firebirdberlin.nightdream.NotificationList.NotificationApp;
import com.firebirdberlin.nightdream.NotificationList.NotificationAppList;
import com.firebirdberlin.nightdream.events.AppEventBus;
import com.firebirdberlin.nightdream.events.OnNotificationAppsUpdated;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NotificationListActivity extends AppCompatActivity {

    private static final String TAG = NotificationListActivity.class.getSimpleName();
    List<NotificationApp> notificationApps = new ArrayList<>();
    NotificationAppList notificationAppList;
    private BrowseNotificationApps adapter;
    private final Consumer<Object> eventListener = event -> {
        if (event instanceof OnNotificationAppsUpdated updatedEvent) {
            notificationApps = updatedEvent.notificationApps;
            if (notificationApps != null) {
                notificationAppList.replace(notificationApps);
            }
            adapter.updateData(notificationAppList.get());
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

        Log.d(TAG, "NotifyListener started");

        notificationAppList = new NotificationAppList(notificationApps);
        setContentView(R.layout.notification_list_layout);

        adapter = new BrowseNotificationApps(this, notificationApps);

        RecyclerView recyclerView = this.findViewById(R.id.recyclerView);
        recyclerView.setAdapter(adapter);

        // RecyclerView scroll vertical
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.notification_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menuItem_delete_all) {
            MNotificationListener.clearAllNotifications();
        }

        return super.onOptionsItemSelected(item);
    }

}