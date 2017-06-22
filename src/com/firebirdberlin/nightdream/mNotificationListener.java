package com.firebirdberlin.nightdream;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

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
        Notification notification = sbn.getNotification();
        Log.i(TAG,"ID :" + sbn.getId() + "\t" + notification.tickerText + "\t" + sbn.getPackageName());

        Intent i = getIntentForBroadCast(sbn);

        // Filter out whatsapp
        if (sbn!=null && sbn.getPackageName().equalsIgnoreCase("com.whatsapp")){
            i.putExtra("what","whatsapp");
            i.putExtra("action","added");
            sendBroadcast(i);
        } else if (sbn!=null && sbn.getPackageName().equalsIgnoreCase("com.twitter.android")){
            i.putExtra("what","twitter");
            i.putExtra("action","added");
            sendBroadcast(i);
        } else if (sbn!=null && sbn.getPackageName().equalsIgnoreCase("com.google.android.gm")){
            i.putExtra("what","gmail");
            i.putExtra("action","added");
            sendBroadcast(i);
        } else if (sbn!=null && sbn.getPackageName().equalsIgnoreCase("com.android.phone")){
            i.putExtra("what","phone");
            i.putExtra("action","added");
            sendBroadcast(i);
        } else{
            i.putExtra("what","onNotificationPosted :" + sbn.getPackageName() + "n");
            i.putExtra("action","added");
            sendBroadcast(i);
        }

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(TAG,"++++ notification removed ++++");
        Log.i(TAG,"ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText +"\t" + sbn.getPackageName());

        Intent i = getIntentForBroadCast(sbn);
        if (sbn!=null && sbn.getPackageName().equalsIgnoreCase("com.whatsapp")){
            i.putExtra("what","whatsapp");
            i.putExtra("action","removed");
            sendBroadcast(i);
        } else if (sbn!=null && sbn.getPackageName().equalsIgnoreCase("com.twitter.android")){
            i.putExtra("what","twitter");
            i.putExtra("action","removed");
            sendBroadcast(i);
        } else if (sbn!=null && sbn.getPackageName().equalsIgnoreCase("com.google.android.gm")){
            i.putExtra("what","gmail");
            i.putExtra("action","removed");
            sendBroadcast(i);
        } else if (sbn!=null && sbn.getPackageName().equalsIgnoreCase("com.android.phone")){
            i.putExtra("what","phone");
            i.putExtra("action","removed");
            sendBroadcast(i);
        } else{
            i.putExtra("what",sbn.getPackageName());
            i.putExtra("action","removed");
            sendBroadcast(i);
        }
    }

    private Intent getIntentForBroadCast(StatusBarNotification sbn) {
        Intent i = new Intent(Config.ACTION_NOTIFICATION_LISTENER);
        i.putExtra("packageName", sbn.getPackageName());
        Notification notification = sbn.getNotification();
        if (notification != null) {
            //i.putExtra("iconId", notification.icon);
            i.putExtra("iconId", notification.extras.getInt(Notification.EXTRA_SMALL_ICON));
            i.putExtra("tickertext", notification.tickerText);
            i.putExtra("number", notification.number);
        }
        return i;
    }

    class NLServiceReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            try{
            if(intent.getStringExtra("command").equals("clearall")){
                    mNotificationListener.this.cancelAllNotifications();
            }
            else if(intent.getStringExtra("command").equals("list")){
                for (StatusBarNotification sbn : mNotificationListener.this.getActiveNotifications()) {
                     if (sbn!=null && sbn.getPackageName().equalsIgnoreCase("com.whatsapp")){
                        Intent i = getIntentForBroadCast(sbn);
                        i.putExtra("what","whatsapp");
                        i.putExtra("action","added");
                        sendBroadcast(i);
                    } else if (sbn!=null && sbn.getPackageName().equalsIgnoreCase("com.twitter.android")){
                        Intent i = getIntentForBroadCast(sbn);
                        i.putExtra("what","twitter");
                        i.putExtra("action","added");
                        sendBroadcast(i);
                    } else if (sbn!=null && sbn.getPackageName().equalsIgnoreCase("com.google.android.gm")){
                        Intent i = getIntentForBroadCast(sbn);
                        i.putExtra("what","gmail");
                        i.putExtra("action","added");
                        sendBroadcast(i);
                    } else if (sbn!=null && sbn.getPackageName().equalsIgnoreCase("com.android.phone")){
                        Intent i = getIntentForBroadCast(sbn);
                        i.putExtra("what","phone");
                        i.putExtra("action","added");
                        sendBroadcast(i);
                    }
                }
            }
            else if(intent.getStringExtra("command").equals("release")){
                Log.d(TAG,"calling stopSelf()");
                mNotificationListener.this.stopSelf();
            }
            } catch (Exception e) {
                // NULLPointerExceptions ignored
            }
        }
    }

}
