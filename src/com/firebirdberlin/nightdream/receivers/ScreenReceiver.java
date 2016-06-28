package com.firebirdberlin.nightdream.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import de.greenrobot.event.EventBus;

import com.firebirdberlin.nightdream.events.OnScreenOn;

public class ScreenReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            EventBus.getDefault().post(new OnScreenOn());
        }
    }

    public static ScreenReceiver register(Context ctx) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        ScreenReceiver receiver = new ScreenReceiver();
        ctx.registerReceiver(receiver, filter);
        return receiver;
    }

    public static void unregister(Context ctx, BroadcastReceiver receiver) {
        ctx.unregisterReceiver(receiver);
    }
}
