package com.firebirdberlin.nightdream.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.NightDreamActivity;

public class StopServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (action == null) return;
        Log.i("StopServiceReceiver", action + " received!");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(it);
        }

        NightDreamActivity.start(context, Config.ACTION_STOP_BACKGROUND_SERVICE);
    }
}
