package com.firebirdberlin.nightdream;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import de.greenrobot.event.EventBus;

public class mAudioManager{
    final static String TAG = "NightDream.mAudioManager";
    Context mContext;
    AudioManager audiomanage = null;
    int currentRingerMode;
    EventBus bus = EventBus.getDefault();

    private class OnSetRingerModeSilent {
         private int currentRingerMode;

         public OnSetRingerModeSilent(int currentRingerMode) {
             this.currentRingerMode = currentRingerMode;
         }
    };

    // constructor
    public mAudioManager(Context context){
        this.mContext = context;
        audiomanage = null;
        audiomanage = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        currentRingerMode = audiomanage.getRingerMode();
    }

    public void setRingerModeSilent(){
        currentRingerMode = audiomanage.getRingerMode(); // ringer mode to restore
        Log.i(TAG, String.format(" currentRingerMode = %d", currentRingerMode));
        Log.i(TAG, "setRingerModeSilent()");
        audiomanage.setRingerMode(AudioManager.RINGER_MODE_SILENT);

        bus.postSticky(new OnSetRingerModeSilent(currentRingerMode));
    }

    public void restoreRingerMode(){
        Log.i(TAG, "restoreRingerMode()");
        OnSetRingerModeSilent event = bus.removeStickyEvent(OnSetRingerModeSilent.class);
        // nothing to do
        if (event == null) return;

        currentRingerMode = event.currentRingerMode;
        Log.i(TAG, String.format(" > currentRingerMode = %d", currentRingerMode));

        // initial ringer mode was silent, don't have to do anything
        if (currentRingerMode == AudioManager.RINGER_MODE_SILENT) return;

        // The expected ringer mode is silent. Is it still valid ?
        // If not, another app may have changed it. R-E-S-P-E-C-T this setting.
        if (audiomanage.getRingerMode() != AudioManager.RINGER_MODE_SILENT) return;

        // otherwise we will reset the ringer mode
        audiomanage.setRingerMode(currentRingerMode);
    }
}
