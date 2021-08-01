package com.firebirdberlin.nightdream.ui;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
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
import android.webkit.MimeTypeMap;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.Graphics;
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
import com.firebirdberlin.nightdream.ui.background.ImageViewExtended;
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
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NightDreamUI {
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private static String TAG = "NightDreamUI";
    final private Handler handler = new Handler();
    final private Drawable colorTransparent = new ColorDrawable(Color.TRANSPARENT);
    final private Drawable colorBlack = new ColorDrawable(Color.BLACK);
    private final UserInteractionObserver bottomPanelUserInteractionObserver = new UserInteractionObserver() {
        public void notifyAction() {
            resetAlarmClockHideDelay();
        }
    };
    private int screen_alpha_animation_duration = 3000;
    private int screen_transition_animation_duration = 10000;
    private int mode = 2;
    private boolean controlsVisible = false;
    private Context mContext;
    private FrameLayout mainFrame;
    private Drawable bgshape = colorBlack;
    private AlarmClock alarmClock;
    private ConstraintLayout parentLayout;
    private ExifView exifView;
    private ImageViewExtended[] background_images = new ImageViewExtended[2];
    private int background_image_active = 0;
    private ArrayList<File> files;
    private Bitmap preloadBackgroundImage;
    private File preloadBackgroundImageFile;
    private ImageView menuIcon;
    private ImageView nightModeIcon;
    private ImageView radioIcon;
    private LightSensorEventListener lightSensorEventListener = null;
    private ConstraintLayout exifLayoutContainer;
    private ClockLayoutContainer clockLayoutContainer;
    private ClockLayout clockLayout;
    private FlexboxLayout notificationStatusBar;
    private FlexboxLayout sidePanel;
    private final Runnable setupSidePanel = new Runnable() {
        @Override
        public void run() {
            if (sidePanel.getX() < 0) {
                initSidePanel();
            }

        }
    };
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
            clockLayoutContainer.applyScaleFactor(s);
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
    private final Runnable fadeClock = new Runnable() {
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
                postFadeAnimation();
            }
        }
    };
    private float clockLayout_xDelta;
    private float clockLayout_yDelta;
    private int vibrantColor = 0;
    private int vibrantColorDark = 0;
    private long lastAnimationTime = 0L;
    private SoundMeter soundmeter;
    private ProgressBar brightnessProgress;
    private final Runnable hideBrightnessView = new Runnable() {
        @Override
        public void run() {
            brightnessProgress.setVisibility(View.INVISIBLE);
        }
    };
    private final Runnable hideBrightnessLevel = new Runnable() {
        @Override
        public void run() {
            setAlpha(brightnessProgress, 0.f, 2000);
            postDelayed(hideBrightnessView, 2010);
        }
    };
    private BatteryIconView batteryIconView;
    public Runnable initSlideshowBackground = new Runnable() {
        @Override
        public void run() {
            if (settings.getBackgroundMode() == Settings.BACKGROUND_SLIDESHOW) {
                if (preloadBackgroundImage == null) {
                    parentLayout.postDelayed(initSlideshowBackground, 500);
                } else {
                    setupSlideshow();
                }
            }
        }
    };
    private final Runnable backgroundChange = () -> {
        setupBackgroundImage();
        postBackgroundImageChange();
    };
    private final Runnable hideAlarmClock = new Runnable() {
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
    private Window window;
    // move the clock randomly around
    private final Runnable moveAround = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "moveAround.run()");
            removeCallbacks(hideBrightnessLevel);
            hideSystemUI();
            setupScreenAnimation();

            hideBatteryView(2000);
            updateClockPosition();

            updateWeatherData();

            postDelayed(this, Utility.millisToTimeTick(20000));
        }
    };
    private mAudioManager AudioManage;
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;
    private NightDreamBroadcastReceiver broadcastReceiver = null;
    private boolean locked = false;
    private boolean zoomFinished = false;
    private final Runnable zoomIn = new Runnable() {
        @Override
        public void run() {
            removeCallbacks(zoomIn);
            clockLayout.setVisibility(View.INVISIBLE);
            Configuration config = getConfiguration();
            clockLayout.updateLayout(clockLayoutContainer.getWidth(), config);

            float s = getScaleFactor(config);
            // set the right scaling and position
            clockLayout.setScaleFactor(s, false);
            setClockPosition(config);
            // prepare zoom in
            if (!zoomFinished) {
                clockLayout.setScaleFactor(0.1f, false);
            }
            // finally zoom in
            clockLayout.setVisibility(View.VISIBLE);
            clockLayout.setScaleFactor(s, true);

            Utility.turnScreenOn(mContext);
            Utility.hideSystemUI(mContext);
            zoomFinished = true;
        }
    };
    private float last_ambient = 4.0f;
    private float LIGHT_VALUE_DARK = 4.2f;
    private float LIGHT_VALUE_BRIGHT = 40.0f;
    private float LIGHT_VALUE_DAYLIGHT = 300.0f;
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
            clockLayout.setVisibility(View.INVISIBLE);
            setupClockLayout();
            setColor();
            updateWeatherData();
            controlsVisible = true;

            brightnessProgress.setVisibility(View.INVISIBLE);

            showAlarmClock();

            clockLayout.postDelayed(zoomIn, 500);

        }
    };
    private boolean shallMoveClock = false;
    View.OnTouchListener onTouchListenerClockLayout = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            if (locked) {
                return false;
            }
            switch (e.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    if (shallMoveClock) {
                        setClockPosition(
                                e.getRawX() - clockLayout_xDelta,
                                e.getRawY() - clockLayout_yDelta
                        );
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    disableMoveClock();
                    break;
            }
            return false;
        }
    };
    private GestureDetector.SimpleOnGestureListener mSimpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        int[] rect = new int[2];
        int[] size = new int[2];

        @Override
        public void onShowPress(MotionEvent e) {
            super.onShowPress(e);
            Log.i(TAG, "onShowPress");
            if (isInsideClockLayout(e)) {
                clockLayout_xDelta = e.getRawX() - clockLayout.getX();
                clockLayout_yDelta = e.getRawY() - clockLayout.getY();
                enableMoveClock();
            }
        }

        boolean isInsideClockLayout(MotionEvent e) {
            clockLayout.getLocationOnScreen(rect);
            clockLayout.getScaledSize(size);
            int x = (int) e.getRawX();
            int y = (int) e.getRawY();
            return (
                    x > rect[0] && x < rect[0] + size[0] && y > rect[1] && y < rect[1] + size[1]
            );
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) return false;
            Log.i(TAG, "onFling");
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

    @SuppressLint("ClickableViewAccessibility")
    public NightDreamUI(final Context context, Window window) {
        Log.d(TAG, "NightDreamUI()");
        mContext = context;

        mGestureDetector = new GestureDetector(mContext, mSimpleOnGestureListener);
        mScaleDetector = new ScaleGestureDetector(mContext, mOnScaleGestureListener);

        this.window = window;
        View rootView = window.getDecorView().findViewById(android.R.id.content);
        batteryIconView = rootView.findViewById(R.id.batteryIconView);
        bottomPanelLayout = rootView.findViewById(R.id.bottomPanel);
        brightnessProgress = rootView.findViewById(R.id.brightness_progress);
        clockLayout = rootView.findViewById(R.id.clockLayout);
        clockLayoutContainer = rootView.findViewById(R.id.clockLayoutContainer);
        exifLayoutContainer = rootView.findViewById(R.id.containerExifView);

        mainFrame = rootView.findViewById(R.id.main_frame);
        menuIcon = rootView.findViewById(R.id.burger_icon);
        nightModeIcon = rootView.findViewById(R.id.night_mode_icon);
        notificationStatusBar = rootView.findViewById(R.id.notificationstatusbar);
        parentLayout = rootView.findViewById(R.id.background_group);
        radioIcon = rootView.findViewById(R.id.radio_icon);
        sidePanel = rootView.findViewById(R.id.side_panel);

        background_images[0] = rootView.findViewById(R.id.background_view);
        background_images[1] = rootView.findViewById(R.id.background_view2);
        background_image_active = 1;

        bottomPanelLayout.setUserInteractionObserver(bottomPanelUserInteractionObserver);
        alarmClock = bottomPanelLayout.getAlarmClock();

        sidePanel.post(setupSidePanel);

        exifView = new ExifView(mContext);
        exifLayoutContainer.addView(exifView.getView());

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

        menuIcon.setScaleX(.8f);
        menuIcon.setScaleY(.8f);

        settings = new Settings(context);
        AudioManage = new mAudioManager(context);

        checkForReviewRequest();
        clockLayoutContainer.setClockLayout(clockLayout);
    }

    private void postBackgroundImageChange() {
        long delay = Utility.millisUntil(
                15000 * settings.backgroundImageDuration, 10000
        ) - 750;
        postDelayed(backgroundChange, delay);
    }

    private void postFadeAnimation() {
        long delay = Utility.millisUntil(30000, 10000) - 2000;
        postDelayed(fadeClock, delay);
    }

    private void enableMoveClock() {
        shallMoveClock = true;
        Drawable bg = ContextCompat.getDrawable(mContext, R.drawable.border);
        clockLayout.setupBackground(bg);
    }

    private void disableMoveClock() {
        Configuration config = getConfiguration();
        settings.setPositionClock(clockLayout.getX(), clockLayout.getY(), config.orientation);
        shallMoveClock = false;
        clockLayout.setupBackground(null);

    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean on) {
        this.locked = on;
        lockUI(on);
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
        postDelayed(moveAround, Utility.millisToTimeTick(20000));
    }

    public void onResume() {
        Log.d(TAG, "onResume()");
        hideSystemUI();

        settings.reload();
        notificationStatusBar.setClickable(Settings.useNotificationStatusBar(mContext));

        vibrantColor = 0;
        vibrantColorDark = 0;
        mainFrame.setBackgroundColor(0);
        this.locked = settings.isUIlocked;

        removeCallbacks(backgroundChange);
        background_images[0].clearAnimation();
        background_images[1].clearAnimation();
        lastAnimationTime = 0L;
        setScreenOrientation(settings.screenOrientation);

        clockLayoutContainer.post(initClockLayout);
        postFadeAnimation();

        initSidePanel();
        initBackground();
        initBottomPannelLayout();
        setupScreenAnimation();
        lockUI(this.locked);

        Utility.registerEventBus(this);
        broadcastReceiver = registerBroadcastReceiver();
        initLightSensor();
        if (settings.useAmbientNoiseDetection()) {
            soundmeter = new SoundMeter(mContext);
        } else {
            soundmeter = null;
        }
    }

    private void initBottomPannelLayout(){
        bottomPanelLayout.setAlarmUseLongPress(settings.stopAlarmOnLongPress);
        bottomPanelLayout.setAlarmUseSingleTap(settings.stopAlarmOnTap);
        bottomPanelLayout.setShowAlarmsPersistently(settings.showAlarmsPersistently);
        bottomPanelLayout.setUseAlarmSwipeGesture(settings.useAlarmSwipeGesture);
        bottomPanelLayout.setup();
    }

    private void initBackground() {
        if (mode == 0) return;

        preloadBackgroundImage = null;
        preloadBackgroundImageFile = null;
        exifLayoutContainer.setVisibility(View.GONE);

        if (!Utility.isLowRamDevice(mContext)) {
            switch (settings.getBackgroundMode()) {

                case Settings.BACKGROUND_TRANSPARENT: {
                    Log.d(TAG, "BACKGROUND_TRANSPARENT");
                    bgshape = colorTransparent;
                    background_images[0].setImageDrawable(bgshape);
                    background_images[1].setImageDrawable(bgshape);
                    break;
                }

                case Settings.BACKGROUND_GRADIENT: {
                    Log.d(TAG, "BACKGROUND_GRADIENT");
                    bgshape = ContextCompat.getDrawable(mContext, R.drawable.background_gradient);
                    background_images[background_image_active].setImageDrawable(bgshape);
                    break;
                }

                case Settings.BACKGROUND_IMAGE: {
                    Log.d(TAG, "BACKGROUND_IMAGE");
                    setImageScale();

                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Handler handler = new Handler(Looper.getMainLooper());

                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            //doinbackground if cachefile not exists first write to cachefile

                            if (!background_images[background_image_active].existCacheFile()) {
                                background_images[background_image_active].setImageDrawable(colorBlack);
                                background_images[background_image_active].bitmapUriToCache(Uri.parse(settings.backgroundImageURI));
                            }

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    //like onPostExecute()
                                    background_images[background_image_active].setImage(Uri.parse(settings.backgroundImageURI));
                                    setDominantColorFromBitmap(background_images[background_image_active].getBitmap());
                                    if (settings.slideshowStyle == Settings.SLIDESHOW_STYLE_CENTER) {
                                        background_images[(background_image_active + 1) % 2].setImageBitmap(Graphics.blur(mContext, background_images[background_image_active].getBitmap()));
                                        background_images[(background_image_active + 1) % 2].setScaleType(ImageView.ScaleType.CENTER_CROP);
                                    } else {
                                        background_images[(background_image_active + 1) % 2].setImageDrawable(colorBlack);
                                    }
                                }
                            });
                        }
                    });

                    break;
                }

                case Settings.BACKGROUND_SLIDESHOW:
                    Log.d(TAG, "BACKGROUND_SLIDESHOW");
                    loadBackgroundImageFiles();
                    if (files != null && files.size() > 0) {
                        preloadBackgroundImageFile = files.get(new Random().nextInt(files.size()));
                        AsyncTask<File, Integer, Bitmap> runningTask = new preloadImageFromPath();
                        runningTask.execute(preloadBackgroundImageFile);
                        parentLayout.postDelayed(initSlideshowBackground, 500);
                        postBackgroundImageChange();
                    } else {
                        preloadBackgroundImage = null;
                        preloadBackgroundImageFile = null;
                        setupSlideshow();
                    }
                    break;

                case Settings.BACKGROUND_BLACK:
                default:
                    Log.d(TAG, "BACKGROUND_default");
                    bgshape = colorBlack;
                    background_images[background_image_active].setImageDrawable(bgshape);
                    break;
            }
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
        Log.i(TAG, "updateWeatherData() 2");

        DownloadWeatherService.start(mContext);

        // handle outdated weather data
        WeatherEntry entry = settings.weatherEntry;
        if (!entry.isValid()) {
            clockLayout.clearWeather();
        }
    }

    private void updatePollenExposure(WeatherEntry entry) {
        if (settings.showWeather && settings.showPollen) {
            ConstraintLayout pollenContainer = clockLayout.findViewById(R.id.pollen_container);

            if (pollenContainer != null) {
                Log.d(TAG, "pollenCount " + entry.cityName);
                new PollenExposureUpdate(mContext, pollenContainer).execute(entry);
            } else {
                Log.d(TAG, "pollenContainer not found");
            }
        }
    }

    public void setupClockLayout() {
        int layoutId = settings.getClockLayoutID(false);
        clockLayout.setLayout(layoutId);
        clockLayout.setBackgroundTransparency(settings.clockBackgroundTransparency);
        clockLayout.setTypeface(settings.typeface);
        clockLayout.setDateFormat(settings.dateFormat);
        String timeFormat = settings.getTimeFormat(layoutId);
        clockLayout.setTimeFormat(timeFormat, settings.is24HourFormat());
        clockLayout.setTemperature(
                settings.showTemperature, settings.showApparentTemperature, settings.temperatureUnit
        );
        clockLayout.setWindSpeed(settings.showWindSpeed, settings.speedUnit);
        clockLayout.setWeatherLocation(false);

        clockLayout.setShowDivider(settings.getShowDivider(layoutId));
        clockLayout.setMirrorText(settings.clockLayoutMirrorText);
        clockLayout.showDate(settings.showDate);
        clockLayout.showWeather(settings.showWeather);
        clockLayout.setWeatherIconSizeFactor(settings.getWeatherIconSizeFactor(layoutId));
        clockLayout.showPollenExposure(settings.showWeather && settings.showPollen);
        Configuration config = getConfiguration();
        clockLayout.updateLayout(clockLayoutContainer.getWidth(), config);

        clockLayout.update(settings.weatherEntry);
        updatePollenExposure(settings.weatherEntry);
        setClockPosition(config);
    }

    private void setColor() {
        setNightModeIcon();

        int accentColor = getAccentColor();
        int textColor = getSecondaryColor();

        batteryIconView.setColor(textColor);
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
        clockLayout.setPrimaryColor(accentColor, glowRadius, accentColor, textureId, true);
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

    private Drawable loadBackgroundSlideshowImage() {
        Log.d(TAG, "loadBackgroundSlideshowImage");
        if (!settings.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            return new ColorDrawable(Color.BLACK);
        }

        if (files == null || files.isEmpty()) return new ColorDrawable(Color.BLACK);

        File file = files.get(new Random().nextInt(files.size()));
        preloadBackgroundImageFile = file;
        Bitmap bitmap = loadImageFromPath(file);
        bitmap = rescaleBackgroundImage(bitmap);
        setDominantColorFromBitmap(bitmap);
        if (bitmap != null) {
            return new BitmapDrawable(mContext.getResources(), imageFilter(bitmap));
        }
        return new ColorDrawable(Color.BLACK);
    }

    private void setImageScale() {
        switch (settings.slideshowStyle) {
            case Settings.SLIDESHOW_STYLE_CENTER:
                background_images[background_image_active].setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                break;

            case Settings.SLIDESHOW_STYLE_CROPPED:
            default:
                background_images[background_image_active].setScaleType(ImageView.ScaleType.CENTER_CROP);
                break;
        }
    }

    private void setupSlideshow() {
        Log.d(TAG, "setupSlideshow");

        long now = System.currentTimeMillis();
        if (now - lastAnimationTime < 10000) {
            return;
        }

        lastAnimationTime = now;

        if (preloadBackgroundImage == null) {
            bgshape = loadBackgroundSlideshowImage();
        } else {
            bgshape = new BitmapDrawable(mContext.getResources(), preloadBackgroundImage);
            // TODO determine the dominant color in an AsyncTask
            setDominantColorFromBitmap(preloadBackgroundImage);
        }
        if (settings.background_exif) {
            getExifInformation(preloadBackgroundImageFile);
        }

        background_image_active = (background_image_active + 1) % 2;

        setImageScale();

        background_images[background_image_active].setImageDrawable(bgshape);

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
                Animation alpha = AnimationUtils.loadAnimation(
                        mContext.getApplicationContext(), R.anim.fade_in
                );
                animationSet.addAnimation(alpha);
            }

            if (settings.background_movein) {
                Animation translate = null;

                switch (settings.background_movein_style) {
                    case 1:
                        translate = AnimationUtils.loadAnimation(
                                mContext.getApplicationContext(), R.anim.move_in_top
                        );
                        break;
                    case 2:
                        translate = AnimationUtils.loadAnimation(
                                mContext.getApplicationContext(), R.anim.move_in_right
                        );
                        break;
                    case 3:
                        translate = AnimationUtils.loadAnimation(
                                mContext.getApplicationContext(), R.anim.move_in_bottom
                        );
                        break;
                    case 4:
                        translate = AnimationUtils.loadAnimation(
                                mContext.getApplicationContext(), R.anim.move_in_left
                        );
                        break;
                }
                if (translate != null) {
                    animationSet.addAnimation(translate);
                }
            }

            if (settings.background_zoomin) {
                Animation animZoomIn = AnimationUtils.loadAnimation(
                        mContext.getApplicationContext(), R.anim.zoom_in
                );
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
                    exifLayoutContainer.bringToFront();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        }

        background_images[background_image_active].setScaleX(1);
        background_images[background_image_active].setScaleY(1);
        background_images[background_image_active].setTranslationX(0);
        background_images[background_image_active].setTranslationY(0);
        background_images[background_image_active].bringToFront();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            background_images[(background_image_active + 1) % 2].setZ(4);
            background_images[background_image_active].setZ(5);
        }
        background_images[background_image_active].startAnimation(animationSet);

        parentLayout.bringChildToFront(background_images[background_image_active]);
        parentLayout.bringChildToFront(exifLayoutContainer);
        parentLayout.requestLayout();
        parentLayout.invalidate();

        if (files != null && settings.getBackgroundMode() == Settings.BACKGROUND_SLIDESHOW && files.size() > 0) {
            preloadBackgroundImageFile = files.get(new Random().nextInt(files.size()));
            AsyncTask<File, Integer, Bitmap> runningTask = new preloadImageFromPath();
            runningTask.execute(preloadBackgroundImageFile);
        }
    }

    private void loadBackgroundImageFiles() {
        if (!settings.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            return;
        }
        Log.d(TAG, "loadBackgroundImageFiles()");
        File path = settings.getBackgroundImageDir();
        files = Utility.listFiles(path, ".png");
        files.addAll(Utility.listFiles(path, ".jpg"));
    }

    private Bitmap loadImageFromPath(File file) {
        Log.d(TAG, "load image from path");
        String path = file.getAbsolutePath();
        Point display = Utility.getDisplaySize(mContext);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = Graphics.calculateInSampleSize(options, display.x, display.y);
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
        PollenExposureUpdate.cancelUpdate();
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
        Log.d(TAG, "onConfigurationChanged()");
        clockLayout.animate().cancel();
        removeCallbacks(moveAround);
        removeCallbacks(backgroundChange);

        Runnable fixConfig = new Runnable() {
            public void run() {
                float s = getScaleFactor(newConfig);
                clockLayout.setScaleFactor(s);
                Log.i(TAG, "fix = " + clockLayout.getHeight() + " " + s);
                setClockPosition(newConfig);

                postDelayed(moveAround, Utility.millisToTimeTick(20000));
                if (settings.getBackgroundMode() == Settings.BACKGROUND_SLIDESHOW) {
                    postBackgroundImageChange();
                }
                sidePanel.post(setupSidePanel);
            }
        };

        clockLayout.postDelayed(fixConfig, 200);
    }

    private void postDelayed(Runnable runnable, long delayMillis) {
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, delayMillis);
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

    public void setClockPosition(final Configuration config) {
        Point p = settings.getClockPosition(config.orientation);
        if (p != null) {
            setClockPosition(p.x, p.y);
        } else {
            centerClockLayout();
        }
    }

    public void setClockPosition(float x, float y) {
        if (clockLayout.getWidth() > 0 && clockLayout.getHeight() > 0) {

            float scaled_width = clockLayout.getScaledWidth();
            float scaled_height = clockLayout.getScaledHeight();

            float rxpos = clockLayoutContainer.getWidth() - scaled_width;
            float rypos = clockLayoutContainer.getHeight() - scaled_height;

            if (x + (clockLayout.getWidth() - scaled_width) / 2 > rxpos) {
                x = rxpos - ((clockLayout.getWidth() - scaled_width) / 2);
            }

            if (y + (clockLayout.getHeight() - scaled_height) / 2 > rypos) {
                y = rypos - ((clockLayout.getHeight() - scaled_height) / 2);
            }

            if (x + (clockLayout.getWidth() - scaled_width) / 2 < 0) {
                x = -((clockLayout.getWidth() - scaled_width) / 2);
            }

            if (y + (clockLayout.getHeight() - scaled_height) / 2 < 0) {
                y = -((clockLayout.getHeight() - scaled_height) / 2);
            }
            clockLayout.setX(x);
            clockLayout.setY(y);
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
        int w = clockLayoutContainer.getWidth();
        int h = clockLayoutContainer.getHeight();

        float scaledWidth = clockLayout.getScaledWidth();
        float scaledHeight = clockLayout.getScaledHeight();

        int rxpos = (int) (w - scaledWidth);
        int rypos = (int) (h - scaledHeight);

        // determine a random position
        Random random = new Random();
        int i1 = (rxpos > 0) ? random.nextInt(rxpos) : 0;
        int i2 = (rypos > 0) ? random.nextInt(rypos) : 0;

        i1 -= (clockLayout.getWidth() - scaledWidth) / 2;
        i2 -= (clockLayout.getHeight() - scaledHeight) / 2;
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
                background_images[background_image_active].setImageDrawable(colorBlack);
                exifLayoutContainer.setVisibility(View.GONE);
            }
        } else if ((new_mode != 0) && (current_mode == 0)) {
            restoreRingerMode();
            setColor();
            if (settings.hideBackgroundImage) {
                background_images[background_image_active].setImageDrawable(bgshape);
                if (settings.background_exif && settings.getBackgroundMode() == Settings.BACKGROUND_SLIDESHOW) {
                    exifLayoutContainer.setVisibility(View.VISIBLE);
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
        if (shallMoveClock) {
            onTouchListenerClockLayout.onTouch(view, e);
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

    private Bitmap imageFilter(Bitmap bitmap) {
        if ((settings.background_filter == 1) || (settings.getBackgroundMode() != Settings.BACKGROUND_SLIDESHOW)) {
            return bitmap;
        }

        switch (settings.background_filter) {
            case 2:
                return Graphics.desaturate(bitmap);
            case 3:
                return Graphics.sepia(bitmap);
            case 4:
                return Graphics.invert(bitmap);
            case 5:
                return Graphics.contrast(bitmap);
            case 6:
                return Graphics.sketch(bitmap);
            case 7:
                return Graphics.blur(mContext, bitmap);
        }

        return bitmap.copy(Bitmap.Config.ARGB_8888, true);
    }

    private void getExifInformation(File file) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> { //background thread
            Boolean success = exifView.getExifView(mContext, file, getSecondaryColor());
            handler.post(() -> { //like onPostExecute()
                if (success) {
                    exifLayoutContainer.setVisibility(View.VISIBLE);
                } else {
                    exifLayoutContainer.setVisibility(View.GONE);
                }
            });
        });
    }

    private final class preloadImageFromPath extends AsyncTask<File, Integer, Bitmap> {

        @Override
        protected Bitmap doInBackground(File... params) {
            if (files == null || files.isEmpty() || params[0] == null) {
                return null;
            } else {
                return imageFilter(rescaleBackgroundImage(loadImageFromPath(params[0])));
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

    class NightDreamBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (OpenWeatherMapApi.ACTION_WEATHER_DATA_UPDATED.equals(action)) {
                Log.v(TAG, "Weather data updated");
                settings.weatherEntry = settings.getWeatherEntry();
                clockLayout.update(settings.weatherEntry);
                updatePollenExposure(settings.weatherEntry);
                ClockWidgetProvider.updateAllWidgets(context);
            } else if (Config.ACTION_RADIO_STREAM_STARTED.equals(action)) {
                showAlarmClock();
            } else if (Config.ACTION_RADIO_STREAM_STOPPED.equals(action)) {
                setupAlarmClock();
            }
        }
    }
}
