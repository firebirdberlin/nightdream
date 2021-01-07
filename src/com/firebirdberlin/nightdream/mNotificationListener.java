package com.firebirdberlin.nightdream;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.firebirdberlin.nightdream.NotificationList.NotificationApp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class mNotificationListener extends NotificationListenerService {

    public static boolean running = false;
    int minNotificationImportance = 2;
    boolean groupSimilarNotifications = false;
    HashMap<String, HashSet<Integer>> iconIdsByPackage = new HashMap<>();
    private String TAG = this.getClass().getSimpleName();
    private NLServiceReceiver nlServiceReceiver;

    private List<com.firebirdberlin.nightdream.NotificationList.Notification> notificationlist = new ArrayList<>();
    private List<com.firebirdberlin.nightdream.NotificationList.NotificationApp> notificationapplist = new ArrayList<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //retrieving data from the received intent
        final Context context = this;

        if(intent.hasExtra("command")) {
            Log.d(TAG,"NL - onStartCommand:" + intent.getStringExtra("command"));

            switch (intent.getStringExtra("command")) {
                case "getnotificationlist":
                    Log.d(TAG, "getnotificationlist");
                    Intent intent_notificationlist = new Intent("Notification.Action.notificationlist");
                    intent_notificationlist.putParcelableArrayListExtra("notificationlist", (ArrayList<? extends Parcelable>) notificationlist);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent_notificationlist);
                    break;
                case "getnotificationapplist":
                    Log.d(TAG, "getnotificationapplist: "+notificationapplist);
                    Intent intent_notificationapplist = new Intent("Msg");
                    intent_notificationapplist.putParcelableArrayListExtra("notificationapplist", (ArrayList<? extends Parcelable>) notificationapplist);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent_notificationapplist);
                    break;
            }
        }
