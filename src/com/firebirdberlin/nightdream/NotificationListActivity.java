package com.firebirdberlin.nightdream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebirdberlin.nightdream.NotificationList.BrowseNotificationApps;
import com.firebirdberlin.nightdream.NotificationList.NotificationApp;
import com.firebirdberlin.nightdream.NotificationList.NotificationAppList;

import java.util.ArrayList;
import java.util.List;

public class NotificationListActivity extends AppCompatActivity {

    public static String TAG = "NotificationListActivity";
    List<NotificationApp> notificationApps = new ArrayList<>();
    NotificationAppList notificationAppList;
    private BrowseNotificationApps adapter;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive");

            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                for (String key : bundle.keySet()) {
                    Log.d(TAG, key + " : " + (bundle.get(key) != null ? bundle.get(key) : "NULL"));
                }
            }

            notificationApps = intent.getParcelableArrayListExtra("notificationApps");
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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
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

        LocalBroadcastManager.getInstance(this).registerReceiver(
                broadcastReceiver, new IntentFilter(Config.ACTION_NOTIFICATION_APPS_LISTENER)
        );

        mNotificationListener.requestNotificationList(getApplicationContext());
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
        getMenuInflater().inflate(R.menu.notification_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menuItem_delete_all) {
            Intent i = new Intent(Config.ACTION_NOTIFICATION_LISTENER);
            i.putExtra("command", "clearall");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
        }

        return super.onOptionsItemSelected(item);
    }

}