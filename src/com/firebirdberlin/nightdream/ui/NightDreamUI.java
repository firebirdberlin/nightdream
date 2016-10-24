package com.firebirdberlin.nightdream.ui;

import java.io.FileDescriptor;
import java.util.Calendar;
import java.util.Random;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import de.greenrobot.event.EventBus;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.PointTarget;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import com.firebirdberlin.nightdream.AlarmClock;
import com.firebirdberlin.nightdream.AlarmService;
import com.firebirdberlin.nightdream.models.BatteryValue;
import com.firebirdberlin.nightdream.LightSensorEventListener;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.repositories.BatteryStats;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.SoundMeter;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.mAudioManager;
import com.firebirdberlin.nightdream.events.OnClockClicked;
import com.firebirdberlin.nightdream.events.OnLightSensorValueTimeout;
import com.firebirdberlin.nightdream.events.OnNewLightSensorValue;
import com.firebirdberlin.nightdream.events.OnLocationUpdated;
import com.firebirdberlin.nightdream.events.OnWeatherDataUpdated;
import com.firebirdberlin.nightdream.services.LocationService;
import com.firebirdberlin.nightdream.services.DownloadWeatherService;
import com.firebirdberlin.nightdream.ui.ClockLayout;

public class NightDreamUI {
    private static String TAG ="NightDreamUI";
    private int screen_alpha_animation_duration = 3000;
    private int screen_transition_animation_duration = 10000;

    final private Handler handler = new Handler();
    private int mode = 2;
    private boolean isDebuggable;
    private Context mContext;
    private Drawable bgshape;
    private Drawable bgblack;
    private AlarmClock alarmClock;
    private TextView alarmTime = null;
    private ImageView background_image;
    private ImageView settingsIcon;
    private ImageView callIcon, gmailIcon, twitterIcon, whatsappIcon;
    private LightSensorEventListener lightSensorEventListener = null;
    private ClockLayout clockLayout;
    private LinearLayout notificationbar;
    private Settings settings = null;
    private SoundMeter soundmeter;
    private ProgressBar brightnessProgress = null;
    private TextView batteryView = null;
    private TextView gmailNumber, twitterNumber, whatsappNumber;
    private Utility utility = null;
    private View rootView = null;
    private Window window = null;
    private mAudioManager AudioManage = null;
    private ScaleGestureDetector mScaleDetector = null;
    private ShowcaseView showcaseView = null;

    private int dim_offset_init_x = 0;
    private int dim_offset_curr_x = 0;
    public boolean setDimOffset = false;
    private boolean daydreamMode = false;
    private float last_ambient = 4.0f;

    public NightDreamUI(Context context, Window window) {
        mContext = context;
        mScaleDetector = new ScaleGestureDetector(mContext, mOnScaleGestureListener);

        this.window = window;
        rootView = window.getDecorView().findViewById(android.R.id.content);
        background_image = (ImageView) rootView.findViewById(R.id.background_view);
        brightnessProgress = (ProgressBar) rootView.findViewById(R.id.brightness_progress);
        batteryView = (TextView) rootView.findViewById(R.id.batteryView);
        clockLayout = (ClockLayout) rootView.findViewById(R.id.clockLayout);
        alarmClock = (AlarmClock) rootView.findViewById(R.id.AlarmClock);
        alarmTime = (TextView) rootView.findViewById(R.id.textview_alarm_time);
        notificationbar = (LinearLayout) rootView.findViewById(R.id.notificationbar);
        settingsIcon = (ImageView) rootView.findViewById(R.id.settings_icon);

        callIcon = (ImageView) rootView.findViewById(R.id.call_icon);
        gmailIcon = (ImageView) rootView.findViewById(R.id.gmail_icon);
        whatsappIcon = (ImageView) rootView.findViewById(R.id.whatsapp_icon);
        twitterIcon = (ImageView) rootView.findViewById(R.id.twitter_icon);

        gmailNumber = (TextView) rootView.findViewById(R.id.gmail_number);
        whatsappNumber = (TextView) rootView.findViewById(R.id.whatsapp_number);
        twitterNumber = (TextView) rootView.findViewById(R.id.twitter_number);

        // prepare zoom-in effect
        // API level 11
        if (Build.VERSION.SDK_INT >= 12){
            clockLayout.setScaleX(.1f);
            clockLayout.setScaleY(.1f);
            clockLayout.setOnTouchListener(mOnTouchListener);
        } else {
            clockLayout.setOnClickListener(mOnClickListener);
        }

        utility = new Utility(context);
        settings = new Settings(context);
        AudioManage = new mAudioManager(context);

        checkForReviewRequest();
        isDebuggable = utility.isDebuggable();
    }

