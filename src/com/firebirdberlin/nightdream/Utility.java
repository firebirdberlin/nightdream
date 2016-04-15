package com.firebirdberlin.nightdream;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.provider.Settings.System;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import java.io.IOException;
import java.lang.Exception;
import java.lang.Thread;


public class Utility{
    private static final String SCREENSAVER_ENABLED = "screensaver_enabled";
    private static String TAG ="NightDreamActivity";

    private Context mContext;
    int system_brightness_mode = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
    MediaPlayer mMediaPlayer;

    // constructor
    public Utility(Context context){
        this.mContext = context;
        mMediaPlayer = null;
        getSystemBrightnessMode();
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

    public void AlarmPlay() throws IllegalArgumentException, SecurityException,
                                   IllegalStateException, IOException{

        AlarmStop();
        Log.i(TAG, "AlarmPlay()");
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
        }

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        mMediaPlayer.setDataSource(mContext, soundUri);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        mMediaPlayer.setLooping(true);
        mMediaPlayer.prepare();
        mMediaPlayer.start();
    }

    public void AlarmStop(){
        if (mMediaPlayer != null){
            if(mMediaPlayer.isPlaying()) {
                Log.i(TAG, "AlarmStop()");
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

    static public Sensor getLightSensor(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
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

    static long getFirstInstallTime(Context context) {
        if(Build.VERSION.SDK_INT < 9) return 0L;
        try {
            return context.getPackageManager()
                          .getPackageInfo(context.getPackageName(), 0)
                          .firstInstallTime;
        } catch (NameNotFoundException e) {
            return 0L;
        }
    }
}
