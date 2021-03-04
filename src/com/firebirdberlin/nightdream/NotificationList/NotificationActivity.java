package com.firebirdberlin.nightdream.NotificationList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NavUtils;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.mNotificationListener;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    public static String TAG = "NotificationActivity";
    List<Notification> notifications = new ArrayList<>();
    ArrayList<NotificationChecked> selected = new ArrayList<>();
    NotificationShowList notificationList;
    String packageName;
    private NotificationRecyclerViewAdapter adapter;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive");

            if (intent.hasExtra("notifications")) {
                notifications = intent.getParcelableArrayListExtra("notifications");
                if (notifications != null) {
                    notificationList.replace(notifications, packageName);
                }
                if (!intent.hasExtra("removed")) {
                    createChecked();
                }

                adapter.updateData(notificationList.get());
            }
        }
    };

    ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
            //Remove swiped item
            int position = viewHolder.getAdapterPosition();
            deleteItem(position);
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
        notificationList = new NotificationShowList(notifications, getApplicationContext());

        setContentView(R.layout.notification_list_layout);

        adapter = new NotificationRecyclerViewAdapter(notifications);
        RecyclerView recyclerView = this.findViewById(R.id.recyclerView);
        recyclerView.setAdapter(adapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

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

    //Optionsmenu inflate
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.notification_list_submenu, menu);
        return true;
    }

    //item selected in options menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.menuItem_markall:
                if (notificationList != null) {
                    ArrayList<NotificationChecked> get_select = adapter.get_selected();

                    for (int index = 0; index < get_select.size(); index++) {
                        get_select.get(index).setChecked(true);
                    }
                    adapter.set_selected(get_select);
                    adapter.updateData(notificationList.get_notificationshowlist());
                }
                break;
            case R.id.menuItem_delete:
                deleteItem();
                break;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void deleteItem() {
        deleteItem(-1);
    }

    private void deleteItem(int position) {
        if (notificationList != null) {
            List<Notification> Notificationsl = notificationList.get_notificationshowlist();
            ArrayList<String> to_delete = new ArrayList<>();

            if (position == -1) {
                Log.d(TAG, "remove selected");
                ArrayList<NotificationChecked> get_selected = new ArrayList<>(adapter.get_selected());

                Log.d(TAG, "get_selected.size(): " + get_selected.size());
                Log.d(TAG, "Notificationsl.size(): " + Notificationsl.size());
                ArrayList<Integer> toDelete = new ArrayList<>();

                if (get_selected.size() <= Notificationsl.size()) {
                    for (int index = 0; index < get_selected.size(); index++) {
                        if (get_selected.get(index).isChecked()) {
                            Log.d(TAG, "remove index: " + index);
                            toDelete.add(index);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                Log.d(TAG, "add to delete Key: " + Notificationsl.get(index).getNotificationKey());
                                to_delete.add(Notificationsl.get(index).getNotificationKey());
                            } else {
                                to_delete.add(Notificationsl.get(index).getPackageName() + ";" + Notificationsl.get(index).getNotificationTag() + ";" + Notificationsl.get(index).getNotificationID());
                            }
                        }
                    }
                }

                for (int index = toDelete.size() - 1; index >= 0; index--) {
                    int delete = toDelete.get(index);
                    selected.remove(delete);
                    adapter.setChecked(selected);
                    adapter.removeNotification(delete);
                }
            } else {
                Log.d(TAG, "remove swipe: " + position);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    to_delete.add(Notificationsl.get(position).getNotificationKey());
                } else {
                    to_delete.add(Notificationsl.get(position).getPackageName() + ";" + Notificationsl.get(position).getNotificationTag() + ";" + Notificationsl.get(position).getNotificationID());
                }
                selected.remove(position);
                adapter.setChecked(selected);
                adapter.removeNotification(position);
            }

            Intent intent = new Intent(getApplicationContext(), mNotificationListener.class);
            intent.putExtra("command", "deleteNotification");
            intent.putExtra("delete_notification", to_delete);
            //starting service
            startService(intent);
        }
    }

    private void createChecked() {
        Log.d(TAG, "createChecked");
        if (selected.size() > 0) {
            NotificationChecked select = new NotificationChecked();
            selected.add(0, select);
        } else {
            if (notificationList != null) {
                for (int i = 0; i < notificationList.get_notificationshowlist().size(); i++) {
                    NotificationChecked select = new NotificationChecked();
                    selected.add(select);
                }
            }
        }
        adapter.setChecked(selected);
    }

}
