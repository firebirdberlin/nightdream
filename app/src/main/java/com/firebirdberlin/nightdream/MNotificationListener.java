/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.firebirdberlin.nightdream;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;

import com.firebirdberlin.nightdream.NotificationList.NotificationApp;
import com.firebirdberlin.nightdream.events.AppEventBus;
import com.firebirdberlin.nightdream.events.OnMediaNotificationEvent;
import com.firebirdberlin.nightdream.events.OnNotificationAppsUpdated;
import com.firebirdberlin.nightdream.events.OnNotificationPreviewEvent;
import com.firebirdberlin.nightdream.events.OnNotificationsUpdated;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MNotificationListener extends NotificationListenerService {

    public static boolean running = false;
    private static volatile MNotificationListener instance;
    private static final String TAG = MNotificationListener.class.getSimpleName();
    private final ExecutorService notificationExecutor = Executors.newSingleThreadExecutor();
    private final List<com.firebirdberlin.nightdream.NotificationList.Notification> notifications = new ArrayList<>();
    private final List<NotificationApp> notificationApps = new ArrayList<>();
    int minNotificationImportance = 2;

    public static MNotificationListener getInstance() {
        return instance;
    }

    public static void requestNotificationList(Context context) {
        Log.d(TAG, "requestNotificationList()");
        MNotificationListener listener = instance;
        if (listener != null) {
            listener.listNotifications();
        }
    }

    public static void clearNotifications(List<String> notificationKeys) {
        Log.d(TAG, "clearNotifications()");
        MNotificationListener listener = instance;
        if (listener != null && notificationKeys != null) {
            listener.deleteNotification(new ArrayList<>(notificationKeys));
        }
    }

    public static void clearAllNotifications() {
        Log.d(TAG, "clearAllNotifications()");
        MNotificationListener listener = instance;
        if (listener != null) {
            try {
                listener.cancelAllNotifications();
            } catch (Exception e) {
                Log.e(TAG, "Error cancelling all notifications", e);
            }
        }
    }

    private static Bitmap drawableToBitMap(Drawable drawable) {
        if (drawable == null) {
            return null;
        }

        if (drawable instanceof BitmapDrawable bitmapDrawable) {
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        try {
            int width = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 128;
            int height = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 128;
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, width, height);
            drawable.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error converting drawable to bitmap", e);
            return null;
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
        instance = this;
        running = true;
        Log.i(TAG, "**********  Notification listener STARTED");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        instance = null;
        Log.i(TAG, "**********  Notification listener STOPPED");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        minNotificationImportance = Settings.getMinNotificationImportance(this);

        Log.i(TAG, "++++ notification posted ++++");
        logNotification(sbn);
        if (shallIgnoreNotification(sbn)) return;

        Intent i = getIntentForBroadCast(sbn);
        if (i != null) {
            AppEventBus.publish(new OnNotificationPreviewEvent("added_preview", i));
        }

        listNotifications();

        if (!Utility.isScreenOn(this)) {
            conditionallyStartActivity();
        }
    }

    private boolean shallIgnoreNotification(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        if ("com.firebirdberlin.nightdream".equals(packageName)
                || "com.firebirdberlin.nightdream.noGms".equals(packageName)) {
            Log.d(TAG, "shallIgnoreNotification: own package " + packageName);
            return true;
        }

        Notification notification = sbn.getNotification();
        if (notification == null) {
            Log.d(TAG, "shallIgnoreNotification: notification is null");
            return true;
        }

        Bundle extras = notification.extras;
        String template = null;
        try {
            if (extras != null) {
                template = (String) extras.getCharSequence("android.template");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading notification extras in shallIgnoreNotification", e);
            Log.d(TAG, "shallIgnoreNotification: exception reading extras");
            return true;
        }
        if (template != null && template.contains("MediaStyle")) {
            Log.w(TAG, "MediaStyle notification found");
            return false;
        }

        if (!sbn.isClearable() || sbn.isOngoing()) {
            Log.d(TAG, "shallIgnoreNotification: not clearable or ongoing");
            return true;
        }
        if ((notification.flags & Notification.FLAG_GROUP_SUMMARY) > 0) {
            Log.d(TAG, "shallIgnoreNotification: group summary");
            return true;
        }

        int importance = getImportance(sbn);
        Log.d(TAG, "shallIgnoreNotification: importance=" + importance + " min=" + minNotificationImportance);
        return importance < minNotificationImportance;
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
            String template = IntentCompat.getSerializableExtra(i, "template", String.class);
            if (template != null && template.contains("MediaStyle")) {
                AppEventBus.publish(new OnMediaNotificationEvent("removed_media", i));
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void deleteNotification(ArrayList<String> delete) {
        Log.d(TAG, "deleteNotification");

        if (delete != null) {
            for (String notificationId : delete) {
                Log.d(TAG, "Notification to delete: " + notificationId);
                // Modern way to cancel notification, though the deprecated method is still functional for now.
                // cancelNotification(notificationId);
                // The split logic is for pre-Lollipop, which is not relevant given the SDK_INT check is not needed.
                String[] separated = notificationId.split(";");
                if (separated.length == 3) {
                     // Deprecated method used here for compatibility, but consider modern approach
                     cancelNotification(separated[0], separated[1], Integer.parseInt(separated[2]));
                } else {
                    // For modern versions, just the key should be enough.
                    cancelNotification(notificationId);
                }
            }
        }
    }

    private void listNotifications() {
        Log.d(TAG, "listNotifications()");
        minNotificationImportance = Settings.getMinNotificationImportance(this);
        notifications.clear();
        notificationApps.clear();

        clearNotificationUI();
        notificationExecutor.execute(() -> {
            StatusBarNotification[] notificationList = null;
            try {
                notificationList = MNotificationListener.this.getActiveNotifications();
            } catch (Throwable t) {
                Log.e(TAG, "Error getting active notifications", t);
            }

            if (notificationList == null) return;

            for (StatusBarNotification sbn : notificationList) {
                try {
                    if (sbn == null) continue;
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
                    String template = IntentCompat.getSerializableExtra(i, "template", String.class);
                    if (template == null) {
                        continue;
                    }
                    if (template.contains("MediaStyle")) {
                        AppEventBus.publish(new OnMediaNotificationEvent("added_media", i));
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
                        if (app == null) continue;
                        String appName = app.getName();
                        if (appName == null) appName = "";
                        addApp = addApp && !appName.equals(applicationName);
                        if (appName.equals(applicationName)) {
                            if (app.getPostTimestamp() < sbn.getPostTime()) {
                                app.setPostTimestamp(sbn.getPostTime());
                            }
                        }
                    }
                    if (addApp) {
                        notificationApps.add(new NotificationApp(i));
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Error processing notification in listNotifications", t);
                }
            }
            AppEventBus.publish(new OnNotificationsUpdated(new ArrayList<>(notifications)));
            AppEventBus.publish(new OnNotificationAppsUpdated(new ArrayList<>(notificationApps)));
        });
    }

    @SuppressWarnings("deprecation")
    private Intent getIntentForBroadCast(StatusBarNotification sbn) {
        final Context context = this;
        if (sbn == null){
            return null;
        }
        Notification notification = sbn.getNotification();
        if (notification == null) {
            return null;
        }
        String packageName = sbn.getPackageName();
        String applicationName = getApplicationLabel(packageName);
        Bitmap largeIconBitmap = Utility.scaleBitmap(getLargeIconBitmap(context, sbn), 128, 128);
        Notification.Action[] actions = null;
        actions = notification.actions;

        int color = notification.color;
        PendingIntent contentIntent = notification.contentIntent;
        Bitmap bigPicture = null;
        CharSequence summaryText = "";
        CharSequence template = "";
        CharSequence textBigData = "";
        CharSequence textData = "";
        CharSequence titleBigData = "";
        CharSequence titleData = "";
        StringBuilder textLinesData = new StringBuilder();
        Bundle extras = notification.extras;

        try {
            if (extras != null) {
                template = extras.getCharSequence("android.template");
                if (template == null) template = "";
                if (extras.containsKey(Notification.EXTRA_PICTURE)) {
                    Object pictureObj = extras.get(Notification.EXTRA_PICTURE);
                    if (pictureObj instanceof Bitmap) {
                        bigPicture = Utility.scaleBitmap((Bitmap) pictureObj, 128, 128);
                    }
                }

                if (extras.containsKey(Notification.EXTRA_TITLE) && extras.getCharSequence("android.title") != null) {
                    titleData = extras.getCharSequence(Notification.EXTRA_TITLE);
                }
                if (extras.containsKey(Notification.EXTRA_TITLE_BIG) && extras.getCharSequence("android.title.big") != null) {
                    titleBigData = extras.getCharSequence(Notification.EXTRA_TITLE_BIG);
                }

                //this is the longer text shown in the big form of a BigTextStyle notification,
                if (extras.containsKey(Notification.EXTRA_BIG_TEXT) && extras.getCharSequence("android.bigText") != null) {
                    textBigData = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
                }

                //this is the summary information intended to be shown alongside expanded notification
                if (extras.containsKey(Notification.EXTRA_SUMMARY_TEXT) && extras.getCharSequence("android.summaryText") != null) {
                    summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);
                }
                if (extras.containsKey(Notification.EXTRA_TEXT) && extras.getCharSequence("android.text") != null) {
                    textData = extras.getCharSequence(Notification.EXTRA_TEXT);
                }

                //An array of CharSequences to show in InboxStyle expanded notifications
                if (extras.containsKey(Notification.EXTRA_TEXT_LINES)) {
                    CharSequence[] messages = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
                    if (messages != null) {
                        for (CharSequence message : messages) {
                            if (textLinesData.toString().isEmpty()) {
                                textLinesData = new StringBuilder(message.toString());
                            } else {
                                textLinesData.append("<br>").append(message.toString());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading notification extras in getIntentForBroadCast", e);
        }

        // the "ticker" text which is sent to accessibility services.
        CharSequence tickerText = "";
        if (notification.tickerText != null) {
            tickerText = notification.tickerText;
        }


        // Extract MessagingStyle object from the active notification.
        StringBuilder notification_messages = new StringBuilder();
        if (template.toString().contains("MessagingStyle")) {
            try {
                String lastPerson = "";
                NotificationCompat.MessagingStyle activeStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification);
                if (activeStyle != null) {
                    for (NotificationCompat.MessagingStyle.Message message : activeStyle.getMessages()) {
                        if (message.getPerson() != null && message.getPerson().getName() != null) {
                            if (lastPerson.contentEquals(message.getPerson().getName())) {
                                notification_messages.append("<br>").append(message.getText());
                            } else {
                                if (notification_messages.toString().isEmpty()) {
                                    notification_messages = new StringBuilder(message.getPerson().getName() + "<br> " + message.getText());
                                } else {
                                    notification_messages.append("<br>").append(message.getPerson().getName()).append("<br> ").append(message.getText());
                                }
                                lastPerson = message.getPerson().getName().toString();
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                // Defensive catch for library-internal NullPointerException in androidx.core.app.Person$Api28Impl
                Log.e(TAG, "Error extracting MessagingStyle in getIntentForBroadCast", t);
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
        if (extras != null && extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) {
            Object tokenObj = extras.get(Notification.EXTRA_MEDIA_SESSION);
            if (tokenObj instanceof android.media.session.MediaSession.Token) {
                intent.putExtra("mediaSessionToken", (android.media.session.MediaSession.Token) tokenObj);
            }
        }

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
        intent.putExtra("smallIconBitmap", Utility.scaleBitmap(getSmallIconBitmap(context, sbn), 128, 128));
        intent.putExtra("summaryText", summaryText != null ? summaryText.toString() : null);
        intent.putExtra("template", template.toString());
        intent.putExtra("text", textData != null ? textData.toString() : null);
        intent.putExtra("textBig", textBigData != null ? textBigData.toString() : null);
        intent.putExtra("textLines", textLinesData.toString());
        intent.putExtra("tickerText", tickerText);
        intent.putExtra("timestamp", date);
        intent.putExtra("title", titleData != null ? titleData.toString() : null);
        intent.putExtra("titleBig", titleBigData != null ? titleBigData.toString() : null);
        intent.putExtra("key", sbn.getKey());

        intent.setPackage(getPackageName());
        return intent;
    }

    private Bitmap getSmallIconBitmap(Context context, StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        Icon icon = notification.getSmallIcon();
        if (icon == null) return null;
        Drawable drawable = icon.loadDrawable(context);
        if (drawable == null) return null; // Added null check for drawable
        return drawableToBitMap(drawable);
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
            Log.e(TAG, "Error getting notification icon from package", e);
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private int getIconId(Notification notification) {
        Log.d(TAG, "getIconId(" + notification.toString() + ")");
        int iconId = -1;
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                Icon icon = notification.getSmallIcon();
                iconId = icon != null ? icon.getResId() : -1;
            } else {
                iconId = notification.extras.getInt(Notification.EXTRA_SMALL_ICON, -1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting icon ID", e);
        }
        Log.d(TAG, "getIconId result: " + iconId);
        return iconId;
    }

    int getImportance(StatusBarNotification sbn) {
        Ranking ranking = getRanking(sbn);
        if (ranking != null) {
            return ranking.getImportance();
        }
        return android.app.NotificationManager.IMPORTANCE_DEFAULT;
    }

    Ranking getRanking(StatusBarNotification sbn) {
        if (sbn == null) return null;
        String notificationKey = sbn.getKey();
        RankingMap rankingMap = getCurrentRanking();
        if (rankingMap == null) {
            return null;
        }
        Ranking ranking = new Ranking();
        if (rankingMap.getRanking(notificationKey, ranking)) {
            return ranking;
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private void logNotification(StatusBarNotification sbn) {
        if (sbn == null || Build.VERSION.SDK_INT < 26) return;
        Notification notification = sbn.getNotification();
        if (notification == null) return;
        CharSequence title = "";
        CharSequence text = "";
        try {
            if (notification.extras != null) {
                title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
                text = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading notification extras in logNotification", e);
        }
        String group_key = notification.getGroup();
        Log.i(TAG, "ID :" + sbn.getId()
                + "\t" + title
                + "\t" + text
                + "\t" + notification.tickerText
                + "\t" + notification.number
                + "\t" + sbn.getPackageName()
                + "\t" + notification.priority // Deprecated
                + "\t" + group_key
        );
        //Log.d(TAG, notification.toString());
    }

    private void clearNotificationUI() {
        AppEventBus.publish(new OnNotificationPreviewEvent("clear", null));
    }

    private String getApplicationLabel(String packageName) {
        final PackageManager pm = getApplicationContext().getPackageManager();
        ApplicationInfo ai;

        try {
            ai = pm.getApplicationInfo(packageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package name not found: " + packageName, e);
            return "(unknown)";
        }
        return (String) pm.getApplicationLabel(ai);
    }

    @SuppressWarnings("deprecation")
    private Bitmap getLargeIconBitmap(Context context, StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();

        Icon largeIcon = notification.getLargeIcon();
        if (largeIcon != null) {
            try {
                Drawable d = largeIcon.loadDrawable(context);
                if (d != null) {
                    Bitmap bmp = drawableToBitMap(d);
                    if (bmp != null) return bmp;
                }
            } catch (Exception ignored) {
            }
        }

        Bundle extras = notification.extras;
        if (extras != null) {
            try {
                Object largeIconObj = extras.get(Notification.EXTRA_LARGE_ICON);
                if (largeIconObj instanceof Bitmap) {
                    return (Bitmap) largeIconObj;
                } else if (largeIconObj instanceof Icon) {
                    Drawable d = ((Icon) largeIconObj).loadDrawable(context);
                    if (d != null) {
                        Bitmap bmp = drawableToBitMap(d);
                        if (bmp != null) return bmp;
                    }
                }

                Object largeIconBigObj = extras.get(Notification.EXTRA_LARGE_ICON_BIG);
                if (largeIconBigObj instanceof Bitmap) {
                    return (Bitmap) largeIconBigObj;
                } else if (largeIconBigObj instanceof Icon) {
                    Drawable d = ((Icon) largeIconBigObj).loadDrawable(context);
                    if (d != null) {
                        Bitmap bmp = drawableToBitMap(d);
                        if (bmp != null) return bmp;
                    }
                }

                Object pictureObj = extras.get(Notification.EXTRA_PICTURE);
                if (pictureObj instanceof Bitmap) {
                    return (Bitmap) pictureObj;
                } else if (pictureObj instanceof Icon) {
                    Drawable d = ((Icon) pictureObj).loadDrawable(context);
                    if (d != null) {
                        Bitmap bmp = drawableToBitMap(d);
                        if (bmp != null) return bmp;
                    }
                }

                if (extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) {
                    Object tokenObj = extras.get(Notification.EXTRA_MEDIA_SESSION);
                    if (tokenObj instanceof android.media.session.MediaSession.Token token) {
                        android.media.session.MediaController controller = new android.media.session.MediaController(context, token);
                        android.media.MediaMetadata metadata = controller.getMetadata();
                        if (metadata != null) {
                            Bitmap art = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART);
                            if (art == null) {
                                art = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART);
                            }
                            if (art == null) {
                                String uriStr = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART_URI);
                                if (Utility.isEmpty(uriStr)) {
                                    uriStr = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ART_URI);
                                }
                                if (!Utility.isEmpty(uriStr)) {
                                    art = Utility.getBitmapFromUri(context, uriStr);
                                }
                            }
                            if (art != null) {
                                return art;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (notification.largeIcon != null) {
            return notification.largeIcon;
        }

        return null;
    }
}
