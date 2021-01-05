package com.firebirdberlin.nightdream.NotificationList;

import android.annotation.SuppressLint;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.widget.RemoteViews;

import com.firebirdberlin.nightdream.Config;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationService extends NotificationListenerService {

    public static String TAG = "NotificationService";
    Context context;
    private int nAdded=0;
    private List<com.firebirdberlin.nightdream.NotificationList.Notification> notificationlist = new ArrayList<>();
    private List <com.firebirdberlin.nightdream.NotificationList.NotificationApp> notificationapplist = new ArrayList<>();
    private BroadcastReceiver nlservicereceiver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //retrieving data from the received intent

        if(intent.hasExtra("command")) {
            Log.d(TAG,"NL - onStartCommand:" + intent.getStringExtra("command"));

            switch (intent.getStringExtra("command")) {
                case "statistik":
                    Log.d(TAG, "statistik");
                    Intent intent_statistik = new Intent("Msg");
                    intent_statistik.putExtra("serviceMessage", "Added: " + nAdded);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent_statistik);
                    break;
                case "getnotificationlist":
                    Log.d(TAG, "getnotificationlist");
                    Intent intent_notificationlist = new Intent("Notification.Action.notificationlist");
                    intent_notificationlist.putExtra("getnotificationlist", "Added: " + nAdded);
                    intent_notificationlist.putParcelableArrayListExtra("notificationlist", (ArrayList<? extends Parcelable>) notificationlist);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent_notificationlist);
                    break;
                case "getnotificationapplist":
                    Log.d(TAG, "getnotificationapplist");
                    Intent intent_notificationapplist = new Intent("Msg");
                    intent_notificationapplist.putParcelableArrayListExtra("notificationapplist", (ArrayList<? extends Parcelable>) notificationapplist);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent_notificationapplist);
                    break;
                case "delete_notification":
                    String notifypackage = "";

                    try {
                        if (notificationlist != null) {

                            //delete notifications

                            ArrayList<com.firebirdberlin.nightdream.NotificationList.Notification> get_delete_in = new ArrayList<>();

                            ArrayList<com.firebirdberlin.nightdream.NotificationList.Notification> to_delete = new ArrayList<>();

                            ArrayList<Integer> delete = intent.getIntegerArrayListExtra("to_delete_notification");
                            assert delete != null;
                            for (int index = 0; index < delete.size(); index++) {

                                //for (int delete : Objects.requireNonNull(intent.getIntegerArrayListExtra("to_delete_notification"))) {

                                notifypackage = notificationlist.get(delete.get(index)).get_notification_package();
                                get_delete_in.add(notificationlist.get((Integer) delete.get(index)));
                                // notificationlist.remove(notificationlist.get(test));
                            }

                            for (com.firebirdberlin.nightdream.NotificationList.Notification notify : get_delete_in) {
                                notificationlist.remove(notify);
                            }

                            //delete from notificationapplist
                            if (!notifypackage.equals("")) {
                                int found = 0;
                                for (com.firebirdberlin.nightdream.NotificationList.Notification search_package : notificationlist) {
                                    if (search_package.get_notification_package().equals(notifypackage)) {
                                        found++;
                                    }
                                }

                                if (found == 0) {
                                    NotificationApp app_to_delete = null;
                                    for (NotificationApp deleteapp : notificationapplist) {
                                        if (deleteapp.get_notificationapp_package().equals(notifypackage)) {
                                            app_to_delete = deleteapp;
                                        }
                                    }
                                    notificationapplist.remove(app_to_delete);
                                }
                            }
                        }//end if
                    }
                    catch (Exception ex){
                        Log.e(TAG, "Unkonwn Error in Delete Apps NotificationService ", ex);
                    }
                    Intent intent_notificationlist_delete = new Intent("Notification.Action.notificationlist");
                    intent_notificationlist_delete.putParcelableArrayListExtra("notificationlist", (ArrayList<? extends Parcelable>) notificationlist);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent_notificationlist_delete);
                    break;
            }
        }

        if(intent.hasExtra("getnotificationlist")) {
            Log.d(TAG,"getnotificationlist");
            Intent i1 = new  Intent("Msg");
            i1.putParcelableArrayListExtra("notificationlist", (ArrayList<? extends Parcelable>) notificationlist);
            LocalBroadcastManager.getInstance(context).sendBroadcast(i1);
        }
        super.onStartCommand(intent, flags, startId);

        //prevent the automatic service termination
        return START_STICKY;
    }


    @Override
    public void onCreate() {
        Log.d(TAG,"onCreate");
        super.onCreate();
        context = getApplicationContext();

        nlservicereceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

            }

        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.firebirdberlin.nightdream.NOTIFICATION_LISTENER_SERVICE");

        try {
            registerReceiver(nlservicereceiver, filter);
            Log.d(TAG, "registerReceiver");
        }
        catch (Exception ex) {
            Log.e(TAG, "Error: "+ex);
        }

    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
    }

    @SuppressLint("RestrictedApi")
    @RequiresApi(api = Build.VERSION_CODES.P)
    @Deprecated
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(TAG,"onNotificationPosted");
        Intent msgrcv = new Intent("Msg");

        //making informations as html
        StringBuilder HTMLtext = new StringBuilder();

        String titleData="", titleBigData="", textData="", textLinesData="",template="", summaryText="", bigiconData="";

        String packageName = sbn.getPackageName();
        msgrcv.putExtra("package", packageName);
        HTMLtext.append("<b> PackageName: </b>").append(packageName).append("<br>");

        //get application packageName
        final PackageManager pm = getApplicationContext().getPackageManager();
        ApplicationInfo ai;

        try {
            ai = pm.getApplicationInfo( packageName, 0);
        }
        catch (final PackageManager.NameNotFoundException e){
            ai = null;
        } final

        String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
        msgrcv.putExtra("applicationname", applicationName);

        //get small icon from notification
        Icon smallicon = sbn.getNotification().getSmallIcon();
        if (smallicon != null) {
            msgrcv.putExtra("icon", smallicon);
            msgrcv.putExtra("iconresid", smallicon.getResId());
            HTMLtext.append("<b> SmallIcon: </b>").append(smallicon).append("<br>");
            HTMLtext.append("<b> SmallIcon - ResID: </b>").append(smallicon.getResId()).append("<br>");

            //SmallIcon to Bitmap
            Bitmap picture = drawableToBitMap(smallicon.loadDrawable(context));
            msgrcv.putExtra("bitmap", picture);
            //HTMLtext = HTMLtext + "<b> Bitmap: </b>" + picture + "<br>";
        }
        else{
            HTMLtext.append("<b> SmallIcon: </b> Not found<br>");
        }

        //get large icon from notification
        Icon largeicon = sbn.getNotification().getLargeIcon();
        if (largeicon != null) {
            Bitmap largeiconbitmap = drawableToBitMap(largeicon.loadDrawable(context));

            msgrcv.putExtra("largeicon", largeicon);
            msgrcv.putExtra("largeiconbitmap", largeiconbitmap );
            HTMLtext.append("<b> LargeIcon: </b>").append(largeicon).append("<br>");
        }
        else {
            HTMLtext.append("<b> LargeIcon: </b> Not found<br>");
        }

        msgrcv.putExtra("id", sbn.getId() );
        HTMLtext.append("<b> ID: </b>").append(sbn.getId()).append("<br>");

        if (!sbn.getNotification().getChannelId().equals(""))
        {
            msgrcv.putExtra("channelid", sbn.getNotification().getChannelId() );
            HTMLtext.append("<b> ChannelID: </b>").append(sbn.getNotification().getChannelId()).append("<br>");
        }

        Notification notification = sbn.getNotification();

        msgrcv.putExtra("notificationpriority", notification.priority );
        HTMLtext.append("<b> Priority: </b>").append(notification.priority ).append("<br>");

        // check the notification's flags for either Notification#FLAG_ONGOING_EVENT or Notification#FLAG_NO_CLEAR.
        msgrcv.putExtra("clearable", sbn.isClearable() );
        HTMLtext.append("<b> Clearable: </b>").append(sbn.isClearable() ).append("<br>");

        msgrcv.putExtra("ongoing", sbn.isOngoing() );
        HTMLtext.append("<b> Ongoing: </b>").append(sbn.isOngoing() ).append("<br>");


        msgrcv.putExtra("key", sbn.getKey() );
        HTMLtext.append("<b> Key: </b>").append(sbn.getKey() ).append("<br>");

        if (notification.getSortKey() != null) {
            msgrcv.putExtra("sortkey", notification.getSortKey());
            HTMLtext.append("<b> SortKey: </b>").append(notification.getSortKey()).append("<br>");
        }

        msgrcv.putExtra("color", notification.color);
        HTMLtext.append("<b> Color: </b>").append(notification.color).append("<br>");

        msgrcv.putExtra("visibility", notification.visibility);
        HTMLtext.append("<b> Visibility: </b>").append(notification.visibility).append("<br>");

        //get channelinformations
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = manager.getNotificationChannel(sbn.getNotification().getChannelId());

        if (channel != null)
        {
            // the user visible description of this channel.
            msgrcv.putExtra("channeldescription", channel.getDescription() );
            HTMLtext.append("<b> ChannelDescription: </b>").append(channel.getDescription() ).append("<br>");

            // the user specified importance for notifications posted to this channel.
            msgrcv.putExtra("channelimportance", channel.getImportance() );
            HTMLtext.append("<b> ChannelImportance: </b>").append(channel.getImportance() ).append("<br>");

            // what group this channel belongs to.
            if ( channel.getGroup() != null) {
                msgrcv.putExtra("channelgroup", channel.getGroup());
                HTMLtext.append("<b> ChannelGroup: </b>").append(channel.getGroup()).append("<br>");
            }

            // the user visible name of this channel.
            msgrcv.putExtra("channelname", channel.getName() );
            HTMLtext.append("<b> ChannelName: </b>").append(channel.getName() ).append("<br>");

            // whether or not notifications posted to this channel are shown on the lockscreen in full or redacted form.
            msgrcv.putExtra("lockscreenvisibility", channel.getLockscreenVisibility() );
            HTMLtext.append("<b> ChannelLockscreenVisibility: </b>").append(channel.getLockscreenVisibility() ).append("<br>");
        }

        //get extra informations from notification
        Bundle extras = sbn.getNotification().extras;

        //Actions in Notification
        try {
            Notification.Action[] action = sbn.getNotification().actions;
            HTMLtext.append("<b> Notification Actions: </b><br>");
            msgrcv.putExtra("action", action);
            for (Notification.Action actionin : sbn.getNotification().actions) {
                HTMLtext.append("&nbsp - Action Title: ").append(actionin.title.toString()).append("<br>");
                HTMLtext.append("&nbsp - Action Intent: ").append(actionin.actionIntent).append("<br>");
            }
        }catch (Exception ex){

        }

        //get content Intent
        PendingIntent contentIntent = sbn.getNotification().contentIntent;
        if (contentIntent != null) {
            HTMLtext.append("<b> ContentIntent: </b>").append(contentIntent).append("<br>");
        }
        msgrcv.putExtra("contentintent", contentIntent);

        //Show Notification Keys
        HTMLtext.append("<b> Notification Keys: </b><br>");

        for (String key : extras.keySet()) {
            Object value = extras.get(key);
            HTMLtext.append("-").append(key).append("<br>");
            //Test for Android Wearable
            if("android.wearable.EXTENSIONS".equals(key)) {
                Bundle wearBundle = ((Bundle) value);
                assert wearBundle != null;
                for (String keyInner : wearBundle.keySet()) {
                    Object valueInner = wearBundle.get(keyInner);
                    HTMLtext.append("&nbsp &nbsp - InnerKey: ").append(keyInner).append("<br>");

                    if(keyInner != null && valueInner != null) {
                        //HTMLtext = HTMLtext + "<b> --- KeyInner: </b>" +  "<br>";
                        if ("actions".equals(keyInner) && valueInner instanceof ArrayList) {
                            //  HTMLtext = HTMLtext + "<b> --- Actions: </b>" +  "<br>";
                            ArrayList<Notification.Action> actions = new ArrayList<>();
                            actions.addAll((ArrayList) valueInner);
                            for (Notification.Action act : actions) {
                                Bundle extrasact = act.getExtras();
                                for (String keyact : extrasact.keySet()) {
                                    HTMLtext.append("&nbsp &nbsp &nbsp -").append(keyact).append("<br>");
                                }
                                if (act.getRemoteInputs() != null) {//API > 20 needed
                                    android.app.RemoteInput[] remoteInputs = act.getRemoteInputs();
                                    HTMLtext.append("&nbsp &nbsp &nbsp &nbsp - RemoteInputs inside").append("<br>");
                                }
                            }
                        }
                    }
                }
            }
        }

        //this is the bigPicture of the BigPictureStyle
        Bitmap bigpicture = null;
        if(extras.containsKey(Notification.EXTRA_PICTURE)){
            bigpicture = (Bitmap) extras.get(Notification.EXTRA_PICTURE);
            HTMLtext.append("<b> BigPicture: </b>").append(bigpicture).append("<br>");
        }
        msgrcv.putExtra("bigpicture", bigpicture);

        //this is the title of the notification
        if(extras.containsKey(Notification.EXTRA_TITLE) && extras.getString("android.title") != null){
            titleData = extras.getString("android.title");
            HTMLtext.append("<b> Titel: </b>").append(titleData).append("<br>");
        }
        msgrcv.putExtra("title", titleData);

        //this is the title of the notification when shown in expanded form
        if(extras.containsKey(Notification.EXTRA_TITLE_BIG) && extras.getCharSequence("android.title.big") != null){
            titleBigData = extras.getCharSequence("android.title.big").toString();
            HTMLtext.append("<b> TitelBig: </b>").append(titleBigData).append("<br>");
        }
        msgrcv.putExtra("titlebig", titleBigData);

        String textBigData="";
        //this is the longer text shown in the big form of a BigTextStyle notification,
        if(extras.containsKey(Notification.EXTRA_BIG_TEXT) && extras.getCharSequence("android.bigText") != null){
            textBigData = extras.getCharSequence("android.bigText").toString();
            HTMLtext.append("<b> TextBig: </b>").append(textBigData).append("<br>");
        }
        msgrcv.putExtra("textbig", textBigData);

        //this is the summary information intended to be shown alongside expanded notification
        if(extras.containsKey(Notification.EXTRA_SUMMARY_TEXT) && extras.getCharSequence("android.summaryText") != null){
            summaryText = extras.getCharSequence("android.summaryText").toString();
            HTMLtext.append("<b> SummaryText: </b>").append(summaryText).append("<br>");
        }
        msgrcv.putExtra("summarytext", summaryText);

        //this is the main text
        if(extras.containsKey(Notification.EXTRA_TEXT) && extras.getCharSequence("android.text") != null){
            textData = extras.getCharSequence("android.text").toString();
            HTMLtext.append("<b> Text: </b>").append(textData).append("<br>");
        }
        msgrcv.putExtra("text", textData);

        //A string representing the name of the specific Notification.Style used to create this notification.
        if(extras.containsKey(Notification.EXTRA_TEMPLATE) && extras.getCharSequence("android.template") != null){
            template = extras.getCharSequence("android.template").toString();
            HTMLtext.append("<b> Template: </b>").append(template).append("<br>");
        }
        msgrcv.putExtra("template", template);

        // the "ticker" text which is sent to accessibility services.
        CharSequence ticker="";
        if(sbn.getNotification().tickerText!=null){
            ticker = sbn.getNotification().tickerText;
            HTMLtext.append("<b> Ticker: </b>").append(ticker).append("<br>");
        }
        msgrcv.putExtra("ticker", ticker);

        //An array of CharSequences to show in InboxStyle expanded notifications
        if(extras.containsKey(Notification.EXTRA_TEXT_LINES)){
            CharSequence[] messages = extras.getCharSequenceArray("android.textLines");
            if (messages != null) {
                for (CharSequence message : messages) {
                    if (textLinesData.equals("")) {
                        textLinesData = message.toString();
                    } else {
                        textLinesData = textLinesData + "<br>" + message.toString();
                    }
                }
            }
            HTMLtext.append("<b> TextLines: </b><br>").append(textLinesData).append("<br>");
        }
        msgrcv.putExtra("textlines", textLinesData);

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
                }
                else{
                    if (notification_messages.toString().toString().equals(""))
                    {
                        notification_messages = new StringBuilder(message.getPerson().getName() + "<br> " + message.getText());
                    }
                    else{
                        notification_messages.append("<br>").append(message.getPerson().getName()).append("<br> ").append(message.getText());
                    }

                    lastPerson =  message.getPerson().getName().toString();
                }
            }
            HTMLtext.append("<b> Notification Messages: </b><br>").append(notification_messages).append("<br>");
        }catch (Exception e) {
        }
        msgrcv.putExtra("messages", notification_messages.toString());

        //Contentview

        RemoteViews Rviews = sbn.getNotification().contentView;

        int layoutid = -1;
        String layoutpackage = "";

        if (Rviews!=null) {
            //Returns the layout id of the root layout associated with this RemoteViews
            layoutid = Rviews.getLayoutId();
            HTMLtext.append("<b> Content - LayoutID: </b>").append(layoutid).append("<br>");
            layoutpackage = Rviews.getPackage();
            HTMLtext.append("<b> Content - LayoutPackage: </b>").append(layoutpackage).append("<br>");
            msgrcv.putExtra("view", Rviews);
            //HTMLtext = HTMLtext + "<b> LayoutID: </b>" + Rviews + "<br>";
        }

        //BigContentview
        Rviews = sbn.getNotification().bigContentView;

        layoutid = -1;
        layoutpackage = "";

        if (Rviews!=null) {
            //Returns the layout id of the root layout associated with this RemoteViews
            layoutid = Rviews.getLayoutId();
            HTMLtext.append("<b> BigContent - LayoutID: </b>").append(layoutid).append("<br>");
            layoutpackage = Rviews.getPackage();
            HTMLtext.append("<b> BigContent - LayoutPackage: </b>").append(layoutpackage).append("<br>");
            msgrcv.putExtra("bigview", Rviews);
            //HTMLtext = HTMLtext + "<b> LayoutID: </b>" + Rviews + "<br>";
        }

        //get date and time
        Date myDate = new Date();
        //date formatation
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.ZZZZ",java.util.Locale.getDefault());
        //formate date
        String date = dateFormat.format(myDate);
        //date formatation
        dateFormat = new SimpleDateFormat("HH:mm",java.util.Locale.getDefault());
        String postTime = dateFormat.format(sbn.getPostTime());

        msgrcv.putExtra("posttimestamp", String.valueOf(sbn.getPostTime()));
        msgrcv.putExtra("timestamp", date);
        msgrcv.putExtra("posttime", postTime);
        msgrcv.putExtra("channelid",sbn.getNotification().getChannelId());
        msgrcv.putExtra("htmltext", HTMLtext.toString());

        nAdded++;

        //Add new notification
        notificationlist.add(new com.firebirdberlin.nightdream.NotificationList.Notification(getApplicationContext(), msgrcv));


        Log.d(TAG, "sendBroadcast");

        //Send notificationlist
        Intent notificationlist_intent =  new Intent("Notification.Action.notificationlist");
        notificationlist_intent.putParcelableArrayListExtra("notificationlist", (ArrayList<? extends Parcelable>) notificationlist);
        LocalBroadcastManager.getInstance(context).sendBroadcast(notificationlist_intent);

        //Add AppList
        Intent notificationapplist_intent =  new Intent("Msg");

        notificationapplist_intent.putExtra("name",msgrcv.getStringExtra("applicationname"));
        notificationapplist_intent.putExtra("package",msgrcv.getStringExtra("package"));
        notificationapplist_intent.putExtra("timestamp",msgrcv.getStringExtra("posttime"));
        notificationapplist_intent.putExtra("posttimestamp", String.valueOf(sbn.getPostTime()));

        //Remove old application from applicationapplist
        /*
        for (Iterator <NotificationApp> iterator = notificationapplist.iterator(); iterator.hasNext();) {

            NotificationApp notifyapp = iterator.next();
            if (notifyapp.get_notificationapp_name().equals(msgrcv.getStringExtra("applicationname"))) {
                iterator.remove();
            }
        }
        */

        for (int index = 0; index < notificationapplist.size(); index++) {
            if (notificationapplist.get(index).get_notificationapp_name().equals(msgrcv.getStringExtra("applicationname"))) {
                notificationapplist.remove(notificationapplist.get(index));
            }
        }


        notificationapplist.add(new com.firebirdberlin.nightdream.NotificationList.NotificationApp(getApplicationContext(), notificationapplist_intent));
        notificationapplist_intent.putParcelableArrayListExtra("notificationapplist", (ArrayList<? extends Parcelable>) notificationapplist);

        //Send notificationapplist
        LocalBroadcastManager.getInstance(context).sendBroadcast(notificationapplist_intent);


    }

    private static Bitmap drawableToBitMap(Drawable drawable) {
        if (drawable == null) {
            return null;
        }

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = ((BitmapDrawable) drawable);
            return bitmapDrawable.getBitmap();
        }
        else {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                    drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            drawable.draw(canvas);
            return bitmap;
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        //do something or nothing
    }

    @Override
    public void onDestroy() {

        try{
            if(nlservicereceiver!=null)
                unregisterReceiver(nlservicereceiver);

        }catch(Exception e){}

        super.onDestroy();
    }

}