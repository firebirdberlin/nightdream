package com.firebirdberlin.nightdream.ui;

import java.io.FileDescriptor;
import java.util.Calendar;
import java.util.Random;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.GestureDetector;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import de.greenrobot.event.EventBus;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.PointTarget;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import com.firebirdberlin.nightdream.AlarmClock;
import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.models.BatteryValue;
import com.firebirdberlin.nightdream.models.WeatherEntry;
import com.firebirdberlin.nightdream.LightSensorEventListener;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.repositories.BatteryStats;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.SoundMeter;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.mAudioManager;
import com.firebirdberlin.nightdream.events.OnLightSensorValueTimeout;
import com.firebirdberlin.nightdream.events.OnNewLightSensorValue;
import com.firebirdberlin.openweathermapapi.OpenWeatherMapApi;
import com.firebirdberlin.nightdream.services.AlarmHandlerService;
import com.firebirdberlin.nightdream.services.RadioStreamService;
import com.firebirdberlin.nightdream.services.WeatherService;
import com.firebirdberlin.nightdream.ui.BatteryView;
import com.firebirdberlin.nightdream.ui.ClockLayout;
import com.firebirdberlin.nightdream.ui.WebRadioImageView;


public class NightDreamUI {
    private static String TAG ="NightDreamUI";
    private int screen_alpha_animation_duration = 3000;
    private int screen_transition_animation_duration = 10000;

    final private Handler handler = new Handler();
    private int mode = 2;
    private boolean isDebuggable;
    private boolean controlsVisible = false;
    private BatteryValue batteryValue;
    private Context mContext;
    private Drawable bgshape;
    private Drawable bgblack;
    private AlarmClock alarmClock;
    private TextView alarmTime = null;
    private ImageView background_image;
    private ImageView menuIcon;
    private ImageView settingsIcon;
    private ImageView nightModeIcon;
    private WebRadioImageView radioIcon;
    private ImageView callIcon, gmailIcon, twitterIcon, whatsappIcon;
    private LightSensorEventListener lightSensorEventListener = null;
    private FrameLayout clockLayoutContainer;
    private ClockLayout clockLayout;
    private LinearLayout notificationbar;
    private LinearLayout sidePanel;
    private Settings settings = null;
    private SoundMeter soundmeter;
    private ProgressBar brightnessProgress = null;
    private BatteryView batteryView = null;
    private TextView gmailNumber, twitterNumber, whatsappNumber;
    private Utility utility = null;
    private View rootView = null;
    private Window window = null;
    private mAudioManager AudioManage = null;
    private ScaleGestureDetector mScaleDetector = null;
    private GestureDetector mGestureDetector = null;
    private NightDreamBroadcastReceiver broadcastReceiver = null;

    private ShowcaseView showcaseView = null;

    private boolean daydreamMode = false;
    private float last_ambient = 4.0f;

