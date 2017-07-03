package com.firebirdberlin.nightdream;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import java.util.HashSet;
import com.firebirdberlin.nightdream.Config;

public class mNotificationListener extends NotificationListenerService {

    private String TAG = this.getClass().getSimpleName();
    private NLServiceReceiver nlservicereciver;

    @Override
    public void onCreate() {
        super.onCreate();
        nlservicereciver = new NLServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.ACTION_NOTIFICATION_LISTENER);
        registerReceiver(nlservicereciver, filter);
        Log.i(TAG,"**********  Notification listener STARTED");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"**********  Notification listener STOPPED");
        unregisterReceiver(nlservicereciver);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        Log.i(TAG,"++++ notification posted ++++");
        logNotification(sbn);

        if ( ! isClearable(sbn)) return;
        listNotifications();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(TAG,"++++ notification removed ++++");
        logNotification(sbn);

        if ( ! isClearable(sbn)) return;

        listNotifications();
    }


    class NLServiceReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {

            String command = intent.getStringExtra("command");
            if (command == null) return;

            if( command.equals("clearall") ) {
                    mNotificationListener.this.cancelAllNotifications();
            } else
            if ( command.equals("list") ) {
                listNotifications();
            } else
            if ( command.equals("release") ) {
                Log.d(TAG,"calling stopSelf()");
                mNotificationListener.this.stopSelf();
            }
        }
    }

    private void listNotifications() {
        clearNotificationUI();
        HashSet<String> groupKeys = new HashSet<String>();

        StatusBarNotification[] notificationList = null;
        try {
            notificationList = mNotificationListener.this.getActiveNotifications();
        } catch (SecurityException e) {
            //Notification listener service is not yet registered.
        }
        if (notificationList == null) return;

        for (StatusBarNotification sbn : notificationList) {
            Notification notification = sbn.getNotification();
            if (notification == null) continue;

            logNotification(sbn);

            if (Build.VERSION.SDK_INT >= 20) {
                String key = notification.getGroup();
                if (key != null) {
                    if (groupKeys.contains(key)) continue;
                    groupKeys.add(key);
                }
            }

            if (Build.VERSION.SDK_INT >= 18) {
                if ( ! sbn.isClearable() ) {
                    continue;
                }
            }

            Intent i = getIntentForBroadCast(sbn);
            if (i != null) {
                i.putExtra("action", "added");
                LocalBroadcastManager.getInstance(this).sendBroadcast(i);
            }
        }
    }

    private Intent getIntentForBroadCast(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        if (notification != null ) {
            Intent i = new Intent(Config.ACTION_NOTIFICATION_LISTENER);
            i.putExtra("packageName", sbn.getPackageName());
            i.putExtra("iconId", getIconId(notification));
            i.putExtra("tickertext", notification.tickerText);
            i.putExtra("number", notification.number);
            return i;
        }
        return null;
    }

    private boolean isClearable(StatusBarNotification sbn) {
        if (Build.VERSION.SDK_INT >= 18) {
            return sbn.isClearable();
        } else {
            Notification notification = sbn.getNotification();
            if (notification == null) return true;

            return (((notification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT)
                    || ((notification.flags & Notification.FLAG_NO_CLEAR) == Notification.FLAG_NO_CLEAR));
        }
    }

    private int getIconId(Notification notification) {
        if (notification == null) return -1;
        if (Build.VERSION.SDK_INT >= 19) {
            return notification.extras.getInt(Notification.EXTRA_SMALL_ICON);
        }
        return notification.icon;
    }

    private void logNotification(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        CharSequence title = "";
        CharSequence text = "";
        if (Build.VERSION.SDK_INT >= 19) {
            title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
            text = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
        }
        Log.i(TAG,"ID :" + sbn.getId()
                + "\t" + title
                + "\t" + text
                + "\t" + notification.tickerText
                + "\t" + String.valueOf(notification.number)
                + "\t" + sbn.getPackageName());
        Log.d(TAG, notification.toString());
    }

    private void clearNotificationUI() {
        Intent i = new Intent(Config.ACTION_NOTIFICATION_LISTENER);
        i.putExtra("action", "clear");
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }
}
