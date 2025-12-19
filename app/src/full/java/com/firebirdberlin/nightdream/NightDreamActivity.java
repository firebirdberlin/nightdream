/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.firebirdberlin.nightdream;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.multidex.MultiDex;

import com.firebirdberlin.AvmAhaApi.AvmAhaRequestTask;
import com.firebirdberlin.AvmAhaApi.models.AvmAhaDevice;
import com.firebirdberlin.nightdream.events.OnLightSensorValueTimeout;
import com.firebirdberlin.nightdream.events.OnNewAmbientNoiseValue;
import com.firebirdberlin.nightdream.events.OnNewLightSensorValue;
import com.firebirdberlin.nightdream.models.BatteryValue;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.models.TimeRange;
import com.firebirdberlin.nightdream.receivers.LocationUpdateReceiver;
import com.firebirdberlin.nightdream.receivers.PowerConnectionReceiver;
import com.firebirdberlin.nightdream.receivers.ScheduledAutoStartReceiver;
import com.firebirdberlin.nightdream.repositories.BatteryStats;
import com.firebirdberlin.nightdream.repositories.FlashlightProvider;
import com.firebirdberlin.nightdream.services.AlarmHandlerService;
import com.firebirdberlin.nightdream.services.AlarmService;
import com.firebirdberlin.nightdream.services.CheckChargingStateJob;
import com.firebirdberlin.nightdream.services.DownloadWeatherModel;
import com.firebirdberlin.nightdream.services.DownloadWeatherService;
import com.firebirdberlin.nightdream.services.RadioStreamService;
import com.firebirdberlin.nightdream.services.ScreenWatcherService;
import com.firebirdberlin.nightdream.services.SqliteIntentService;
import com.firebirdberlin.nightdream.ui.BottomPanelLayout;
import com.firebirdberlin.nightdream.ui.ClockLayoutContainer;
import com.firebirdberlin.nightdream.ui.NightDreamUI;
import com.firebirdberlin.nightdream.ui.SidePanel;
import com.firebirdberlin.nightdream.ui.SleepTimerDialogFragment;
import com.firebirdberlin.nightdream.ui.StopBackgroundServiceDialogFragment;
import com.firebirdberlin.nightdream.util.DevicePolicyWrapper;
import com.firebirdberlin.nightdream.viewmodels.BatteryReferenceViewModel;
import com.firebirdberlin.openweathermapapi.OpenWeatherMapApi;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;
import com.firebirdberlin.radiostreamapi.models.FavoriteRadioStations;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.Subscribe;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class NightDreamActivity extends BillingHelperActivity
        implements View.OnTouchListener,
        LocationUpdateReceiver.AsyncResponse,
        AvmAhaRequestTask.AsyncResponse,
        SleepTimerDialogFragment.SleepTimerDialogListener {
    private static final int PENDING_INTENT_STOP_APP = 1;
    private static final int MINIMUM_APP_RUN_TIME_MILLIS = 45000;
    public static String TAG = "NightDreamActivity";
    public static boolean isRunning = false;
    static long lastNoiseTime = System.currentTimeMillis();
    private static final int MODE_NIGHT = 0;
    private static final int MODE_DAY = 2;
    private static int mode = MODE_DAY;
    private static Context context = null;
    final private Handler handler = new Handler();
    private final Runnable finishApp = this::finish;
    private final Runnable runnableSetupNightMode = this::setupNightMode;
    public CastContext mCastContext;
    Sensor lightSensor = null;
    mAudioManager AudioManage = null;
    private ImageView alarmClockIcon;
    private BottomPanelLayout bottomPanelLayout;
    private ClockLayoutContainer clockLayoutContainer;
    private SidePanel sidePanel;
    private boolean screenWasOn = false;
    private float last_ambient = 4.0f;
    private NightDreamUI nightDreamUI = null;
    private NotificationReceiver nReceiver = null;
    private LocationUpdateReceiver locationReceiver = null;
    private NightDreamBroadcastReceiver broadcastReceiver = null;
    private PowerSupplyReceiver powerSupplyReceiver = null;
    private long resumeTime = -1L;
    private TextToSpeech textToSpeech;
    private CastSession mCastSession;
    private SessionManagerListener<CastSession> mSessionManagerListener;
    private Settings mySettings = null;
    private Configuration prevConfig;
    private final long startTime = System.currentTimeMillis();
    private static final String UTTERANCE_ID_SPEAK_TIME = "UTTERANCE_ID_SPEAK_TIME";

    GestureDetector.SimpleOnGestureListener mSimpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public void onLongPress(@NonNull MotionEvent e) {
            if (nightDreamUI.isLocked() || !mySettings.isSpeakTimeEnabled()) {
                return;
            }

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(mySettings.getFullTimeFormat(), Locale.getDefault());
            String text = String.format(
                    "%s %s",
                    context.getString(R.string.speakTime),
                    simpleDateFormat.format(Calendar.getInstance().getTime())
            );


            if (textToSpeech != null) {
                // TODO implement audio ducking
                if (!text.isEmpty()) {
                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID_SPEAK_TIME);
                } else {
                    Log.w("TTS", "Attempted to speak null or empty text.");
                }
            }
            super.onLongPress(e);
        }

        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            if (!mySettings.doubleTapToFinish) {
                return false;
            }
            finish();
            return true;
        }
    };
    private final LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(@NonNull final Location location) {
            if ((mySettings.getLocation().getLongitude() != location.getLongitude()) ||
                    (mySettings.getLocation().getLatitude() != location.getLatitude())) {
                City city = new City();
                city.lat = location.getLatitude();
                city.lon = location.getLongitude();
                city.name = "current";
                Log.i(TAG, "current location: " + city);
                if (mySettings != null) {
                    mySettings.setLocation(location);
                }
                onLocationUpdated();
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(@NonNull String s) {

        }

        @Override
        public void onProviderDisabled(@NonNull String s) {

        }
    };
    private final Runnable checkKeepScreenOn = () -> setKeepScreenOn(shallKeepScreenOn(mode));
    private boolean isChargingWireless = false;
    private DevicePolicyWrapper devicePolicyWrapper;
    private GestureDetector mGestureDetector = null;
    private final Runnable lockDevice = () -> {
        if (devicePolicyWrapper != null) {
            devicePolicyWrapper.lockDeviceIfNeeded(mySettings);
        }
    };
    private LocationManager locationManager = null;
    private FlashlightProvider flash = null;
    private final Runnable setScreenOff = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(setScreenOff);
            int new_mode = nightDreamUI.determineScreenMode(last_ambient);
            setMode(new_mode);
        }
    };
    static public void start(Context context) {
        NightDreamActivity.start(context, null);
    }

    static public void start(Context context, String action) {
        // todo do not start with an active call
        Utility.turnScreenOn(context);
        Intent intent = getDefaultIntent(context);
        intent.setAction(action);
        context.startActivity(intent);
    }

    static private Intent getDefaultIntent(Context context) {
        Intent intent = new Intent(context, NightDreamActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    public static Context getAppContext() {
        return context;
    }

    private void setupCastListener() {
        mCastContext = CastContext.getSharedInstance(this);
        mSessionManagerListener = new SessionManagerListener<CastSession>() {
            @Override
            public void onSessionEnded(@NonNull CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionResumed(@NonNull CastSession session, boolean wasSuspended) {
                onApplicationConnected(session);
            }

            @Override
            public void onSessionResumeFailed(@NonNull CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarted(@NonNull CastSession session, @NonNull String sessionId) {
                onApplicationConnected(session);
            }

            @Override
            public void onSessionStartFailed(@NonNull CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarting(@NonNull CastSession session) {
            }

            @Override
            public void onSessionEnding(@NonNull CastSession session) {
            }

            @Override
            public void onSessionResuming(@NonNull CastSession session, @NonNull String sessionId) {
            }

            @Override
            public void onSessionSuspended(@NonNull CastSession session, int reason) {
            }

            private void onApplicationConnected(CastSession castSession) {
                mCastSession = castSession;
                if (RadioStreamService.isRunning) {
                    RadioStreamService.loadRemoteMediaListener(castSession);
                }
            }

            private void onApplicationDisconnected() {
                if (RadioStreamService.isRunning) {
                    FavoriteRadioStations stations = mySettings.getFavoriteRadioStations();
                    if ((stations != null) && (stations.numAvailableStations() != 0)) {
                        int stationIndex = Settings.getLastActiveRadioStation(context);
                        RadioStreamService.startStream(context, stationIndex);
                    }
                }
            }
        };
        mCastContext.getSessionManager().addSessionManagerListener(
                mSessionManagerListener, CastSession.class
        );
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        Log.i(TAG, "onCreate() starts: " + (System.currentTimeMillis() - startTime) + " ms");
        MultiDex.install(this);
        context = this;

        setContentView(R.layout.main);
        Log.i(TAG, "setContentView took: " + (System.currentTimeMillis() - startTime) + " ms");

        Window window = getWindow();
        window.getDecorView().post(() -> Utility.hideSystemUI(window));
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
            window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            );
        }

        nightDreamUI = new NightDreamUI(this, window);
        AudioManage = new mAudioManager(this);
        mySettings = new Settings(this);
        setupCastListener();

        alarmClockIcon = findViewById(R.id.alarm_clock_icon);
        bottomPanelLayout = findViewById(R.id.bottomPanel);
        clockLayoutContainer = findViewById(R.id.clockLayoutContainer);
        sidePanel = findViewById(R.id.side_menu);

        ImageView background_image = findViewById(R.id.background_view);
        background_image.setOnTouchListener(this);

        devicePolicyWrapper = new DevicePolicyWrapper(this);
        mGestureDetector = new GestureDetector(this, mSimpleOnGestureListener);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(this::initTextToSpeech);

        Log.i(TAG, "onCreate took: " + (System.currentTimeMillis() - startTime) + " ms");
    }

    Snackbar snackbar = null;

    private class CanDrawOverlaysPermissionListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Utility.requestPermissionCanDrawOverlays(context);
            dismissSnackBar();
        }
    }


    private void conditionallyShowSnackBar() {
        Log.i(TAG, "conditionallyShowSnackBar");
        long daysSinceInstall = Utility.getDaysSinceFirstInstall(context);
        if (
                !Utility.hasPermissionCanDrawOverlays(context)
                && daysSinceInstall < 3
                && !AlarmHandlerService.alarmIsRunning()
                && !Utility.isLowRamDevice(context)
        ) {
            View view = findViewById(android.R.id.content);
            snackbar = Snackbar.make(view, R.string.permission_request_overlays, Snackbar.LENGTH_INDEFINITE);
            int color = Utility.getRandomMaterialColor(context);
            int textColor = Utility.getContrastColor(color);
            View snackbarView = snackbar.getView();
            snackbarView.setBackgroundColor(color);
            snackbar.setDuration(2 * 60_000);
            snackbar.setActionTextColor(textColor);

            TextView tv = snackbarView.findViewById(R.id.snackbar_text);
            tv.setTextColor(textColor);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tv.setAutoSizeTextTypeUniformWithConfiguration(
                        10, // minTextSize
                        20, // maxTextSize
                        2,  // autoSizeStepGranularity
                        TypedValue.COMPLEX_UNIT_SP // unit
                );
            }

            snackbar.setAction(android.R.string.ok, new CanDrawOverlaysPermissionListener());
            snackbar.show();
        } else {
            dismissSnackBar();
        }
    }

    void dismissSnackBar() {
        if (snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
            snackbar = null;
        }
    }

    void initTextToSpeech() {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status != TextToSpeech.ERROR && textToSpeech != null) {
                try {
                    textToSpeech.setLanguage(Locale.getDefault());
                } catch (OutOfMemoryError e) {
                    Log.e(TAG, "OutOfMemoryError setting TextToSpeech language", e);
                    textToSpeech = null; // Mark TTS as unusable
                }
            } else {
                textToSpeech = null;
            }
        });
    }

    public void setupFlashlight() {
        if (flash == null) {
            flash = new FlashlightProvider(this);
        }
        sidePanel.post(() -> {
            sidePanel.setTorchIconVisibility(flash.hasCameraFlash());
            sidePanel.setTorchIconActive(flash.isFlashlightOn());
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart()");

        setExcludeFromRecents();
        Utility.registerEventBus(this);
        nightDreamUI.onStart();

        lightSensor = Utility.getLightSensor(this);
        if (lightSensor == null) {
            last_ambient = 400.0f;
        }

        BatteryValue batteryValue = new BatteryStats(this).reference;
        this.isChargingWireless = batteryValue.isChargingWireless;

        Utility.createNotificationChannels(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Log.i(TAG, "onStart took: " + (System.currentTimeMillis() - startTime) + " ms");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
        ) {
            Log.i(TAG, "volume button event");
            if (mySettings.snoozeUsingVolumeButtons && AlarmHandlerService.alarmIsRunning()) {
                AlarmHandlerService.snooze(context);
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.d(TAG, "onWindowFocusChanged()");
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            nightDreamUI.onResume();
            Intent intent = getIntent();

            String action = intent.getAction();
            if (Config.ACTION_STOP_BACKGROUND_SERVICE.equals(action)) {
                showStopBackgroundServicesDialog();
                intent.setAction(null);
            } else if ("start standby mode".equals(action)) {
                if (mySettings.alwaysOnStartWithLockedUI) {
                    nightDreamUI.setLocked(true);
                }
                setMode(mode);
            } else if ("start night mode".equals(action)) {
                last_ambient = mySettings.minIlluminance;
                setMode(0);
                if (lightSensor == null) {
                    handler.postDelayed(setScreenOff, 20000);
                }
            } else {
                setMode(mode);
            }

            setupNightMode();
            setupFlashlight();
            setupRadioStreamUI();
            setupAlarmClockIcon();

            showToastIfNotCharging();

            final Context context = this;
            clockLayoutContainer.post(() -> {
                if (Settings.showNotification(this)) {
                    Intent i = new Intent(Config.ACTION_NOTIFICATION_LISTENER);
                    i.putExtra("command", "list");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                }
            });

            if (mySettings.getWeatherAutoLocationEnabled() && hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());
                executor.execute(() -> {
                    getLastKnownLocation();
                    handler.post(() -> {
                        if (hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                            locationManager.requestLocationUpdates(
                                    LocationManager.NETWORK_PROVIDER, 15 * 60000, 10000, locationListener
                            );
                        }
                    });
                });
            }

            if (mySettings.shallShowWeather()) {
                AtomicReference<WeatherEntry> oldWeatherEntry = new AtomicReference<>();
                DownloadWeatherModel.observe(this, weatherEntry -> {
                    Log.d(TAG, "onChanged weatherEntry: " + weatherEntry);
                    if (weatherEntry != oldWeatherEntry.get()) {
                        Log.d(TAG, "onChanged inside weatherEntry: " + oldWeatherEntry);
                        oldWeatherEntry.set(weatherEntry);
                        nightDreamUI.weatherDataUpdated(context);
                    }
                });
            }
            SqliteIntentService.scheduleAlarm(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
        prevConfig = new Configuration(getResources().getConfiguration());

        isRunning = true;
        resumeTime = System.currentTimeMillis();
        screenWasOn = false;
        mySettings = new Settings(this);
        handler.postDelayed(lockDevice, Utility.getScreenOffTimeout(this));
        if (mySettings.shallActivateDoNotDisturb()) {
            AudioManage.activateDnDMode(true, mySettings.activateDoNotDisturbAllowPriority);
        }
        ScreenWatcherService.conditionallyStart(this, mySettings);

        scheduleShutdown();
        nReceiver = registerNotificationReceiver();
        broadcastReceiver = registerBroadcastReceiver();
        powerSupplyReceiver = registerShutdownReceiver();
        locationReceiver = LocationUpdateReceiver.register(this, this);
        nReceiver.setColor((mode == MODE_NIGHT) ? mySettings.secondaryColorNight : mySettings.secondaryColor);

        if (AlarmHandlerService.alarmIsRunning()) {
            nightDreamUI.showAlarmClock();
            // TODO optionally enable the Flashlight
        }

        conditionallyShowSnackBar();
        setKeepScreenOn(true);
        Log.i(TAG, "onResume took: " + (System.currentTimeMillis() - startTime) + " ms");
    }

    private void setExcludeFromRecents() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        boolean isConfiguredAsDaydream = Utility.isConfiguredAsDaydream(this);
        List<ActivityManager.AppTask> tasks = (am != null) ? am.getAppTasks() : null;
        if (tasks != null) {
            for (ActivityManager.AppTask task : tasks)
                task.setExcludeFromRecents(isConfiguredAsDaydream);
        }
    }

    private void showToastIfNotCharging() {
        if (mySettings.showBatteryWarning && !Utility.isPlugged(this)) {
            Toast.makeText(this,
                    R.string.showBatteryWarningMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void showStopBackgroundServicesDialog() {
        FragmentManager fm = getSupportFragmentManager();
        if (!fm.isDestroyed()) {
            StopBackgroundServiceDialogFragment dialog = new StopBackgroundServiceDialogFragment();
            dialog.show(fm, "sleep_timer");
        }
    }

    void setupNightMode() {
        if (mySettings.nightModeActivationMode != Settings.NIGHT_MODE_ACTIVATION_SCHEDULED) return;

        Calendar start = new SimpleTime(mySettings.nightModeTimeRangeStartInMinutes).getCalendar();
        Calendar end = new SimpleTime(mySettings.nightModeTimeRangeEndInMinutes).getCalendar();

        TimeRange timerange = new TimeRange(start, end);
        int new_mode = (timerange.inRange()) ? 0 : 2;
        toggleNightMode(new_mode);

        Calendar time = timerange.getNextEvent();
        long delta = time.getTimeInMillis() - System.currentTimeMillis();
        handler.postDelayed(runnableSetupNightMode, delta);
    }

    void setupRadioStreamUI() {
        switch (RadioStreamService.streamingMode) {
            case ALARM:
                setVolumeControlStream(RadioStreamService.currentStreamType);
                nightDreamUI.showAlarmClock();
                break;
            case RADIO:
                setVolumeControlStream(AudioManager.STREAM_MUSIC);
                break;
            default:
                setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
                break;
        }
    }

    @Override

    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
        setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

        nightDreamUI.onPause();

        handler.removeCallbacks(finishApp);
        handler.removeCallbacks(runnableSetupNightMode);
        handler.removeCallbacks(lockDevice);
        handler.removeCallbacks(checkKeepScreenOn);

        PowerConnectionReceiver.schedule(this);
        ScheduledAutoStartReceiver.schedule(this);
        unregister(powerSupplyReceiver);
        unregisterLocalReceiver(nReceiver);
        unregisterLocalReceiver(broadcastReceiver);
        LocationUpdateReceiver.unregister(this, locationReceiver);

        if (mySettings.shallActivateDoNotDisturb()) {
            AudioManage.activateDnDMode(false, mySettings.activateDoNotDisturbAllowPriority);
        }

        if (!mySettings.allow_screen_off
                || mode != MODE_NIGHT
                || !screenWasOn
                || Utility.isScreenOn(this)) {
            nightDreamUI.restoreRingerMode();
        }

        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
        CheckChargingStateJob.schedule(this);
        isRunning = false;
    }

    private void unregister(BroadcastReceiver receiver) {
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {

        }
    }

    private void unregisterLocalReceiver(BroadcastReceiver receiver) {
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {

        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop()");

        nightDreamUI.onStop();
        Utility.unregisterEventBus(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");

        // do not restore ringer mode, otherwise calls will ring
        // after the dream has ended
        //audiomanage.setRingerMode(currentRingerMode);

        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
        broadcastReceiver = null;
        powerSupplyReceiver = null;
        nReceiver = null;
    }

    private NotificationReceiver registerNotificationReceiver() {
        NotificationReceiver receiver = new NotificationReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.ACTION_NOTIFICATION_LISTENER);
        filter.addAction(Config.ACTION_NOTIFICATION_APPS_LISTENER);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
        return receiver;
    }

    private NightDreamBroadcastReceiver registerBroadcastReceiver() {
        Log.d(TAG, "registerReceiver()");
        NightDreamBroadcastReceiver receiver = new NightDreamBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.ACTION_RADIO_STREAM_STARTED);
        filter.addAction(Config.ACTION_RADIO_STREAM_STOPPED);
        filter.addAction(OpenWeatherMapApi.ACTION_WEATHER_DATA_UPDATED);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
        return receiver;
    }

    private PowerSupplyReceiver registerShutdownReceiver() {
        Log.d(TAG, "registerShutdownReceiver()");
        PowerSupplyReceiver receiver = new PowerSupplyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        registerReceiver(receiver, filter);
        return receiver;
    }

    public boolean onTouch(View view, MotionEvent e) {
        return mGestureDetector.onTouchEvent(e) || nightDreamUI.onTouch(view, e);
    }

    // click on the settings icon
    @SuppressWarnings("UnusedParameters")
    public void onSettingsClick(View v) {
        PreferencesActivity.start(this);
    }

    @SuppressWarnings("UnusedParameters")
    public void onRadioClick(View v) {
        if (AlarmHandlerService.alarmIsRunning()) {
            AlarmHandlerService.stop(this);
        }
        if (!isPurchased(PurchaseManager.ITEM_WEB_RADIO)) {
            showSubscriptionDialog();
            return;
        }

        BottomPanelLayout.Panel panel = bottomPanelLayout.getActivePanel();
        if (panel == BottomPanelLayout.Panel.WEB_RADIO) {
            if (RadioStreamService.isRunning) RadioStreamService.stop(this);
            bottomPanelLayout.setActivePanel(BottomPanelLayout.Panel.ALARM_CLOCK);
            if (mCastSession != null) {
                RemoteMediaClient mRemoteMediaPlayer = mCastSession.getRemoteMediaClient();
                if (mRemoteMediaPlayer != null) {
                    mRemoteMediaPlayer.stop();
                }
                SessionManager mSessionManager = mCastContext.getSessionManager();
                mSessionManager.endCurrentSession(true);
            }
            RadioStreamService.isRunning = false;
        } else {
            bottomPanelLayout.setActivePanel(BottomPanelLayout.Panel.WEB_RADIO);
        }
        sidePanel.setRadioIconActive(panel != BottomPanelLayout.Panel.WEB_RADIO);
        nightDreamUI.showAlarmClock();
    }

    public void onLocationFailure() {
    }

    public void onLocationUpdated() {
        Log.d("DownloadWeatherService", "onLocationUpdated()");
        DownloadWeatherService.start(this, mySettings);
    }

    private void setupAlarmClockIcon() {
        alarmClockIcon.setVisibility(View.VISIBLE);
        Utility.setIconSize(this, alarmClockIcon);
    }

    public void onWeatherForecastClick(View v) {
        WeatherForecastActivity.start(this);
    }

    public void onNotificationListClick(View v) {
        if (nightDreamUI.isLocked()) return;
        int resId = Settings.getNotificationContainerResourceId(this);
        FlexboxLayout notificationLayout = findViewById(resId);
        if (notificationLayout != null && notificationLayout.getChildCount() > 0) {
            NotificationListActivity.start(this);
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void onAlarmClockClick(View v) {
        SetAlarmClockActivity.start(this);
    }

    @SuppressWarnings("UnusedParameters")
    public void onNightModeClick(View v) {
        if (lightSensor == null
                || mySettings.nightModeActivationMode == Settings.NIGHT_MODE_ACTIVATION_MANUAL) {
            int new_mode = (mode == MODE_NIGHT) ? MODE_DAY : MODE_NIGHT;
            toggleNightMode(new_mode);
        }
    }

    private void toggleNightMode(int new_mode) {
        if (lightSensor == null) {
            last_ambient = (new_mode == MODE_DAY) ? 400.f : mySettings.minIlluminance;
        }
        setMode(new_mode);
    }

    private void onChangeNightMode(int new_mode) {
        if (new_mode == MODE_NIGHT) {
        }
    }

    public void onTorchClick(View v) {
        if (flash == null) return;

        flash.toggleFlashlight();
        setupFlashlight();
    }

    public void onBulbClick(View v) {
        if (flash == null) return;

        SmartHomeActivity.start(this);
    }

    public void onAhaConnectionError(String message) {
    }
    public void onAhaRequestFinished() {
        Log.d(TAG, "onAhaRequestFinished");
    }
    public void onAhaDeviceListReceived(List<AvmAhaDevice> deviceList) {
        Log.d(TAG, "onAhaDeviceListReceived");
    }
    public void onAhaDeviceStateChanged(AvmAhaDevice device) {
        Log.d(TAG, "onAhaDeviceStateChanged");
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int diff = newConfig.diff(prevConfig);
        if ((nightDreamUI != null) && (diff != 0)) {
            nightDreamUI.onConfigurationChanged(newConfig);
        }
        prevConfig = new Configuration(newConfig);
    }

    private void setMode(int new_mode) {
        nightDreamUI.setMode(new_mode, last_ambient);
        if (
                (mode != MODE_NIGHT && new_mode == MODE_NIGHT)
                        || (mode == MODE_NIGHT && new_mode != MODE_NIGHT)
        ){
            onChangeNightMode(new_mode);
        }
        mode = new_mode;
    }

    private boolean shallKeepScreenOn(int mode) {
        screenWasOn = screenWasOn || Utility.isScreenOn(this);
        boolean isCharging = Utility.isPlugged(this);
        long now = Calendar.getInstance().getTimeInMillis();

        Log.d(TAG, "screenWasOn = " + screenWasOn);
        Log.d(TAG, "mode = " + mode);
        Log.d(TAG, "isCharging = " + isCharging);
        Log.d(TAG, "now - resumeTime < MINIMUM_APP_RUN_TIME_MILLIS = " +
                (now - resumeTime < MINIMUM_APP_RUN_TIME_MILLIS));
        Log.d(TAG, "screen is locked " + Utility.isScreenLocked(context));

        long nextAlarmTime = mySettings.getAlarmTime().getTimeInMillis();
        if ( // keep screen on
                now - resumeTime < MINIMUM_APP_RUN_TIME_MILLIS // 45 seconds after resume
                        || (0 < nextAlarmTime - now && nextAlarmTime - now < 600000) // 1000 * 60 * 10 = 10 minutes
                        || AlarmService.isRunning
                        || RadioStreamService.isRunning) {
            Log.d(TAG, "shallKeepScreenOn() 1 true");
            return true;
        }

        BatteryStats batteryStats = new BatteryStats(context);
        Log.i(TAG, "1 " + (isCharging || mySettings.isWithinAlwaysOnTime(batteryStats.reference.level)));
        if (isCharging && (mode > 0 || mode == 0 && !mySettings.allow_screen_off)) {
            Log.d(TAG, "shallKeepScreenOn() -> true (charging)");
            return true;
        }

        if (mySettings.isWithinAlwaysOnTime(batteryStats.reference.level)) {
            Log.d(TAG, "shallKeepScreenOn() -> true (within always on time)");
            return true;
        }

        if (mySettings.isWithinScheduledAutoStartTimeRange(isCharging)) {
            Log.d(TAG, "shallKeepScreenOn() -> true (within scheduled autostart time)");
            return true;
        }

        if (!isCharging && (
                mySettings.batteryTimeout == -1
                        || (now - resumeTime) / 60000 < mySettings.batteryTimeout
        )
        ) {
            Log.d(TAG, "shallKeepScreenOn() -> true (not charging, waiting for battery timeout)");
            return true;
        }

        Log.d(TAG, "shallKeepScreenOn() false");
        return false;
    }

    @Subscribe
    public void onEvent(OnNewLightSensorValue event) {
        Log.i(TAG, event.value + " lux, n=" + event.n);
        last_ambient = event.value;
        handleBrightnessChange();
    }

    @Subscribe
    public void onEvent(OnLightSensorValueTimeout event) {
        last_ambient = (event.value >= 0.f) ? event.value : mySettings.minIlluminance;
        Log.i(TAG, "Static for 15s: " + last_ambient + " lux.");
        handleBrightnessChange();
    }

    @Subscribe
    public void onEvent(OnNewAmbientNoiseValue event) {
        double ambient_noise_threshold = (mode == 0) ?
                mySettings.NOISE_AMPLITUDE_WAKE : mySettings.NOISE_AMPLITUDE_SLEEP;
        if (event.value > ambient_noise_threshold) {
            lastNoiseTime = System.currentTimeMillis();
            Log.i(TAG, "Sound is noisy! " + event.value);
        }
        handleBrightnessChange();
    }

    private void handleBrightnessChange() {
        if (mySettings.nightModeActivationMode == Settings.NIGHT_MODE_ACTIVATION_AUTOMATIC) {
            int new_mode = nightDreamUI.determineScreenMode(last_ambient);
            setMode(new_mode);
        }
    }

    public void setKeepScreenOn(boolean keepScreenOn) {
        Log.i(TAG, "setKeepScreenOn(" + keepScreenOn + ") requested.");
        Window window = getWindow();
        int currentFlags = window.getAttributes().flags;
        boolean isFlagCurrentlySet = (currentFlags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0;

        // Only act if the desired state is different from the current state
        if (keepScreenOn && !isFlagCurrentlySet) {
            Log.i(TAG, "Flag is not set. Adding FLAG_KEEP_SCREEN_ON.");
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            window.addFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        } else if (!keepScreenOn && isFlagCurrentlySet) {
            Log.i(TAG, "Flag is set. Clearing FLAG_KEEP_SCREEN_ON.");
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            window.clearFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            }
        } else {
            // Do nothing, the state is already correct. This prevents resetting the screen timeout.
            Log.d(TAG, "setKeepScreenOn: State is already correct. No change needed.");
        }

        // Continue to schedule the next check
        handler.removeCallbacks(checkKeepScreenOn);
        handler.postDelayed(checkKeepScreenOn, Utility.millisToTimeTick(MINIMUM_APP_RUN_TIME_MILLIS));
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Log.i(TAG, "new intent received");
        //now getIntent() should always return the last received intent
    }

    private void scheduleShutdown() {
        if (mySettings == null) return;

        Calendar calendar = null;
        if (PowerConnectionReceiver.shallAutostart(this, mySettings)
                && mySettings.autostartTimeRangeStartInMinutes != mySettings.autostartTimeRangeEndInMinutes) {
            SimpleTime simpleEndTime = new SimpleTime(mySettings.autostartTimeRangeEndInMinutes);
            calendar = simpleEndTime.getCalendar();
        }

        scheduleShutdown(calendar);
    }

    private void scheduleShutdown(Calendar calendar) {
        Log.i(TAG, "scheduleShutdown(" + calendar + ")");
        handler.removeCallbacks(finishApp);
        if (calendar == null) return;
        long deltaMillis = calendar.getTimeInMillis() - System.currentTimeMillis();
        handler.postDelayed(finishApp, deltaMillis);
    }

    @Override
    public void onSleepTimeSelected(int minutes) {
        nightDreamUI.reconfigure();
    }

    @Override
    public void onSleepTimeDismissed() {
        nightDreamUI.reconfigure();
    }

    private void getLastKnownLocation() {
        Location location = Utility.getLastKnownLocation(this);
        if (location != null) {
            mySettings.setLocation(location);
            // onLocationUpdated();
        }
    }

    public class NightDreamBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive() -> ");
            if (intent == null) return;
            String action = intent.getAction();
            Log.i(TAG, "action -> " + action);
            if (OpenWeatherMapApi.ACTION_WEATHER_DATA_UPDATED.equals(action)) {
                // #TODO can this be deleted ?
                //mySettings = new Settings(context);
            } else if (Config.ACTION_RADIO_STREAM_STARTED.equals(action)
                    || Config.ACTION_RADIO_STREAM_STOPPED.equals(action)) {
                setupRadioStreamUI();
            }

        }
    }

    class PowerSupplyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive() -> ");
            if (intent == null) return;

            BatteryStats stats = new BatteryStats(context);
            BatteryReferenceViewModel.updateIfNecessary(context, stats.reference);

            String action = intent.getAction();
            Log.i(TAG, "action -> " + action);
            if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                scheduleShutdown();
                nightDreamUI.onPowerDisconnected();
                if (
                        mySettings.handle_power_disconnection
                        &&  !mySettings.isWithinAlwaysOnTime(stats.reference.level)
                ) {
                    handler.removeCallbacks(finishApp);
                    if (isChargingWireless) {
                        handler.postDelayed(finishApp, 5000);
                    } else {
                        finish();
                    }
                }
            } else if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                scheduleShutdown();
                handler.removeCallbacks(finishApp);
                nightDreamUI.onPowerConnected();
                isChargingWireless = stats.reference.isChargingWireless;
            }
        }
    }
}
