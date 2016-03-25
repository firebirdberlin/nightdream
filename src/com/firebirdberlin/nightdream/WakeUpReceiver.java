package com.firebirdberlin.nightdream;
import android.app.Activity;
import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;
import android.content.SharedPreferences;

public class WakeUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        String cmd = "";
        if (extras != null) cmd = extras.getString("cmd");

        Intent i = new Intent("com.firebirdberlin.nightdream.NOTIFICATION_LISTENER");
        i.putExtra("what", "alarm");
        i.putExtra("action", "start");
        context.sendBroadcast(i);
    }
}
