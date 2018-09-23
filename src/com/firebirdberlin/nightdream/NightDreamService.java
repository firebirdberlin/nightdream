package com.firebirdberlin.nightdream;

import android.annotation.TargetApi;
import android.os.Build;
import android.service.dreams.DreamService;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class NightDreamService extends DreamService {

    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        Utility.turnScreenOn(this);
        NightDreamActivity.start(this);
        finish();
    }

}
