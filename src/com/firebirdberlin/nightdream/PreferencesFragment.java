package com.firebirdberlin.nightdream;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.ext.SdkExtensions;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.firebirdberlin.nightdream.receivers.PowerConnectionReceiver;
import com.firebirdberlin.nightdream.receivers.WakeUpReceiver;
import com.firebirdberlin.nightdream.services.ScreenWatcherService;
import com.firebirdberlin.nightdream.ui.ClockLayoutPreviewPreference;
import com.firebirdberlin.nightdream.viewmodels.RSSViewModel;
import com.firebirdberlin.nightdream.widget.ClockWidgetProvider;
import com.google.android.material.snackbar.Snackbar;
import com.rarepebble.colorpicker.ColorPreference;

import java.io.File;
import java.util.Vector;

import de.firebirdberlin.preference.InlineSeekBarPreference;

public class PreferencesFragment extends PreferenceFragmentCompat {
    public static final String TAG = "PreferencesFragment";
    public static final String PREFS_KEY = "NightDream preferences";
    private final Handler handler = new Handler();
    private Vector<AlertDialog> dialogs = new Vector<>();
    Snackbar snackbar;
    String rootKey;
    DaydreamSettingsObserver daydreamSettingsObserver = null;
    private Settings settings = null;
    private Context mContext = null;
    Preference.OnPreferenceClickListener purchasePreferenceClickListener =
            preference -> {
                showPurchaseDialog();
                return true;
            };
    SharedPreferences.OnSharedPreferenceChangeListener prefChangedListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if (sharedPreferences == null || key == null) return;
                    if (isAdded() && "clock".equals(rootKey)) {
                        View v = getView();
                        if (v != null) {
                            v.post(
                                    () -> {
                                        ClockLayoutPreviewPreference preview = findPreference("clockLayoutPreview");
                                        if (preview != null) {
                                            preview.invalidate();
                                        }
                                    }
                            );
                        }

                    }
                    settings = new Settings(mContext);

                    switch (key) {
                        case "brightness_offset":
                            int offsetInt = sharedPreferences.getInt("brightness_offset", 0);
                            settings.setBrightnessOffset(offsetInt / 100.f);
                            break;
                        case "autoBrightness":
                            InlineSeekBarPreference pref = findPreference("brightness_offset");
                            // reset the brightness level
                            settings.setBrightnessOffset(0.8f);
                            if (pref != null) {
                                pref.setProgress(80);
                            }
                            setupBrightnessControls(sharedPreferences);
                            break;
                        case "minBrightness":
                            int value = sharedPreferences.getInt("minBrightness", 1);
                            settings.setNightModeBrightness(value / 100.f);
                            break;
                        case "nightModeBrightnessInt":
                            int value1 = sharedPreferences.getInt("nightModeBrightnessInt", 0);
                            settings.setNightModeBrightness(value1 / 100.f);
                            break;
                        case "backgroundMode":
                            setupBackgroundImageControls(sharedPreferences);
                            break;
                        case "clockLayout":
                            Log.d(TAG, String.format("%s = %s", key, sharedPreferences.getString(key, "none")));
                            resetScaleFactor(sharedPreferences);
                            break;
                        case "nightModeActivationMode":
                            setupNightModePreferences(sharedPreferences);
                            break;
                        case "useDeviceLock":
                            setupDeviceAdministratorPermissions(sharedPreferences);
                            break;
                        case "handle_power":
                        case "standbyEnabledWhileDisconnected":
                        case "scheduledAutoStartEnabled":
                        case "autostartForNotifications":
                            setupStandByService(sharedPreferences);
                            break;
                        case "Night.muteRinger":
                            setupNotificationAccessPermission(sharedPreferences, "Night.muteRinger");
                            break;
                        case "activateDoNotDisturb":
                            setupNotificationAccessPermission(sharedPreferences, "activateDoNotDisturb");
                            break;
                        case "enableRSS":
                            if (settings.rssEnabled) {
                                RSSViewModel.loadDataPeriodicFromWorker(mContext, (LifecycleOwner) mContext);
                            } else {
                                RSSViewModel.stopWorker();
                            }
                            break;
                        case "rssURL":
                        case "rssCharSetMode":
                            if (settings.rssEnabled) {
                                RSSViewModel.refreshDataFromWorker(mContext);
                            }
                            break;
                        case "rssIntervalMode":
                            RSSViewModel.setIntervalMode(settings.rssIntervalMode);
                            break;
                        case "rssTickerSpeed":
                            RSSViewModel.setTickerSpeed(settings.rssTickerSpeed);
                            break;
                        case "rssTextSizeMode":
                            RSSViewModel.setTextSize(settings.rssTextSize);
                            break;
                    }

