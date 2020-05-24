package com.firebirdberlin.nightdream;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.firebirdberlin.nightdream.events.OnLightSensorValueTimeout;
import com.firebirdberlin.nightdream.events.OnNewAmbientNoiseValue;
import com.firebirdberlin.nightdream.events.OnNewLightSensorValue;
import com.firebirdberlin.nightdream.models.BatteryValue;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.models.TimeRange;
import com.firebirdberlin.nightdream.receivers.LocationUpdateReceiver;
import com.firebirdberlin.nightdream.receivers.NightModeReceiver;
import com.firebirdberlin.nightdream.receivers.PowerConnectionReceiver;
import com.firebirdberlin.nightdream.receivers.ScheduledAutoStartReceiver;
import com.firebirdberlin.nightdream.receivers.ScreenReceiver;
import com.firebirdberlin.nightdream.repositories.BatteryStats;
import com.firebirdberlin.nightdream.repositories.FlashlightProvider;
import com.firebirdberlin.nightdream.services.AlarmHandlerService;
import com.firebirdberlin.nightdream.services.AlarmService;
import com.firebirdberlin.nightdream.services.DownloadWeatherService;
import com.firebirdberlin.nightdream.services.RadioStreamService;
import com.firebirdberlin.nightdream.services.ScreenWatcherService;
import com.firebirdberlin.nightdream.ui.BottomPanelLayout;
import com.firebirdberlin.nightdream.ui.NightDreamUI;
import com.firebirdberlin.nightdream.ui.RadioInfoDialogFragment;
import com.firebirdberlin.nightdream.ui.SleepTimerDialogFragment;
import com.firebirdberlin.nightdream.ui.StopBackgroundServiceDialogFragment;
import com.firebirdberlin.openweathermapapi.OpenWeatherMapApi;
import com.firebirdberlin.openweathermapapi.models.City;
import com.google.android.flexbox.FlexboxLayout;

import org.greenrobot.eventbus.Subscribe;

import java.util.Calendar;
import java.util.List;

