package com.firebirdberlin.nightdream.NotificationList;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.graphics.drawable.BitmapDrawable;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.firebirdberlin.nightdream.NotificationList.checked;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.mNotificationListener;


public class NotificationActivity  extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener{

    public static String TAG = "NotificationListActivity";
    private RecyclerView recyclerView;
    private CustomRecyclerViewAdapter adapter;
    List<Notification> notificationlist = new ArrayList<Notification>();
    ArrayList<checked> selected = new ArrayList<>();
    notificationshowlist notificationshowlist;
    String packagename;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if(intent != null) {
            packagename = intent.getStringExtra("packagename");
        }

        //Toast.makeText(this, "OnClick: "+packagename , Toast.LENGTH_LONG).show();

        Log.d(TAG,"NotificationActivity started");
        notificationshowlist = new notificationshowlist(notificationlist, getApplicationContext());

        setContentView(R.layout.notification_list_layout);

        //Adapter init
        adapter = new CustomRecyclerViewAdapter(this, notificationlist);

        //recycleview init
        this.recyclerView = (RecyclerView) this.findViewById(R.id.recycleview);
        recyclerView.setAdapter(adapter);

        // RecyclerView scroll vertical
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);

        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, new IntentFilter("Notification.Action.notificationlist"));

        //Intent intentstart = new Intent(getApplicationContext(), NotificationService.class);
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
                //Toast.makeText(NotificationActivity.this, "test:" + notificationlist, Toast.LENGTH_SHORT).show();
                notificationshowlist.change_notificationshowlist(notificationlist, packagename);
                createChecked();

                adapter.updateData(notificationshowlist.get_notificationshowlist());
            }
        }
    };

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

    }



    private void createChecked() {
        if (selected.size() > 0 ){
            checked select = new checked();
            selected.add(0,select);
        }
        else{
            for (int i = 0; i < notificationshowlist.get_notificationshowlist().size(); i++) {
                checked select = new checked();
                selected.add(select);
            }
        }
        adapter.setChecked(selected);
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