    public NightDreamUI(Context context, Window window) {
        mContext = context;
        mScaleDetector = new ScaleGestureDetector(mContext, mOnScaleGestureListener);
        mGestureDetector = new GestureDetector(mContext, mSimpleOnGestureListener);

        this.window = window;
        rootView = window.getDecorView().findViewById(android.R.id.content);
        background_image = (ImageView) rootView.findViewById(R.id.background_view);
        brightnessProgress = (ProgressBar) rootView.findViewById(R.id.brightness_progress);
        batteryView = (BatteryView) rootView.findViewById(R.id.batteryView);
        clockLayoutContainer = (FrameLayout) rootView.findViewById(R.id.clockLayoutContainer);
        clockLayout = (ClockLayout) rootView.findViewById(R.id.clockLayout);
        alarmClock = (AlarmClock) rootView.findViewById(R.id.AlarmClock);
        alarmTime = (TextView) rootView.findViewById(R.id.textview_alarm_time);
        notificationbar = (LinearLayout) rootView.findViewById(R.id.notificationbar);
        sidePanel = (LinearLayout) rootView.findViewById(R.id.side_panel);
        menuIcon = (ImageView) rootView.findViewById(R.id.burger_icon);
        settingsIcon = (ImageView) rootView.findViewById(R.id.settings_icon);
        nightModeIcon = (ImageView) rootView.findViewById(R.id.night_mode_icon);
        radioIcon = (WebRadioImageView) rootView.findViewById(R.id.radio_icon);

        callIcon = (ImageView) rootView.findViewById(R.id.call_icon);
        gmailIcon = (ImageView) rootView.findViewById(R.id.gmail_icon);
        whatsappIcon = (ImageView) rootView.findViewById(R.id.whatsapp_icon);
        twitterIcon = (ImageView) rootView.findViewById(R.id.twitter_icon);

        gmailNumber = (TextView) rootView.findViewById(R.id.gmail_number);
        whatsappNumber = (TextView) rootView.findViewById(R.id.whatsapp_number);
        twitterNumber = (TextView) rootView.findViewById(R.id.twitter_number);

        menuIcon.setOnClickListener(onMenuItemClickListener);

        // prepare zoom-in effect
        // API level 11
        if (Build.VERSION.SDK_INT >= 12){
            menuIcon.setScaleX(.8f);
            menuIcon.setScaleY(.8f);
            clockLayout.setScaleX(.1f);
            clockLayout.setScaleY(.1f);
        }

        utility = new Utility(context);
        settings = new Settings(context);
        AudioManage = new mAudioManager(context);

        alarmClock.setSettings(settings);
        checkForReviewRequest();
        isDebuggable = utility.isDebuggable();
    }

    public NightDreamUI(Context context, Window window, boolean daydreamMode) {
        this(context, window);
        this.daydreamMode = daydreamMode;
    }

    public void onStart() {
        handler.postDelayed(moveAround, 30000);
    }

