package com.firebirdberlin.nightdream.ui;

import android.Manifest;
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
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.LightSensorEventListener;
import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.SoundMeter;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.events.OnLightSensorValueTimeout;
import com.firebirdberlin.nightdream.events.OnNewLightSensorValue;
import com.firebirdberlin.nightdream.mAudioManager;
import com.firebirdberlin.nightdream.repositories.VibrationHandler;
import com.firebirdberlin.nightdream.services.AlarmHandlerService;
import com.firebirdberlin.nightdream.services.DownloadWeatherService;
import com.firebirdberlin.nightdream.widget.ClockWidgetProvider;
import com.firebirdberlin.openweathermapapi.OpenWeatherMapApi;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;
import com.google.android.flexbox.FlexboxLayout;

import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class NightDreamUI {
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private static String TAG = "NightDreamUI";
    final private Handler handler = new Handler();
    final private Drawable bgblack = new ColorDrawable(Color.BLACK);
    private int screen_alpha_animation_duration = 3000;
    private int screen_transition_animation_duration = 10000;
    private int mode = 2;
    private boolean isDebuggable;
    private boolean controlsVisible = false;
    private Context mContext;
    private FrameLayout mainFrame;
    private Drawable bgshape = bgblack;
    private AlarmClock alarmClock;

    private ConstraintLayout parentLayout;
    private ImageView[] background_images = new ImageView[2];
    private int background_image_active = 0;

    private TextView textViewExif;

    private ArrayList<File> files;

    private Bitmap preloadBackgroundImage;
    private File preloadBackgroundImageFile;

    private ImageView menuIcon;
    private ImageView nightModeIcon;
    private ImageView radioIcon;
    private LightSensorEventListener lightSensorEventListener = null;
    private FrameLayout clockLayoutContainer;
    private ClockLayout clockLayout;
    private FlexboxLayout notificationStatusBar;
    private FlexboxLayout sidePanel;
    private BottomPanelLayout bottomPanelLayout;
    private Settings settings;
    OnScaleGestureListener mOnScaleGestureListener = new OnScaleGestureListener() {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            Log.d(TAG, "onScaleBegin");
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Log.d(TAG, "onScale");
            float s = detector.getScaleFactor();
            applyScaleFactor(s);
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            Log.d(TAG, "onScaleEnd");
            float s = clockLayout.getAbsScaleFactor();
            Configuration config = getConfiguration();
            settings.setScaleClock(s, config.orientation);
        }
    };
    private int vibrantColor = 0;
    private int vibrantColorDark = 0;
    private long lastAnimationTime = 0L;
    private SoundMeter soundmeter;
    private ProgressBar brightnessProgress;
    private BatteryIconView batteryIconView;
    public Runnable initSlideshowBackground = new Runnable() {
        @Override
        public void run() {
            if (settings.getBackgroundMode() == Settings.BACKGROUND_SLIDESHOW) {
                if (preloadBackgroundImage == null) {
                    parentLayout.postDelayed(initSlideshowBackground, 500);
                } else {
                    setupBackgroundImage();
                }
            }
        }
    };
    private Window window;
    private mAudioManager AudioManage;
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;
    private NightDreamBroadcastReceiver broadcastReceiver = null;
    private boolean locked = false;
    private float last_ambient = 4.0f;
    private float LIGHT_VALUE_DARK = 4.2f;
    private float LIGHT_VALUE_BRIGHT = 40.0f;
    private float LIGHT_VALUE_DAYLIGHT = 300.0f;
    private Runnable zoomIn = new Runnable() {
        @Override
        public void run() {
            Configuration config = getConfiguration();
            clockLayout.updateLayout(clockLayoutContainer.getWidth(), config);
            //clockLayout.update(settings.weatherEntry);

            float s = getScaleFactor(config);
            clockLayout.setScaleFactor(s, true);
            Utility.turnScreenOn(mContext);
            Utility.hideSystemUI(mContext);
        }
    };
    private Runnable hideBrightnessView = new Runnable() {
        @Override
        public void run() {
            brightnessProgress.setVisibility(View.INVISIBLE);
        }
    };
    private Runnable hideBrightnessLevel = new Runnable() {
        @Override
        public void run() {
            setAlpha(brightnessProgress, 0.f, 2000);
            handler.postDelayed(hideBrightnessView, 2010);
        }
    };
    private Runnable backgroundChange = new Runnable() {
        @Override
        public void run() {
            setupBackgroundImage();
            handler.postDelayed(this, 15000 * settings.backgroundImageDuration);
        }
    };
    private Runnable fadeClock = new Runnable() {
        @Override
        public void run() {
            if (settings.screenProtection == Settings.ScreenProtectionModes.FADE) {
                AlphaAnimation alpha;
                alpha = new AlphaAnimation(1.0f, 0.0f);
                alpha.setDuration(2000);
                alpha.setFillAfter(true);

                AnimationSet animationSet = new AnimationSet(true);

                animationSet.addAnimation(alpha);

                animationSet.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        AlphaAnimation alpha;
                        alpha = new AlphaAnimation(0.0f, 1.0f);
                        alpha.setDuration(2000);
                        alpha.setFillAfter(true);
                        clockLayoutContainer.startAnimation(alpha);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                clockLayoutContainer.startAnimation(animationSet);
                handler.postDelayed(this, 30000);
            }
        }
    };
    // move the clock randomly around
    private Runnable moveAround = new Runnable() {
        @Override
        public void run() {
            removeCallbacks(hideBrightnessLevel);
            hideSystemUI();
            setupScreenAnimation();

            hideBatteryView(2000);
            updateClockPosition();

            updateWeatherData();

            handler.postDelayed(this, 60000);
        }
    };
    private Runnable hideAlarmClock = new Runnable() {
        @Override
        public void run() {
            if (alarmClock.isInteractive() || AlarmHandlerService.alarmIsRunning()) {
                handler.postDelayed(hideAlarmClock, 20000);
                return;
            }
            controlsVisible = false;
            hideBatteryView(2000);
            setAlpha(menuIcon, 0.f, 2000);

            bottomPanelLayout.hide();
            if (mode == 0) {
                setAlpha(notificationStatusBar, 0.f, 2000);
            }
            hideSidePanel();
        }
    };
    private final UserInteractionObserver bottomPanelUserInteractionObserver = new UserInteractionObserver() {
        public void notifyAction() {
            resetAlarmClockHideDelay();
        }
    };
    private boolean blinkStateOn = false;
    private Runnable blink = new Runnable() {
        public void run() {
            handler.removeCallbacks(blink);
            if (AlarmHandlerService.alarmIsRunning()) {
                blinkStateOn = !blinkStateOn;
                float alpha = (blinkStateOn) ? 1.f : 0.5f;
                setAlpha(menuIcon, alpha, 0);
                handler.postDelayed(blink, 1000);
            } else {
                blinkStateOn = false;
            }
        }
    };
    public Runnable initClockLayout = new Runnable() {
        @Override
        public void run() {
            setupClockLayout();
            setColor();
            updateWeatherData();
            controlsVisible = true;

            brightnessProgress.setVisibility(View.INVISIBLE);

            showAlarmClock();
            setupShowcase();
            clockLayout.post(new Runnable() {
                public void run() {
                    handler.postDelayed(zoomIn, 500);
                }
            });
        }
    };
    private GestureDetector.SimpleOnGestureListener mSimpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        int[] rect = new int[2];

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            clockLayoutContainer.getLocationOnScreen(rect);
            if (e1.getY() < rect[1]) return false;

            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
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
        public boolean onSingleTapUp(MotionEvent e) {
            Log.w(TAG, "single tap up");

            showAlarmClock();
            resetAlarmClockHideDelay();

            if (AlarmHandlerService.alarmIsRunning()) {
                alarmClock.snooze();
            }

            removeCallbacks(hideBrightnessLevel);
            removeCallbacks(hideBrightnessView);
            setBrightnessProgress();
            brightnessProgress.setVisibility(View.VISIBLE);
            setAlpha(brightnessProgress, 1.f, 0);
            handler.postDelayed(hideBrightnessLevel, 1000);

            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d(TAG, "onScroll");
            if (
                    clockLayoutContainer == null || mContext == null || settings == null
                            || brightnessProgress == null
            ) return false;
            if (brightnessProgress.getVisibility() == View.VISIBLE) {
                clockLayoutContainer.getLocationOnScreen(rect);
                if (e1.getY() < rect[1] && e2.getY() < rect[1]) {
                    Point size = Utility.getDisplaySize(mContext);
                    float dx = -2.f * (distanceX / size.x);
                    float value = (mode == 0) ? settings.nightModeBrightness : settings.dim_offset;
                    value += dx;
                    value = to_range(value, -1.f, 1.f);

                    setAlpha(brightnessProgress, 1.f, 0);

                    dimScreen(0, last_ambient, value);
                    if (mode != 0) {
                        settings.setBrightnessOffset(value);
                    } else {
                        settings.setNightModeBrightness(value);
                    }
                    setBrightnessProgress();
                }
                removeCallbacks(hideBrightnessLevel);
                removeCallbacks(hideBrightnessView);
                handler.postDelayed(hideBrightnessLevel, 1000);
            }
            return false;
        }
    };
    private Runnable setupSidePanel = new Runnable() {
        @Override
        public void run() {
            if (sidePanel.getX() < 0) {
                initSidePanel();
            }

        }
    };
    /*
    private OnShowcaseEventListener showcaseViewEventListener = new OnShowcaseEventListener() {
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
            showAlarmClock();
        }

        @Override
        public void onShowcaseViewShow(ShowcaseView view) {
            Log.i(TAG, "onShowcaseViewShow()");
            removeCallbacks(moveAround);
            removeCallbacks(hideAlarmClock);
            setAlpha(clockLayout, 0.2f, 0);
            setAlpha(bottomPanelLayout, 0.2f, 0);
        }

        @Override
        public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {
            Log.i(TAG, "onShowcaseViewTouchBlocked()");

        }
    };
     */

    public NightDreamUI(Context context, Window window) {
        mContext = context;

        mGestureDetector = new GestureDetector(mContext, mSimpleOnGestureListener);
        mScaleDetector = new ScaleGestureDetector(mContext, mOnScaleGestureListener);

        this.window = window;
        View rootView = window.getDecorView().findViewById(android.R.id.content);
        mainFrame = rootView.findViewById(R.id.main_frame);

        parentLayout = rootView.findViewById(R.id.background_group);

        background_images[0] = rootView.findViewById(R.id.background_view);
        background_images[1] = rootView.findViewById(R.id.background_view2);
        background_image_active = 0;

        textViewExif = rootView.findViewById(R.id.textViewExif);

        brightnessProgress = rootView.findViewById(R.id.brightness_progress);
        batteryIconView = rootView.findViewById(R.id.batteryIconView);
        clockLayoutContainer = rootView.findViewById(R.id.clockLayoutContainer);
        clockLayout = rootView.findViewById(R.id.clockLayout);
        bottomPanelLayout = rootView.findViewById(R.id.bottomPanel);
        bottomPanelLayout.setUserInteractionObserver(bottomPanelUserInteractionObserver);
        alarmClock = bottomPanelLayout.getAlarmClock();
        notificationStatusBar = rootView.findViewById(R.id.notificationstatusbar);
        menuIcon = rootView.findViewById(R.id.burger_icon);
        nightModeIcon = rootView.findViewById(R.id.night_mode_icon);
        radioIcon = rootView.findViewById(R.id.radio_icon);
        sidePanel = rootView.findViewById(R.id.side_panel);
        sidePanel.post(setupSidePanel);

        OnClickListener onMenuItemClickListener = new OnClickListener() {
            public void onClick(View v) {
                if (locked) return;
                toggleSidePanel();
            }
        };
        menuIcon.setOnClickListener(onMenuItemClickListener);
        View.OnLongClickListener onMenuItemLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                locked = !locked;
                settings.setUILocked(locked);
                lockUI(locked);
                if (locked) {
                    hideSidePanel();
                }
                VibrationHandler handler = new VibrationHandler(mContext);
                handler.startOneShotVibration(50);
                return true;
            }
        };
        menuIcon.setOnLongClickListener(onMenuItemLongClickListener);

        // prepare zoom-in effect
        menuIcon.setScaleX(.8f);
        menuIcon.setScaleY(.8f);
        clockLayout.setScaleFactor(.1f);

        settings = new Settings(context);
        AudioManage = new mAudioManager(context);

        checkForReviewRequest();
        isDebuggable = Utility.isDebuggable(context);
    }

    private void resetAlarmClockHideDelay() {
        removeCallbacks(hideAlarmClock);
        handler.postDelayed(hideAlarmClock, 20000);
    }

    void setBrightnessProgress() {
        float value = (mode == 0) ? settings.nightModeBrightness : settings.dim_offset;
        value = to_range(value, -1.f, 1.f);
        int intValue = (int) (100.f * (value + 1.f));
        brightnessProgress.setProgress(intValue);
    }

    public void onStart() {
        Log.d(TAG, "onStart()");
        handler.postDelayed(moveAround, 30000);
        handler.postDelayed(fadeClock, 30000);
    }

    public void onResume() {
        Log.d(TAG, "onResume()");
        hideSystemUI();
        settings.reload();
        vibrantColor = 0;
        vibrantColorDark = 0;
        this.locked = settings.isUIlocked;

        removeCallbacks(backgroundChange);
        background_images[0].clearAnimation();
        background_images[0].setImageDrawable(bgblack);
        background_images[1].clearAnimation();
        background_images[1].setImageDrawable(bgblack);
        lastAnimationTime = 0L;
        setScreenOrientation(settings.screenOrientation);
        initSidePanel();
        initBackground();
        bottomPanelLayout.setAlarmUseLongPress(settings.stopAlarmOnLongPress);
        bottomPanelLayout.setAlarmUseSingleTap(settings.stopAlarmOnTap);
        bottomPanelLayout.setShowAlarmsPersistently(settings.showAlarmsPersistently);
        bottomPanelLayout.setUseAlarmSwipeGesture(settings.useAlarmSwipeGesture);
        bottomPanelLayout.setup();
        setupScreenAnimation();
        lockUI(this.locked);

        clockLayoutContainer.post(initClockLayout);

        Utility.registerEventBus(this);
        broadcastReceiver = registerBroadcastReceiver();
        initLightSensor();
        if (settings.useAmbientNoiseDetection()) {
            soundmeter = new SoundMeter(isDebuggable);
        } else {
            soundmeter = null;
        }

    }

    private void initBackground() {
        switch (settings.getBackgroundMode()) {
            case Settings.BACKGROUND_SLIDESHOW:
                loadBackgroundImageFiles();
                if (files.size() > 0) {
                    preloadBackgroundImageFile = files.get(new Random().nextInt(files.size()));
                    AsyncTask<File, Integer, Bitmap> runningTask = new preloadImageFromPath();
                    runningTask.execute(preloadBackgroundImageFile);
                    parentLayout.postDelayed(initSlideshowBackground, 500);
                    handler.postDelayed(backgroundChange, 15000 * settings.backgroundImageDuration);
                }
                break;
            default:
                setupBackgroundImage();
                break;
        }
    }

    private void initLightSensor() {
        if (Settings.NIGHT_MODE_ACTIVATION_AUTOMATIC == settings.nightModeActivationMode
                || settings.autoBrightness) {
            lightSensorEventListener = new LightSensorEventListener(mContext);
            lightSensorEventListener.register();
        } else {
            lightSensorEventListener = null;
        }
    }

    private void updateWeatherData() {
        if (!settings.showWeather) return;

        DownloadWeatherService.start(mContext);

        // handle outdated weather data
        WeatherEntry entry = settings.weatherEntry;
        long diff = entry.ageMillis();
        if (entry.timestamp == -1L || diff > 8 * 60 * 60 * 1000) {
            clockLayout.clearWeather();
        }
    }

    public void setupClockLayout() {
        if (settings.screenProtection != Settings.ScreenProtectionModes.MOVE) {
            centerClockLayout();
        }
        int layoutId = settings.getClockLayoutID(false);
        clockLayout.setLayout(layoutId);
        clockLayout.setDateFormat(settings.dateFormat);

        String timeFormat = settings.getTimeFormat(layoutId);
        clockLayout.setTimeFormat(timeFormat, settings.is24HourFormat());
        clockLayout.setTypeface(settings.typeface);
        clockLayout.setTemperature(
                settings.showTemperature, settings.showApparentTemperature, settings.temperatureUnit
        );
        clockLayout.setWindSpeed(settings.showWindSpeed, settings.speedUnit);

        clockLayout.setShowDivider(settings.getShowDivider(layoutId));
        clockLayout.setMirrorText(settings.clockLayoutMirrorText);
        clockLayout.showDate(settings.showDate);
        clockLayout.showWeather(settings.showWeather);
        clockLayout.setWeatherIconSizeFactor(settings.getWeatherIconSizeFactor(layoutId));
        Configuration config = getConfiguration();
        clockLayout.updateLayout(clockLayoutContainer.getWidth(), config);
        clockLayout.update(settings.weatherEntry);
    }

    private void setColor() {
        setNightModeIcon();

        int accentColor = getAccentColor();
        int textColor = getSecondaryColor();

        batteryIconView.setColor(textColor);
        textViewExif.setTextColor(textColor);
        menuIcon.setColorFilter(textColor, PorterDuff.Mode.SRC_ATOP);

        // colorize icons in the side panel
        for (int i = 0; i < sidePanel.getChildCount(); i++) {
            View view = sidePanel.getChildAt(i);
            colorizeImageView(view, textColor);
            if (view instanceof LinearLayout) {
                LinearLayout layout = ((LinearLayout) view);
                for (int j = 0; j < layout.getChildCount(); j++) {
                    colorizeImageView(layout.getChildAt(j), textColor);
                }
            }
        }

        updateRadioIconColor();
        ((NightDreamActivity) mContext).setupFlashlight();

        bottomPanelLayout.setCustomColor(accentColor, textColor);
        int clockLayoutId = settings.getClockLayoutID(false);
        int glowRadius = settings.getGlowRadius(clockLayoutId);
        int textureId = settings.getTextureResId(clockLayoutId);
        clockLayout.setPrimaryColor(accentColor, glowRadius, accentColor, textureId);
        clockLayout.setSecondaryColor(textColor);

        Drawable brightnessDrawable = brightnessProgress.getProgressDrawable();
        if (Build.VERSION.SDK_INT < 21) {
            brightnessDrawable.setColorFilter(accentColor, PorterDuff.Mode.MULTIPLY);
        } else {
            brightnessProgress.setProgressTintList(ColorStateList.valueOf(accentColor));
            brightnessProgress.setProgressBackgroundTintList(
                    ColorStateList.valueOf(adjustAlpha(accentColor, 0.4f)));
        }
        Utility.colorizeView(notificationStatusBar, textColor);
    }

    private void updateRadioIconColor() {
        int accentColor = getAccentColor();
        int textColor = getSecondaryColor();
        final boolean webRadioViewActive = bottomPanelLayout.isWebRadioViewActive();
        final int color = (webRadioViewActive ? accentColor : textColor);
        radioIcon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        Utility.setIconSize(mContext, radioIcon);
    }

    public int getAccentColor() {
        if (vibrantColor != 0 && mode != 0) return vibrantColor;
        return (mode == 0) ? settings.clockColorNight : settings.clockColor;
    }

    public int getSecondaryColor() {
        return (mode == 0) ? settings.secondaryColorNight : settings.secondaryColor;
    }

    private void colorizeImageView(View view, int color) {
        if (view instanceof ImageView) {
            ((ImageView) view).setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }
    }

    private void setNightModeIcon() {
        if (settings.nightModeActivationMode == Settings.NIGHT_MODE_ACTIVATION_MANUAL
                || Utility.getLightSensor(mContext) == null) {
            nightModeIcon.setVisibility(View.VISIBLE);
        } else {
            nightModeIcon.setVisibility(View.GONE);
        }
        nightModeIcon.setImageResource((mode == 0) ? R.drawable.ic_moon : R.drawable.ic_sun);
        Utility.setIconSize(mContext, nightModeIcon);
    }

    private void setupBackgroundImage() {
        if (mode == 0) return;
        bgshape = bgblack;
        textViewExif.setVisibility(View.GONE);

        if (!Utility.isLowRamDevice(mContext)) {
            switch (settings.getBackgroundMode()) {
                case Settings.BACKGROUND_BLACK: {
                    bgshape = bgblack;
                    break;
                }
                case Settings.BACKGROUND_GRADIENT: {
                    bgshape = ContextCompat.getDrawable(mContext, R.drawable.background_gradient);
                    break;
                }
                case Settings.BACKGROUND_IMAGE: {
                    bgshape = loadBackgroundImage();
                    break;
                }
                case Settings.BACKGROUND_SLIDESHOW: {
                    if (preloadBackgroundImage == null) {
                        bgshape = loadBackgroundSlideshowImage();
                    } else {
                        bgshape = new BitmapDrawable(mContext.getResources(), preloadBackgroundImage);
                        // TODO determine the dominant color in an AsyncTask
                        setDominantColorFromBitmap(preloadBackgroundImage);
                    }
                    if (settings.background_exif) {
                        try {
                            textViewExif.setVisibility(View.VISIBLE);
                            textViewExif.setText(getExifInformation());
                        } catch (IOException | IndexOutOfBoundsException e) {
                            textViewExif.setText("");
                            Log.e(TAG, "exception: ", e);
                        }
                    }
                    break;
                }
            }
        }

        if (settings.hideBackgroundImage && mode == 0) {
            background_images[background_image_active].setImageDrawable(bgblack);
            textViewExif.setVisibility(View.GONE);
        } else {

            background_image_active = (background_image_active + 1) % 2;

            switch (settings.slideshowStyle) {
                case Settings.SLIDESHOW_STYLE_CENTER:
                    background_images[background_image_active].setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    break;
                case Settings.SLIDESHOW_STYLE_CROPPED:
                default:
                    background_images[background_image_active].setScaleType(ImageView.ScaleType.CENTER_CROP);
                    break;
            }

            background_images[background_image_active].setImageDrawable(bgshape);
            setupSlideshowAnimation();
        }
    }

    private double convertArcMinToDegrees(String[] separated) {
        double convert;
        String[] separated2 = separated[2].split("/");
        convert = Double.parseDouble(separated2[0]) / Double.parseDouble(separated2[1]) / 60;
        String[] separated1 = separated[1].split("/");
        convert = (Double.parseDouble(separated1[0]) + convert) / Double.parseDouble(separated1[1]) / 60;
        String[] separated0 = separated[0].split("/");
        convert = Double.parseDouble(separated0[0]) + convert;
        return convert;
    }

    private String getExifInformation() throws IOException, IndexOutOfBoundsException {
        if (preloadBackgroundImageFile == null) {
            return "";
        }

        String exifDate = "";
        String exifTime = "";
        String exifCity = "";
        String exifCountry = "";

        ExifInterface exif = new ExifInterface(preloadBackgroundImageFile);

        String tagDateTime = exif.getAttribute(ExifInterface.TAG_DATETIME);
        if (tagDateTime != null) {
            String[] exifDateTime = tagDateTime.split(" ");
            String[] exifDateSplit = exifDateTime[0].split(":");
            exifDate = exifDateSplit[2] + "." + exifDateSplit[1] + "." + exifDateSplit[0];
            exifTime = exifDateTime[1];
        }

        String tagGpsLatitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
        String tagGpsLongitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
        if (tagGpsLatitude != null && tagGpsLongitude != null) {
            String[] separatedLat = tagGpsLatitude.split(",");
            String[] separatedLong = tagGpsLongitude.split(",");

            Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());

            List<Address> addressList = geocoder.getFromLocation(
                    convertArcMinToDegrees(separatedLat),
                    convertArcMinToDegrees(separatedLong),
                    1
            );
            if (addressList != null && addressList.size() > 0) {
                Address address = addressList.get(0);
                exifCity = address.getLocality();
                exifCountry = address.getCountryName();
            }
        }
        return exifDate + "\n" + exifTime + "\n" + exifCity + "\n" + exifCountry;
    }

    private Drawable loadBackgroundSlideshowImage() {
        Log.d(TAG, "loadBackgroundSlideshowImage");
        if (!settings.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            return new ColorDrawable(Color.BLACK);
        }

        if (files.isEmpty()) return new ColorDrawable(Color.BLACK);

        File file = files.get(new Random().nextInt(files.size()));
        preloadBackgroundImageFile = file;
        Bitmap bitmap = loadImageFromPath(file);
        bitmap = rescaleBackgroundImage(bitmap);
        setDominantColorFromBitmap(bitmap);
        if (bitmap != null) {
            return new BitmapDrawable(mContext.getResources(), bitmap);
        }
        return new ColorDrawable(Color.BLACK);
    }

    private void setupSlideshowAnimation() {
        Log.d(TAG, "setup SlideshowAnimation");

        long now = System.currentTimeMillis();
        if (now - lastAnimationTime < 10000) {
            return;
        }

        lastAnimationTime = now;

        background_images[background_image_active].setScaleX(1);
        background_images[background_image_active].setScaleY(1);
        background_images[background_image_active].setTranslationX(0);
        background_images[background_image_active].setTranslationY(0);

        AnimationSet animationSet = new AnimationSet(true);

        //prevent flicker - do not remove
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(1);
        animation.setFillAfter(true);
        animationSet.addAnimation(animation);

        if (settings.getBackgroundMode() == Settings.BACKGROUND_SLIDESHOW) {

            if (settings.background_fadein) {
                Animation alpha = AnimationUtils.loadAnimation(mContext.getApplicationContext(),
                        R.anim.fade_in);
                animationSet.addAnimation(alpha);
            }

            if (settings.background_movein) {
                Animation translate = null;

                switch (settings.background_movein_style) {
                    case 1:
                        translate = AnimationUtils.loadAnimation(mContext.getApplicationContext(),
                                R.anim.move_in_top);
                        break;
                    case 2:
                        translate = AnimationUtils.loadAnimation(mContext.getApplicationContext(),
                                R.anim.move_in_right);
                        break;
                    case 3:
                        translate = AnimationUtils.loadAnimation(mContext.getApplicationContext(),
                                R.anim.move_in_bottom);
                        break;
                    case 4:
                        translate = AnimationUtils.loadAnimation(mContext.getApplicationContext(),
                                R.anim.move_in_left);
                        break;
                }
                if (translate != null) {
                    animationSet.addAnimation(translate);
                }
            }

            if (settings.background_zoomin) {
                Animation animZoomIn = AnimationUtils.loadAnimation(mContext.getApplicationContext(),
                        R.anim.zoom_in);
                animZoomIn.setDuration(50000 * settings.backgroundImageDuration);
                animationSet.addAnimation(animZoomIn);
            }

            animationSet.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    if (settings.slideshowStyle == Settings.SLIDESHOW_STYLE_CENTER) {
                        Animation alpha = AnimationUtils.loadAnimation(mContext.getApplicationContext(), R.anim.fade_out);
                        background_images[(background_image_active + 1) % 2].startAnimation(alpha);
                    }
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        }

        background_images[background_image_active].startAnimation(animationSet);
        parentLayout.bringChildToFront(background_images[background_image_active]);
        parentLayout.bringChildToFront(textViewExif);

        if (files != null && settings.getBackgroundMode() == Settings.BACKGROUND_SLIDESHOW && files.size() > 0) {
            AsyncTask<File, Integer, Bitmap> runningTask = new preloadImageFromPath();
            preloadBackgroundImageFile = files.get(new Random().nextInt(files.size()));
            runningTask.execute(preloadBackgroundImageFile);
        }
    }

    private void loadBackgroundImageFiles() {
        if (!settings.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            return;
        }
        Log.d(TAG, "loadBackgroundImageFiles()");
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM); //+ "/Camera");
        files = Utility.listFiles(path, ".jpg");
    }

    private Drawable loadBackgroundImage() {
        try {
            Drawable cached = loadBackgroundImageFromCache();
            if (cached != null) {
                return cached;
            }

            Bitmap bgimage = loadBackgroundBitmap();
            bgimage = rescaleBackgroundImage(bgimage);
            AsyncTask<Bitmap, Integer, Bitmap> runningTask = new writeBackgroundImageToCache();
            runningTask.execute(bgimage);
            if (bgimage != null) {
                return new BitmapDrawable(mContext.getResources(), bgimage);
            }
        } catch (OutOfMemoryError e) {
            Toast.makeText(
                    mContext, "Out of memory. Please, try to scale down your image.",
                    Toast.LENGTH_LONG
            ).show();
        } catch (Exception e) {
            //pass
        }
        return new ColorDrawable(Color.BLACK);
    }

    private Bitmap loadBackgroundBitmap() throws Exception {
        Point display = Utility.getDisplaySize(mContext);
        if (!settings.backgroundImageURI.isEmpty()) {
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
            Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
            parcelFileDescriptor.close();

            int rotation = Utility.getCameraPhotoOrientation(fileDescriptor);
            if (rotation != 0) {
                bitmap = Utility.rotateBitmap(bitmap, rotation);
            }

            return bitmap;
        } else if (!settings.backgroundImagePath().isEmpty()) {
            // deprecated legacy version
            String bgpath = settings.backgroundImagePath();
            File file = new File(bgpath);
            return loadImageFromPath(file);
        }
        return null;
    }

    private Bitmap loadImageFromPath(File file) {
        Log.d(TAG, "load image from path");
        String path = file.getAbsolutePath();
        Point display = Utility.getDisplaySize(mContext);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = Utility.calculateInSampleSize(options, display.x, display.y);
        //RGB565 to reduce memory consumption
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap bitmap = (BitmapFactory.decodeFile(path, options));

        int rotation = Utility.getCameraPhotoOrientation(file);
        if (rotation != 0) {
            bitmap = Utility.rotateBitmap(bitmap, rotation);
        }
        return bitmap;
    }

    private Bitmap rescaleBackgroundImage(Bitmap bgimage) {
        Log.d(TAG, "rescaleBackgroundImage");
        if (bgimage == null) return null;

        Point display = Utility.getDisplaySize(mContext);
        int nw = bgimage.getWidth();
        int nh = bgimage.getHeight();
        boolean scaling_needed = false;
        if (bgimage.getHeight() > display.y) {
            nw = (int) ((display.y / (float) bgimage.getHeight()) * bgimage.getWidth());
            nh = display.y;
            scaling_needed = true;
        }

        if (nw > display.x) {
            nh = (int) ((display.x / (float) bgimage.getWidth()) * bgimage.getHeight());
            nw = display.x;
            scaling_needed = true;
        }

        if (scaling_needed) {
            bgimage = Bitmap.createScaledBitmap(bgimage, nw, nh, false);
        }
        return bgimage;
    }

    private BitmapDrawable loadBackgroundImageFromCache() {
        Log.d(TAG, "load image from cache");
        File cacheFile = new File(mContext.getCacheDir(), Config.backgroundImageCacheFilename);
        if (cacheFile.exists()) {
            Bitmap bgimage = BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
            setDominantColorFromBitmap(bgimage);
            Log.d(TAG, "loading image from cache");
            return new BitmapDrawable(mContext.getResources(), bgimage);
        }
        return null;
    }

    private void setDominantColorFromBitmap(Bitmap bitmap) {
        Log.d(TAG, "setDominantColorFromBitmap");

        if (!settings.background_mode_auto_color) return;
        int defaultColor = (mode == 0) ? settings.clockColorNight : settings.clockColor;
        int color = Utility.getVibrantColorFromPalette(bitmap, defaultColor);
        vibrantColorDark = Utility.getDarkMutedColorFromPalette(bitmap, Color.BLACK);

        if (color != defaultColor) {
            vibrantColor = color;
        } else {
            vibrantColor = 0;
        }
        setColor();
        mainFrame.setBackgroundColor(vibrantColorDark);
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    private void setupAlarmClock() {
        bottomPanelLayout.setAlarmUseLongPress(settings.stopAlarmOnLongPress);
        bottomPanelLayout.setAlarmUseSingleTap(settings.stopAlarmOnTap);
        bottomPanelLayout.setShowAlarmsPersistently(settings.showAlarmsPersistently);
        bottomPanelLayout.setUseAlarmSwipeGesture(settings.useAlarmSwipeGesture);
        bottomPanelLayout.setup();
        bottomPanelLayout.show();
    }

    public void reconfigure() {
        hideSystemUI();
        bottomPanelLayout.invalidate();
    }

    public void onPause() {
        Utility.unregisterEventBus(this);
        if (lightSensorEventListener != null) {
            lightSensorEventListener.unregister();
        }
        unregister(broadcastReceiver);
    }

    public void onStop() {
        removeCallbacks(moveAround);
        removeCallbacks(hideAlarmClock);
        removeCallbacks(initClockLayout);
        removeCallbacks(fadeClock);
        removeCallbacks(backgroundChange);
        removeCallbacks(zoomIn);
        if (soundmeter != null) {
            soundmeter.stopMeasurement();
            soundmeter = null;
        }

    }

    public void onDestroy() {

    }

    private void removeCallbacks(Runnable runnable) {
        if (runnable == null) return;

        handler.removeCallbacks(runnable);
    }

    public void onConfigurationChanged(final Configuration newConfig) {
        clockLayout.animate().cancel();
        removeCallbacks(moveAround);
        removeCallbacks(backgroundChange);

        Runnable fixConfig = new Runnable() {
            public void run() {
                clockLayout.updateLayout(clockLayoutContainer.getWidth(), newConfig);
                centerClockLayout();
                float s = getScaleFactor(newConfig);
                clockLayout.setScaleFactor(s);

                handler.postDelayed(moveAround, 60000);
                handler.postDelayed(backgroundChange, 15000 * settings.backgroundImageDuration);
                sidePanel.post(setupSidePanel);
            }
        };

        handler.postDelayed(fixConfig, 200);
    }

    private void setupScreenAnimation() {
        if (Utility.isCharging(mContext)) {
            screen_alpha_animation_duration = 3000;
            screen_transition_animation_duration = 10000;
        } else {
            screen_alpha_animation_duration = 0;
            screen_transition_animation_duration = 0;
        }
    }

    private void centerClockLayout() {
        clockLayout.setTranslationX(0);
        clockLayout.setTranslationY(0);
        clockLayout.invalidate();
    }

    private void updateClockPosition() {
        if (settings.screenProtection != Settings.ScreenProtectionModes.MOVE) {
            return;
        }
        Random random = new Random();
        int w = clockLayoutContainer.getWidth();
        int h = clockLayoutContainer.getHeight();
        Log.i(TAG, w + "x" + h);
        // determine a random position
        int scaled_width = Math.abs((int) (clockLayout.getWidth() * clockLayout.getScaleX()));
        int scaled_height = Math.abs((int) (clockLayout.getHeight() * clockLayout.getScaleY()));
        Log.i(TAG, scaled_width + "x" + scaled_height);
        int rxpos = w - scaled_width;
        int rypos = h - scaled_height;

        int i1 = (rxpos > 0) ? random.nextInt(rxpos) : 0;
        int i2 = (rypos > 0) ? random.nextInt(rypos) : 0;

        i1 -= (clockLayout.getWidth() - scaled_width) / 2;
        i2 -= (clockLayout.getHeight() - scaled_height) / 2;
        Log.i(TAG, i1 + "x" + i2);
        clockLayout.animate().setDuration(screen_transition_animation_duration).x(i1).y(i2);
    }

    private float to_range(float value, float min, float max) {
        if (value > max) return max;
        if (value < min) return min;
        return value;
    }

    private void dimScreen(int millis, float light_value, float add_brightness) {
        LIGHT_VALUE_DARK = settings.minIlluminance;
        float v;
        float brightness;
        if (mode != 0 && settings.autoBrightness && Utility.getLightSensor(mContext) != null) {
            float luminance_offset = LIGHT_VALUE_BRIGHT * add_brightness;
            if (light_value > LIGHT_VALUE_BRIGHT && add_brightness > 0.f) {
                luminance_offset = LIGHT_VALUE_DAYLIGHT * add_brightness;
            }
            v = (light_value + luminance_offset - LIGHT_VALUE_DARK) / (LIGHT_VALUE_BRIGHT - LIGHT_VALUE_DARK);
            v = 0.3f + 0.7f * v;

            brightness = (light_value + luminance_offset - LIGHT_VALUE_BRIGHT) / (LIGHT_VALUE_DAYLIGHT - LIGHT_VALUE_BRIGHT);
        } else {
            if (mode == 0) {
                v = 1.f + settings.nightModeBrightness;
                brightness = settings.nightModeBrightness;
            } else {
                v = 1.f + add_brightness;
                brightness = add_brightness;
            }
        }

        float minBrightness = Math.max(1.f + settings.nightModeBrightness, 0.05f);
        v = to_range(v, minBrightness, 1.f);
        int backgroundMode = settings.getBackgroundMode();
        if (
                backgroundMode == Settings.BACKGROUND_IMAGE
                        || backgroundMode == Settings.BACKGROUND_SLIDESHOW
        ) {
            v = to_range(v, 0.5f, 1.f);
        }

        brightness = getValidBrightnessValue(brightness);
        setBrightness(brightness);

        //if ( showcaseView == null && !AlarmHandlerService.alarmIsRunning()) {
        if (!AlarmHandlerService.alarmIsRunning()) {
            setAlpha(clockLayout, v, millis);
        }

        if (bottomPanelLayout.isClickable()) {
            setAlpha(bottomPanelLayout, v, millis);
            v = to_range(v, 0.6f, 1.f);
            setAlpha(menuIcon, v, millis);
        }

        if (batteryViewShallBeVisible()) {
            v = to_range(v, 0.6f, 1.f);
            setAlpha(batteryIconView, v, millis);
        } else {
            hideBatteryView(millis);
        }

        if (mode == 0 && !controlsVisible) {
            setAlpha(notificationStatusBar, 0.0f, millis);
        } else {
            // increase minimum alpha value for the notification bar
            v = to_range(v, 0.6f, 1.f);
            setAlpha(notificationStatusBar, v, millis);
        }

        if (light_value + 0.2f < settings.minIlluminance) {
            settings.setMinIlluminance(light_value + 0.2f);
        }
    }

    private float getValidBrightnessValue(float value) {
        float minBrightness = getMinAllowedBrightness();
        float maxBrightness = getMaxAllowedBrightness();
        return to_range(value, minBrightness, maxBrightness);
    }

    private float getMinAllowedBrightness() {
        // On some screens (as the Galaxy S2) a value of 0 means the screen is completely dark.
        // Therefore a minimum value must be set to preserve the visibility of the clock.
        float minBrightness = Math.max(settings.nightModeBrightness, 0.01f);
        if (settings.autoBrightness) {
            minBrightness = Math.min(minBrightness, 0.1f);
        }
        if (AlarmHandlerService.alarmIsRunning()) return 0.5f;
        return minBrightness;
    }

    private float getMaxAllowedBrightness() {
        float maxBrightness = settings.autoBrightness ? Math.min(settings.maxBrightness, 1.f) : 1.f;
        if (!Utility.isCharging(mContext)) {
            return Math.min(settings.maxBrightnessBattery, maxBrightness);
        }
        return maxBrightness;
    }

    private void setBrightness(float value) {
        Log.d(TAG, "setBrightness(float value)");
        Log.i(TAG, String.format("new brightness value %.2f", value));
        LayoutParams layout = window.getAttributes();
        layout.screenBrightness = value;
        layout.buttonBrightness = LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
        window.setAttributes(layout);
        fadeSoftButtons();
    }

    private void setScreenOrientation(int orientation) {
        ((AppCompatActivity) mContext).setRequestedOrientation(orientation);
    }

    private void fadeSoftButtons() {
        if (Build.VERSION.SDK_INT < 19) {
            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    }

    private void hideSystemUI() {
        Utility.hideSystemUI(window);
    }

    public int determineScreenMode(float light_value, boolean isNoisy) {

        Log.d(TAG, "Sound is noisy " + isNoisy);
        LIGHT_VALUE_DARK = settings.minIlluminance + 1.f;
        if (light_value <= LIGHT_VALUE_DARK && !isNoisy) {
            return 0;
        } else if (light_value < LIGHT_VALUE_BRIGHT / 2.f) { // night shift, desk light on
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
        if ((new_mode == 0) && (current_mode != 0)) {
            if (settings.muteRinger) AudioManage.setRingerModeSilent();
            setColor();
            if (settings.hideBackgroundImage) {
                background_images[background_image_active].setImageDrawable(bgblack);
                textViewExif.setVisibility(View.GONE);
            }
        } else if ((new_mode != 0) && (current_mode == 0)) {
            restoreRingerMode();
            setColor();
            if (settings.hideBackgroundImage) {
                background_images[background_image_active].setImageDrawable(bgshape);
                if (settings.background_exif && settings.getBackgroundMode() == Settings.BACKGROUND_SLIDESHOW) {
                    textViewExif.setVisibility(View.VISIBLE);
                }
            }
        }

        float dim_offset = settings.dim_offset;
        if ((new_mode == 1) && (current_mode == 0)) {
            dim_offset += 0.1f;
        }
        dimScreen(screen_alpha_animation_duration, light_value, dim_offset);

        if (soundmeter != null) {
            if (new_mode == 0 && !soundmeter.isRunning()) {
                soundmeter.startMeasurement(3000);
            } else if (new_mode == 1 && !soundmeter.isRunning()) {
                soundmeter.startMeasurement(30000);
            } else if (new_mode > 1) {
                soundmeter.stopMeasurement();
            }
        }
    }

    private void setAlpha(View v, float alpha, int millis) {
        if (v == null) return;

        float oldValue = v.getAlpha();
        if (alpha != oldValue) {
            v.animate().setDuration(millis).alpha(alpha);
        }
    }

    public void restoreRingerMode() {
        if (AudioManage == null) {
            return;
        }
        if (settings.muteRinger) AudioManage.restoreRingerMode();
    }

    private void toggleSidePanel() {
        float x = sidePanel.getX();
        if (x < 0.f) {
            showSidePanel();
        } else {
            hideSidePanel();
        }
    }

    private void showSidePanel() {
        updateRadioIconColor();
        sidePanel.animate().setDuration(250).x(0);
        handler.postDelayed(hideAlarmClock, 20000);
    }

    private void hideSidePanel() {
        int w = sidePanel.getWidth();
        sidePanel.animate().setDuration(250).x(-w);
    }

    public boolean sidePanelIsHidden() {
        float x = sidePanel.getX();
        return (x < 0.f);
    }

    public void initSidePanel() {
        if (sidePanel == null) return;
        sidePanel.setX(-1000f);
    }

    private void hideBatteryView(int millis) {
        if (!batteryViewShallBeVisible()) {
            setAlpha(batteryIconView, 0.f, millis);
        }
    }

    private boolean batteryViewShallBeVisible() {
        return (
                controlsVisible ||
                        (settings.persistentBatteryValueWhileCharging && batteryIconView.shallBeVisible())
        );
    }

    public void showAlarmClock() {
        removeCallbacks(hideAlarmClock);
        handler.postDelayed(hideAlarmClock, 20000);
        controlsVisible = true;
        setupAlarmClock();
        if (AlarmHandlerService.alarmIsRunning()) {
            blinkIfLocked();
        }
        dimScreen(0, last_ambient, settings.dim_offset);
    }

    public void onPowerConnected() {
        setupScreenAnimation();
        showAlarmClock();
    }

    public void onPowerDisconnected() {
        setupScreenAnimation();
        showAlarmClock();
    }

    private void blinkIfLocked() {
        handler.removeCallbacks(blink);
        if (locked && AlarmHandlerService.alarmIsRunning()) {
            handler.postDelayed(blink, 1000);
        } else {
            if (AlarmHandlerService.alarmIsRunning()) {
                alarmClock.activateAlarmUI();
            }
            blinkStateOn = false;
            setAlpha(menuIcon, 1.f, 0);
        }
    }

    private Configuration getConfiguration() {
        return mContext.getResources().getConfiguration();
    }

    private void applyScaleFactor(float factor) {
        int width = clockLayoutContainer.getWidth();
        int height = clockLayoutContainer.getHeight();
        factor *= clockLayout.getAbsScaleFactor();
        int new_width = (int) (clockLayout.getWidth() * factor);
        int new_height = (int) (clockLayout.getHeight() * factor);
        if (factor > 0.5f && new_width < width && new_height < height) {
            clockLayout.setScaleFactor(factor);
            keepClockWithinContainer(new_width, new_height, width, height);
        }
    }

    /**
     * make sure the clock does not run over clockLayoutContainer edges after scaling
     */
    private void keepClockWithinContainer(int newClockWidth, int newClockHeight, int containerWidth, int containerHeight) {

        final float distanceX = containerWidth - newClockWidth - Math.abs(clockLayout.getTranslationX()) * 2f;
        final float distanceY = containerHeight - newClockHeight - Math.abs(clockLayout.getTranslationY()) * 2f;


        if (distanceX < 0 || distanceY < 0) {

            // stop animation, otherwise it gets out of screen while animation is in progress
            clockLayout.animate().cancel();

            if (distanceX < 0) {
                // move clock to left or right screen edge
                final float correctionX = (newClockWidth - containerWidth) * 0.5f * (clockLayout.getTranslationX() < 0 ? 1f : -1f);
                clockLayout.setTranslationX(correctionX);
            }

            if (distanceY < 0) {
                // move clock to top or bottom screen edge
                final float correctionY = (newClockHeight - containerHeight) * 0.5f * (clockLayout.getTranslationY() < 0 ? 1f : -1f);
                clockLayout.setTranslationY(correctionY);
            }
        }
    }

    private float getScaleFactor(Configuration config) {
        float s = settings.getScaleClock(config.orientation);
        float max = getMaxScaleFactor();
        if (s < 0.f) {
            s = getProposedScaleFactor(max);
            settings.setScaleClock(0.8f * max, config.orientation);
        }
        Log.d(TAG, String.format("getScaleFactor > %f %f", s, max));

        s = Math.min(s, max);
        if (s > 0.5f && !Float.isNaN(s) && !Float.isInfinite(s)) {
            return s;
        }
        return 1.f;
    }

    private float getMaxScaleFactor() {
        Log.d(TAG, String.format("getMaxScaleFactor > %d %d",
                clockLayoutContainer.getWidth(),
                clockLayout.getWidth()));
        float factor_x = (float) clockLayoutContainer.getWidth() / clockLayout.getWidth();
        float factor_y = (float) clockLayoutContainer.getHeight() / clockLayout.getHeight();
        return Math.min(factor_x, factor_y);
    }

    private float getProposedScaleFactor(float maxScaleFactor) {
        return 0.8f * maxScaleFactor;
    }

    public void setLocked(boolean on) {
        this.locked = on;
        lockUI(on);
    }

    private void lockUI(boolean on) {
        bottomPanelLayout.setLocked(on);
        showAlarmClock();
        int resId = on ? R.drawable.ic_lock : R.drawable.ic_menu;
        menuIcon.setImageDrawable(ContextCompat.getDrawable(mContext, resId));
        if (AlarmHandlerService.alarmIsRunning()) {
            blinkIfLocked();
        }
    }

    public boolean onTouch(View view, MotionEvent e) {
        if (locked) {
            handler.removeCallbacks(hideAlarmClock);
            setAlpha(menuIcon, 1.f, 250);
            setAlpha(notificationStatusBar, 1.f, 250);
            setAlpha(batteryIconView, 1.f, 250);
            setAlpha(bottomPanelLayout, 1.f, 250);
            controlsVisible = true;
            handler.postDelayed(hideAlarmClock, 5000);

            // allow to snooze alarms in locked mode
            if (AlarmHandlerService.alarmIsRunning()) {
                alarmClock.snooze();
            }

            return true;
        }
        boolean event_consumed = mGestureDetector.onTouchEvent(e);
        if (mScaleDetector != null) {
            mScaleDetector.onTouchEvent(e);
        }
        return true;
    }

    private void checkForReviewRequest() {
        // ask only once
        if (settings.lastReviewRequestTime != 0L) return;

        long firstInstallTime = Utility.getFirstInstallTime(mContext);
        Log.i(TAG, "First install time: " + firstInstallTime);
        Calendar install_time = Calendar.getInstance();
        install_time.setTimeInMillis(firstInstallTime);

        Calendar twenty_days_ago = Calendar.getInstance();
        int hour = twenty_days_ago.get(Calendar.HOUR_OF_DAY);

        twenty_days_ago.add(Calendar.DATE, -20);

        if (install_time.before(twenty_days_ago) && hour >= 18) {
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
            NotificationCompat.Builder note =
                    Utility.buildNotification(mContext, Config.NOTIFICATION_CHANNEL_ID_DEVMSG)
                            .setContentTitle(mContext.getString(R.string.app_name))
                            .setContentText(mContext.getString(R.string.review_request))
                            .setSmallIcon(R.drawable.ic_clock)
                            .setContentIntent(pIntent)
                            .setAutoCancel(true)
                            .setDefaults(0);

            Notification n = note.build();

            NotificationManager notificationManager =
                    (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.notify(0, n);

        }
        /* else handle your error case: the device has no way to handle market urls */

    }

    private NightDreamBroadcastReceiver registerBroadcastReceiver() {
        NightDreamBroadcastReceiver receiver = new NightDreamBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(OpenWeatherMapApi.ACTION_WEATHER_DATA_UPDATED);
        filter.addAction(Config.ACTION_RADIO_STREAM_STARTED);
        filter.addAction(Config.ACTION_RADIO_STREAM_STOPPED);
        filter.addAction(Config.ACTION_RADIO_STREAM_READY_FOR_PLAYBACK);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(receiver, filter);
        return receiver;
    }

    private void unregister(BroadcastReceiver receiver) {
        try {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {
        }

    }

    @Subscribe
    public void onEvent(OnNewLightSensorValue event) {
        last_ambient = event.value;
        dimScreen(screen_alpha_animation_duration, last_ambient, settings.dim_offset);
    }

    @Subscribe
    public void onEvent(OnLightSensorValueTimeout event) {
        last_ambient = (event.value >= 0.f) ? event.value : settings.minIlluminance;
        dimScreen(screen_alpha_animation_duration, last_ambient, settings.dim_offset);
    }

    private void setupShowcase() {
/*
        if (showcaseView != null) {
            return;
        }
        long firstInstallTime = Utility.getFirstInstallTime(mContext);
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

        setupShowcaseForScreenLock();
 */
    }

    private void setupShowcaseView() {
/*
        if (showcaseView == null) return;
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
        hideSystemUI();
 */
    }

    private void setupShowcaseForScreenLock() {
/*
        if (showcaseView != null) {
            return;
        }

        if ( this.locked ) {
            showcaseView = new ShowcaseView.Builder((Activity) mContext)
                .setTarget(new ViewTarget(menuIcon))
                .hideOnTouchOutside()
                .setContentTitle(mContext.getString(R.string.showcase_title_screen_lock))
                .setContentText(mContext.getString(R.string.showcase_text_screen_lock))
                .setShowcaseEventListener(showcaseViewEventListener)
                .singleShot(SHOWCASE_ID_SCREEN_LOCK)
                .build();
            showcaseView.hideButton();
            showShowcase();
        }

 */
    }

    private void showShowcase() {
/*
        showcaseView.show();
        if ( !showcaseView.isShowing()) {
            showcaseView = null;
        }
 */
    }

    private final class preloadImageFromPath extends AsyncTask<File, Integer, Bitmap> {

        @Override
        protected Bitmap doInBackground(File... params) {
            if (files == null || files.isEmpty() || params[0] == null) {
                return null;
            } else {
                return rescaleBackgroundImage(loadImageFromPath(params[0]));
            }
        }

        @Override
        protected void onProgressUpdate(Integer... params) {
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            preloadBackgroundImage = result;
        }
    }

    private final class writeBackgroundImageToCache extends AsyncTask<Bitmap, Integer, Bitmap> {

        @Override
        protected Bitmap doInBackground(Bitmap... params) {
            if (params[0] == null) {
                return null;
            } else {
                Log.d(TAG, "writing image to cache");
                File cacheFile = new File(mContext.getCacheDir(), Config.backgroundImageCacheFilename);
                Bitmap bgimage = params[0];
                try {
                    FileOutputStream out = new FileOutputStream(cacheFile);
                    bgimage.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return bgimage;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... params) {
        }

        @Override
        protected void onPostExecute(Bitmap result) {

        }
    }

    class NightDreamBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (OpenWeatherMapApi.ACTION_WEATHER_DATA_UPDATED.equals(action)) {
                Log.v(TAG, "Weather data updated");
                settings.weatherEntry = settings.getWeatherEntry();
                clockLayout.update(settings.weatherEntry);
                ClockWidgetProvider.updateAllWidgets(context);
            } else if (Config.ACTION_RADIO_STREAM_STARTED.equals(action)) {
                showAlarmClock();
            } else if (Config.ACTION_RADIO_STREAM_STOPPED.equals(action)) {
                setupAlarmClock();
            }
        }
    }
}
