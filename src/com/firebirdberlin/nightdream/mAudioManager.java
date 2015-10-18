package com.firebirdberlin.nightdream;

import android.content.Context;
import android.media.AudioManager;

public class mAudioManager{
    Context mContext;
    AudioManager audiomanage;
    int currentRingerMode;

    // constructor
    public mAudioManager(Context context){
        this.mContext = context;
        audiomanage = null;
        audiomanage = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        currentRingerMode = audiomanage.getRingerMode();
    }

    public void setRingerModeSilent(){
        currentRingerMode = audiomanage.getRingerMode(); // ringer mode to restore
        audiomanage.setRingerMode(AudioManager.RINGER_MODE_SILENT);
    }

    public void restoreRingerMode(){
        // initial ringer mode was silent, don't have to do anything
        if (currentRingerMode == AudioManager.RINGER_MODE_SILENT) return;

        // The expected ringer mode is silent. Is it still valid ?
        // If not, another app may have changed it. R-E-S-P-E-C-T this setting.
        if (audiomanage.getRingerMode() != AudioManager.RINGER_MODE_SILENT) return;

        // otherwise we will reset the ringer mode
        audiomanage.setRingerMode(currentRingerMode);
    }

    public void restoreRingerMode(int mode){
        audiomanage.setRingerMode(mode);
    }

    public int getSystemRingerMode(){
        return currentRingerMode;
    }

}