    public void onResume() {

        hideSystemUI();
        settings.reload();

        setScreenOrientation(settings.screenOrientation);
        initSidePanel();
        updateBatteryValue();
        updateBatteryView();
        setupScreenAnimation();
        clockLayoutContainer.post(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= 12){
                    handler.postDelayed(zoomIn, 500);
                }
                setupClockLayout();
                setColor();
                updateWeatherData();
                controlsVisible = true;

                brightnessProgress.setVisibility(View.INVISIBLE);
                setupBackgroundImage();

                showAlarmClock();
                setupShowcase();
            }
        });


        EventBus.getDefault().register(this);
        broadcastReceiver = registerBroadcastReceiver();
        initLightSensor();
        if (settings.useAmbientNoiseDetection()){
            soundmeter = new SoundMeter(isDebuggable);
        } else {
            soundmeter = null;
        }

    }

    private void initLightSensor() {
        if ( Settings.NIGHT_MODE_ACTIVATION_AUTOMATIC == settings.nightModeActivationMode
                || settings.autoBrightness ) {
            lightSensorEventListener = new LightSensorEventListener(mContext);
            lightSensorEventListener.register();
        } else {
            lightSensorEventListener = null;
        }
    }

    private long lastLocationRequest = 0L;
    private void updateWeatherData() {
        if (! settings.showWeather ) return;

        WeatherEntry entry = settings.weatherEntry;
        long now = System.currentTimeMillis();
        long requestAge = now - lastLocationRequest;
        long diff = entry.ageMillis();

        Log.d(TAG, "Weather: data age " + diff );
        Log.d(TAG, "Time since last request " + requestAge );
        if (diff < 0L || ( diff > 90 * 60 * 1000 && requestAge > 15 * 60 * 1000)) {
            Log.d(TAG, "Weather data outdated. Trying to refresh ! (" + diff + ")");
            lastLocationRequest = now;
            WeatherService.start(mContext, settings.weatherCityID);
        }

        // handle outdated weather data
        if (entry.timestamp == -1L || diff > 8 * 60 * 60 * 1000) {
            clockLayout.clearWeather();
        }
    }

    private void setupClockLayout() {

        if ( !settings.restless_mode ) {
            centerClockLayout();
        }

        clockLayout.setLayout(settings.clockLayout);
        clockLayout.setDateFormat(settings.dateFormat);
        clockLayout.setTimeFormat(settings.timeFormat12h, settings.timeFormat24h);
        clockLayout.setTypeface(settings.typeface);
        clockLayout.setTemperature(settings.showTemperature, settings.temperatureUnit);
        clockLayout.setWindSpeed(settings.showWindSpeed, settings.speedUnit);

        clockLayout.showDate(settings.showDate);
        clockLayout.showWeather(settings.showWeather);
        Configuration config = getConfiguration();
        clockLayout.updateLayout(clockLayoutContainer.getWidth(), config);
        clockLayout.update(settings.weatherEntry);
    }

    void setColor() {
        setNightModeIcon();

        int accentColor = (mode == 0) ? settings.clockColorNight : settings.clockColor;
        int textColor = (mode == 0) ? settings.secondaryColorNight : settings.secondaryColor;

        alarmTime.setTextColor(textColor);
        batteryView.setTextColor(textColor);
        gmailNumber.setTextColor(textColor);
        twitterNumber.setTextColor(textColor);
        whatsappNumber.setTextColor(textColor);
        menuIcon.setColorFilter( textColor, PorterDuff.Mode.SRC_ATOP );
        nightModeIcon.setColorFilter( textColor, PorterDuff.Mode.SRC_ATOP );
        settingsIcon.setColorFilter( textColor, PorterDuff.Mode.MULTIPLY );
        callIcon.setColorFilter( textColor, PorterDuff.Mode.MULTIPLY );
        gmailIcon.setColorFilter( textColor, PorterDuff.Mode.MULTIPLY );
        twitterIcon.setColorFilter( textColor, PorterDuff.Mode.MULTIPLY );
        whatsappIcon.setColorFilter( textColor, PorterDuff.Mode.MULTIPLY );
        alarmClock.setCustomColor(accentColor, textColor);
        clockLayout.setPrimaryColor(accentColor);
        clockLayout.setSecondaryColor(textColor);

        if ( RadioStreamService.streamingMode == RadioStreamService.StreamingMode.RADIO ) {
            setRadioIconActive();
        } else {
            setRadioIconInactive();
        }

        Drawable brightnessDrawable = brightnessProgress.getProgressDrawable();
        if (Build.VERSION.SDK_INT < 21) {
            brightnessDrawable.setColorFilter(accentColor, PorterDuff.Mode.MULTIPLY);
        } else {
            brightnessProgress.setProgressTintList(ColorStateList.valueOf(accentColor));
            brightnessProgress.setProgressBackgroundTintList(
                    ColorStateList.valueOf(adjustAlpha(accentColor, 0.4f)));
        }
    }

    public void setRadioIconActive() {
        int accentColor = (mode == 0) ? settings.clockColorNight : settings.clockColor;
        radioIcon.setColorFilter( accentColor, PorterDuff.Mode.SRC_ATOP );
    }

    public void setRadioIconInactive() {
        int textColor = (mode == 0) ? settings.secondaryColorNight : settings.secondaryColor;
        radioIcon.setColorFilter( textColor, PorterDuff.Mode.SRC_ATOP );
    }

    private void setNightModeIcon() {
        if (Build.VERSION.SDK_INT < 14) {
            nightModeIcon.setVisibility(View.GONE);
            return;
        }

        if (settings.nightModeActivationMode == Settings.NIGHT_MODE_ACTIVATION_MANUAL
                || Utility.getLightSensor(mContext) == null ) {
            nightModeIcon.setVisibility(View.VISIBLE);
        } else {
            nightModeIcon.setVisibility(View.GONE);
        }
        nightModeIcon.setImageResource( (mode == 0) ? R.drawable.ic_moon : R.drawable.ic_sun );
    }

    private void setupBackgroundImage() {
        bgblack = new ColorDrawable(Color.BLACK);
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

        if ( settings.hideBackgroundImage && mode == 0 ) {
            background_image.setImageDrawable(bgblack);
        } else {
            background_image.setImageDrawable(bgshape);
        }
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
            return new ColorDrawable(Color.BLACK);
        } catch (Exception e) {
            return new ColorDrawable(Color.BLACK);
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

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
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
            alarmClock.cancelAlarm(); //remove any internal alarms first
            String nextAlarm = alarmClock.getNextSystemAlarmTime();

            if ( Build.VERSION.SDK_INT >= 19
                    && nextAlarm != null
                    && nextAlarm.isEmpty() ) {
                nextAlarm = mContext.getString(R.string.set_alarm);
            }
            alarmTime.setText(nextAlarm);
            if (! daydreamMode) {
                alarmTime.setOnClickListener(onStockAlarmTimeClickListener);
                alarmTime.setClickable(true);
            }
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
        if ( lightSensorEventListener != null ) {
            lightSensorEventListener.unregister();
        }
        unregister(broadcastReceiver);
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
        removeCallbacks(moveAround);
        Runnable fixConfig = new Runnable() {
                public void run() {
                    clockLayout.updateLayout(clockLayoutContainer.getWidth(), newConfig);
                    //clockLayout.update(settings.weatherEntry);
                    centerClockLayout();
                    float s = getScaleFactor(newConfig);
                    clockLayout.setScaleFactor(s);

                    if ( showcaseView == null ) {
                        handler.postDelayed(moveAround, 60000);
                    } else {
                        setupShowcaseView();
                    }
                }
        };
        handler.postDelayed(fixConfig, 200);
    }

    public void updateBatteryView() {
        BatteryValue reference = settings.loadBatteryReference();
        batteryView.update(batteryValue, reference);
    }

    public void updateBatteryValue() {
        Log.d(TAG, "updating battery value");
        BatteryStats battery = new BatteryStats(mContext);
        this.batteryValue = battery.reference;
    }

    public void setupScreenAnimation() {
        if (this.batteryValue.isCharging) {
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
        Random random = new Random();
        int w = clockLayoutContainer.getWidth();
        int h = clockLayoutContainer.getHeight();
        Log.i(TAG, String.valueOf(w) + "x" + String.valueOf(h));
        if (Build.VERSION.SDK_INT < 12) {
            // make back panel fully transparent
            clockLayout.setBackgroundColor(Color.parseColor("#00000000"));

            int rxpos = (w - clockLayout.getWidth()) / 2;
            int rypos = (h - clockLayout.getHeight()) / 2; // API level 1

            rxpos = to_range(rxpos, 1, w);
            rypos = to_range(rypos, 1, h);

            int i1 = random.nextInt(2 * rxpos);
            int i2 = 90 + random.nextInt(2 * rypos);

            clockLayout.setPadding(i1, i2, 0, 0);
            clockLayout.invalidate();
        } else {
            // determine a random position
            // api level 12
            int scaled_width = (int) (clockLayout.getWidth() * clockLayout.getScaleX());
            int scaled_height = (int) (clockLayout.getHeight() * clockLayout.getScaleY());
            Log.i(TAG, String.valueOf(scaled_width) + "x" + String.valueOf(scaled_height));
            int rxpos = w - scaled_width;
            int rypos = h - scaled_height;

            int i1 = (rxpos > 0) ? random.nextInt(rxpos) : 0;
            int i2 = (rypos > 0) ? random.nextInt(rypos) : 0;

            i1 -= (clockLayout.getWidth() - scaled_width) / 2;
            i2 -= (clockLayout.getHeight() - scaled_height) / 2;
            Log.i(TAG, String.valueOf(i1) + "x" + String.valueOf(i2));
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

    private void dimScreen(int millis, float light_value, float add_brightness){
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
        if (settings.background_mode == Settings.BACKGROUND_IMAGE) {
            v = to_range(v, 0.5f, 1.f);
        }

        brightness = to_range(brightness, 0.01f, 1.f);
        // On some screens (as the Galaxy S2) a value of 0 means the screen is completely dark.
        // Therefore a minimum value must be set to preserve the visibility of the clock.

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
            setAlpha(menuIcon, v, millis);
        }

        if ( batteryViewShallBeVisible() ) {
            v = to_range(v, 0.6f, 1.f);
            setAlpha(batteryView, v, millis);
        } else {
            setAlpha(batteryView, 0.0f, millis);
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
        layout.buttonBrightness = LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
        window.setAttributes(layout);
        fadeSoftButtons();
    }

    private void setScreenOrientation(int orientation) {
        if (daydreamMode) return;
        ((Activity) mContext).setRequestedOrientation(orientation);
    }

    private void fadeSoftButtons() {
        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 19){
            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= 19){
            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
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

    public void setMode(int new_mode, float light_value) {
        Log.d(TAG, String.format("setMode %d -> %d", mode, new_mode));
        int current_mode = mode;
        mode = new_mode;
        if ((new_mode == 0) && (current_mode != 0)){
            if (settings.muteRinger) AudioManage.setRingerModeSilent();
            setColor();
            if ( settings.hideBackgroundImage ) {
                background_image.setImageDrawable(bgblack);
            }
        } else
        if ((new_mode != 0) && (current_mode == 0)){
            restoreRingerMode();
            setColor();
            if ( settings.hideBackgroundImage ) {
                background_image.setImageDrawable(bgshape);
            }
        }

        float dim_offset = settings.dim_offset;
        if ((new_mode == 1) && (current_mode == 0)) {
            dim_offset += 0.1f;
        }
        dimScreen(screen_alpha_animation_duration, light_value, dim_offset);

        if (soundmeter != null) {
            if (new_mode == 0 && soundmeter.isRunning() == false) {
                soundmeter.startMeasurement(3000);
            } else if (new_mode == 1 && soundmeter.isRunning() == false){
                soundmeter.startMeasurement(60000);
            } else if (new_mode > 1) {
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
            Configuration config = getConfiguration();
            clockLayout.updateLayout(clockLayoutContainer.getWidth(), config);
            //clockLayout.update(settings.weatherEntry);

            float s = getScaleFactor(config);
            clockLayout.animate().setDuration(1000).scaleX(s).scaleY(s);
       }
    };

    // move the clock randomly around
    private Runnable moveAround = new Runnable() {
       @Override
       public void run() {
           removeCallbacks(hideBrightnessLevel);
           updateBatteryValue();
           setupScreenAnimation();

           hideBatteryView(2000);
           updateBatteryView();

           updateClockPosition();
           updateWeatherData();

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
           if ( alarmClock.isInteractive() || AlarmHandlerService.alarmIsRunning() ) {
               handler.postDelayed(hideAlarmClock, 20000);
               return;
           }
           controlsVisible = false;
           hideBatteryView(2000);
           setAlpha(menuIcon, 0.f, 2000);
           alarmClock.isVisible = false;
           alarmClock.setClickable(false);
           alarmTime.setClickable(false);
           setAlpha(alarmClock, 0.f, 2000);
           setAlpha(alarmTime, 0.f, 2000);
           hideSidePanel();
       }
    };

    private void toggleSidePanel() {
        if (Build.VERSION.SDK_INT > 11) {
           float x = sidePanel.getX();
           if (x < 0.f ) {
               showSidePanel();
           } else {
               hideSidePanel();
           }
        } else {
            if ( sidePanel.getVisibility() == View.VISIBLE ) {
               hideSidePanel();
            } else {
               showSidePanel();
            }
        }

    }
    private void showSidePanel() {
        if (Build.VERSION.SDK_INT > 11) {
            sidePanel.animate().setDuration(250).x(0);
        } else {
            sidePanel.setVisibility(View.VISIBLE);
            sidePanel.setClickable(true);
        }
        handler.postDelayed(hideAlarmClock, 20000);
    }

    private void hideSidePanel() {
        int w = sidePanel.getWidth();
        if (Build.VERSION.SDK_INT > 11) {
            sidePanel.animate().setDuration(250).x(-w);
        } else {
            sidePanel.setVisibility(View.INVISIBLE);
            sidePanel.setClickable(false);
        }
    }

    private void initSidePanel() {
        if (Build.VERSION.SDK_INT > 11) {
            sidePanel.setX(-1000);
        } else {
            sidePanel.setVisibility(View.INVISIBLE);
            sidePanel.setClickable(false);
        }
    }

    private void hideBatteryView(int millis) {
        if (! batteryViewShallBeVisible() ) {
            setAlpha(batteryView, 0.f, millis);
        }
    }

    private boolean batteryViewShallBeVisible() {
        return (controlsVisible ||
                (settings.persistentBatteryValueWhileCharging &&
                 batteryView.shallBeVisible(this.batteryValue))
               );
    }

    public void showAlarmClock() {
        removeCallbacks(hideAlarmClock);
        handler.postDelayed(hideAlarmClock, 20000);
        controlsVisible = true;
        setupAlarmClock();
        alarmClock.invalidate();
        dimScreen(0, last_ambient, settings.dim_offset);
    }

    private Configuration getConfiguration() {
        return mContext.getResources().getConfiguration();
    }

    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    GestureDetector.SimpleOnGestureListener mSimpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {
            int rect[] = new int[2];
            clockLayoutContainer.getLocationOnScreen(rect);
            if (e1.getY() < rect[1]) return false;

            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH){
                return false;
            }
            // right to left swipe
            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                Log.w(TAG, "left swipe");
                hideSidePanel();
            }
            // left to right swipe
            else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                Log.w(TAG, "right swipe");
                showSidePanel();
            }
            return false;
        }

        @Override
        public boolean onSingleTapUp (MotionEvent e) {
            Log.w(TAG, "single tap up");
            updateBatteryValue();
            updateBatteryView();

            showAlarmClock();
            removeCallbacks(hideAlarmClock);
            handler.postDelayed(hideAlarmClock, 20000);

            if ( AlarmHandlerService.alarmIsRunning() ) {
                alarmClock.snooze();
            }
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            int rect[] = new int[2];
            clockLayoutContainer.getLocationOnScreen(rect);
            if (e1.getY() < rect[1]) {
                Point size = utility.getDisplaySize();
                float dx = -2.f * (distanceX / size.x);
                settings.dim_offset += dx;
                settings.dim_offset = to_range(settings.dim_offset, -1.f, 1.f);

                int value = (int) (100.f * (settings.dim_offset + 1.f));
                brightnessProgress.setProgress(value);
                brightnessProgress.setVisibility(View.VISIBLE);

                setAlpha(brightnessProgress, 1.f, 0);
                removeCallbacks(hideBrightnessLevel);
                removeCallbacks(hideBrightnessView);
                handler.postDelayed(hideBrightnessLevel, 1000);

                dimScreen(0, last_ambient, settings.dim_offset);
                settings.setBrightnessOffset(settings.dim_offset);
            }
            return false;
        }
    };

    OnScaleGestureListener mOnScaleGestureListener = new OnScaleGestureListener() {
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            float s = clockLayout.getScaleX();
            Configuration config = getConfiguration();
            settings.setScaleClock(s, config.orientation);
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float s = detector.getScaleFactor();
            applyScaleFactor(s);
            return true;
        }
    };

    private void applyScaleFactor(float factor) {
        int screen_width = clockLayoutContainer.getWidth();
        int screen_height = clockLayoutContainer.getHeight();
        factor *= clockLayout.getScaleX();
        int new_width = (int) (clockLayout.getWidth() * factor);
        int new_height = (int) (clockLayout.getHeight() * factor);
        if (factor > 0.5f && new_width <= screen_width && new_height <= screen_height) {
            clockLayout.setScaleFactor(factor);
        }
    }

    private float getScaleFactor(Configuration config) {
        float s = settings.getScaleClock(config.orientation);
        float max = getMaxScaleFactor();
        return ( s < max ) ? s : max;
    }

    private float getMaxScaleFactor() {
        float factor_x = (float) clockLayoutContainer.getWidth() / clockLayout.getWidth();
        float factor_y = (float) clockLayoutContainer.getHeight() / clockLayout.getHeight();
        return (factor_x < factor_y ) ? factor_x : factor_y;
    }

    OnClickListener onMenuItemClickListener = new OnClickListener() {
        public void onClick(View v) {
            toggleSidePanel();
        }
    };

    OnClickListener onStockAlarmTimeClickListener = new OnClickListener() {
        public void onClick(View v) {
            if (Build.VERSION.SDK_INT < 19) return;

            Intent mClockIntent = new Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS);
            mClockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(mClockIntent);
        }
    };

    public boolean onTouch(View view, MotionEvent e, float last_ambient) {
        boolean event_consumed = mGestureDetector.onTouchEvent(e);
        mScaleDetector.onTouchEvent(e);
        return true;
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

    class NightDreamBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ( OpenWeatherMapApi.ACTION_WEATHER_DATA_UPDATED.equals(action) ) {
                Log.v(TAG, "Weather data updated");
                settings.weatherEntry = settings.getWeatherEntry();
                clockLayout.update(settings.weatherEntry);
            } else
            if ( Config.ACTION_ALARM_SET.equals(action) ) {
                if (showcaseView != null) showcaseView.hide();
                setupShowcaseForAlarmDeletion();
                if (intent.hasExtra("alarmTime")) {
                    settings.nextAlarmTime = intent.getLongExtra("alarmTime", 0L);
                    Log.w(TAG, String.format("alarm time: %d", settings.nextAlarmTime));
                    alarmClock.setSettings(settings);
                    alarmClock.invalidate();
                }
            } else
            if ( Config.ACTION_ALARM_STOPPED.equals(action) ) {
                settings.updateNextAlarmTime();
                Log.w(TAG, String.format("alarm time: %d", settings.nextAlarmTime));
                alarmClock.setSettings(settings);
                alarmClock.invalidate();
            } else
            if ( Config.ACTION_ALARM_DELETED.equals(action) ) {
                if (showcaseView != null) showcaseView.hide();
                settings.updateNextAlarmTime();
                Log.w(TAG, String.format("alarm time: %d", settings.nextAlarmTime));
                alarmClock.setSettings(settings);
                alarmClock.invalidate();
            }
        }
    }

    private NightDreamBroadcastReceiver registerBroadcastReceiver() {
        NightDreamBroadcastReceiver receiver = new NightDreamBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(OpenWeatherMapApi.ACTION_WEATHER_DATA_UPDATED);
        filter.addAction(Config.ACTION_ALARM_SET);
        filter.addAction(Config.ACTION_ALARM_STOPPED);
        filter.addAction(Config.ACTION_ALARM_DELETED);
        mContext.registerReceiver(receiver, filter);
        return receiver;
    }

    private void unregister(BroadcastReceiver receiver) {
        try {
            mContext.unregisterReceiver(receiver);
        } catch ( IllegalArgumentException e ) {

        }
    }

    public void onEvent(OnNewLightSensorValue event){
        last_ambient = event.value;
        dimScreen(screen_alpha_animation_duration, last_ambient, settings.dim_offset);
    }

    public void onEvent(OnLightSensorValueTimeout event){
        last_ambient = (event.value >= 0.f) ? event.value : settings.minIlluminance;
        dimScreen(screen_alpha_animation_duration, last_ambient, settings.dim_offset);
    }

    static int showcaseCounter = 0;
    static final long SHOWCASE_ID_ONBOARDING = 1;
    static final long SHOWCASE_ID_ALARMS = 2;
    static final long SHOWCASE_ID_ALARM_DELETION = 3;
    private void setupShowcase() {
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
            .setShowcaseEventListener(showcaseViewEventListener)
            .setOnClickListener(showCaseOnClickListener)
            .singleShot(SHOWCASE_ID_ONBOARDING)
            .build();

        showcaseView.setShowcaseTag(SHOWCASE_ID_ONBOARDING);
        showcaseView.showButton();
        showShowcase();
        setupShowcaseForQuickAlarms();
        setupShowcaseForAlarmDeletion();
    }

    View.OnClickListener showCaseOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showcaseCounter++;
            setupShowcaseView();
        }
    };

    void setupShowcaseView() {
        if (showcaseView.getShowcaseTag() != SHOWCASE_ID_ONBOARDING) return;

        switch(showcaseCounter) {
            case 0:
                showcaseView.setShowcase(Target.NONE, true);
                showcaseView.setContentTitle(mContext.getString(R.string.welcome_screen_title1));
                showcaseView.setContentText(mContext.getString(R.string.welcome_screen_text1));
                showcaseView.setBlockAllTouches(true);
                showcaseView.showButton();
                break;
            case 1:
                showcaseView.setShowcase(new ViewTarget(menuIcon), true);
                showcaseView.setContentTitle(mContext.getString(R.string.welcome_screen_title2));
                showcaseView.setContentText(mContext.getString(R.string.welcome_screen_text2));
                showcaseView.setBlockAllTouches(false);
                showcaseView.setBlocksTouches(true);
                showcaseView.showButton();
                break;
            case 2:
                Point size = utility.getDisplaySize();
                showcaseView.setShowcase(new PointTarget(size.x/2, 20), true);
                showcaseView.setBlockAllTouches(false);
                showcaseView.setContentTitle(mContext.getString(R.string.welcome_screen_title3));
                showcaseView.setContentText(mContext.getString(R.string.welcome_screen_text3));
                showcaseView.showButton();
                break;
            case 3:
                setAlpha(clockLayout, 1.f, 500);
                showcaseView.setShowcase(new ViewTarget(clockLayout), true);
                showcaseView.setContentTitle(mContext.getString(R.string.welcome_screen_title4));
                showcaseView.setContentText(mContext.getString(R.string.welcome_screen_text4));
                showcaseView.setBlockAllTouches(false);
                showcaseView.setBlocksTouches(true);
                //showcaseView.setBlockAllTouches(true);
                showcaseView.showButton();
                break;
            default:
                showcaseView.hide();
                break;
        }
    }

    private void setupShowcaseForQuickAlarms() {
        if ( showcaseView != null || daydreamMode) {
            return;
        }

        if (settings.useInternalAlarm) {
            Point size = utility.getDisplaySize();
            showcaseView = new ShowcaseView.Builder((Activity) mContext)
                .setTarget(new PointTarget(0, size.y))
                .hideOnTouchOutside()
                .setContentTitle(mContext.getString(R.string.use_internal_alarm))
                .setContentText(mContext.getString(R.string.showcase_text_set_alarms))
                .setShowcaseEventListener(showcaseViewEventListener)
                .singleShot(SHOWCASE_ID_ALARMS)
                .build();
            showcaseView.hideButton();
            showShowcase();
        }

    }

    private void setupShowcaseForAlarmDeletion() {
        if ( showcaseView != null || daydreamMode) {
            return;
        }

        if ( alarmClock.isAlarmSet() ) {
            Point size = utility.getDisplaySize();
            showcaseView = new ShowcaseView.Builder((Activity) mContext)
                .setTarget(new PointTarget(size.x, size.y))
                .hideOnTouchOutside()
                .setContentTitle(mContext.getString(R.string.use_internal_alarm))
                .setContentText(mContext.getString(R.string.showcase_text_delete_alarms))
                .setShowcaseEventListener(showcaseViewEventListener)
                .singleShot(SHOWCASE_ID_ALARM_DELETION)
                .build();
            showcaseView.hideButton();
            showShowcase();
        }

    }

    private void showShowcase() {
        showcaseView.show();
        if ( !showcaseView.isShowing()) {
            showcaseView = null;
        }
    }

    OnShowcaseEventListener showcaseViewEventListener = new OnShowcaseEventListener() {
        @Override
        public void onShowcaseViewHide(ShowcaseView view) {
            Log.i(TAG, "onShowcaseViewHide()");

        }

        @Override
        public void onShowcaseViewDidHide(ShowcaseView view) {
            Log.i(TAG, "onShowcaseViewDidHide()");
            showcaseView = null;
            handler.postDelayed(moveAround, 30000);
            handler.postDelayed(hideAlarmClock, 20000);
            brightnessProgress.setVisibility(View.INVISIBLE);
            updateBatteryValue();
            updateBatteryView();
            showAlarmClock();

            setupShowcaseForQuickAlarms();
            setupShowcaseForAlarmDeletion();
        }

        @Override
        public void onShowcaseViewShow(ShowcaseView view) {
            Log.i(TAG, "onShowcaseViewShow()");
            removeCallbacks(moveAround);
            removeCallbacks(hideAlarmClock);
            setAlpha(clockLayout, 0.2f, 0);
            setAlpha(alarmTime, 0.2f, 0);
        }

        @Override
        public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {
            Log.i(TAG, "onShowcaseViewTouchBlocked()");

        }
    };

}
