package com.firebirdberlin.nightdream;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.service.dreams.DreamService;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class NightDreamService extends DreamService {

    final Context context = this;
    Handler handler = new Handler();
    Runnable startDelayed = new Runnable() {
        @Override
        public void run() {
            Utility.turnScreenOn(context);
            handler.removeCallbacks(startDelayed);
            NightDreamActivity.start(context);
            finish();
        }
    };

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setScreenBright(true);
        setFullscreen(true);
    }

    public void onDreamingStarted() {
        handler.postDelayed(startDelayed, 5000);
    }

    public void onDreamingStopped() {
        handler.removeCallbacks(startDelayed);
    }
}