    public NightDreamUI(Context context, Window window, boolean daydreamMode) {
        this(context, window);
        this.daydreamMode = daydreamMode;
    }

    public void onStart() {
        setAlpha(settingsIcon, .5f, 100);
        updateBatteryView();
        if (Build.VERSION.SDK_INT >= 12){
            handler.postDelayed(zoomIn, 500);
        }
        handler.postDelayed(moveAround, 30000);
        handler.postDelayed(hideAlarmClock, 20000);
    }

    public void onResume() {
        LocationService.start(mContext);
        settings.reload();

        EventBus.getDefault().register(this);
        lightSensorEventListener = new LightSensorEventListener(mContext);
        lightSensorEventListener.register();

        setupScreenAnimation();
        setupClockLayout();
        setColor();
        setupAlarmClock();

        if (settings.useAmbientNoiseDetection()){
            soundmeter = new SoundMeter(isDebuggable);
        } else {
            soundmeter = null;
        }

        showShowcase();
    }

    private void setupClockLayout() {
        clockLayout.setTimeFormat();
        clockLayout.setDateFormat(settings.dateFormat);
        if (settings.showDate){
            clockLayout.showDate();
        } else {
            clockLayout.hideDate();
        }

        if ( !settings.restless_mode ) {
            centerClockLayout();
        }

        clockLayout.setTypeface(settings.typeface);
        clockLayout.setPrimaryColor(settings.clockColor);
        clockLayout.setSecondaryColor(settings.secondaryColor);
        clockLayout.setTertiaryColor(settings.tertiaryColor);
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    void setColor() {
        alarmTime.setTextColor(settings.secondaryColor);
        batteryView.setTextColor(settings.secondaryColor);
        gmailNumber.setTextColor(settings.secondaryColor);
        twitterNumber.setTextColor(settings.secondaryColor);
        whatsappNumber.setTextColor(settings.secondaryColor);
        settingsIcon.setColorFilter( settings.secondaryColor, PorterDuff.Mode.MULTIPLY );
        callIcon.setColorFilter( settings.secondaryColor, PorterDuff.Mode.MULTIPLY );
        gmailIcon.setColorFilter( settings.secondaryColor, PorterDuff.Mode.MULTIPLY );
        twitterIcon.setColorFilter( settings.secondaryColor, PorterDuff.Mode.MULTIPLY );
        whatsappIcon.setColorFilter( settings.secondaryColor, PorterDuff.Mode.MULTIPLY );
        alarmClock.setCustomColor(settings.clockColor, settings.secondaryColor);

        Drawable brightnessDrawable = brightnessProgress.getProgressDrawable();
        if (Build.VERSION.SDK_INT < 21) {
            brightnessDrawable.setColorFilter(settings.clockColor, PorterDuff.Mode.MULTIPLY);
        } else {
            brightnessProgress.setProgressTintList(ColorStateList.valueOf(settings.clockColor));
            brightnessProgress.setProgressBackgroundTintList(
                    ColorStateList.valueOf(adjustAlpha(settings.clockColor, 0.4f)));
        }
        brightnessProgress.setVisibility(View.INVISIBLE);

        bgblack = new ColorDrawable(Color.parseColor("#000000"));
        bgshape = bgblack;
        switch (settings.background_mode){
            case Settings.BACKGROUND_BLACK: {
                bgshape = bgblack;
                break;
            }
            case Settings.BACKGROUND_GRADIENT: {

                bgshape = getDrawable(R.drawable.background_gradient);
                break;
            }
            case Settings.BACKGROUND_IMAGE: {
                bgshape = loadBackGroundImage();
                break;
            }
        }

        background_image.setImageDrawable(bgshape);

    }

    private Drawable getDrawable(int resId) {
        if (Build.VERSION.SDK_INT < 21) {
            return deprecatedGetDrawable(resId);
        }
        return mContext.getResources().getDrawable(resId, null);

    }

    @SuppressWarnings("deprecation")
    private Drawable deprecatedGetDrawable(int resId) {
        return mContext.getResources().getDrawable(resId);
    }

    private void setupAlarmClock() {
        if ( ! settings.useInternalAlarm ) {
            String nextAlarm = alarmClock.getNextSystemAlarmTime();

            if ( Build.VERSION.SDK_INT >= 19
                    && nextAlarm != null
                    && nextAlarm.isEmpty() ) {
                nextAlarm = mContext.getString(R.string.set_alarm);
            }
            alarmTime.setText(nextAlarm);
            alarmTime.setOnClickListener(onStockAlarmTimeClickListener);
            alarmTime.setClickable(true);
            alarmClock.removeAlarm();
        } else {
            alarmTime.setOnClickListener(null);
            alarmTime.setClickable(false);
        }

        int visibility = settings.useInternalAlarm ? View.GONE : View.VISIBLE;
        alarmTime.setVisibility(visibility);
        alarmClock.isVisible = settings.useInternalAlarm;
        alarmClock.setClickable(true);
    }


    public void onPause() {
        EventBus.getDefault().unregister(this);
        lightSensorEventListener.unregister();
    }

    public void onStop() {
        removeCallbacks(moveAround);
        removeCallbacks(hideAlarmClock);
        removeCallbacks(zoomIn);
        if (soundmeter != null){
            soundmeter.stopMeasurement();
            soundmeter = null;
        }

    }

    public void onDestroy() {

    }

    private void removeCallbacks(Runnable runnable) {
        if (handler == null) return;
        if (runnable == null) return;

        handler.removeCallbacks(runnable);
    }

    public void onConfigurationChanged(final Configuration newConfig) {
        ViewTreeObserver observer = clockLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                fixScaleFactor();
                clockLayout.updateLayout(newConfig);
                clockLayout.setDesiredClockWidth();
                ViewTreeObserver observer = clockLayout.getViewTreeObserver();
                if (observer != null) {
                    observer.removeOnGlobalLayoutListener(this);
                }
            }
        });

        removeCallbacks(moveAround);
        if ( showcaseView != null ) {
            setupShowcaseView();
        } else {
            handler.postDelayed(moveAround, 1000);
        }
    }

    public void updateBatteryView() {
        BatteryValue reference = settings.loadBatteryReference();
        BatteryStats battery = new BatteryStats(mContext);
        BatteryValue batteryValue = battery.reference;
        float percentage = batteryValue.getPercentage();
        if (batteryValue.isCharging) {
            if (percentage < 95.){
                long est = batteryValue.getEstimateMillis(reference)/1000; // estimated seconds
                formatBatteryEstimate(percentage, est);
            }  else {
                batteryView.setText(String.format("%3d %%", (int) percentage));
            }
        } else { // not charging
            long est = batteryValue.getDischargingEstimateMillis(reference)/1000; // estimated seconds
            formatBatteryEstimate(percentage, est);
        }
        batteryView.setVisibility(View.VISIBLE);
    }

    private void formatBatteryEstimate(float percentage, long est) {
        Log.i(TAG, String.valueOf(est));
        if (est > 0){
            long h = est / 3600;
            long m  = ( est % 3600 ) / 60;
            batteryView.setText(String.format("% 3d %% -- %02d:%02d",
                                              (int) percentage, (int) h, (int) m));
        } else {
            batteryView.setText(String.format("%3d %%", (int) percentage));
        }
    }

    public void setupScreenAnimation() {
        BatteryStats battery = new BatteryStats(mContext);
        BatteryValue batteryValue = battery.reference;
        if (batteryValue.isCharging) {
            screen_alpha_animation_duration = 3000;
            screen_transition_animation_duration = 10000;
        } else {
            screen_alpha_animation_duration = 0;
            screen_transition_animation_duration = 0;
        }
    }

    private void centerClockLayout() {
        if (Build.VERSION.SDK_INT >= 11) {
            clockLayout.setTranslationX(0);
            clockLayout.setTranslationY(0);
        } else {
            clockLayout.setPadding(0, 0, 0, 0);
        }
        clockLayout.invalidate();
    }

    public void updateClockPosition() {
        if ( !settings.restless_mode) {
            return;
        }
        setupScreenAnimation();
        Random random = new Random();
        Point size = utility.getDisplaySize();
        int w = size.x;
        int h = size.y;
        if (Build.VERSION.SDK_INT < 12) {
            // make back panel fully transparent
            clockLayout.setBackgroundColor(Color.parseColor("#00000000"));
            // random x position
            int rxpos = (w - clockLayout.getWidth()) / 2;
            int rypos = (h - clockLayout.getHeight()) / 2; // API level 1

            rxpos = to_range(rxpos, 1, w);
            rypos = to_range(rypos, 1, h);

            int i1 = random.nextInt(2 * rxpos);
            int i2 = 90 + random.nextInt(2 * rypos);

            clockLayout.setPadding(i1, i2, 0, 0);
            clockLayout.invalidate();
        } else {
            // determine a randowm position
            // lower 150 px is reserved for alarm clockLayout
            // upper 90 px is for the battery stats
            int scaled_width = (int) (clockLayout.getWidth() * clockLayout.getScaleX());
            int scaled_height = (int) (clockLayout.getHeight() * clockLayout.getScaleY());
            int rxpos = w - scaled_width ; // API level 11
            int rypos = h - scaled_height - 150 - 90; // API level 11

            int i1 = (rxpos > 0) ? random.nextInt(rxpos) : 0;
            int i2 = (rypos > 0) ? 90 + random.nextInt(rypos) : 90;

            i1 -= (clockLayout.getWidth() - scaled_width) / 2;
            i2 -= (clockLayout.getHeight() - scaled_height) / 2;

            // api level 12
            clockLayout.animate().setDuration(screen_transition_animation_duration).x(i1).y(i2);
        }
    }

    private float LIGHT_VALUE_DARK = 4.2f;
    private float LIGHT_VALUE_BRIGHT = 40.0f;
    private float LIGHT_VALUE_DAYLIGHT = 300.0f;

    private int to_range(int value, int min, int max){
        if (value > max) return max;
        if (value < min) return min;
        return value;
    }

    private float to_range(float value, float min, float max){
        if (value > max) return max;
        if (value < min) return min;
        return value;
    }

    public void dimScreen(int millis, float light_value, float add_brightness){
        LIGHT_VALUE_DARK = settings.minIlluminance;
        float v = 0.f;
        float brightness = 0.f;
        if (settings.autoBrightness && Utility.getLightSensor(mContext) != null) {
            float luminance_offset = LIGHT_VALUE_BRIGHT * add_brightness;
            if (light_value > LIGHT_VALUE_BRIGHT && add_brightness > 0.f) {
                luminance_offset = LIGHT_VALUE_DAYLIGHT * add_brightness;
            }
            v = (light_value + luminance_offset - LIGHT_VALUE_DARK)/(LIGHT_VALUE_BRIGHT - LIGHT_VALUE_DARK);
            v = 0.3f + 0.7f * v;

            brightness = (light_value + luminance_offset - LIGHT_VALUE_BRIGHT)/(LIGHT_VALUE_DAYLIGHT - LIGHT_VALUE_BRIGHT);
        } else {
            v = 1.f + add_brightness;
            brightness = add_brightness;

            if ( daydreamMode ) {
                v *= 0.5f;
                brightness = 0.f;
            }
        }

        v = to_range(v, 0.05f, 1.f);
        brightness = to_range(brightness, 0.01f, 1.f);
        // On some screens (as the Galaxy S2) a value of 0 means the screen is completely dark.
        // Therefore a minimum value must be set to preserve the visibility of the clock.

        Log.d(TAG, "light value : " + String.valueOf(light_value));
        Log.d(TAG, "a : " + String.valueOf(v) + " | b : " + String.valueOf(brightness));

        setBrightness(brightness);

        if ( showcaseView == null ) {
            setAlpha(clockLayout, v, millis);
        }
        if ( alarmClock.isClickable() ) {
            setAlpha(alarmClock, v, millis);
            if ( showcaseView == null ) {
                setAlpha(alarmTime, v, millis);
            }
            v = to_range(v, 0.6f, 1.f);
            setAlpha(batteryView, v, millis);
            setAlpha(settingsIcon, v, millis);
        }

        if ( mode == 0 ) {
            setAlpha(notificationbar, 0.0f, millis);
        } else {
            // increase minimum alpha value for the notification bar
            v = to_range(v, 0.6f, 1.f);
            setAlpha(notificationbar, v, millis);
        }

        if ( light_value + 0.2f < settings.minIlluminance ) {
            settings.setMinIlluminance(light_value + 0.2f);
        }
    }

    private void setBrightness(float value) {
        LayoutParams layout = window.getAttributes();
        layout.screenBrightness = value;
        layout.buttonBrightness = 0.f;
        window.setAttributes(layout);
        hideSoftButtons();
    }

    private void hideSoftButtons() {
        if (Build.VERSION.SDK_INT >= 14){
            clockLayout.setSystemUiVisibility(View. SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    }

    public int determineScreenMode(int current_mode, float light_value, double last_ambient_noise){

        LIGHT_VALUE_DARK = settings.minIlluminance;
        double ambient_noise_threshold = settings.NOISE_AMPLITUDE_SLEEP;
        if (current_mode == 0){
            ambient_noise_threshold = settings.NOISE_AMPLITUDE_WAKE;
        }

        if (light_value <= LIGHT_VALUE_DARK
                && ( ( settings.useAmbientNoiseDetection() == false)
                    || last_ambient_noise < ambient_noise_threshold)){
            return 0;
        } else if (light_value < LIGHT_VALUE_BRIGHT/2.f) { // night shift, desk light on
            return 1;
        } else if (light_value < LIGHT_VALUE_BRIGHT) { // night shift, desk light on
            return 2;
        }
        // day mode
        return 3;
    }

    public void switchModes(float light_value, double last_ambient_noise){
        int current_mode = mode;
        mode = determineScreenMode(current_mode, light_value, last_ambient_noise);

        if ((mode == 0) && (current_mode != 0)){
            if (settings.muteRinger) AudioManage.setRingerModeSilent();
            background_image.setImageDrawable(bgblack);
        } else
        if ((mode != 0) && (current_mode == 0)){
            restoreRingerMode();
            background_image.setImageDrawable(bgshape);
        }

        float dim_offset = settings.dim_offset;
        if ((mode == 1) && (current_mode == 0)) {
            dim_offset += 0.1f;
        }
        dimScreen(screen_alpha_animation_duration, light_value, dim_offset);

        if (soundmeter != null) {
            if (mode == 0 && soundmeter.isRunning() == false) {
                soundmeter.startMeasurement(3000);
            } else if (mode == 1 && soundmeter.isRunning() == false){
                soundmeter.startMeasurement(60000);
            } else if (mode > 1) {
                soundmeter.stopMeasurement();
            }
        }
    }

    public void setAlpha(View v, float alpha, int millis){
        if (v == null) return;

        if (Build.VERSION.SDK_INT < 14) {
             final AlphaAnimation animation = new AlphaAnimation(alpha, alpha);
             animation.setDuration(millis);
             animation.setFillAfter(true);
             v.startAnimation(animation);
        } else { // should work from 12 on but had a bug report for 13 !?!
            float oldValue = v.getAlpha();
            if (alpha != oldValue) {
                v.animate().setDuration(millis).alpha(alpha);
            }
        }
    }

    public void restoreRingerMode() {
        if ( AudioManage == null ) {
            return;
        }
        if (settings.muteRinger) AudioManage.restoreRingerMode();
    }

    // only called for apilevel >= 12
    private Runnable zoomIn = new Runnable() {
        @Override
        public void run() {
            Configuration config = mContext.getResources().getConfiguration();
            clockLayout.updateLayout(config);
            clockLayout.setDesiredClockWidth();

            float s = settings.scaleClock;
            clockLayout.animate().setDuration(1000).scaleX(s).scaleY(s);
       }
    };

    // move the clock randomly around
    private Runnable moveAround = new Runnable() {
       @Override
       public void run() {
           removeCallbacks(hideBrightnessLevel);
           updateClockPosition();

           handler.postDelayed(this, 60000);
       }
    };

    private Runnable hideBrightnessLevel = new Runnable() {
       @Override
       public void run() {
           setAlpha(brightnessProgress, 0.f, 2000);
           handler.postDelayed(hideBrightnessView, 2010);
       }
    };

    private Runnable hideBrightnessView = new Runnable() {
       @Override
       public void run() {
           brightnessProgress.setVisibility(View.INVISIBLE);
       }
    };

    private Runnable hideAlarmClock = new Runnable() {
       @Override
       public void run() {
           if ( alarmClock.isInteractive() || AlarmService.isRunning) {
               handler.postDelayed(hideAlarmClock, 20000);
               return;
           }
           setAlpha(batteryView, 0.f, 2000);
           setAlpha(settingsIcon, 0.f, 2000);
           alarmClock.isVisible = false;
           alarmClock.setClickable(false);
           alarmTime.setClickable(false);
           setAlpha(alarmClock, 0.f, 2000);
           setAlpha(alarmTime, 0.f, 2000);
       }
    };

    public void onClockClicked() {
        brightnessProgress.setVisibility(View.INVISIBLE);
        updateBatteryView();
        showAlarmClock();
    }

    public void showAlarmClock() {
        removeCallbacks(hideAlarmClock);
        handler.postDelayed(hideAlarmClock, 20000);

        setupAlarmClock();

        dimScreen(0, last_ambient, settings.dim_offset);
    }

    private boolean multi_finger_gesture = false;
    OnTouchListener mOnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    multi_finger_gesture = false;
                    return true;
                case MotionEvent.ACTION_POINTER_UP:
                    multi_finger_gesture = true;
                    return true;
                case MotionEvent.ACTION_UP:
                    if (multi_finger_gesture == false) {
                        EventBus.getDefault().post(new OnClockClicked());
                        onClockClicked();
                        return true;
                    }
                    multi_finger_gesture = false;
                default:
                    return mScaleDetector.onTouchEvent(event);
            }
        }
    };

    OnScaleGestureListener mOnScaleGestureListener = new OnScaleGestureListener() {
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            float s = clockLayout.getScaleX();
            settings.setScaleClock(s);
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float s = detector.getScaleFactor();
            applyScaleFactor(s);
            return false;
        }
    };

    private void applyScaleFactor(float factor) {
        Point size = utility.getDisplaySize();
        int screen_width = size.x;
        factor *= clockLayout.getScaleX();
        int new_width = (int) (clockLayout.getWidth() * factor);
        if (factor > 0.5f && new_width <= screen_width) {
            clockLayout.setScaleX(factor);
            clockLayout.setScaleY(factor);
            clockLayout.invalidate();
        }
    }

    private void fixScaleFactor() {
        if ( Build.VERSION.SDK_INT < 12 ) return;

        Point size = utility.getDisplaySize();
        int screen_width = size.x;
        float factor = clockLayout.getScaleX();
        int new_width = (int) (clockLayout.getWidth() * factor);
        if (new_width > screen_width) {
            factor = 1.f;
            settings.setScaleClock(factor);
            clockLayout.setScaleX(factor);
            clockLayout.setScaleY(factor);
            clockLayout.invalidate();
        }
    }

    OnClickListener mOnClickListener = new OnClickListener() {
        public void onClick(View v) {
            onClockClicked();
        }
    };

    OnClickListener onStockAlarmTimeClickListener = new OnClickListener() {
        public void onClick(View v) {
            Log.i(TAG, "ACTION_SHOW_ALARMS");
            if (Build.VERSION.SDK_INT < 19) return;

            Intent mClockIntent = new Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS);
            mClockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(mClockIntent);
        }
    };

    public boolean onTouch(View view, MotionEvent e, float last_ambient) {
        if (utility == null) return false;
        boolean event_consumed = false;
        Point click = new Point((int) e.getX(),(int) e.getY());
        Point size = utility.getDisplaySize();

        // handle the visibility of the alarm clock
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (click.y >= 0.2 * size.y) {// upper 20% of the screen
                    brightnessProgress.setVisibility(View.INVISIBLE);
                    updateBatteryView();
                }

                showAlarmClock();
                event_consumed = true;
                break;
            case MotionEvent.ACTION_UP:
                removeCallbacks(hideAlarmClock);
                handler.postDelayed(hideAlarmClock, 20000);
                event_consumed = true;
                break;
        }

        // set the screen brightness factor
        if (click.y < 0.2 * size.y) {// upper 20% of the screen
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    removeCallbacks(hideBrightnessLevel);
                    removeCallbacks(hideBrightnessView);
                    handler.postDelayed(hideBrightnessLevel, 1000);
                    setDimOffset = true;
                    dim_offset_init_x = click.x;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (setDimOffset == false) return false;
                    dim_offset_curr_x = click.x;

                    float dx = 2.f * (dim_offset_curr_x - dim_offset_init_x) / size.x;
                    settings.dim_offset += dx;
                    settings.dim_offset = to_range(settings.dim_offset, -1.f, 1.f);

                    int value = (int) (100.f * (settings.dim_offset + 1.f));
                    brightnessProgress.setProgress(value);
                    brightnessProgress.setVisibility(View.VISIBLE);
                    setAlpha(brightnessProgress, 1.f, 0);
                    removeCallbacks(hideBrightnessLevel);
                    removeCallbacks(hideBrightnessView);
                    handler.postDelayed(hideBrightnessLevel, 1000);

                    dim_offset_init_x = click.x;

                    dimScreen(0, last_ambient, settings.dim_offset);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (setDimOffset == true){
                        setDimOffset = false;
                        settings.setBrightnessOffset(settings.dim_offset);
                        handler.postDelayed(hideBrightnessLevel, 1000);
                        return true;
                    }
                    break;
            }
        }
        return event_consumed;
    }

    public Drawable loadBackGroundImage() {
        try{
            Point display = utility.getDisplaySize();

            Bitmap bgimage = loadBackgroundBitmap();

            int nw = bgimage.getWidth();
            int nh = bgimage.getHeight();
            boolean scaling_needed =false;
            if ( bgimage.getHeight() > display.y ){
                nw = (int) ((display.y /(float) bgimage.getHeight()) * bgimage.getWidth());
                nh = display.y;
                scaling_needed = true;
            }

            if ( nw > display.x ){
                nh = (int) ((display.x / (float) bgimage.getWidth()) * bgimage.getHeight());
                nw = display.x;
                scaling_needed = true;
            }
            if (scaling_needed){
                bgimage = Bitmap.createScaledBitmap(bgimage,nw, nh, false);
            }

            return new BitmapDrawable(mContext.getResources(), bgimage);
        } catch (OutOfMemoryError e){
            Toast.makeText(mContext, "Out of memory. Please, try to scale down your image.",
                    Toast.LENGTH_LONG).show();
            return new ColorDrawable(Color.parseColor("#000000"));
        } catch (Exception e) {
            return new ColorDrawable(Color.parseColor("#000000"));
        }
    }

    private Bitmap loadBackgroundBitmap() throws Exception {
        Bitmap bitmap = null;
        Point display = utility.getDisplaySize();
        if (settings.backgroundImageURI != "") {
            // version for Android 4.4+ (KitKat)
            Uri uri = Uri.parse(settings.backgroundImageURI);
            ParcelFileDescriptor parcelFileDescriptor =
                    mContext.getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();


            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

            // Calculate inSampleSize
            options.inSampleSize = Utility.calculateInSampleSize(options, display.x, display.y);

                // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
            parcelFileDescriptor.close();
            return bitmap;
        } else
        if (settings.backgroundImagePath() != "" ) {
            // deprecated legacy version
            String bgpath = settings.backgroundImagePath();
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(bgpath, options);

            // Calculate inSampleSize
            options.inSampleSize = Utility.calculateInSampleSize(options, display.x, display.y);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            return bitmap = (BitmapFactory.decodeFile(bgpath, options));
        }

        return bitmap;
    }

    private void checkForReviewRequest() {
        if (Build.VERSION.SDK_INT < 11) return;

        // ask only once
        if (settings.lastReviewRequestTime != 0L) return;

        long firstInstallTime = utility.getFirstInstallTime(mContext);
        Log.i(TAG, "First install time: " + String.valueOf(firstInstallTime));
        Calendar install_time = Calendar.getInstance();
        install_time.setTimeInMillis(firstInstallTime);

        Calendar two_months_ago = Calendar.getInstance();
        two_months_ago.add(Calendar.DATE, -60);

        if (install_time.before(two_months_ago)) {
            sendReviewRequest();
            settings.setLastReviewRequestTime(Calendar.getInstance().getTimeInMillis());
        }
    }

    private void sendReviewRequest() {

        final Uri uri = Uri.parse("market://details?id=" + mContext.getPackageName());
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        if (mContext.getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
            PendingIntent pIntent = PendingIntent.getActivity(mContext, 0, intent, 0);

            // build notification
            Notification n = new NotificationCompat.Builder(mContext)
                .setContentTitle(mContext.getString(R.string.app_name))
                .setContentText(mContext.getString(R.string.review_request))
                .setSmallIcon(R.drawable.ic_clock)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .setDefaults(0)
                .build();

            NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(mContext.NOTIFICATION_SERVICE);

            notificationManager.notify(0, n);

        }
        else {
            /* handle your error case: the device has no way to handle market urls */
        }
    }

    public void onEvent(OnNewLightSensorValue event){
        last_ambient = event.value;
        dimScreen(screen_alpha_animation_duration, last_ambient, settings.dim_offset);
    }

    public void onEvent(OnLocationUpdated event){
        if ( event == null ) return;
        if ( event.entry != null ) {
            DownloadWeatherService.start(mContext, event.entry);
        } else {
            clockLayout.clearWeather();
        }

    }

    public void onEvent(OnWeatherDataUpdated event){
        clockLayout.update(event.entry);
    }

    public void onEvent(OnLightSensorValueTimeout event){
        last_ambient = (event.value >= 0.f) ? event.value : settings.minIlluminance;
        dimScreen(screen_alpha_animation_duration, last_ambient, settings.dim_offset);
    }

    static int showcaseCounter = 0;

    private void showShowcase() {
        // daydreams cannot be cast to an activity
        if ( showcaseView != null || daydreamMode) {
            return;
        }

        long firstInstallTime = utility.getFirstInstallTime(mContext);
        Calendar install_time = Calendar.getInstance();
        install_time.setTimeInMillis(firstInstallTime);

        Calendar start_date = Calendar.getInstance();
        start_date.set(Calendar.YEAR, 2016);
        start_date.set(Calendar.MONTH, Calendar.JULY);
        start_date.set(Calendar.DAY_OF_MONTH, 3);
        start_date.set(Calendar.SECOND, 0);
        start_date.set(Calendar.MINUTE, 0);
        start_date.set(Calendar.HOUR_OF_DAY, 0);

        if (install_time.before(start_date)) {
            return;
        }

        showcaseCounter = 0;
        showcaseView = new ShowcaseView.Builder((Activity) mContext)
            //.withMaterialShowcase()
            .blockAllTouches()
            .setContentTitle(mContext.getString(R.string.welcome_screen_title1))
            .setContentText(mContext.getString(R.string.welcome_screen_text1))
            .setOnClickListener(showCaseOnClickListener)
            .singleShot(1)
            .build();

        showcaseView.show();

        if (showcaseView.isShowing()) {
            removeCallbacks(moveAround);
            removeCallbacks(hideAlarmClock);
            setAlpha(clockLayout, 0.2f, 0);
            setAlpha(alarmTime, 0.2f, 0);
        } else {
            showcaseView = null;
        }
    }

    View.OnClickListener showCaseOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showcaseCounter++;
            setupShowcaseView();
        }
    };

    void setupShowcaseView() {
        switch(showcaseCounter) {
            case 0:
                showcaseView.setShowcase(Target.NONE, true);
                showcaseView.setContentTitle(mContext.getString(R.string.welcome_screen_title1));
                showcaseView.setContentText(mContext.getString(R.string.welcome_screen_text1));
                showcaseView.setBlockAllTouches(true);
                break;
            case 1:
                showcaseView.setShowcase(new ViewTarget(settingsIcon), true);
                showcaseView.setContentTitle(mContext.getString(R.string.welcome_screen_title2));
                showcaseView.setContentText(mContext.getString(R.string.welcome_screen_text2));
                showcaseView.setBlockAllTouches(true);
                break;
            case 2:
                Point size = utility.getDisplaySize();
                showcaseView.setShowcase(new PointTarget(size.x/2, 20), true);
                showcaseView.setBlockAllTouches(false);
                showcaseView.setContentTitle(mContext.getString(R.string.welcome_screen_title3));
                showcaseView.setContentText(mContext.getString(R.string.welcome_screen_text3));
                break;
            case 3:
                setAlpha(clockLayout, 1.f, 500);
                showcaseView.setShowcase(new ViewTarget(clockLayout), true);
                showcaseView.setContentTitle(mContext.getString(R.string.welcome_screen_title4));
                showcaseView.setContentText(mContext.getString(R.string.welcome_screen_text4));
                showcaseView.setBlockAllTouches(true);
                break;
            default:
                showcaseView.hide();
                showcaseView = null;
                handler.postDelayed(moveAround, 30000);
                handler.postDelayed(hideAlarmClock, 20000);
                onClockClicked();
                break;
        }
    }
}
