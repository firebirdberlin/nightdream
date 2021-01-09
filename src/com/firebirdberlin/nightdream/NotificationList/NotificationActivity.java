package com.firebirdberlin.nightdream.NotificationList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.mNotificationListener;


public class NotificationActivity  extends AppCompatActivity {

    public static String TAG = "NotificationListActivity";
    private RecyclerView recyclerView;
    private CustomRecyclerViewAdapter adapter;
    List<Notification> notificationlist = new ArrayList<>();
    notificationshowlist notificationshowlist;
    String packagename;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if(intent != null) {
            packagename = intent.getStringExtra("packagename");
        }

        Log.d(TAG,"NotificationActivity started");
        notificationshowlist = new notificationshowlist(notificationlist, getApplicationContext());

        setContentView(R.layout.notification_list_layout);

        //Adapter init
        adapter = new CustomRecyclerViewAdapter(this, notificationlist);

        //recycleview init
        this.recyclerView = this.findViewById(R.id.recycleview);
        recyclerView.setAdapter(adapter);

        // RecyclerView scroll vertical
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);

        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, new IntentFilter("Notification.Action.notificationlist"));

        Intent intentstart = new Intent(getApplicationContext(), mNotificationListener.class);
        intentstart.putExtra("command", "getnotificationlist");
        //starting service
        startService(intentstart);
    }

    private BroadcastReceiver onNotice= new BroadcastReceiver() {
        @Override

        public void onReceive(Context context, Intent intent) {

            Log.d(TAG,"NotActivity - Broadcastreceiver ");

            if ( intent.hasExtra("notificationlist")) {
                notificationlist = intent.getParcelableArrayListExtra("notificationlist");
                notificationshowlist.change_notificationshowlist(notificationlist, packagename);

                adapter.updateData(notificationshowlist.get_notificationshowlist());
            }
        }
    };

    @Override
    public void onDestroy() {

        try {
            if(onNotice!=null)
                unregisterReceiver(onNotice);
        }
        catch (Exception ex ){}

        super.onDestroy();
    }

}
