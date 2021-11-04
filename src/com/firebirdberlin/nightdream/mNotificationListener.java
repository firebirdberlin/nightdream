package com.firebirdberlin.nightdream;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
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

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.firebirdberlin.nightdream.NotificationList.NotificationApp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class mNotificationListener extends NotificationListenerService {

    public static boolean running = false;
    private final String TAG = this.getClass().getSimpleName();
    private final List<com.firebirdberlin.nightdream.NotificationList.Notification> notifications = new ArrayList<>();
    private final List<NotificationApp> notificationApps = new ArrayList<>();
    int minNotificationImportance = 2;
    private NLServiceReceiver nlServiceReceiver;

    public static void requestNotificationList(Context context) {
        Log.d("mNotificationListener", "requestNotificationList()");
        Intent i = new Intent(Config.ACTION_NOTIFICATION_LISTENER);
        i.putExtra("command", "list");
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        //prevent the automatic service termination
        return START_STICKY;
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

        Log.i(TAG, "++++ notification posted ++++");
        logNotification(sbn);
        if (shallIgnoreNotification(sbn)) return;

        Intent i = getIntentForBroadCast(sbn);
        if (i != null) {
            i.setAction(Config.ACTION_NOTIFICATION_LISTENER);
            i.putExtra("action", "added_preview");
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);
        }

        listNotifications();

        if (!Utility.isScreenOn(this)) {
            conditionallyStartActivity();
        }
    }

    private boolean shallIgnoreNotification(StatusBarNotification sbn) {

        Notification notification = sbn.getNotification();
        if (notification == null) return true;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            Bundle extras = notification.extras;
            String template = (String) extras.getCharSequence("android.template");
            if (template != null && template.contains("MediaStyle")) {
                Log.w(TAG, "MediaStyle notification found");
                return false;
            }
        }

        if (!isClearable(sbn) || sbn.isOngoing()) return true;
        if ((notification.flags & Notification.FLAG_GROUP_SUMMARY) > 0) {
            return true;
        }

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

        try {
            final SensorManager mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            Sensor mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            mSensorManager.registerListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent sensorEvent) {
                    if (sensorEvent.values[0] > 0) {
                        startActivity(context);
                    }
                    mSensorManager.unregisterListener(this);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int i) {

                }
            }, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        } catch (IllegalStateException | NullPointerException e) {
            // Sensor is not present or could not be initiated
            startActivity(context);
        }
    }

    void startActivity(Context context) {
         if (!Utility.isScreenOn(context)) {
             NightDreamActivity.start(context, "start standby mode");
         }
     }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(TAG, "++++ notification removed ++++");
        logNotification(sbn);

        listNotifications();

        Intent i = getIntentForBroadCast(sbn);
        if (i != null) {
            String template = i.getStringExtra("template");
            if (template != null) {
                if (template.contains("MediaStyle")) {
                    i.setAction(Config.ACTION_NOTIFICATION_LISTENER);
                    i.putExtra("action", "removed_media");
                    LocalBroadcastManager.getInstance(this).sendBroadcast(i);
                }
            }
        }
    }

    private void deleteNotification(ArrayList<String> delete) {
        Log.d(TAG, "deleteNotification");

        if (delete != null) {
            for (String notificationId : delete) {
                Log.d(TAG, "Notification to delete: " + notificationId);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    String[] separated = notificationId.split(";");
                    cancelNotification(separated[0], separated[1], Integer.parseInt(separated[2]));
                } else {
                    cancelNotification(notificationId);
                }
            }
        }
    }

    private void listNotifications() {
        minNotificationImportance = Settings.getMinNotificationImportance(this);
        notifications.clear();
        notificationApps.clear();

        clearNotificationUI();
        StatusBarNotification[] notificationList = null;
        try {
            notificationList = mNotificationListener.this.getActiveNotifications();
        } catch (RuntimeException | OutOfMemoryError ignored) {

        }

        if (notificationList == null) return;

        for (StatusBarNotification sbn : notificationList) {
            Notification notification = sbn.getNotification();
            if (notification == null) continue;

            logNotification(sbn);

            if (shallIgnoreNotification(sbn)) {
                continue;
            }

            Intent i = getIntentForBroadCast(sbn);
            if (i == null) {
                continue;
            }
            String template = i.getStringExtra("template");
            if (template == null) {
                continue;
            }
            if (template.contains("MediaStyle")) {
                i.setAction(Config.ACTION_NOTIFICATION_LISTENER);
                i.putExtra("action", "added_media");
                LocalBroadcastManager.getInstance(this).sendBroadcast(i);
                continue;
            }
            notifications.add(
                    new com.firebirdberlin.nightdream.NotificationList.Notification(
                            getApplicationContext(), i
                    )
            );

            String applicationName = i.getStringExtra("applicationName");
            boolean addApp = true;
            for (NotificationApp app : notificationApps) {
                addApp = addApp && !app.getName().equals(applicationName);
                if (app.getName().equals(applicationName)) {
                    if (app.getPostTimestamp() < sbn.getPostTime()) {
                        app.setPostTimestamp(sbn.getPostTime());
                    }
                }
            }
            if (addApp) {
                notificationApps.add(new NotificationApp(i));
            }

        }
        Intent intentList = new Intent("Notification.Action.notificationList");
        intentList.putParcelableArrayListExtra("notifications", (ArrayList<? extends Parcelable>) notifications);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentList);

        Intent intentAppsList = new Intent(Config.ACTION_NOTIFICATION_APPS_LISTENER);
        intentAppsList.putExtra("action", "scan");
        intentAppsList.putParcelableArrayListExtra("notificationApps", (ArrayList<? extends Parcelable>) notificationApps);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intentAppsList);
    }

    private Intent getIntentForBroadCast(StatusBarNotification sbn) {
        final Context context = this;
        Notification notification = sbn.getNotification();
        if (notification == null) {
            return null;
        }
        String packageName = sbn.getPackageName();
        String applicationName = getApplicationLabel(packageName);
        Bitmap largeIconBitmap = getLargeIconBitmap(context, sbn);
        Notification.Action[] actions = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            actions = notification.actions;
        }

        int color = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            color = notification.color;
        }
        PendingIntent contentIntent = notification.contentIntent;
        Bitmap bigPicture = null;
        CharSequence summaryText = "";
        CharSequence template = "";
        CharSequence textBigData = "";
        CharSequence textData = "";
        CharSequence titleBigData = "";
        CharSequence titleData = "";
        StringBuilder textLinesData = new StringBuilder();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            Bundle extras = notification.extras;

            template = extras.getCharSequence("android.template");
            if (template == null) template = "";
            if (extras.containsKey(Notification.EXTRA_PICTURE)) {
                bigPicture = (Bitmap) extras.get(Notification.EXTRA_PICTURE);
            }

            if (extras.containsKey(Notification.EXTRA_TITLE) && extras.getCharSequence("android.title") != null) {
                titleData = extras.getCharSequence("android.title");
            }
            if (extras.containsKey(Notification.EXTRA_TITLE_BIG) && extras.getCharSequence("android.title.big") != null) {
                titleBigData = extras.getCharSequence("android.title.big");
            }

            //this is the longer text shown in the big form of a BigTextStyle notification,
            if (extras.containsKey(Notification.EXTRA_BIG_TEXT) && extras.getCharSequence("android.bigText") != null) {
                textBigData = extras.getCharSequence("android.bigText");
            }

            //this is the summary information intended to be shown alongside expanded notification
            if (extras.containsKey(Notification.EXTRA_SUMMARY_TEXT) && extras.getCharSequence("android.summaryText") != null) {
                summaryText = extras.getCharSequence("android.summaryText");
            }
            if (extras.containsKey(Notification.EXTRA_TEXT) && extras.getCharSequence("android.text") != null) {
                textData = extras.getCharSequence("android.text");
            }

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
        }

        // the "ticker" text which is sent to accessibility services.
        CharSequence tickerText = "";
        if (notification.tickerText != null) {
            tickerText = notification.tickerText;
        }


        // Extract MessagingStyle object from the active notification.
        StringBuilder notification_messages = new StringBuilder();
        {
            String lastPerson = "";
            NotificationCompat.MessagingStyle activeStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification);
            if (activeStyle != null) {
                for (NotificationCompat.MessagingStyle.Message message : activeStyle.getMessages()) {
                    if (message.getPerson() != null && message.getPerson().getName() != null) {
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
                }
            }
        }

        //get date and time
        long postTimestamp = sbn.getPostTime();
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.ZZZZ", Locale.getDefault());
        String date = dateFormat.format(now);

        dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String postTime = dateFormat.format(postTimestamp);

        Intent intent = new Intent();
        intent.putExtra("actions", actions);
        intent.putExtra("applicationName", applicationName);
        intent.putExtra("bigPicture", bigPicture);
        intent.putExtra("bigContentView", notification.bigContentView);
        intent.putExtra("color", color);
        intent.putExtra("contentIntent", contentIntent);
        intent.putExtra("contentView", notification.contentView);
        intent.putExtra("iconId", getIconId(sbn.getNotification()));
        intent.putExtra("id", sbn.getId());
        intent.putExtra("tag", sbn.getTag());
        intent.putExtra("isClearable", sbn.isClearable());
        intent.putExtra("largeIconBitmap", largeIconBitmap);
        intent.putExtra("messages", notification_messages.toString());
        intent.putExtra("number", notification.number);
        intent.putExtra("packageName", packageName);
        intent.putExtra("postTime", postTime);
        intent.putExtra("postTimestamp", postTimestamp);
        intent.putExtra("smallIconBitmap", getSmallIconBitmap(context, sbn));
        intent.putExtra("summaryText", summaryText.toString());
        intent.putExtra("template", template.toString());
        intent.putExtra("text", textData.toString());
        intent.putExtra("textBig", textBigData.toString());
        intent.putExtra("textLines", textLinesData.toString());
        intent.putExtra("tickerText", tickerText);
        intent.putExtra("timestamp", date);
        intent.putExtra("title", titleData.toString());
        intent.putExtra("titleBig", titleBigData.toString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.putExtra("key", sbn.getKey());
        }

        return intent;
    }

    private Bitmap getSmallIconBitmap(Context context, StatusBarNotification sbn) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Notification notification = sbn.getNotification();
            Icon icon = notification.getSmallIcon();
            if (icon == null) return null;
            return drawableToBitMap(icon.loadDrawable(context));
        } else {
            return drawableToBitMap(
                    getNotificationIconFromPackage(
                            context, sbn.getPackageName(), getIconId(sbn.getNotification())
                    )
            );
        }
    }

    private Drawable getNotificationIconFromPackage(Context context, String packageName, int id) {
        Log.d(TAG, "getNotificationIcon for id = " + id);
        if (packageName == null || id == -1) {
            return null;
        }
        try {
            Context remotePackageContext = context.getApplicationContext().createPackageContext(packageName, 0);
            return ContextCompat.getDrawable(remotePackageContext, id);
        } catch (NullPointerException | PackageManager.NameNotFoundException | Resources.NotFoundException e) {
            return null;
        }
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

    private void clearNotificationUI() {
        Intent i = new Intent(Config.ACTION_NOTIFICATION_LISTENER);
        i.putExtra("action", "clear");
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    private String getApplicationLabel(String packageName) {
        final PackageManager pm = getApplicationContext().getPackageManager();
        ApplicationInfo ai;

        try {
            ai = pm.getApplicationInfo(packageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            ai = null;
        }
        return (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
    }

    private Bitmap getLargeIconBitmap(Context context, StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Icon largeIcon = notification.getLargeIcon();
            if (largeIcon != null) {
                return drawableToBitMap(largeIcon.loadDrawable(context));
            }
        } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Bundle extras = notification.extras;
            if (extras.containsKey(Notification.EXTRA_PICTURE)) {
                return (Bitmap) extras.get(Notification.EXTRA_PICTURE);
            }
        }
        return null;
    }

    class NLServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String command = intent.getStringExtra("command");
            if (command == null) return;

            switch (command) {
                case "clear":
                    deleteNotification(intent.getStringArrayListExtra("notificationKeys"));
                    break;
                case "clearall":
                    mNotificationListener.this.cancelAllNotifications();
                    break;
                case "list":
                    listNotifications();
                    break;
                case "release":
                    Log.d(TAG, "calling stopSelf()");
                    mNotificationListener.this.stopSelf();
                    break;
            }
        }
    }
}