public class NightDreamActivity extends BillingHelperActivity
                                implements View.OnTouchListener,
                                           NightModeReceiver.Event,
                                           LocationUpdateReceiver.AsyncResponse,
                                           SleepTimerDialogFragment.SleepTimerDialogListener,
                                           RadioInfoDialogFragment.RadioInfoDialogListener
{
    public static String TAG ="NightDreamActivity";
    public static boolean isRunning = false;
    private static int PENDING_INTENT_STOP_APP = 1;
    private static int MINIMUM_APP_RUN_TIME_MILLIS = 45000;
    final private Handler handler = new Handler();
    protected PowerManager.WakeLock wakelock;
    Sensor lightSensor = null;
    private static int mode = 2;
    mAudioManager AudioManage = null;
    private ImageView weatherIcon;
    private ImageView alarmClockIcon;
    private ImageView radioIcon;
    private ImageView torchIcon;
    private BottomPanelLayout bottomPanelLayout;
    private FrameLayout clockLayoutContainer;
    private boolean screenWasOn = false;
    private Context context = null;
    private float last_ambient = 4.0f;
    private NightDreamUI nightDreamUI = null;
    private NotificationReceiver nReceiver = null;
    private LocationUpdateReceiver locationReceiver = null;
    private NightModeReceiver nightModeReceiver = null;
    private NightDreamBroadcastReceiver broadcastReceiver = null;
    private PowerSupplyReceiver powerSupplyReceiver = null;
    private long resumeTime = -1L;

    private Settings mySettings = null;
    GestureDetector.SimpleOnGestureListener mSimpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (mySettings.doubleTapToFinish) {
                finish();
                return true;
            }
            return false;
        }
    };
    private boolean isChargingWireless = false;
    private DevicePolicyManager mgr = null;
    private ComponentName cn = null;
    private GestureDetector mGestureDetector = null;
    static long lastNoiseTime = System.currentTimeMillis();
    private Runnable finishApp = new Runnable() {
        @Override
        public void run() {
            finish();
        }
    };

    private Runnable checkKeepScreenOn = new Runnable() {
        @Override
        public void run() {
            setKeepScreenOn(shallKeepScreenOn(mode));
        }
    };

    private Runnable alwaysOnTimeout = new Runnable() {
        @Override
        public void run() {
            if ( Utility.isCharging(context) && mode > 0) return;

            mySettings.updateNextAlwaysOnTime();
            setKeepScreenOn(shallKeepScreenOn(mode));
            triggerAlwaysOnTimeout();
        }
    };

    private Runnable lockDevice = new Runnable() {
        @Override
        public void run() {
           if (mySettings.useDeviceLock && mgr.isAdminActive(cn) && !isLocked()) {
                   mgr.lockNow();
               Utility.turnScreenOn(context);
           }
        }
    };

    static public void start(Context context) {
        NightDreamActivity.start(context,  null);
    }

    static public void start(Context context, String action) {
        // todo do not start with an active call
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.main);

        Log.i(TAG, "onCreate()");
        Window window = getWindow();

        nightDreamUI = new NightDreamUI(this, window);
        AudioManage = new mAudioManager(this);
        mySettings = new Settings(this);

        // allow the app to be displayed above the keyguard
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        setKeepScreenOn(true);
        bottomPanelLayout = findViewById(R.id.bottomPanel);
        weatherIcon = findViewById(R.id.icon_weather_forecast);
        alarmClockIcon = findViewById(R.id.alarm_clock_icon);
        radioIcon = findViewById(R.id.radio_icon);
        ImageView background_image = findViewById(R.id.background_view);
        background_image.setOnTouchListener(this);
        mgr = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        cn = new ComponentName(this, AdminReceiver.class);
        mGestureDetector = new GestureDetector(this, mSimpleOnGestureListener);
        clockLayoutContainer = findViewById(R.id.clockLayoutContainer);
    }

    public void setupFlashlight() {
        torchIcon = findViewById(R.id.torch_icon);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (flash == null) {
                flash = new FlashlightProvider(this);
            }
            torchIcon.setVisibility(flash.hasCameraFlash() ? View.VISIBLE : View.GONE);
            FlexboxLayout sidePanel = findViewById(R.id.side_panel);
            sidePanel.post(new Runnable() {
                @Override
                public void run() {
                    if (flash.isFlashlightOn()) {
                        Log.w(TAG, "Flashlight is ON");
                        setIconActive(torchIcon);
                    } else {
                        Log.w(TAG, "Flashlight is OFF");
                        setIconInactive(torchIcon);
                    }
                }
            });
        } else {
            torchIcon.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart()");

        setExcludeFromRecents();
        setKeepScreenOn(true);

        Utility.registerEventBus(this);
        nightDreamUI.onStart();

        lightSensor = Utility.getLightSensor(this);
        if ( lightSensor == null ){
            last_ambient = 400.0f;
        }

        BatteryValue batteryValue = new BatteryStats(this).reference;
        this.isChargingWireless = batteryValue.isChargingWireless;

        Utility.createNotificationChannels(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
        isRunning = true;
        resumeTime = System.currentTimeMillis();
        screenWasOn = false;
        setKeepScreenOn(true);
        mySettings = new Settings(this);
        handler.postDelayed(lockDevice, Utility.getScreenOffTimeout(this));
        if ( mySettings.activateDoNotDisturb ) {
            AudioManage.activateDnDMode(true, mySettings.activateDoNotDisturbAllowPriority);
        }
        ScreenWatcherService.conditionallyStart(this, mySettings);

        scheduleShutdown();
        setupAlarmClockIcon();
        nightDreamUI.onResume();
        nReceiver = registerNotificationReceiver();
        nightModeReceiver = NightModeReceiver.register(this, this);
        broadcastReceiver = registerBroadcastReceiver();
        powerSupplyReceiver = registerShutdownReceiver();
        locationReceiver = LocationUpdateReceiver.register(this, this);

        nReceiver.setColor(mySettings.secondaryColor);
        Intent intent = getIntent();

        String action = intent.getAction();
        if (Config.ACTION_STOP_BACKGROUND_SERVICE.equals(action)) {
            showStopBackgroundServicesDialog();
            intent.setAction(null);
        } else
        if ("start standby mode".equals(action)) {
            if (mySettings.alwaysOnStartWithLockedUI) {
                nightDreamUI.setLocked(true);
            }
            setMode(mode);
        } else
        if ("start night mode".equals(action)) {
            last_ambient = mySettings.minIlluminance;
            setMode(0);
            if ( lightSensor == null ) {
                handler.postDelayed(setScreenOff, 20000);
            }
        } else {
            setMode(mode);
        }

        if ( AlarmHandlerService.alarmIsRunning() ) {
            nightDreamUI.showAlarmClock();
            // TODO optionally enable the Flashlight
        }

        setupNightMode();
        setupFlashlight();
        setupRadioStreamUI();

        BottomPanelLayout.Panel activePanel = BottomPanelLayout.Panel.ALARM_CLOCK;
        if (intent.getAction() != null && Config.ACTION_SHOW_RADIO_PANEL.equals(intent.getAction())) {
            activePanel = BottomPanelLayout.Panel.WEB_RADIO;
            // clear the action so that it won't be re-delivered.
            intent.setAction("");
            bottomPanelLayout.onResume();
        }

        bottomPanelLayout.setActivePanel(activePanel);
        triggerAlwaysOnTimeout();
        showToastIfNotCharging();

        final Context context = this;
        clockLayoutContainer.post(new Runnable() {
            @Override
            public void run() {
                // ask for active notifications
                if (Build.VERSION.SDK_INT >= 18){

                    Intent i = new Intent(Config.ACTION_NOTIFICATION_LISTENER);
                    i.putExtra("command", "list");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(i);
                }
            }
        });
    }

    private void setExcludeFromRecents() {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            boolean isConfiguredAsDaydream = Utility.isConfiguredAsDaydream(this);
            List<ActivityManager.AppTask> tasks = (am != null) ? am.getAppTasks() : null;
            if (tasks != null) {
                for (ActivityManager.AppTask task : tasks)
                    task.setExcludeFromRecents(isConfiguredAsDaydream);
            }
        }
    }

    private void showToastIfNotCharging() {
        if (mySettings.showBatteryWarning && ! Utility.isCharging(this) ) {
            Toast.makeText(this,
                    R.string.showBatteryWarningMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void showStopBackgroundServicesDialog() {
        FragmentManager fm = getSupportFragmentManager();
        StopBackgroundServiceDialogFragment dialog = new StopBackgroundServiceDialogFragment();
        dialog.show(fm, "sleep_timer");
    }

    public void onSwitchNightMode() {
        setupNightMode();
    }

    void setupNightMode() {
        if (mySettings.nightModeActivationMode != Settings.NIGHT_MODE_ACTIVATION_SCHEDULED) return;

        Calendar start = new SimpleTime(mySettings.nightModeTimeRangeStartInMinutes).getCalendar();
        Calendar end = new SimpleTime(mySettings.nightModeTimeRangeEndInMinutes).getCalendar();

        TimeRange timerange = new TimeRange(start, end);
        int new_mode = ( timerange.inRange() ) ? 0 : 2;
        toggleNightMode(new_mode);
        NightModeReceiver.schedule(this, timerange);
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
        Log.i(TAG ,"onPause()");
        setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

        nightDreamUI.onPause();

        handler.removeCallbacks(finishApp);
        handler.removeCallbacks(lockDevice);
        handler.removeCallbacks(alwaysOnTimeout);
        handler.removeCallbacks(checkKeepScreenOn);

        PowerConnectionReceiver.schedule(this);
        ScheduledAutoStartReceiver.schedule(this);
        cancelShutdown();
        NightModeReceiver.cancel(this);
        unregister(nightModeReceiver);
        unregister(powerSupplyReceiver);
        unregisterLocalReceiver(nReceiver);
        unregisterLocalReceiver(broadcastReceiver);
        LocationUpdateReceiver.unregister(this, locationReceiver);

        if ( mySettings.activateDoNotDisturb ) {
            AudioManage.activateDnDMode(false, mySettings.activateDoNotDisturbAllowPriority);
        }
        if (mySettings.allow_screen_off && mode == 0
                && screenWasOn && !Utility.isScreenOn(this) ){ // screen off in night mode
            startBackgroundListener();
        } else {
            nightDreamUI.restoreRingerMode();
        }
        isRunning = false;
    }

    private void unregister(BroadcastReceiver receiver) {
        try {
            unregisterReceiver(receiver);
        } catch ( IllegalArgumentException ignored) {

        }
    }

    private void unregisterLocalReceiver(BroadcastReceiver receiver) {
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        } catch ( IllegalArgumentException ignored) {

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

        nightModeReceiver = null;
        broadcastReceiver = null;
        powerSupplyReceiver = null;
        nReceiver  = null;
    }

    private NotificationReceiver registerNotificationReceiver() {
        NotificationReceiver receiver = new NotificationReceiver(this);
        IntentFilter filter = new IntentFilter(Config.ACTION_NOTIFICATION_LISTENER);
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
        filter.addAction(Config.ACTION_SHUT_DOWN);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
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
        if ( AlarmHandlerService.alarmIsRunning() ) {
            AlarmHandlerService.stop(this);
        }
        if (!isPurchased(BillingHelperActivity.ITEM_WEB_RADIO)) {
            showPurchaseDialog();
            return;
        }

        BottomPanelLayout.Panel panel = bottomPanelLayout.getActivePanel();
        if (panel == BottomPanelLayout.Panel.WEB_RADIO) {
            if (RadioStreamService.isRunning) RadioStreamService.stop(this);
            bottomPanelLayout.setActivePanel(BottomPanelLayout.Panel.ALARM_CLOCK);
            setIconInactive(radioIcon);
        } else {
            bottomPanelLayout.setActivePanel(BottomPanelLayout.Panel.WEB_RADIO);
            setIconActive(radioIcon);
        }
        nightDreamUI.showAlarmClock();
    }

    public void setIconActive(ImageView icon) {
        int accentColor = nightDreamUI.getAccentColor();
        icon.setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP);
        Utility.setIconSize(this, icon);
    }

    public void setIconInactive(ImageView icon) {
        int textColor = nightDreamUI.getSecondaryColor();
        icon.setColorFilter(textColor, PorterDuff.Mode.SRC_ATOP);
        Utility.setIconSize(this, icon);
    }

    public void onLocationFailure() { }
    public void onLocationUpdated() {
        DownloadWeatherService.start(this);
    }

    private void setupAlarmClockIcon() {

        if (!mySettings.useInternalAlarm) {
            alarmClockIcon.setVisibility(View.GONE);
        } else {
            alarmClockIcon.setVisibility(View.VISIBLE);
        }
        Utility.setIconSize(this, alarmClockIcon);
    }

    public void onWeatherForecastClick(View v) {
        WeatherForecastActivity.start(this);
    }

    @SuppressWarnings("UnusedParameters")
    public void onAlarmClockClick(View v) {
        SetAlarmClockActivity.start(this);
    }

    @SuppressWarnings("UnusedParameters")
    public void onNightModeClick(View v) {
        if ( lightSensor == null
                || mySettings.nightModeActivationMode == Settings.NIGHT_MODE_ACTIVATION_MANUAL ) {
            int new_mode = ( mode == 0) ? 2 : 0;
            toggleNightMode(new_mode);
        }
    }

    private void toggleNightMode(int new_mode) {
        if ( lightSensor == null ) {
            last_ambient = ( new_mode == 2 ) ? 400.f : mySettings.minIlluminance;
        }
        setMode(new_mode);
    }

    private FlashlightProvider flash = null;
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onTorchClick(View v) {
        if (flash == null) return;

        flash.toggleFlashlight();
        setupFlashlight();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        if (nightDreamUI != null) {
            nightDreamUI.onConfigurationChanged(newConfig);
        }
    }

    private void setMode(int new_mode) {
        nightDreamUI.setMode(new_mode, last_ambient);

        setKeepScreenOn(shallKeepScreenOn(new_mode)); // allow the screen to go off
        if (mode != 0 && new_mode == 0) {
            triggerAlwaysOnTimeout();
        }
        mode = new_mode;
    }

    private Runnable setScreenOff = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(setScreenOff);
            int new_mode = nightDreamUI.determineScreenMode(last_ambient, isNoisy());
            setMode(new_mode);
        }
    };

    private boolean isLocked() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager == null) {
            return false;
        }
        return keyguardManager.inKeyguardRestrictedInputMode();
    }

    private void startBackgroundListener() {
        Intent i = new Intent(this, NightModeListener.class);
        Utility.startForegroundService(this, i);
    }

    private boolean shallKeepScreenOn(int mode) {
        screenWasOn = screenWasOn || Utility.isScreenOn(this);
        boolean isCharging = Utility.isCharging(this);
        long now = Calendar.getInstance().getTimeInMillis();

        Log.d(TAG, "screenWasOn = " + screenWasOn);
        Log.d(TAG, "mode = " + mode);
        Log.d(TAG, "isCharging = " + isCharging);
        Log.d(TAG, "now - resumeTime < MINIMUM_APP_RUN_TIME_MILLIS = " +
                (now - resumeTime < MINIMUM_APP_RUN_TIME_MILLIS));

        if (ScheduledAutoStartReceiver.shallAutostart(this, mySettings)) {
            return true;
        }

        if (mode > 0 && ScreenReceiver.shallActivateStandby(this, mySettings)) {
            return true;
        }

        long nextAlarmTime = mySettings.getAlarmTime().getTimeInMillis();
        if ( // keep screen on
                now - resumeTime < MINIMUM_APP_RUN_TIME_MILLIS // 45 seconds after resume
                || (0 < nextAlarmTime - now && nextAlarmTime - now < 600000) // 1000 * 60 * 10 = 10 minutes
                || AlarmService.isRunning
                || RadioStreamService.isRunning) {
            Log.d(TAG, "shallKeepScreenOn() 1 true");
            return true;
        }

        Log.i(TAG, "1 " + (isCharging || mySettings.isAlwaysOnAllowed()));
        if ((isCharging || mySettings.isAlwaysOnAllowed()) && (mode > 0 || mode == 0 && !mySettings.allow_screen_off)) {
            Log.d(TAG, "shallKeepScreenOn() 2 true");
            return true;
        }


        Log.d(TAG, "shallKeepScreenOn() false");
        return false;
    }

    @Subscribe
    public void onEvent(OnNewLightSensorValue event){
        Log.i(TAG, event.value + " lux, n=" + event.n);
        last_ambient = event.value;
        handleBrightnessChange();
    }

    @Subscribe
    public void onEvent(OnLightSensorValueTimeout event){
        last_ambient = (event.value >= 0.f) ? event.value : mySettings.minIlluminance;
        Log.i(TAG, "Static for 15s: " + last_ambient + " lux.");
        handleBrightnessChange();
        handler.postDelayed(checkKeepScreenOn, MINIMUM_APP_RUN_TIME_MILLIS);
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

    boolean isNoisy() {
        if (!mySettings.useAmbientNoiseDetection()) {
            return false;
        }
        long now = System.currentTimeMillis();
        return (now - lastNoiseTime < MINIMUM_APP_RUN_TIME_MILLIS);
    }

    private void handleBrightnessChange() {
        if (mySettings.nightModeActivationMode == Settings.NIGHT_MODE_ACTIVATION_AUTOMATIC) {
            int new_mode = nightDreamUI.determineScreenMode(last_ambient, isNoisy());
            setMode(new_mode);
        }
    }

    public void setKeepScreenOn(boolean keepScreenOn) {
        if( keepScreenOn ) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Log.i(TAG, "new intent received");
        //now getIntent() should always return the last received intent
    }

    private PendingIntent getShutdownIntent() {
        Intent alarmIntent = new Intent(Config.ACTION_SHUT_DOWN);
        return PendingIntent.getBroadcast(
                this,
                PENDING_INTENT_STOP_APP,
                alarmIntent,
                0
        );
    }

    private void scheduleShutdown() {
        if (mySettings == null) return;

        cancelShutdown();

        Calendar calendar = null;
        if (PowerConnectionReceiver.shallAutostart(this, mySettings)
                && mySettings.autostartTimeRangeStartInMinutes != mySettings.autostartTimeRangeEndInMinutes) {
            SimpleTime simpleEndTime = new SimpleTime(mySettings.autostartTimeRangeEndInMinutes);
            calendar = simpleEndTime.getCalendar();
        }

        if (ScheduledAutoStartReceiver.shallAutostart(this, mySettings)
                &&mySettings.scheduledAutoStartTimeRangeEndInMinutes != mySettings.scheduledAutoStartTimeRangeEndInMinutes ) {
            SimpleTime simpleEndTime = new SimpleTime(mySettings.scheduledAutoStartTimeRangeEndInMinutes);
            Calendar calendar2 = simpleEndTime.getCalendar();
            if (calendar == null) {
                calendar = calendar2;
            } else
            if (calendar2.before(calendar)) {
                calendar = calendar2;
            }
        }

        scheduleShutdown(calendar);
    }

    private void scheduleShutdown(Calendar calendar) {
        if (calendar == null) return;
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        PendingIntent pendingIntent = getShutdownIntent();
        alarmManager.cancel(pendingIntent);
        if (Build.VERSION.SDK_INT >= 19) {
            try {
                alarmManager.setExact(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
            } catch (SecurityException ignored) {}
        } else {
            deprecatedSetAlarm(alarmManager, calendar, pendingIntent);
        }
    }

    @SuppressWarnings("deprecation")
    private void deprecatedSetAlarm(AlarmManager alarmManager, Calendar calendar, PendingIntent pendingIntent) {
        alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
    }

    private void cancelShutdown() {
        PendingIntent pendingIntent = getShutdownIntent();
        pendingIntent.cancel();
    }

    private void triggerAlwaysOnTimeout() {
        handler.removeCallbacks(alwaysOnTimeout);
        boolean isCharging = Utility.isCharging(this);
        Log.d(TAG, "triggerAlwaysOnTimeout");
        if ((! isCharging && mySettings.batteryTimeout > 0) ||
                (isCharging && mode == 0)) {
            long timeout = 60000 * mySettings.batteryTimeout;

            if ( isCharging ) {
                timeout = MINIMUM_APP_RUN_TIME_MILLIS;
            }

            Log.d(TAG, "triggerAlwaysOnTimeout " + (timeout / 1000) + " seconds");
            handler.postDelayed(alwaysOnTimeout, 60000 * mySettings.batteryTimeout);
        }
    }

    @Override
    public void onSleepTimeSelected(int minutes) {
        nightDreamUI.reconfigure();
    }

    @Override
    public void onSleepTimeDismissed() {
        nightDreamUI.reconfigure();
    }

    @Override
    public void onRadioInfoDialogDismissed()  {
        nightDreamUI.reconfigure();
    }

    public class NightDreamBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive() -> ");
            if (intent == null) return;
            String action = intent.getAction();
            Log.i(TAG, "action -> " + action);
            if (OpenWeatherMapApi.ACTION_WEATHER_DATA_UPDATED.equals(action)) {
                mySettings = new Settings(context);
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
            String action = intent.getAction();
            Log.i(TAG, "action -> " + action);
            if (Config.ACTION_SHUT_DOWN.equals(action)) {
                // this receiver is needed to shutdown the app at the end of the autostart time range
                if (mySettings.handle_power_disconnection_at_time_range_end &&
                        ! Utility.isConfiguredAsDaydream(context)) {
                    finish();
                }
            }
            else
            if (Intent.ACTION_POWER_DISCONNECTED.equals(action)){
                nightDreamUI.onPowerDisconnected();
                if ( mySettings.handle_power_disconnection ) {
                    handler.removeCallbacks(finishApp);
                    if ( isChargingWireless ) {
                        handler.postDelayed(finishApp, 5000);
                    } else {
                        finish();
                    }
                } else {
                    triggerAlwaysOnTimeout();
                }
            }
            else
            if (Intent.ACTION_POWER_CONNECTED.equals(action)){
                handler.removeCallbacks(finishApp);
                handler.removeCallbacks(alwaysOnTimeout);
                nightDreamUI.onPowerConnected();
                BatteryStats stats = new BatteryStats(context);
                isChargingWireless = stats.reference.isChargingWireless;
            }
        }
    }
}
