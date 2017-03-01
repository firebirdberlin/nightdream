package com.firebirdberlin.nightdream;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.util.Log;

import java.lang.String;

import com.firebirdberlin.nightdream.Config;

public class accNotificationListener extends AccessibilityService {
    private boolean isInit;
    @Override
    protected void onServiceConnected() {
        if (isInit) {
            return;
        }
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
        setServiceInfo(info);
        isInit = true;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            //Do something, eg getting packagename
            final String packagename = String.valueOf(event.getPackageName());
            Log.w("NightDreamAccessibilityEvent", packagename + " " + event.getText().toString() );

            // Filter out whatsapp
            if (event!=null && event.getPackageName().equals("com.whatsapp")){
                Intent i = new  Intent(Config.ACTION_NOTIFICATION_LISTENER);
                i.putExtra("what","whatsapp");
                i.putExtra("action","added");
                i.putExtra("tickertext",event.getText().toString());
                i.putExtra("number",1);
                sendBroadcast(i);
            } else if (event!=null && event.getPackageName().equals("com.twitter.android")){
                Intent i = new  Intent(Config.ACTION_NOTIFICATION_LISTENER);
                i.putExtra("what","twitter");
                i.putExtra("action","added");
                i.putExtra("tickertext",event.getText().toString());
                i.putExtra("number",1);
                sendBroadcast(i);
            } else if (event!=null && event.getPackageName().equals("com.google.android.gm")){
                Intent i = new  Intent(Config.ACTION_NOTIFICATION_LISTENER);
                i.putExtra("what","gmail");
                i.putExtra("action","added");
                i.putExtra("tickertext",event.getText().toString());
                i.putExtra("number",1);
                sendBroadcast(i);
            } else if (event!=null && event.getPackageName().equals("com.android.phone")){
                Intent i = new  Intent(Config.ACTION_NOTIFICATION_LISTENER);
                i.putExtra("what","phone");
                i.putExtra("action","added");
                i.putExtra("tickertext",event.getText().toString());
                i.putExtra("number",1);
                sendBroadcast(i);
            } else{
                Intent i = new  Intent(Config.ACTION_NOTIFICATION_LISTENER);
                i.putExtra("what","onNotificationPosted :" + event.getPackageName() + "n");
                i.putExtra("action","added");
                i.putExtra("tickertext",event.getText().toString());
                i.putExtra("number",1);
                sendBroadcast(i);
            }
        }
    }

    @Override
    public void onInterrupt() {
        isInit = false;
    }
}
