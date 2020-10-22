package com.firebirdberlin.nightdream;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.widget.Toast;

import com.firebirdberlin.nightdream.NotificationList.BrowseNotificationApps;
import com.firebirdberlin.nightdream.NotificationList.NotificationApp;
import com.firebirdberlin.nightdream.NotificationList.NotificationAppShowList;

import java.util.ArrayList;
import java.util.List;

public class NotificationListActivity extends AppCompatActivity {

    public static String TAG = "NotificationListActivity";
    private RecyclerView recyclerView;
    private BrowseNotificationApps adapter;

    private static Context context;

    List<NotificationApp> notificationapplist = new ArrayList<>();
    NotificationAppShowList notificationappshowlist;

    public static void start(Context context) {
        Intent intent = new Intent(context, NotificationListActivity.class);
        context.startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG,"NotifyListener started");

        context = getApplicationContext();

        notificationappshowlist = new NotificationAppShowList(notificationapplist, context);

        setContentView(R.layout.notification_list_layout);

        isaccessgranted();

        //Adapter init
        adapter = new BrowseNotificationApps(this, notificationapplist);

        //recycleview init
        this.recyclerView = this.findViewById(R.id.recycleview);
        recyclerView.setAdapter(adapter);

        // RecyclerView scroll vertical
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);

        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, new IntentFilter("Msg"));

        //Intent intent = new Intent(getApplicationContext(), NotificationService.class);
        Intent intent = new Intent(getApplicationContext(), mNotificationListener.class);
        intent.putExtra("command", "getnotificationapplist");
        //starting service
        startService(intent);
        Log.d(TAG,"getnotificationapplist intent started");
    }

    //access granted to notification policity
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void isaccessgranted() {

        Log.d(TAG, "isaccessgranted");
        boolean isGranted;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager n = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            isGranted = n.isNotificationPolicyAccessGranted();
        }
        else {
            isGranted = mNotificationListener.running;
        }

        if (!isGranted) {
            new AlertDialog.Builder(NotificationListActivity.this)
                    .setTitle(R.string.showNotificationsAccessNotGranted)
                    .setMessage(R.string.showNotificationsAlertText)

                    // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            startActivityForResult(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS), 0);
                        }
                    })

                    // A null listener allows the button to dismiss the dialog and take no further action.
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    private BroadcastReceiver onNotice= new BroadcastReceiver() {
        @Override

        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"Broadcastreceiver");

            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                for (String key : bundle.keySet()) {
                    Log.d(TAG, key + " : " + (bundle.get(key) != null ? bundle.get(key) : "NULL"));
                }
            }

            notificationapplist = intent.getParcelableArrayListExtra("notificationapplist");
            if (notificationapplist != null) {
                notificationappshowlist.change_notificationsapphowlist(notificationapplist);
            }

            adapter.updateData(notificationappshowlist.get_notificationappshowlist());
        }
    };

    public static Context getContext() {
        return NotificationListActivity.context;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Intent intent = new Intent(getApplicationContext(), mNotificationListener.class);
        intent.putExtra("command", "getnotificationapplist");
        //starting service
        startService(intent);
    }

    @Override
    public void onDestroy() {

        try {
            if (onNotice != null)
                unregisterReceiver(onNotice);
        }catch (Exception ex){}

        super.onDestroy();
    }

}