/*
        if(intent.hasExtra("getnotificationlist")) {
            Log.d(TAG,"getnotificationlist");
            Intent i1 = new  Intent("Msg");
            i1.putParcelableArrayListExtra("notificationlist", (ArrayList<? extends Parcelable>) notificationlist);
            LocalBroadcastManager.getInstance(context).sendBroadcast(i1);
        }

 */
        super.onStartCommand(intent, flags, startId);

        //prevent the automatic service termination
        return START_STICKY;
    }

    private static Bitmap drawableToBitMap(Drawable drawable) {
        if (drawable == null) {
            return null;
        }

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = ((BitmapDrawable) drawable);
            return bitmapDrawable.getBitmap();
        } else {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                    drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            drawable.draw(canvas);
            return bitmap;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        nlServiceReceiver = new NLServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.ACTION_NOTIFICATION_LISTENER);
        LocalBroadcastManager.getInstance(this).registerReceiver(nlServiceReceiver, filter);
        Log.i(TAG, "**********  Notification listener STARTED");
        running = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        Log.i(TAG, "**********  Notification listener STOPPED");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(nlServiceReceiver);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        minNotificationImportance = Settings.getMinNotificationImportance(this);
        groupSimilarNotifications = Settings.groupSimilarNotifications(this);

        Log.i(TAG, "++++ notification posted ++++");
        logNotification(sbn);

        Intent i = getIntentForBroadCast(sbn);
        if (i != null) {
            i.putExtra("action", "added_preview");
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);
        }

        if (shallIgnoreNotification(sbn)) return;
        listNotifications();

        if (Build.VERSION.SDK_INT >= 28) {
            getNotificationListData();
        }

        if (!Utility.isScreenOn(this)) {
            conditionallyStartActivity();
        }
    }

    private boolean shallIgnoreNotification(StatusBarNotification sbn) {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            Bundle extras = sbn.getNotification().extras;
            if (
                    extras.containsKey(Notification.EXTRA_TEMPLATE)
                            && extras.getCharSequence("android.template") != null
            ) {
                if (((String) extras.getCharSequence("android.template")).contains("MediaStyle")) {
                    Log.w(TAG, "MediaStyle notification found");
                    return false;
                }
            }
        }

        if (!isClearable(sbn)) return true;
        Notification notification = sbn.getNotification();

        if (notification == null) return true;

        if (groupSimilarNotifications && isIconIdInCache(sbn)) {
            return true;
        }

        //if ( getTitle(sbn) == null ) return true;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return (notification.priority < minNotificationImportance - 3);
        }
        int importance = getImportance(sbn);
        return (importance < minNotificationImportance);
    }

    private void conditionallyStartActivity() {
        final Context context = this;
        Settings settings = new Settings(this);
        if (!settings.autostartForNotifications) return;
        final SensorManager mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager == null) return;

        Sensor mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (mProximity == null) return;

        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (sensorEvent.values[0] > 0 && !Utility.isScreenOn(context)) {
                    NightDreamActivity.start(context, "start standby mode");
                }
                mSensorManager.unregisterListener(this);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        }, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(TAG, "++++ notification removed ++++");
        logNotification(sbn);

        listNotifications();
    }

    private void listNotifications() {
        minNotificationImportance = Settings.getMinNotificationImportance(this);
        groupSimilarNotifications = Settings.groupSimilarNotifications(this);
        clearNotificationUI();
        HashSet<String> groupKeys = new HashSet<>();

        StatusBarNotification[] notificationList = null;
        try {
            notificationList = mNotificationListener.this.getActiveNotifications();
        } catch (RuntimeException | OutOfMemoryError ignored) {

        }

        if (notificationList == null) return;

        iconIdsByPackage.clear();
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

            if (shallIgnoreNotification(sbn)) {
                continue;
            }

            addIconIdToCache(sbn);

            Intent i = getIntentForBroadCast(sbn);
            if (i != null) {
                i.putExtra("action", "added");

                // Utility.logIntent(TAG, "notification intent", i);
                LocalBroadcastManager.getInstance(this).sendBroadcast(i);
            }
        }
    }

    void addIconIdToCache(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        Notification notification = sbn.getNotification();
        if (notification == null) {
            return;
        }
        HashSet<Integer> ids = iconIdsByPackage.get(packageName);
        if (ids == null) {
            ids = new HashSet<>();
            iconIdsByPackage.put(packageName, ids);
        }
        ids.add(getIconId(notification));
    }

    boolean isIconIdInCache(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        HashSet<Integer> iconIds = iconIdsByPackage.get(packageName);
        Notification notification = sbn.getNotification();
        int iconID = getIconId(notification);
        return (iconID > 0 && iconIds != null && iconIds.contains(iconID));
    }

    private Intent getIntentForBroadCast(StatusBarNotification sbn) {
        final Context context = this;
        Notification notification = sbn.getNotification();
        if (notification != null) {
            Intent i = new Intent(Config.ACTION_NOTIFICATION_LISTENER);
            i.putExtra("packageName", sbn.getPackageName());
            i.putExtra("iconId", getIconId(notification));
            i.putExtra("tickerText", notification.tickerText);
            i.putExtra("number", notification.number);


            //get extra information
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                Bundle extras = notification.extras;
                if (
                        extras.containsKey(Notification.EXTRA_TEMPLATE)
                                && extras.getCharSequence("android.template") != null
                ) {
                    i.putExtra("template", extras.getCharSequence("android.template").toString());
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    Icon largeIcon = notification.getLargeIcon();
                    if (largeIcon != null) {
                        Bitmap largeIconBitmap = drawableToBitMap(largeIcon.loadDrawable(context));
                        i.putExtra("largeIconBitmap", largeIconBitmap);
                    }
                } else {
                    if (extras.containsKey(Notification.EXTRA_PICTURE)) {
                        i.putExtra("largeIconBitmap", (Bitmap) extras.get(Notification.EXTRA_PICTURE));
                    }
                }

                //notification actions
                try {
                    Notification.Action[] actions = notification.actions;
                    i.putExtra("actions", actions);
                } catch (Exception ignored) {

                }
                //get application packageName
                final PackageManager pm = getApplicationContext().getPackageManager();
                ApplicationInfo info;
                try {
                    info = pm.getApplicationInfo(sbn.getPackageName(), 0);
                } catch (final PackageManager.NameNotFoundException e) {
                    info = null;
                }
                final String applicationName = (String) (info != null ? pm.getApplicationLabel(info) : "(unknown)");
                i.putExtra("applicationName", applicationName);

                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                String postTime = dateFormat.format(sbn.getPostTime());
                i.putExtra("postTimestamp", postTime);

                if (extras.containsKey(Notification.EXTRA_TITLE) && extras.getCharSequence("android.title") != null) {
                    i.putExtra("title", extras.getCharSequence("android.title").toString());
                }

                if (extras.containsKey(Notification.EXTRA_TEXT) && extras.getCharSequence("android.text") != null) {
                    i.putExtra("text", extras.getCharSequence("android.text").toString());
                }

                //get content Intent from notification
                if (sbn.getNotification().contentIntent != null) {
                    i.putExtra("contentintent", sbn.getNotification().contentIntent);
                }
            }

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
        Log.d(TAG, "getIconId(" + notification.toString() + ")");
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                Icon icon = notification.getSmallIcon();
                return icon.getResId();
            } else if (Build.VERSION.SDK_INT >= 19) {
                return notification.extras.getInt(Notification.EXTRA_SMALL_ICON);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return notification.icon;
    }

    int getImportance(StatusBarNotification sbn) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Ranking ranking = getRanking(sbn);
            if (ranking != null) {
                return ranking.getImportance();
            }
        }
        return 0;
    }

    Ranking getRanking(StatusBarNotification sbn) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String notificationKey = sbn.getKey();
            RankingMap rankingMap = getCurrentRanking();
            Ranking ranking = new Ranking();
            rankingMap.getRanking(notificationKey, ranking);
            return ranking;
        }
        return null;
    }

    private void logNotification(StatusBarNotification sbn) {
        if (sbn == null || Build.VERSION.SDK_INT < 26) return;
        Notification notification = sbn.getNotification();
        CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
        String group_key = notification.getGroup();
        Log.i(TAG, "ID :" + sbn.getId()
                + "\t" + title
                + "\t" + text
                + "\t" + notification.tickerText
                + "\t" + notification.number
                + "\t" + sbn.getPackageName()
                + "\t" + notification.priority
                + "\t" + group_key
        );
        //Log.d(TAG, notification.toString());
    }

    private CharSequence getTitle(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        CharSequence title = "";
        CharSequence text = "";
        if (Build.VERSION.SDK_INT >= 19) {
            return notification.extras.getCharSequence(Notification.EXTRA_TITLE);
        }
        return null;
    }

    private void clearNotificationUI() {
        Intent i = new Intent(Config.ACTION_NOTIFICATION_LISTENER);
        i.putExtra("action", "clear");
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    class NLServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String command = intent.getStringExtra("command");
            if (command == null) return;

            switch (command) {
                case "clearall":
                    mNotificationListener.this.cancelAllNotifications();
                    break;
                case "list":
                    listNotifications();
                    if (Build.VERSION.SDK_INT >= 28) {
                        getNotificationListData();
                    }
                    break;
                case "release":
                    Log.d(TAG, "calling stopSelf()");
                    mNotificationListener.this.stopSelf();
                    break;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void getNotificationListData() {
        Log.d(TAG, "notificationList");
        minNotificationImportance = Settings.getMinNotificationImportance(this);
        groupSimilarNotifications = false;
        notificationapplist.clear();
        notificationlist.clear();

        final Context context = this;

        StatusBarNotification[] notificationList = null;
        try {
            notificationList = mNotificationListener.this.getActiveNotifications();
        } catch (RuntimeException | OutOfMemoryError ignored) {

        }

        if (notificationList == null) return;

        for (StatusBarNotification sbn : notificationList) {

            if (shallIgnoreNotification(sbn)) {
                continue;
            }

            //get extra informations from notification
            Bundle extras = sbn.getNotification().extras;
            if (((String) extras.getCharSequence("android.template"))!= null && ((String) extras.getCharSequence("android.template")).contains("MediaStyle")) {
                continue;
            }

            Intent msgrcv = new Intent("Msg");

            String titleData = "";
            String titleBigData = "";
            String textData = "";
            StringBuilder textLinesData = new StringBuilder();
            String template = "";
            String summaryText = "";

            String packageName = sbn.getPackageName();
            msgrcv.putExtra("package", packageName);

            //get application packageName
            final PackageManager pm = getApplicationContext().getPackageManager();
            ApplicationInfo ai;

            try {
                ai = pm.getApplicationInfo(packageName, 0);
            } catch (final PackageManager.NameNotFoundException e) {
                ai = null;
            }
            final

            String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
            msgrcv.putExtra("applicationname", applicationName);

            //get small icon from notification
            msgrcv.putExtra("iconresid", getIconId(sbn.getNotification()));

            //get large icon from notification
            Icon largeIcon;
            largeIcon = sbn.getNotification().getLargeIcon();
            if (largeIcon != null) {
                Bitmap largeiconbitmap = drawableToBitMap(largeIcon.loadDrawable(context));

                msgrcv.putExtra("largeicon", largeIcon);
                msgrcv.putExtra("largeiconbitmap", largeiconbitmap);
            }

            msgrcv.putExtra("id", sbn.getId());

            Notification notification = sbn.getNotification();

            // check the notification's flags for either Notification#FLAG_ONGOING_EVENT or Notification#FLAG_NO_CLEAR.
            msgrcv.putExtra("clearable", sbn.isClearable());

            //check the notification's icon color
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                msgrcv.putExtra("color", notification.color);
            }
            else {
                msgrcv.putExtra("color", 0);
            }

            //Actions in Notification
            try {
                Notification.Action[] action = sbn.getNotification().actions;
                msgrcv.putExtra("action", action);
            } catch (Exception ex) {
                Log.e(TAG, ex.toString());
            }

            //get content Intent
            PendingIntent contentIntent = sbn.getNotification().contentIntent;
            msgrcv.putExtra("contentintent", contentIntent);

            //this is the bigPicture of the BigPictureStyle
            Bitmap bigpicture = null;
            if (extras.containsKey(Notification.EXTRA_PICTURE)) {
                bigpicture = (Bitmap) extras.get(Notification.EXTRA_PICTURE);
            }
            msgrcv.putExtra("bigpicture", bigpicture);

            //this is the title of the notification
            if (extras.containsKey(Notification.EXTRA_TITLE) && extras.getString("android.title") != null) {
                titleData = extras.getString("android.title");
            }
            msgrcv.putExtra("title", titleData);

            //this is the title of the notification when shown in expanded form
            if (extras.containsKey(Notification.EXTRA_TITLE_BIG) && extras.getCharSequence("android.title.big") != null) {
                titleBigData = extras.getCharSequence("android.title.big").toString();
            }
            msgrcv.putExtra("titlebig", titleBigData);

            String textBigData = "";
            //this is the longer text shown in the big form of a BigTextStyle notification,
            if (extras.containsKey(Notification.EXTRA_BIG_TEXT) && extras.getCharSequence("android.bigText") != null) {
                textBigData = extras.getCharSequence("android.bigText").toString();
            }
            msgrcv.putExtra("textbig", textBigData);

            //this is the summary information intended to be shown alongside expanded notification
            if (extras.containsKey(Notification.EXTRA_SUMMARY_TEXT) && extras.getCharSequence("android.summaryText") != null) {
                summaryText = extras.getCharSequence("android.summaryText").toString();
            }
            msgrcv.putExtra("summarytext", summaryText);

            //this is the main text
            if (extras.containsKey(Notification.EXTRA_TEXT) && extras.getCharSequence("android.text") != null) {
                textData = extras.getCharSequence("android.text").toString();
            }
            msgrcv.putExtra("text", textData);

            //A string representing the name of the specific Notification.Style used to create this notification.
            if (extras.containsKey(Notification.EXTRA_TEMPLATE) && extras.getCharSequence("android.template") != null) {
                template = extras.getCharSequence("android.template").toString();
            }
            msgrcv.putExtra("template", template);

            // the "ticker" text which is sent to accessibility services.
            CharSequence ticker = "";
            if (sbn.getNotification().tickerText != null) {
                ticker = sbn.getNotification().tickerText;
            }
            msgrcv.putExtra("ticker", ticker);

            //An array of CharSequences to show in InboxStyle expanded notifications
            if (extras.containsKey(Notification.EXTRA_TEXT_LINES)) {
                CharSequence[] messages = extras.getCharSequenceArray("android.textLines");
                if (messages != null) {
                    for (CharSequence message : messages) {
                        if (textLinesData.toString().equals("")) {
                            textLinesData = new StringBuilder(message.toString());
                        } else {
                            textLinesData.append("<br>").append(message.toString());
                        }
                    }
                }
            }
            msgrcv.putExtra("textlines", textLinesData.toString());

            // Extract MessagingStyle object from the active notification.
            StringBuilder notification_messages = new StringBuilder();
            try {
                String lastPerson = "";
                NotificationCompat.MessagingStyle activeStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(sbn.getNotification());
                assert activeStyle != null;
                for (NotificationCompat.MessagingStyle.Message message : activeStyle.getMessages()) {
                    assert message.getPerson() != null;
                    if (lastPerson.contentEquals(message.getPerson().getName())) {
                        notification_messages.append("<br>").append(message.getText());
                    } else {
                        if (notification_messages.toString().equals("")) {
                            notification_messages = new StringBuilder(message.getPerson().getName() + "<br> " + message.getText());
                        } else {
                            notification_messages.append("<br>").append(message.getPerson().getName()).append("<br> ").append(message.getText());
                        }

                        lastPerson = message.getPerson().getName().toString();
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG,ex.toString());
            }
            msgrcv.putExtra("messages", notification_messages.toString());

            //Contentview
            RemoteViews Rviews = sbn.getNotification().contentView;

            if (Rviews != null) {
                //Returns the layout id of the root layout associated with this RemoteViews
                msgrcv.putExtra("view", Rviews);
            }

            //BigContentview
            Rviews = sbn.getNotification().bigContentView;

            if (Rviews != null) {
                //Returns the layout id of the root layout associated with this RemoteViews
                msgrcv.putExtra("bigview", Rviews);
            }

            //get date and time
            Date myDate = new Date();
            //date formatation
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.ZZZZ", java.util.Locale.getDefault());
            //formate date
            String date = dateFormat.format(myDate);
            //date formatation
            dateFormat = new SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            String postTime = dateFormat.format(sbn.getPostTime());

            msgrcv.putExtra("posttimestamp", String.valueOf(sbn.getPostTime()));
            msgrcv.putExtra("timestamp", date);
            msgrcv.putExtra("posttime", postTime);

            //Add new notification
            notificationlist.add(new com.firebirdberlin.nightdream.NotificationList.Notification(getApplicationContext(), msgrcv));

            Log.d(TAG, "sendBroadcast");

            //Send notificationlist
            Intent notificationlist_intent = new Intent("Notification.Action.notificationlist");
            notificationlist_intent.putParcelableArrayListExtra("notificationlist", (ArrayList<? extends Parcelable>) notificationlist);
            LocalBroadcastManager.getInstance(context).sendBroadcast(notificationlist_intent);

            //AppList
            Intent notificationapplist_intent = new Intent("Msg");

            notificationapplist_intent.putExtra("name", msgrcv.getStringExtra("applicationname"));
            notificationapplist_intent.putExtra("package", msgrcv.getStringExtra("package"));
            notificationapplist_intent.putExtra("timestamp", msgrcv.getStringExtra("posttime"));
            notificationapplist_intent.putExtra("posttimestamp", String.valueOf(sbn.getPostTime()));

            boolean addApp = true;

            //Remove old application from notificationapplist
            for (int index = 0; index < notificationapplist.size(); index++) {
                if (notificationapplist.get(index).get_notificationapp_name().equals(msgrcv.getStringExtra("applicationname"))) {
                    addApp = false;
                    if ( Long.parseLong(notificationapplist.get(index).get_notificationapp_posttime()) < sbn.getPostTime()) {
                        notificationapplist.remove(notificationapplist.get(index));
                        addApp = true;
                    }
                }
            }

            //Add to Applist
            if (addApp) {
                notificationapplist.add(new com.firebirdberlin.nightdream.NotificationList.NotificationApp(getApplicationContext(), notificationapplist_intent));
            }

            notificationapplist_intent.putParcelableArrayListExtra("notificationapplist", (ArrayList<? extends Parcelable>) notificationapplist);

            //Send notificationapplist
            LocalBroadcastManager.getInstance(context).sendBroadcast(notificationapplist_intent);
        }
    }


}
