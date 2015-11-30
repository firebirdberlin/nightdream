package com.firebirdberlin.nightdream;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.provider.Settings.System;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import java.io.IOException;
import java.lang.Exception;
import java.lang.Thread;


public class Utility{
    private static final String SCREENSAVER_ENABLED = "screensaver_enabled";

    private Context mContext;
    int system_brightness_mode = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
    MediaPlayer mMediaPlayer;

    // constructor
    public Utility(Context context){
        this.mContext = context;
        mMediaPlayer = null;
        getSystemBrightnessMode();
    }

    public void setAlarm(int hour, int min){
        // alarms cannot be deleted !!!
        Intent NewAlarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
        NewAlarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        NewAlarmIntent.putExtra(AlarmClock.EXTRA_HOUR, hour);
        NewAlarmIntent.putExtra(AlarmClock.EXTRA_MINUTES, min);
        NewAlarmIntent.putExtra(AlarmClock.EXTRA_SKIP_UI,true);
        mContext.startActivity(NewAlarmIntent);
    }

    public void openAlarmConfig(){
        if(Build.VERSION.SDK_INT > 18) {
            Intent intent = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        }
    }

    public long TimeToLong(int hour, int min){
        return (long) hour * 60 * 60 * 1000 + min * 60 * 1000;
    }

    public void PlayNotification(){
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(mContext, notification);
            r.play();
            //Thread.sleep(300);
            //r.stop();
        } catch (Exception e) {}
    }

    public void PlayAlarmSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            Ringtone r = RingtoneManager.getRingtone(mContext, notification);
            r.play();
        } catch (Exception e) {}
    }

    public void AlarmPlay() throws IllegalArgumentException, SecurityException, IllegalStateException,
            IOException{

            AlarmStop();
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (mMediaPlayer == null)
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(mContext, soundUri);
            final AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mMediaPlayer.setLooping(true);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            }
    }


    public void AlarmStop(){
        if (mMediaPlayer != null){
            if(mMediaPlayer.isPlaying()){
                mMediaPlayer.stop();
                mMediaPlayer.release();
            }
            mMediaPlayer = null;
        }
    }


    public boolean AlarmRunning(){
        if (mMediaPlayer == null) return false;
        return mMediaPlayer.isPlaying();
    }


    public Point getDisplaySize(){
        Point size = new Point();
        if(Build.VERSION.SDK_INT < 13) {
            WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            size.x = display.getWidth();
            size.y = display.getHeight();
        } else if (Build.VERSION.SDK_INT < 17) {
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(metrics);
            size.x = metrics.widthPixels;
            size.y = metrics.heightPixels;
        } else {
            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            display.getSize(size);
        }

        return size;
    }


    public boolean isDebuggable(){
        return ( 0 != ( mContext.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE ) );
    }


    public int getStatusBarHeight() {
      int result = 0;
      int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
      if (resourceId > 0) {
          result = mContext.getResources().getDimensionPixelSize(resourceId);
      }
      return result;
    }


    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }


    public String getNextAlarmFormatted(){
        return Settings.System.getString(mContext.getContentResolver(),
                Settings.System.NEXT_ALARM_FORMATTED);
    }


    public void getSystemBrightnessMode(){
        system_brightness_mode = Settings.System.getInt(mContext.getContentResolver(),
                                                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                                                        Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }

    public void setManualBrightnessMode(){
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    public void setAutoBrightnessMode(){
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }


    public void restoreSystemBrightnessMode(){
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                system_brightness_mode);
    }

    public static boolean isDaydreamEnabled(final Context c) {
        return 1 == android.provider.Settings.Secure.getInt(c.getContentResolver(), SCREENSAVER_ENABLED, -1);
    }

    public static boolean isDaydreamEnabledOnDock(final Context c) {
        return 1 == android.provider.Settings.Secure.getInt(c.getContentResolver(), "screensaver_activate_on_dock", -1);
    }

    public static boolean isDaydreamEnabledOnSleep(final Context c) {
        return 1 == android.provider.Settings.Secure.getInt(c.getContentResolver(), "screensaver_activate_on_sleep", -1);
    }

    static public void toggleComponentState(Context context, Class component, boolean on){
        ComponentName receiver = new ComponentName(context, component);
        PackageManager pm = context.getPackageManager();
        int new_state = (on) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        pm.setComponentEnabledSetting(receiver, new_state, PackageManager.DONT_KILL_APP);
    }
}

    /**
 * Converts an HSL color value to RGB. Conversion formula
 * adapted from http://en.wikipedia.org/wiki/HSL_color_space.
 * Assumes h, s, and l are contained in the set [0, 1] and
 * returns r, g, and b in the set [0, 255].
 *
 * @param   Number  h       The hue
 * @param   Number  s       The saturation
 * @param   Number  l       The lightness
 * @return  Array           The RGB representation
 */
    //function hslToRgb(h, s, l){
        //var r, g, b;

        //if(s == 0){
            //r = g = b = l; // achromatic
        //}else{
            //function hue2rgb(p, q, t){
                //if(t < 0) t += 1;
                //if(t > 1) t -= 1;
                //if(t < 1/6) return p + (q - p) * 6 * t;
                //if(t < 1/2) return q;
                //if(t < 2/3) return p + (q - p) * (2/3 - t) * 6;
                //return p;
            //}

            //var q = l < 0.5 ? l * (1 + s) : l + s - l * s;
            //var p = 2 * l - q;
            //r = hue2rgb(p, q, h + 1/3);
            //g = hue2rgb(p, q, h);
            //b = hue2rgb(p, q, h - 1/3);
        //}

        //return [r * 255, g * 255, b * 255];
    //}

/**
 * Converts an RGB color value to HSL. Conversion formula
 * adapted from http://en.wikipedia.org/wiki/HSL_color_space.
 * Assumes r, g, and b are contained in the set [0, 255] and
 * returns h, s, and l in the set [0, 1].
 *
 * @param   Number  r       The red color value
 * @param   Number  g       The green color value
 * @param   Number  b       The blue color value
 * @return  Array           The HSL representation
 */
//function rgbToHsl(r, g, b){
    //r /= 255, g /= 255, b /= 255;
    //var max = Math.max(r, g, b), min = Math.min(r, g, b);
    //var h, s, l = (max + min) / 2;

    //if(max == min){
        //h = s = 0; // achromatic
    //}else{
        //var d = max - min;
        //s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
        //switch(max){
            //case r: h = (g - b) / d + (g < b ? 6 : 0); break;
            //case g: h = (b - r) / d + 2; break;
            //case b: h = (r - g) / d + 4; break;
        //}
        //h /= 6;
    //}

    //return [h, s, l];
//}
