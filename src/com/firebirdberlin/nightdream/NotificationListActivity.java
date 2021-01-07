package com.firebirdberlin.nightdream;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.widget.Toast;

import com.firebirdberlin.nightdream.NotificationList.BrowseNotificationApps;
import com.firebirdberlin.nightdream.NotificationList.CustomRecyclerViewAdapter;
import com.firebirdberlin.nightdream.NotificationList.Notification;
import com.firebirdberlin.nightdream.NotificationList.NotificationApp;
import com.firebirdberlin.nightdream.NotificationList.NotificationAppShowList;

import java.util.ArrayList;
import java.util.List;

public class NotificationListActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener{

    public static String TAG = "NotificationListActivity";
    private RecyclerView recyclerView;
    private BrowseNotificationApps adapter;

    private static Context context;
    private SharedPreferences sharedPreferences;

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

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        isaccessgranted();

        //Adapter init
        //adapter = new CustomRecyclerViewAdapter(this, notificationlist);
        adapter = new BrowseNotificationApps(this, notificationapplist);

        //recycleview init
        this.recyclerView = (RecyclerView) this.findViewById(R.id.recycleview);
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
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void isaccessgranted() {
        NotificationManager n = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (!n.isNotificationPolicyAccessGranted()) {

            new AlertDialog.Builder(NotificationListActivity.this)
                    .setTitle("Notification access")
                    .setMessage("Notification access not granted.")

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
        else {
            return;
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

            if ( intent.hasExtra("serviceMessage")) {
                String message = intent.getStringExtra("serviceMessage");
                Toast.makeText(NotificationListActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            notificationapplist = intent.getParcelableArrayListExtra("notificationapplist");
            // Toast.makeText(MainActivity.this, "test:" + notificationapplist, Toast.LENGTH_SHORT).show();
            if (notificationapplist != null) {
                notificationappshowlist.change_notificationsapphowlist(notificationapplist);
            }

            adapter.updateData(notificationappshowlist.get_notificationappshowlist());
        }
    };

    public static Context getContext() {
        return NotificationListActivity.context;
    }

     //listen for changes in SharedPreferences
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        switch (key) {
            case "showongoing":
            case "shownotclearable":
                notificationappshowlist.change_notificationsapphowlist(notificationapplist);
                break;
        }

        //adapter.updateData(notificationshowlist.get_notificationshowlist());
        adapter.updateData(notificationappshowlist.get_notificationappshowlist());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Toast.makeText(this, "Return to Sender", Toast.LENGTH_SHORT).show();
       // Intent intent = new Intent(getApplicationContext(), NotificationService.class);
        Intent intent = new Intent(getApplicationContext(), mNotificationListener.class);
        intent.putExtra("command", "getnotificationapplist");
        //starting service
        startService(intent);
    }

    @Override
    public void onDestroy() {

        try{
            if(onNotice!=null)
                unregisterReceiver(onNotice);

        }catch(Exception e){}

        super.onDestroy();
    }


}
