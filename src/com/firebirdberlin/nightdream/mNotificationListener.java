package com.firebirdberlin.nightdream;

import android.app.Notification;
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
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class mNotificationListener extends NotificationListenerService {

    public static boolean running = false;
    int minNotificationImportance = 2;
    boolean groupSimilarNotifications = false;
    HashMap<String, HashSet<Integer>> iconIdsByPackage = new HashMap<>();
    private String TAG = this.getClass().getSimpleName();
    private NLServiceReceiver nlServiceReceiver;

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

        if (shallIgnoreNotification(sbn)) return;
        listNotifications();

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
        Log.i(TAG, "listNotifications()");
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

                if (extras.containsKey(Notification.EXTRA_TITLE) && extras.getString("android.title") != null) {
                    i.putExtra("title", extras.getString("android.title"));
                }

                if (extras.containsKey(Notification.EXTRA_TEXT) && extras.getCharSequence("android.text") != null) {
                    i.putExtra("text", extras.getCharSequence("android.text").toString());
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

            if (command.equals("clearall")) {
                mNotificationListener.this.cancelAllNotifications();
            } else if (command.equals("list")) {
                listNotifications();
            } else if (command.equals("release")) {
                Log.d(TAG, "calling stopSelf()");
                mNotificationListener.this.stopSelf();
            }
        }
    }
}