                    Log.i(TAG, "prefChangedListener called. Key: " + key);

                    // update all widgets via intent, so the are repainted with current settings
                    ClockWidgetProvider.updateAllWidgets(mContext);
                }
            };

    private final Runnable runnableNotificationAccessChanged = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(runnableNotificationAccessChanged);
            if (Build.VERSION.SDK_INT < 18 || !"notifications".equals(rootKey) || !isAdded()) {
                return;
            }
            Log.i(TAG, "Runnable called");
            Preference preference = findPreference("showNotification");

            if (preference != null) {
                if (isNotificationAccessDenied()) {
                    preference.setSummary(getString(R.string.showNotificationsAccessNotGranted));
                    preference.setEnabled(false);
                } else {
                    preference.setSummary(getString(R.string.showNotificationsAccessGranted));
                    preference.setEnabled(true);
                }
                handler.postDelayed(runnableNotificationAccessChanged, 2000);
            }
        }
    };

    private final ActivityResultLauncher<String> readExternalStoragePermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), result -> {
                if (result) {
                    Log.d(TAG, "readExternalStoragePermission: PERMISSION GRANTED");
                } else {
                    Log.d(TAG, "readExternalStoragePermission: PERMISSION DENIED");
                    Toast.makeText(getActivity(), "Permission denied !", Toast.LENGTH_LONG).show();
                }
            });

    ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                // Callback is invoked after the user selects a media item or closes the
                // photo picker.
                if (uri != null) {
                    Log.d("PhotoPicker", "Selected URI: " + uri);
                    File directory = new File(mContext.getFilesDir() + "/backgroundImages");
                    Utility.prepareDirectory(directory);
                    Utility.copyToDirectory(mContext, uri, directory, "image_0.jpg");
                } else {
                    Log.d("PhotoPicker", "No media selected");
                }
            });

    // Registers a photo picker activity launcher in multi-select mode.
    // In this example, the app lets the user select up to 5 media files.
    ActivityResultLauncher<PickVisualMediaRequest> pickMultipleMedia =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(getMaxNumImages()), uris -> {
                // Callback is invoked after the user selects media items or closes the
                // photo picker.
                if (!uris.isEmpty()) {
                    Log.d("PhotoPicker", "Number of items selected: " + uris.size());
                    File directory = new File(mContext.getFilesDir() + "/backgroundImages");
                    Utility.prepareDirectory(directory);
                    int count = 0;
                    for (Uri uri: uris) {
                        count += 1;
                        String name = "image_" + count + ".jpg";
                        Utility.copyToDirectory(mContext, uri, directory, name);
                    }
                } else {
                    Log.d("PhotoPicker", "No media selected");
                }
            });

    int getMaxNumImages() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 2) {
            return MediaStore.getPickImagesMaxLimit();
        }
        return 100;
    }

    boolean isNotificationAccessDenied() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager n = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            return !n.isNotificationPolicyAccessGranted();
        } else {
            return !mNotificationListener.running;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        dismissSnackBar();
        closeAlertDialogs();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        daydreamSettingsObserver = new DaydreamSettingsObserver(new Handler());
        mContext.getContentResolver().registerContentObserver(
                android.provider.Settings.Secure.getUriFor("screensaver_enabled"),
                true,
                daydreamSettingsObserver);
        mContext.getContentResolver().registerContentObserver(
                android.provider.Settings.Secure.getUriFor("screensaver_components"),
                true,
                daydreamSettingsObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            mContext.getContentResolver().unregisterContentObserver(daydreamSettingsObserver);
        } catch (IllegalArgumentException | NullPointerException ignored) {

        }
    }

    private void resetAlwaysOnModeIfNotPurchased() {
        if (isPurchased(BillingHelperActivity.ITEM_ACTIONS)) {
            return;
        }
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("standbyEnabledWhileDisconnected", false);
        editor.putBoolean("scheduledAutoStartEnabled", false);
        editor.apply();
    }

    private void resetUseDeviceLockIfNotPurchased() {
        if (isPurchased(BillingHelperActivity.ITEM_ACTIONS)) {
            return;
        }
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("useDeviceLock", false);
        editor.apply();
        setupDeviceAdministratorPermissions(prefs);
    }

    void onPurchasesInitialized() {
        resetAlwaysOnModeIfNotPurchased();
        resetUseDeviceLockIfNotPurchased();
    }

    private void togglePurchasePreferences() {
        boolean isPurchasedDonation = isPurchased(BillingHelperActivity.ITEM_DONATION);
        boolean isPurchasedWeather = isPurchased(BillingHelperActivity.ITEM_WEATHER_DATA);
        boolean isPurchasedActions = isPurchased(BillingHelperActivity.ITEM_ACTIONS);
        Log.i(TAG, "actions: " + isPurchasedActions);
        Log.i(TAG, "weather: " + isPurchasedWeather);
        Log.i(TAG, "donation: " + isPurchasedDonation);
        enablePreference("expert_screen", isPurchasedActions);
        enablePreference("scheduled_autostart_screen", isPurchasedActions);
        enablePreference("activateDoNotDisturb", isPurchasedActions);
        enablePreference("speakTime", isPurchasedActions);
        enablePreference("useDeviceLock", isPurchasedActions);
        enablePreference("category_notifications", isPurchasedActions);

        boolean enableNightMode =
                isPurchasedActions
                        || Utility.wasInstalledBefore(mContext, 2019, 11, 15);
        enablePreference("category_night_mode_display_management", enableNightMode);

        showPreference("donation_category", !isPurchasedDonation);
        showPreference("purchaseDesignPackage", !isPurchasedWeather);

        showPreference("purchaseActions", !isPurchasedActions);
        showPreference("purchaseActions2", !isPurchasedActions);
        showPreference("purchaseActions3", !isPurchasedActions);
    }

    private void showPurchaseDialog() {
        PreferencesActivity activity = ((PreferencesActivity) mContext);
        activity.showPurchaseDialog();
    }

    private boolean isPurchased(String sku) {
        PreferencesActivity activity = ((PreferencesActivity) mContext);
        return activity.isPurchased(sku);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(TAG, "onCreatePreferences rootKey: " + rootKey);
        handler.removeCallbacks(runnableNotificationAccessChanged);
        this.rootKey = rootKey;

        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey("rootKey")) {
            rootKey = getArguments().getString("rootKey");
            Log.d(TAG, "onCreatePreferences getArgument: " + rootKey);
        }

        getPreferenceManager().setSharedPreferencesName(PREFS_KEY);

        if (rootKey != null) {
            switch (rootKey) {
                case "autostart":
                    setPreferencesFromResource(R.xml.preferences_autostart, rootKey);

                    if (! isIgnoringBatteryOptimization()) {
                        showPreference("startBatteryOptimization");
                    } else {
                        hidePreference("startBatteryOptimization");
                    }
                    break;
                case "clock":
                    setPreferencesFromResource(R.xml.preferences_clock, rootKey);
                    break;
                case "background":
                    setPreferencesFromResource(R.xml.preferences_background, rootKey);
                    break;
                case "rss":
                    setPreferencesFromResource(R.xml.preferences_rss, rootKey);
                    break;
                case "brightness":
                    setPreferencesFromResource(R.xml.preferences_brightness, rootKey);
                    break;
                case "behaviour":
                    setPreferencesFromResource(R.xml.preferences_behaviour, rootKey);
                    break;
                case "nightmode":
                    setPreferencesFromResource(R.xml.preferences_nightmode, rootKey);
                    break;
                case "notifications":
                    if (Build.VERSION.SDK_INT >= 18) {
                        setPreferencesFromResource(R.xml.preferences_notifications, rootKey);

                        Preference showNotificationPreference = findPreference("showNotification");
                        if (showNotificationPreference != null) {
                            handler.post(runnableNotificationAccessChanged);
                        }

                        if (showNotificationPreference != null) {

                            if (isNotificationAccessDenied()) {
                                showNotificationPreference.setSummary(getString(R.string.showNotificationsAccessNotGranted));
                                showNotificationPreference.setEnabled(false);

                                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                                builder.setTitle(getString(R.string.showNotificationsAccessNotGranted))
                                        .setMessage(getString(R.string.showNotificationsAlertText))
                                        .setPositiveButton(android.R.string.yes, null)
                                        .setIcon(android.R.drawable.ic_dialog_alert);
                                AlertDialog alertdialog = builder.create();
                                dialogs.add(alertdialog);
                                alertdialog.show();
                            } else {
                                showNotificationPreference.setSummary(getString(R.string.showNotificationsAccessGranted));
                                showNotificationPreference.setEnabled(true);
                            }
                        }
                    }
                    break;
                case "help":
                    setPreferencesFromResource(R.xml.preferences_help_feedback, rootKey);
                    break;
                case "about":
                    setPreferencesFromResource(R.xml.preferences_about, rootKey);
                    break;
                default:
                    if (getId() != R.id.right) {
                        setPreferencesFromResource(R.xml.preferences, null);
                    }
                    break;
            }
        } else {
            if (getId() != R.id.right) {
                setPreferencesFromResource(R.xml.preferences, null);
            }
        }

        initPurchasePreference("purchaseActions");
        initPurchasePreference("purchaseActions2");
        initPurchasePreference("purchaseActions3");
        initPurchasePreference("donation_play");
        initPurchasePreference("purchaseDesignPackage");
        initPurchasePreference("purchaseDesignPackageBackground");
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference != null && rootKey == null) {
            dismissSnackBar();
        }

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        init();
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        prefs.unregisterOnSharedPreferenceChangeListener(prefChangedListener);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void init() {
        Log.d(TAG, "init rootkey: " + rootKey);
        final Context context = mContext;

        togglePurchasePreferences();
        settings = new Settings(mContext);

        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        prefs.registerOnSharedPreferenceChangeListener(prefChangedListener);

        Resources res = getResources();
        boolean enabled = res.getBoolean(R.bool.use_NotificationListenerService);
        if (!enabled) {
            removePreference("startNotificationService");
            removePreference("autostartForNotifications");
        }

        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey("rootKey")) {
            rootKey = getArguments().getString("rootKey");
            Log.d(TAG, "init getArgument: " + rootKey);
        }

        if ("autostart".equals(rootKey)) {
            Preference prefHandlePower = findPreference("handle_power");
            if (prefHandlePower != null) {
                prefHandlePower.setOnPreferenceChangeListener((preference, new_value) -> {
                    boolean on = Boolean.parseBoolean(new_value.toString());
                    Utility.toggleComponentState(context, PowerConnectionReceiver.class, on);
                    return true;
                });
            }
        } else if ("background".equals(rootKey)) {
            setupBackgroundImageControls(prefs);
            Preference chooseImage = findPreference("chooseBackgroundImage");
            if (chooseImage != null) {
                chooseImage.setOnPreferenceClickListener(preference -> {
                    pickMedia.launch(
                            new PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                            .build()
                    );
                    return true;
                });
            }
            Preference chooseDirectory = findPreference("chooseDirectoryBackgroundImage");
            if (chooseDirectory != null) {
                chooseDirectory.setOnPreferenceClickListener(preference -> {
                    pickMultipleMedia.launch(new PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                            .build());
                    return true;
                });
            }
        } else if ("brightness".equals(rootKey)) {
            setupBrightnessControls(prefs);
            setupLightSensorPreferences();
        } else if ("behaviour".equals(rootKey)) {
            initUseDeviceLockPreference();
            setupDoNotDisturbPreference();
        } else if ("nightmode".equals(rootKey)) {
            setupLightSensorPreferences();
            setupNightModePreferences(prefs);

        } else if ("about".equals(rootKey)) {

            Preference tinytimetracker = findPreference("tinytimetracker");
            if (tinytimetracker != null){
                tinytimetracker.setOnPreferenceClickListener(preference -> {
                    Uri marketUri = Uri.parse("market://details?id=com.firebirdberlin.tinytimetracker");
                    Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
                    startActivity(Intent.createChooser(marketIntent, "dialogTitle"));
                    return true;
                });
            }

            Preference recommendApp = findPreference("recommendApp");
            if (recommendApp != null) {
                recommendApp.setOnPreferenceClickListener(preference -> {
                    recommendApp();
                    return true;
                });
            }

            Preference exportPreferences = findPreference("exportPreferences");
            if (exportPreferences != null) {
                exportPreferences.setOnPreferenceClickListener(preference -> {
                    ExportPreferences export = new ExportPreferences(getActivity());
                    export.execute();
                    return true;
                });
            }

            Preference uninstallApp = findPreference("uninstallApp");
            if (uninstallApp != null) {
                uninstallApp.setOnPreferenceClickListener(preference -> {
                    uninstallApplication();
                    return true;
                });
            }
            Preference resetToDefaults = findPreference("reset_to_defaults");
            if (resetToDefaults != null) {

                resetToDefaults.setOnPreferenceClickListener(preference -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle(getResources().getString(R.string.confirm_reset))
                            .setMessage(getResources().getString(R.string.confirm_reset_question))
                            .setNegativeButton(android.R.string.no, null)
                            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                                settings.clear();
                                getPreferenceScreen().removeAll();
                                WakeUpReceiver.cancelAlarm(mContext);
                                DataSource db = new DataSource(context);
                                db.open();
                                db.dropData();
                                db.close();
                                Settings.storeWeatherDataPurchase(
                                        mContext,
                                        isPurchased(BillingHelperActivity.ITEM_WEATHER_DATA),
                                        isPurchased(BillingHelperActivity.ITEM_DONATION)
                                );
                                PreferencesActivity activity = ((PreferencesActivity) mContext);
                                activity.initFragment();
                            });

                    AlertDialog alertdialog = builder.create();
                    dialogs.add(alertdialog);
                    alertdialog.show();

                    return true;
                });

            }

        } else {
            // main page
            setupDaydreamPreferences();
            setupTranslationRequest();
            setupNotificationCategory();
            setupBackgroundCategory();
        }


        if (rootKey == null || "autostart".equals(rootKey)) {
            conditionallyShowSnackBar();
        }

        if (isAdded() && Utility.isEmpty(rootKey)) {
            conditionallyShowSnackBarPurchase();
        }
    }

    private void initPurchasePreference(String key) {
        Preference purchasePreference = findPreference(key);
        if (purchasePreference != null) {
            purchasePreference.setOnPreferenceClickListener(purchasePreferenceClickListener);
        }
    }

    private void setupLightSensorPreferences() {
        if (Utility.getLightSensor(mContext) == null) {
            Log.d(TAG, "no light sensor");

            ListPreference nightModePref = findPreference("nightModeActivationMode");
            if (nightModePref != null) {
                nightModePref.setEntries(
                        new String[]{
                                getString(R.string.night_mode_activation_manual),
                                getString(R.string.night_mode_activation_scheduled),
                        }
                );
                nightModePref.setEntryValues(new String[]{"0", "2"});
            }
        }
    }

    private void setupTranslationRequest() {
        if (!Utility.languageIs("de", "en")) {
            showPreference("translations_wanted");
        }
    }

    private void setupNotificationCategory() {
        Resources res = getResources();
        boolean enabled = res.getBoolean(R.bool.use_NotificationListenerService);
        if (!enabled) {
            hidePreference("notifications");
        } else {
            showPreference("notifications");
        }
    }

    private void setupBackgroundCategory() {
        if (Utility.isLowRamDevice(mContext)) {
            hidePreference("background");
        } else {
            showPreference("background");
        }
    }

    private void recommendApp() {
        String body = "https://play.google.com/store/apps/details?id=com.firebirdberlin.nightdream";
        String subject = getResources().getString(R.string.recommend_app_subject);
        String description = getResources().getString(R.string.recommend_app_desc);
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);
        startActivity(Intent.createChooser(sharingIntent, description));
    }

    private void setupDoNotDisturbPreference() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            removePreference("activateDoNotDisturb");
        }
    }

    private void setupBrightnessControls(SharedPreferences prefs) {
        if (!isAdded()) return;
        Preference brightnessOffset = findPreference("brightness_offset");
        if (brightnessOffset == null) {
            return;
        }
        boolean hasLightSensor = (Utility.getLightSensor(mContext) != null);
        boolean on = prefs.getBoolean("autoBrightness", false) && hasLightSensor;

        showPreference("autoBrightness", hasLightSensor);
        String title = getString(R.string.brightness);
        if (on) {
            title = getString(R.string.brightness_offset);
        }
        brightnessOffset.setTitle(title);
        PreferenceScreen category = findPreference("brightness");
        if (category == null) return;

        removePreference("maxBrightnessBattery");
        removePreference("nightModeBrightnessInt");
        removePreference("maxBrightness");
        removePreference("minBrightness");
        float nightModeBrightness = prefs.getFloat("nightModeBrightness", 0.01f);
        SharedPreferences.Editor prefEditor = prefs.edit();
        if (on) {
            prefEditor.putInt("minBrightness", (int) (100 * nightModeBrightness));
            prefEditor.apply();

            InlineSeekBarPreference prefMinBrightness = new InlineSeekBarPreference(mContext);
            prefMinBrightness.setKey("minBrightness");
            prefMinBrightness.setTitle(getString(R.string.minBrightness));
            prefMinBrightness.setSummary("");
            prefMinBrightness.setRange(-100, 100);
            prefMinBrightness.setDefaultValue(0);
            prefMinBrightness.setIconSpaceReserved(false);

            InlineSeekBarPreference prefMaxBrightness = new InlineSeekBarPreference(mContext);
            prefMaxBrightness.setKey("maxBrightness");
            prefMaxBrightness.setTitle(getString(R.string.maxBrightness));
            prefMaxBrightness.setSummary("");
            prefMaxBrightness.setRange(1, 100);
            prefMaxBrightness.setDefaultValue(50);
            prefMaxBrightness.setIconSpaceReserved(false);

            category.addPreference(prefMinBrightness);
            category.addPreference(prefMaxBrightness);
        } else {
            prefEditor.putInt("nightModeBrightnessInt", (int) (100 * nightModeBrightness));
            prefEditor.apply();
            InlineSeekBarPreference prefNightModeBrightness = new InlineSeekBarPreference(mContext);
            prefNightModeBrightness.setKey("nightModeBrightnessInt");
            prefNightModeBrightness.setTitle(getString(R.string.brightness_night_mode));
            prefNightModeBrightness.setSummary("");
            prefNightModeBrightness.setRange(-100, 100);
            prefNightModeBrightness.setDefaultValue(0);
            prefNightModeBrightness.setIconSpaceReserved(false);
            category.addPreference(prefNightModeBrightness);
        }

        InlineSeekBarPreference prefMaxBrightnessBattery = new InlineSeekBarPreference(mContext);
        prefMaxBrightnessBattery.setKey("maxBrightnessBattery");
        prefMaxBrightnessBattery.setTitle(getString(R.string.maxBrightnessBattery));
        prefMaxBrightnessBattery.setSummary("");
        prefMaxBrightnessBattery.setRange(1, 100);
        prefMaxBrightnessBattery.setDefaultValue(25);
        prefMaxBrightnessBattery.setIconSpaceReserved(false);
        category.addPreference(prefMaxBrightnessBattery);
    }

    private void setupBackgroundImageControls(SharedPreferences prefs) {
        String selection = prefs.getString("backgroundMode", "1");
        showPreference("chooseBackgroundImage", "3".equals(selection));

        boolean on = "2".equals(selection);
        showPreference("gradientStartColor", on);
        showPreference("gradientEndColor", on);

        on = "4".equals(selection);
        showPreference("backgroundImageDuration", on);
        showPreference("backgroundImageZoomIn", on);
        showPreference("backgroundImageFadeIn", on);
        showPreference("backgroundImageMoveIn", on);
        showPreference("backgroundMovein", on);
        showPreference("backgroundImageFilter", on);
        showPreference("backgroundEXIF", on);
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            showPreference("chooseDirectoryBackgroundImage", on);
        }

        on = Utility.equalsAny(selection, "3", "4");
        showPreference("autoAccentColor", on);
        showPreference("slideshowStyle", on);
        showPreference("hideBackgroundImage", !"1".equals(selection));
        showPreference("clockBackgroundTransparency", !"1".equals(selection));

        boolean isPurchasedWeather = isPurchased(BillingHelperActivity.ITEM_WEATHER_DATA);
        showPreference("purchaseDesignPackageBackground", "4".equals(selection) && !isPurchasedWeather);
        boolean shallEnable = !"4".equals(selection) || isPurchasedWeather;
        enablePreference("autoAccentColor", shallEnable);
        enablePreference("backgroundEXIF", shallEnable);
        enablePreference("backgroundImageDuration", shallEnable);
        enablePreference("backgroundImageFadeIn", shallEnable);
        enablePreference("backgroundImageFilter", shallEnable);
        enablePreference("backgroundImageMoveIn", shallEnable);
        enablePreference("backgroundImageZoomIn", shallEnable);
        enablePreference("backgroundMovein", shallEnable);
        enablePreference("chooseDirectoryBackgroundImage", shallEnable);
        enablePreference("hideBackgroundImage", shallEnable);
        enablePreference("slideshowStyle", shallEnable);
    }

    private void setupNightModePreferences(SharedPreferences prefs) {
        int nightModeActivationMode = Integer.parseInt(prefs.getString("nightModeActivationMode", "1"));
        Log.i(TAG, "setupNightModePreferences " + nightModeActivationMode);
        enablePreference(
                "nightmode_timerange",
                nightModeActivationMode == Settings.NIGHT_MODE_ACTIVATION_SCHEDULED
        );
    }

    private void initUseDeviceLockPreference() {
        SwitchPreferenceCompat pref = findPreference("useDeviceLock");
        if (pref == null) {
            return;
        }

        DevicePolicyManager mgr = (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName cn = new ComponentName(mContext, AdminReceiver.class);
        if (pref.isChecked() && !mgr.isAdminActive(cn)) {
            pref.setChecked(false);
        }
    }

    private void setupStandByService(SharedPreferences sharedPreferences) {
        Log.d(TAG, "setupStandByService()");
        if (!isAdded()) return;
        boolean on = isAutostartActivated(sharedPreferences);
        int newState = on ?
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

        if (!on) {
            ScreenWatcherService.stop(mContext);
        }

        PackageManager pm = mContext.getPackageManager();
        pm.setComponentEnabledSetting(
                new ComponentName(mContext, ScreenWatcherService.class),
                newState,
                PackageManager.DONT_KILL_APP
        );

        if (on) {
            ScreenWatcherService.start(mContext);
        }
        conditionallyShowSnackBar();
    }

    private boolean isAutostartActivated(SharedPreferences sharedPreferences) {
        if (Build.VERSION.SDK_INT >= 29 && Utility.isLowRamDevice(mContext)) return false;

        return (
                sharedPreferences.getBoolean("handle_power", false)
                        || sharedPreferences.getBoolean("standbyEnabledWhileDisconnected", false)
                        || sharedPreferences.getBoolean("scheduledAutoStartEnabled", false)
                        || sharedPreferences.getBoolean("autostartForNotifications", false)
        );
    }

    private boolean isIgnoringBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            return (powerManager.isIgnoringBatteryOptimizations(mContext.getPackageName()));
        }

        return true;
    }

    private void setupNotificationAccessPermission(SharedPreferences sharedPreferences, String preferenceKey) {
        if (!isAdded()) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
        boolean on = sharedPreferences.getBoolean(preferenceKey, false);
        NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (on && !notificationManager.isNotificationPolicyAccessGranted()) {

            Intent intent =
                    new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);

            startActivity(intent);
        }
    }

    private void setupDeviceAdministratorPermissions(SharedPreferences sharedPreferences) {
        if (!isAdded()) return;
        boolean on = sharedPreferences.getBoolean("useDeviceLock", false);
        if (on) {
            DevicePolicyManager mgr = (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName cn = new ComponentName(mContext, AdminReceiver.class);
            if (!mgr.isAdminActive(cn)) {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, cn);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        getString(R.string.useDeviceLockExplanation));
                startActivity(intent);
            }

        } else {
            removeActiveAdmin();
        }
    }

    private void removeActiveAdmin() {
        DevicePolicyManager mgr = (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName cn = new ComponentName(mContext, AdminReceiver.class);
        if (mgr.isAdminActive(cn)) {
            mgr.removeActiveAdmin(cn);
        }

    }

    private void uninstallApplication() {
        removeActiveAdmin();
        Uri packageURI = Uri.parse("package:" + NightDreamActivity.class.getPackage().getName());
        Intent intent = new Intent(Intent.ACTION_DELETE, packageURI);
        startActivity(intent);

    }

    private void resetScaleFactor(SharedPreferences sharedPreferences) {
        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        // reset custom scaling
        prefEditor.putFloat("scaleClockLandscape", -1.f);
        prefEditor.putFloat("scaleClockPortrait", -1.f);
        prefEditor.putFloat("scaleClock", -1.f);
        // reset custom translation
        prefEditor.remove("xPosition");
        prefEditor.remove("xPositionLandscape");
        prefEditor.remove("xPositionPortrait");
        prefEditor.remove("yPosition");
        prefEditor.remove("yPositionLandscape");
        prefEditor.remove("yPositionPortrait");
        prefEditor.apply();
    }

    private void enablePreference(String key, boolean on) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setEnabled(on);
        } else {
            Log.w(TAG, "WARNING: preference " + key + " not found.");
        }
    }

    private void showPreference(String key, boolean visible) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setVisible(visible);
        }
    }

    private void showPreference(String key) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setVisible(true);
        }
    }

    private void hidePreference(String key) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setVisible(false);
        }
    }

    private void removePreference(String key) {
        Preference preference = findPreference(key);
        removePreference(preference);
    }

    private void removePreference(Preference preference) {
        PreferenceGroup parent = getParent(getPreferenceScreen(), preference);
        if (parent != null) {
            parent.removePreference(preference);
        }
    }

    private PreferenceGroup getParent(PreferenceGroup root, Preference preference) {
        for (int i = 0; i < root.getPreferenceCount(); i++) {
            Preference p = root.getPreference(i);
            if (p == preference) {
                return root;
            }
            if (p instanceof PreferenceGroup) {
                PreferenceGroup parent = getParent((PreferenceGroup) p, preference);
                if (parent != null) {
                    return parent;
                }
            }
        }
        return null;
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof ColorPreference) {
            ColorPreference cp = (ColorPreference) preference;
            cp.showDialog(this, 0);
        } else super.onDisplayPreferenceDialog(preference);
    }

    private void setupDaydreamPreferences() {
        if (!isAdded()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 29 && Utility.isLowRamDevice(mContext)) {
            hidePreference("autostart");
        }

        enablePreference("autostart", !Utility.isConfiguredAsDaydream(mContext));
        Preference pref = findPreference("autostart");
        if (pref == null) {
            return;
        }
        boolean on = pref.isEnabled();
        String summary = on ? "" : getString(R.string.autostart_message_disabled);
        pref.setSummary(summary);
    }

    private void conditionallyShowSnackBar() {
        if (!Utility.hasPermissionCanDrawOverlays(mContext)) {
            View view = getActivity().findViewById(android.R.id.content);
            snackbar = Snackbar.make(view, R.string.permission_request_overlays, Snackbar.LENGTH_INDEFINITE);
            int color = Utility.getRandomMaterialColor(mContext);
            int textColor = Utility.getContrastColor(color);
            View snackbarView = snackbar.getView();
            snackbarView.setBackgroundColor(color);
            snackbar.setActionTextColor(textColor);

            TextView tv = snackbarView.findViewById(R.id.snackbar_text);
            tv.setTextColor(textColor);

            snackbar.setAction(android.R.string.ok, new CanDrawOverlaysPermissionListener());
            snackbar.show();
        } else {
            dismissSnackBar();
        }
    }

    private void conditionallyShowSnackBarPurchase() {
        SharedPreferences settings = mContext.getSharedPreferences(PREFS_KEY, 0);
        Log.d(TAG, "days: " + Utility.getDaysSinceFirstInstall(mContext));
        long lastTimeShown = settings.getLong("purchaseSnackBarTimestamp", 0L);
        long timeSinceShown = System.currentTimeMillis() - lastTimeShown;
        Log.i(TAG, "timeSinceShown: " + Utility.getDaysSinceFirstInstall(mContext));
        long daysInstalled = Utility.getDaysSinceFirstInstall(mContext);
        if (
                (snackbar != null && snackbar.isShown())
                        || Utility.isAirplaneModeOn(mContext)
                        || isPurchased(BillingHelperActivity.ITEM_DONATION) || isPurchased(BillingHelperActivity.ITEM_PRO)
                        || (isPurchased(BillingHelperActivity.ITEM_WEB_RADIO) && isPurchased(BillingHelperActivity.ITEM_WEATHER_DATA))
                        || daysInstalled < 4
                        || timeSinceShown < 60000 * 60 * 12 // 12 hours
        ) {
            return;
        }
        View view = getActivity().findViewById(android.R.id.content);
        snackbar = Snackbar.make(view, R.string.buy_upgrade_summary, Snackbar.LENGTH_INDEFINITE);
        int color = Utility.getRandomMaterialColor(mContext);
        int textColor = Utility.getContrastColor(color);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(color);
        snackbar.setActionTextColor(textColor);

        TextView tv = snackbarView.findViewById(R.id.snackbar_text);
        tv.setTextColor(textColor);

        snackbar.setAction(android.R.string.ok, new BuyUpgradeListener());
        snackbar.setDuration(10000);
        snackbar.show();
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putLong("purchaseSnackBarTimestamp", System.currentTimeMillis());
        prefEditor.apply();
    }

    void dismissSnackBar() {
        if (snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
            snackbar = null;
        }
    }

    public void closeAlertDialogs() {
        for (AlertDialog dialog : dialogs)
            if ((dialog != null) && dialog.isShowing()) {
                dialog.dismiss();
            }
    }

    private class DaydreamSettingsObserver extends ContentObserver {
        DaydreamSettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            setupDaydreamPreferences();
        }
    }

    public class CanDrawOverlaysPermissionListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (isAdded()) {
                Utility.requestPermissionCanDrawOverlays(mContext);
            }
        }
    }

    public class BuyUpgradeListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (isAdded()) {
                PreferencesActivity activity = ((PreferencesActivity) mContext);
                activity.launchBillingFlow(BillingHelperActivity.ITEM_PRO);

            }
        }
    }
}
