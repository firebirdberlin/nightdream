package com.firebirdberlin.nightdream.widget;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class ClockWidgetTimeTickService extends Service {

    private static final String TAG = "ClockWidgetTimeTickService";

    private static final String ACTION_SHUTDOWN = "SHUTDOWN";

    private TimeReceiver timeReceiver;

    public static void startService(Context context) {
        Intent intent = new Intent(context.getApplicationContext(), ClockWidgetTimeTickService.class);
        context.getApplicationContext().startService(intent);
    }

    public static void stopService(Context context) {
        Intent intent = new Intent(context.getApplicationContext(), ClockWidgetTimeTickService.class);
        intent.putExtra(ACTION_SHUTDOWN, true);
        context.getApplicationContext().startService(intent); // first unregister time tick
        context.getApplicationContext().stopService(intent);
    }

    @Override
    public IBinder onBind(Intent arg0){
        return null;
    }

    @Override
    public void onCreate(){
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            if (intent.hasExtra(ACTION_SHUTDOWN) && intent.getBooleanExtra(ACTION_SHUTDOWN, false)) {

                if(timeReceiver != null) {
                    Log.d(TAG, "unregisterReceiver timeReceiver");
                    unregisterReceiver(timeReceiver);
                    timeReceiver = null;
                }
                stopSelf();
                return START_NOT_STICKY;

            }
        }

        setTimeTick();
        // service should run continuously until it is explicitly stopped
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        if(timeReceiver != null) {
            Log.d(TAG, "onDestroy: unregisterReceiver timeReceiver");
            unregisterReceiver(timeReceiver);
            timeReceiver = null;
        }

        super.onDestroy();
    }

    private void setTimeTick() {
        if (timeReceiver != null) return;
        Log.d(TAG, "register time tick receiver");
        timeReceiver = new TimeReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(timeReceiver, intentFilter);
    }

    static final class TimeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent arg1) {
            //Log.d(TAG, "time tick in ClockWidgetTimeTickService");
            ClockWidgetProvider.updateAllWidgets(context);
        }
    }

}
