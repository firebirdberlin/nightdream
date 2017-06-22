package com.firebirdberlin.nightdream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";
    private static boolean isDebuggable = true;

    ImageView CallIcon;
    ImageView EmailIcon;
    ImageView TwitterIcon;
    ImageView WhatsappIcon;
    TextView EmailNumber;
    TextView TwitterNumber;
    TextView WhatsappNumber;

    private int EmailCount;
    private int TwitterCount;
    private int WhatsappCount;

    public NotificationReceiver(Window window) {
        View v = window.getDecorView().findViewById(android.R.id.content);
        CallIcon = (ImageView) v.findViewById(R.id.call_icon);
        EmailIcon = (ImageView) v.findViewById(R.id.gmail_icon);
        EmailNumber = (TextView) v.findViewById(R.id.gmail_number);
        TwitterIcon = (ImageView) v.findViewById(R.id.twitter_icon);
        TwitterNumber = (TextView) v.findViewById(R.id.twitter_number);
        WhatsappIcon = (ImageView) v.findViewById(R.id.whatsapp_icon);
        WhatsappNumber = (TextView) v.findViewById(R.id.whatsapp_number);

        CallIcon.setVisibility(View.INVISIBLE);
        EmailIcon.setVisibility(View.INVISIBLE);
        EmailNumber.setVisibility(View.INVISIBLE);
        TwitterIcon.setVisibility(View.INVISIBLE);
        TwitterNumber.setVisibility(View.INVISIBLE);
        WhatsappIcon.setVisibility(View.INVISIBLE);
        WhatsappNumber.setVisibility(View.INVISIBLE);
        WhatsappIcon.setVisibility(View.INVISIBLE);

        EmailCount = 0;
        TwitterCount = 0;
        WhatsappCount = 0;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        if (isDebuggable){
            Log.d(TAG, "Broadcast received.");
            dumpIntent(intent);
        }
        String packageName = intent.getStringExtra("packageName");
        int iconId = intent.getIntExtra("iconId", -1);
        Log.i(TAG, String.format("%s: %d", packageName, iconId));
        Drawable icon = getNotificationIcon(context, packageName, iconId);
        try{
            if (intent.getStringExtra("what").equals("whatsapp")){
                if (intent.getStringExtra("action").equals("added")){
                    String temp = intent.getStringExtra("tickertext");
                    int num = intent.getIntExtra("number", 1);
                    WhatsappIcon.setVisibility(View.VISIBLE);
                    WhatsappCount++;
                    if (Build.VERSION.SDK_INT >= 18 && num > 0) WhatsappNumber.setText(String.valueOf(num));
                    else WhatsappNumber.setText(String.valueOf(WhatsappCount));
                    WhatsappNumber.setVisibility(View.VISIBLE);
                } else if (intent.getStringExtra("action").equals("removed")){
                    WhatsappIcon.setVisibility(View.INVISIBLE);
                    WhatsappNumber.setVisibility(View.INVISIBLE);
                    WhatsappCount = 0;
                }
            }else if (intent.getStringExtra("what").equals("twitter")){
                if (intent.getStringExtra("action").equals("added")){
                    String temp = intent.getStringExtra("tickertext");
                    int num = intent.getIntExtra("number", 1);

                    if (icon != null) {
                        Log.i(TAG, "twitter icon is set");
                        TwitterIcon.setImageDrawable(icon);
                    }
                    TwitterIcon.setVisibility(View.VISIBLE);
                    TwitterIcon.invalidate();
                    TwitterCount++;
                    if (Build.VERSION.SDK_INT >= 18 && num > 0) TwitterNumber.setText(String.valueOf(num));
                    else TwitterNumber.setText(String.valueOf(TwitterCount));
                    TwitterNumber.setVisibility(View.VISIBLE);

                } else if (intent.getStringExtra("action").equals("removed")){
                    TwitterIcon.setVisibility(View.INVISIBLE);
                    TwitterIcon.setImageDrawable(icon);
                    TwitterNumber.setVisibility(View.INVISIBLE);
                    TwitterCount = 0;
                }
            }else if (intent.getStringExtra("what").equals("gmail")){
                if (intent.getStringExtra("action").equals("added")){
                    String temp = intent.getStringExtra("tickertext");
                    int num = intent.getIntExtra("number", 1);

                    EmailIcon.setVisibility(View.VISIBLE);
                    EmailCount++;
                    if (Build.VERSION.SDK_INT >= 18 && num > 0)
                        EmailNumber.setText(String.valueOf(num));
                    else
                        EmailNumber.setText(String.valueOf(EmailCount));
                    EmailNumber.setVisibility(View.VISIBLE);
                } else if (intent.getStringExtra("action").equals("removed")){
                    EmailIcon.setVisibility(View.INVISIBLE);
                    EmailNumber.setVisibility(View.INVISIBLE);
                    EmailCount = 0;
                }
            }else if (intent.getStringExtra("what").equals("phone")){
                if (intent.getStringExtra("action").equals("added")){
                    String temp = intent.getStringExtra("tickertext");
                    CallIcon.setVisibility(View.VISIBLE);
                } else if (intent.getStringExtra("action").equals("removed")){
                    CallIcon.setVisibility(View.INVISIBLE);
                }
            }
        } catch (Exception e) {};
    }

    private Drawable getNotificationIcon(Context context, String packageName, int id) {
        if (packageName == null || id == -1) return null;
        //String pack= "com.whatsapp" // ex. for whatsapp;
        try {
            Context remotePackageContext = context.getApplicationContext().createPackageContext(packageName, 0);
            return remotePackageContext.getResources().getDrawable(id);
        } catch (NameNotFoundException e) {
            return null;
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    public static void dumpIntent(Intent i){
        Bundle bundle = i.getExtras();
        if (bundle == null) return;
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            if (value != null) {
                Log.d(TAG, String.format("%s %s (%s)", key,
                            value.toString(), value.getClass().getName()));
            }
        }
    }

}
