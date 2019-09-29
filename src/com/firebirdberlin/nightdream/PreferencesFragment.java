package com.firebirdberlin.nightdream;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.SwitchPreferenceCompat;

import com.android.vending.billing.IInAppBillingService;
import com.firebirdberlin.nightdream.receivers.PowerConnectionReceiver;
import com.firebirdberlin.nightdream.receivers.WakeUpReceiver;
import com.firebirdberlin.nightdream.services.ScreenWatcherService;
import com.firebirdberlin.nightdream.ui.ClockLayoutPreviewPreference;
import com.firebirdberlin.nightdream.widget.ClockWidgetProvider;
import com.firebirdberlin.openweathermapapi.CityIDPreference;
import com.firebirdberlin.openweathermapapi.CityIdDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import de.firebirdberlin.preference.InlineSeekBarPreference;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class PreferencesFragment extends PreferenceFragmentCompat {
    public static final String TAG = "PreferencesFragment";
    public static final String ITEM_DONATION = "donation";
    public static final String ITEM_WEATHER_DATA = "weather_data";
    public static final String ITEM_WEB_RADIO = "web_radio";
    public static final String ITEM_PRO = "pro";
    public static final String ITEM_ACTIONS = "actions";
    public static final int REQUEST_CODE_PURCHASE_DONATION = 1001;
    public static final int REQUEST_CODE_PURCHASE_WEATHER = 1002;
    public static final int REQUEST_CODE_PURCHASE_WEB_RADIO = 1003;
    public static final int REQUEST_CODE_PURCHASE_PRO = 1004;
    public static final int REQUEST_CODE_PURCHASE_ACTIONS = 1005;
    public static final String PREFS_KEY = "NightDream preferences";
    private static final int PRODUCT_ID_WEATHER_DATA = 0;
    private static final int PRODUCT_ID_WEB_RADIO = 1;
    private static final int PRODUCT_ID_DONATION = 2;
    private static final int PRODUCT_ID_PRO = 3;
    private static final int PRODUCT_ID_ACTIONS = 4;
    private static int RESULT_LOAD_IMAGE = 1;
    private static int RESULT_LOAD_IMAGE_KITKAT = 4;
    private final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2;
    private final int PERMISSIONS_REQUEST_RECORD_AUDIO = 3;
    private final int PERMISSIONS_REQUEST_ACCESS_LOCATION = 5;
    public boolean purchased_donation = false;
    public boolean purchased_weather_data = false;
    public boolean purchased_web_radio = false;
    public boolean purchased_pro = false;
    public boolean purchased_actions = false;
    Snackbar snackbar;
    String rootKey;
    DaydreamSettingsObserver daydreamSettingsObserver = null;
    IInAppBillingService mService;
    Preference.OnPreferenceClickListener purchasePreferenceClickListener =
            new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    showPurchaseDialog();
                    return true;
                }
            };
    Preference.OnPreferenceChangeListener recordAudioPrefChangeListener =
            new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object new_value) {
                    boolean on = Boolean.parseBoolean(new_value.toString());
                    if (on && !hasPermission(Manifest.permission.RECORD_AUDIO)) {
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                                PERMISSIONS_REQUEST_RECORD_AUDIO);
                    }
                    return true;
                }
            };
    Preference.OnPreferenceChangeListener fetchWeatherDataPrefChangeListener =
            new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object new_value) {
                    boolean on = Boolean.parseBoolean(new_value.toString());
                    if (on && !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSIONS_REQUEST_ACCESS_LOCATION);
                    }
                    return true;
                }
            };
    private Settings settings = null;
    private Context mContext = null;
    SharedPreferences.OnSharedPreferenceChangeListener prefChangedListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if ("appearance".equals(rootKey)) {
                        ClockLayoutPreviewPreference preview = findPreference("clockLayoutPreview");
                        if (preview != null) {
                            preview.invalidate();
                        }
                    }
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
                            settings.clockLayout = Integer.parseInt(sharedPreferences.getString("clockLayout", "0"));
                            setupClockLayoutPreference();
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
                        case "useInternalAlarm":
                            setupAlarmClock(sharedPreferences);
                            break;
                        case "Night.muteRinger":
                            setupNotificationAccessPermission(sharedPreferences, "Night.muteRinger");
                            break;
                        case "activateDoNotDisturb":
                            setupNotificationAccessPermission(sharedPreferences, "activateDoNotDisturb");
                            break;
                        case "batteryTimeout":
                            settings.batteryTimeout = Integer.parseInt(sharedPreferences.getString("batteryTimeout", "-1"));
                            setupBatteryTimeoutPreference();
                            break;
                        case "weatherProvider":
                            setupWeatherProviderPreference();
                            break;
                    }

                    Log.i(TAG, "prefChangedListener called");

                    // update all widgets via intent, so the are repainted with current settings
                    ClockWidgetProvider.updateAllWidgets(mContext);
                }
            };

    private boolean shallShowPurchaseDialog = false;
    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            Log.i(TAG, "IIAB service connected");
            mService = IInAppBillingService.Stub.asInterface(service);
            getPurchases();
            if (shallShowPurchaseDialog) {
                showPurchaseDialog();
                shallShowPurchaseDialog = false;
            }
        }
    };

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        dismissSnackBar();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // bind the in-app billing service
        Intent serviceIntent =
                new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        getActivity().bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
    }


    @Override
    public void onStart() {
        super.onStart();

        // InAppBillingService service doesnt seem to be available in emulator
        activatePurchasesIfDebuggable();
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

    public void setShowPurchaseDialog() {
        this.shallShowPurchaseDialog = true;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        // unbind the in-app billing service
        if (mService != null) {
            getActivity().unbindService(mServiceConn);
        }

        try {
            mContext.getContentResolver().unregisterContentObserver(daydreamSettingsObserver);
        } catch (IllegalArgumentException | NullPointerException ignored) {

        }
    }

    private void getPurchases() {
        if (Utility.isEmulator()) {
            activatePurchasesIfDebuggable();
            return;
        }
        if (mService == null || getActivity() == null) {
            return;
        }

        Bundle ownedItems;
        try {
            ownedItems = mService.getPurchases(3, getActivity().getPackageName(), "inapp", null);
        } catch (RemoteException e) {
            return;
        }

        if (ownedItems == null) {
            purchased_web_radio = false;
            purchased_actions = false;
            purchased_weather_data = false;
            storeWeatherDataPurchase(false, false);
            return;
        }

        int response = ownedItems.getInt("RESPONSE_CODE");
        if (response == 0) {
            ArrayList<String> ownedSkus =
                    ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
            ArrayList<String> purchaseDataList =
                    ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
            ArrayList<String> signatureList =
                    ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
            String continuationToken = ownedItems.getString("INAPP_CONTINUATION_TOKEN");

            boolean weatherDataIsPurchased = false;
            for (int i = 0; i < purchaseDataList.size(); ++i) {
                String purchaseData = purchaseDataList.get(i);
                String signature = signatureList.get(i);
                String sku = ownedSkus.get(i);

                if (ITEM_DONATION.equals(sku)) {
                    purchased_donation = true;
                    weatherDataIsPurchased = true;
                    purchased_web_radio = true;
                    purchased_actions = true;
                }
                if (ITEM_PRO.equals(sku)) {
                    purchased_pro = true;
                    purchased_web_radio = true;
                    weatherDataIsPurchased = true;
                    purchased_actions = true;
                }
                if (ITEM_WEATHER_DATA.equals(sku)) {
                    weatherDataIsPurchased = true;
                }
                if (ITEM_WEB_RADIO.equals(sku)) {
                    purchased_web_radio = true;
                }
                if (ITEM_ACTIONS.equals(sku)) {
                    purchased_actions = true;
                }

                // do something with this purchase information
                // e.g. display the updated list of products owned by user
                // or consume the purchase
//                try {
//                    JSONObject o = new JSONObject(purchaseData);
//                    String purchaseToken = o.getString("purchaseToken");
//                    mService.consumePurchase(3, getActivity().getPackageName(), purchaseToken);
//                }
//                catch (Exception e) {
//                    e.printStackTrace();
//                }
            }

            storeWeatherDataPurchase(weatherDataIsPurchased, purchased_donation);
            resetAlwaysOnModeIfNotPurchased();
            resetUseDeviceLockIfNotPurchased();
            togglePurchasePreferences();

            // if continuationToken != null, call getPurchases again
            // and pass in the token to retrieve more items
        }

    }

    private void resetAlwaysOnModeIfNotPurchased() {
        if (purchased_actions) {
            return;
        }
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("standbyEnabledWhileDisconnected", false);
        editor.putBoolean("scheduledAutoStartEnabled", false);
        editor.apply();
    }

    private void resetUseDeviceLockIfNotPurchased() {
        if (purchased_actions) {
            return;
        }
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("useDeviceLock", false);
        editor.apply();
        setupDeviceAdministratorPermissions(prefs);
    }

    private void storeWeatherDataPurchase(boolean weatherIsPurchased, boolean donationIsPurchased) {
        purchased_weather_data = weatherIsPurchased;
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("purchasedWeatherData", weatherIsPurchased);
        editor.putBoolean("purchasedDonation", donationIsPurchased);
        editor.apply();
        Log.i(TAG, String.format("purchasedWeatherData = %b", weatherIsPurchased));
    }


    private void togglePurchasePreferences() {
        enablePreference("showWeather", purchased_weather_data);
        enablePreference("expert_screen", purchased_actions);
        enablePreference("scheduled_autostart_screen", purchased_actions);
        enablePreference("activateDoNotDisturb", purchased_actions);
        enablePreference("useDeviceLock", purchased_actions);

        if (!purchased_donation) {
            showPreference("donation_category");
        }

        if (!purchased_weather_data) {
            showPreference("purchaseWeatherData");
            showPreference("purchaseDesignPackage");
        }

        if (!purchased_actions) {
            showPreference("purchaseActions");
            showPreference("purchaseActions2");
        }
    }

    public void purchaseIntent(String sku, int REQUEST_CODE) {
        if (mService == null) return;
        try {
            String developerPayload = "abcdefghijklmnopqrstuvwxyz";
            Bundle buyIntentBundle = mService.getBuyIntent(
                    3, getActivity().getPackageName(),
                    sku, "inapp", developerPayload
            );
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            getActivity().startIntentSenderForResult(pendingIntent.getIntentSender(),
                    REQUEST_CODE, new Intent(), 0, 0, 0);
        } catch (RemoteException | SendIntentException ignored) {
        }
    }


    public void showPurchaseDialog() {
        Log.i(TAG, "showPurchaseDialog()");
        if (purchased_donation) return;
        List<CharSequence> entries = new ArrayList<>();
        final List<Integer> values = new ArrayList<>();
        HashMap<String, String> prices = getPrices();

        if (!purchased_pro && !purchased_weather_data && !purchased_web_radio) {
            entries.add(getProductWithPrice(prices, R.string.product_name_pro, ITEM_PRO));
            values.add(PRODUCT_ID_PRO);
        }

        if (!purchased_weather_data) {
            entries.add(getProductWithPrice(prices, R.string.product_name_weather, ITEM_WEATHER_DATA));
            values.add(PRODUCT_ID_WEATHER_DATA);
        }

        if (!purchased_web_radio) {
            entries.add(getProductWithPrice(prices, R.string.product_name_webradio, ITEM_WEB_RADIO));
            values.add(PRODUCT_ID_WEB_RADIO);
        }

        if (!purchased_actions) {
            entries.add(getProductWithPrice(prices, R.string.product_name_actions, ITEM_ACTIONS));
            values.add(PRODUCT_ID_ACTIONS);
        }

        if (!purchased_donation) {
            entries.add(getProductWithPrice(prices, R.string.product_name_donation, ITEM_DONATION));
            values.add(PRODUCT_ID_DONATION);
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.buy))
                .setItems(
                        entries.toArray(new CharSequence[entries.size()]),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                Log.i(TAG, String.format("selected %d", which));
                                int selected = values.get(which);
                                switch (selected) {
                                    case PRODUCT_ID_WEATHER_DATA:
                                        purchaseIntent(ITEM_WEATHER_DATA, REQUEST_CODE_PURCHASE_WEATHER);
                                        break;
                                    case PRODUCT_ID_WEB_RADIO:
                                        purchaseIntent(ITEM_WEB_RADIO, REQUEST_CODE_PURCHASE_WEB_RADIO);
                                        break;
                                    case PRODUCT_ID_DONATION:
                                        purchaseIntent(ITEM_DONATION, REQUEST_CODE_PURCHASE_DONATION);
                                        break;
                                    case PRODUCT_ID_PRO:
                                        purchaseIntent(ITEM_PRO, REQUEST_CODE_PURCHASE_PRO);
                                        break;
                                    case PRODUCT_ID_ACTIONS:
                                        purchaseIntent(ITEM_ACTIONS, REQUEST_CODE_PURCHASE_ACTIONS);
                                        break;
                                }
                            }
                        })
                .setNeutralButton(android.R.string.cancel, null)
                .show();
    }

    private String getProductWithPrice(HashMap<String, String> prices, int resId, String sku) {
        String price = prices.get(sku);
        if (price != null) {
            return String.format("%s (%s)", getResources().getString(resId), price);
        }
        return getResources().getString(resId);
    }

    private HashMap<String, String> getPrices() {
        HashMap<String, String> map = new HashMap<>();
        if (mService == null) return map;

        ArrayList<String> skuList = new ArrayList<String>();
        skuList.add(ITEM_WEATHER_DATA);
        skuList.add(ITEM_WEB_RADIO);
        skuList.add(ITEM_ACTIONS);
        skuList.add(ITEM_DONATION);
        skuList.add(ITEM_PRO);
        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList("ITEM_ID_LIST", skuList);

        Bundle skuDetails;
        try {
            skuDetails = mService.getSkuDetails(3, getActivity().getPackageName(), "inapp", querySkus);
        } catch (RemoteException e) {
            e.printStackTrace();
            return map;
        }
        final int BILLING_RESPONSE_RESULT_OK = 0;
        int response = skuDetails.getInt("RESPONSE_CODE");
        if (response == BILLING_RESPONSE_RESULT_OK) {
            ArrayList<String> responseList
                    = skuDetails.getStringArrayList("DETAILS_LIST");

            for (String thisResponse : responseList) {
                try {
                    JSONObject object = new JSONObject(thisResponse);
                    String sku = object.getString("productId");
                    String price = object.getString("price");
                    map.put(sku, price);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return map;
    }

    public void showThankYouDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.dialog_title_thank_you))
                .setMessage(R.string.dialog_message_thank_you)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d("Prefs", "rootKey " + rootKey);
        this.rootKey = rootKey;
        getPreferenceManager().setSharedPreferencesName(PREFS_KEY);
        if ("autostart".equals(rootKey)) {
            setPreferencesFromResource(R.xml.preferences_autostart, rootKey);
        } else if ("appearance".equals(rootKey)) {
            setPreferencesFromResource(R.xml.preferences_appearance, rootKey);
        } else if ("behaviour".equals(rootKey)) {
            setPreferencesFromResource(R.xml.preferences_behaviour, rootKey);
        } else if ("nightmode".equals(rootKey)) {
            setPreferencesFromResource(R.xml.preferences_nightmode, rootKey);
        } else if ("weather".equals(rootKey)) {
            setPreferencesFromResource(R.xml.preferences_weather, rootKey);
        } else if ("alarms".equals(rootKey)) {
            setPreferencesFromResource(R.xml.preferences_alarms, rootKey);
        } else if ("about".equals(rootKey)) {
            setPreferencesFromResource(R.xml.preferences_about, rootKey);
        } else {
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }

        initPurchasePreference("purchaseActions");
        initPurchasePreference("donation_play");
        initPurchasePreference("purchaseWeatherData");
        initPurchasePreference("purchaseDesignPackage");
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if ( preference != null && preference.getKey().equals("autostart")) {
            dismissSnackBar();
        }

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        final Context context = mContext;

        settings = new Settings(mContext);

        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        prefs.registerOnSharedPreferenceChangeListener(prefChangedListener);

        if (Build.VERSION.SDK_INT < 18) {
            removePreference("startNotificationService");
            removePreference("autostartForNotifications");
        }


        if ("autostart".equals(rootKey)) {
            Preference prefHandlePower = findPreference("handle_power");
            prefHandlePower.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object new_value) {
                    boolean on = Boolean.parseBoolean(new_value.toString());
                    Utility.toggleComponentState(context, PowerConnectionReceiver.class, on);
                    return true;
                }
            });

            setupBatteryTimeoutPreference();
        } else if ("appearance".equals(rootKey)) {
            setupBrightnessControls(prefs);
            setupBackgroundImageControls(prefs);
            Preference chooseImage = findPreference("chooseBackgroundImage");
            chooseImage.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    checkPermissionAndSelectBackgroundImage();
                    return true;
                }
            });
            setupLightSensorPreferences();
            setupClockLayoutPreference();
        } else if ("behaviour".equals(rootKey)) {
            initUseDeviceLockPreference();
            setupDoNotDisturbPreference();
        } else if ("nightmode".equals(rootKey)) {
            setupNightModePreferences(prefs);
            Preference prefAmbientNoiseDetection = findPreference("ambientNoiseDetection");
            Preference prefAmbientNoiseReactivation = findPreference("reactivate_screen_on_noise");

            prefAmbientNoiseDetection.setOnPreferenceChangeListener(recordAudioPrefChangeListener);
            prefAmbientNoiseReactivation.setOnPreferenceChangeListener(recordAudioPrefChangeListener);
        } else if ("alarms".equals(rootKey)) {

            setupAlarmClockPreferences();
        } else if ("weather".equals(rootKey)) {

            Preference prefFetchWeatherData = findPreference("showWeather");
            prefFetchWeatherData.setOnPreferenceChangeListener(fetchWeatherDataPrefChangeListener);
            setupWeatherProviderPreference();
        } else if ("about".equals(rootKey)) {

            Preference recommendApp = findPreference("recommendApp");
            if (recommendApp != null) {
                recommendApp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        recommendApp();
                        return true;
                    }
                });
            }
            Preference uninstallApp = findPreference("uninstallApp");
            if (uninstallApp != null) {
                uninstallApp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        uninstallApplication();
                        return true;
                    }
                });
            }
            Preference resetToDefaults = findPreference("reset_to_defaults");
            if (resetToDefaults != null) {

                resetToDefaults.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        new AlertDialog.Builder(mContext)
                                .setTitle(getResources().getString(R.string.confirm_reset))
                                .setMessage(getResources().getString(R.string.confirm_reset_question))
                                .setNegativeButton(android.R.string.no, null)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        settings.clear();
                                        getPreferenceScreen().removeAll();
                                        WakeUpReceiver.cancelAlarm(mContext);
                                        DataSource db = new DataSource(context);
                                        db.open();
                                        db.dropData();
                                        db.close();
                                        addPreferencesFromResource(R.xml.preferences);
                                        init();
                                        storeWeatherDataPurchase(purchased_weather_data, purchased_donation);
                                        togglePurchasePreferences();
                                    }
                                }).show();

                        return true;
                    }
                });

            }

        } else {
            // main page
            setupDaydreamPreferences();
            setupTranslationRequest();
        }

        if (rootKey == null || "autostart".equals(rootKey) ) {
            conditionallyShowSnackBar(null);
        }
        if (rootKey == null) {
            conditionallyShowSnackBarPurchase(null);
        }
    }

    void initPurchasePreference(String key) {
        Preference purchasePreference = findPreference(key);
        if (purchasePreference != null) {
            purchasePreference.setOnPreferenceClickListener(purchasePreferenceClickListener);
        }
    }

    private void setupLightSensorPreferences() {
        if ( Utility.getLightSensor(mContext) == null ) {
            Log.d(TAG, "no light sensor");

            removePreference("category_brightness");
            removePreference("reactivate_on_ambient_light_value");

            ListPreference nightModePref = findPreference("nightModeActivationMode");
            nightModePref.setEntries(new String[]{
                                        getString(R.string.night_mode_activation_manual),
                                        getString(R.string.night_mode_activation_scheduled),
            });
            nightModePref.setEntryValues(new String[]{"0", "2"});
        }
    }

    private void setupTranslationRequest() {
        if (!Utility.languageIs("de", "en")) {
            showPreference("translations_wanted");
        }
    }

    private void setupAlarmClockPreferences() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            showPreference("radioStreamActivateWiFi");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == Activity.RESULT_OK && null != data){

            Uri selectedImage = data.getData();
            String picturePath = getRealPathFromURI(selectedImage);
            if (picturePath != null){
                settings.setBackgroundImage(picturePath);
            } else {
                Toast.makeText(getActivity(), "Could not locate image !", Toast.LENGTH_LONG).show();
            }
        }
        else
        if (requestCode == RESULT_LOAD_IMAGE_KITKAT && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            settings.setBackgroundImageURI(uri.toString());
        }
        else
        if (resultCode == Activity.RESULT_OK &&
                (requestCode == REQUEST_CODE_PURCHASE_DONATION ||
                        requestCode == REQUEST_CODE_PURCHASE_PRO ||
                    requestCode == REQUEST_CODE_PURCHASE_WEATHER ||
                        requestCode == REQUEST_CODE_PURCHASE_WEB_RADIO ||
                        requestCode == REQUEST_CODE_PURCHASE_ACTIONS)) {
            Log.i(TAG, "Purchase request for " + String.valueOf(requestCode));
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
            Log.i(TAG, purchaseData);

            try {
                JSONObject jo = new JSONObject(purchaseData);
                String sku = jo.getString("productId");
                if (ITEM_DONATION.equals(sku)) {
                    purchased_donation = true;
                    purchased_actions = true;
                    purchased_web_radio = true;
                    storeWeatherDataPurchase(true, true);
                    showThankYouDialog();
                } else if (ITEM_PRO.equals(sku)) {
                    purchased_pro = true;
                    purchased_actions = true;
                    purchased_web_radio = true;
                    storeWeatherDataPurchase(true, purchased_donation);
                    showThankYouDialog();
                } else if (ITEM_WEATHER_DATA.equals(sku)) {
                    storeWeatherDataPurchase(true, purchased_donation);
                } else if (ITEM_WEB_RADIO.equals(sku)) {
                    purchased_web_radio = true;
                } else if (ITEM_ACTIONS.equals(sku)) {
                    purchased_actions = true;
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
            togglePurchasePreferences();
        }
    }

    public String getRealPathFromURI(Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = getActivity().getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index;
            if (cursor != null) {
                column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private void checkPermissionAndSelectBackgroundImage() {
        if ( ! hasPermissionReadExternalStorage() ) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            return;
        }
        selectBackgroundImage();
    }

    private void selectBackgroundImage() {
        if (Build.VERSION.SDK_INT < 19 ) {
            Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
            getIntent.setType("image/*");

            //Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            //pickIntent.setType("image/*");
            String msg = getString(R.string.background_image_select);
            Intent chooserIntent = Intent.createChooser(getIntent, msg);
            //chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

            startActivityForResult(chooserIntent, RESULT_LOAD_IMAGE);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, RESULT_LOAD_IMAGE_KITKAT);
        }
    }

    private boolean hasPermissionReadExternalStorage() {
        return Build.VERSION.SDK_INT < 23 || (getActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    private boolean hasPermission(String permission) {
        return Build.VERSION.SDK_INT < 23 || (getActivity().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectBackgroundImage();
                } else {
                    Toast.makeText(getActivity(), "Permission denied !", Toast.LENGTH_LONG).show();
                }
                return;
            }
            case PERMISSIONS_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    settings.setReactivateScreenOnNoise(false);
                    settings.setUseAmbientNoiseDetection(false);

                    SwitchPreferenceCompat prefAmbientNoiseDetection = findPreference("ambientNoiseDetection");
                    CheckBoxPreference prefAmbientNoiseReactivation = findPreference("reactivate_screen_on_noise");
                    prefAmbientNoiseDetection.setChecked(false);
                    prefAmbientNoiseReactivation.setChecked(false);
                    Toast.makeText(getActivity(), "Permission denied !", Toast.LENGTH_LONG).show();
                }
                return;
            }
            case PERMISSIONS_REQUEST_ACCESS_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    settings.setFetchWeatherData(false);

                    SwitchPreferenceCompat prefShowWeather = findPreference("showWeather");
                    prefShowWeather.setChecked(false);
                    Toast.makeText(getActivity(), "Permission denied !", Toast.LENGTH_LONG).show();
                }
                return;
            }
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
    private void setupClockLayoutPreference() {

        Preference pref = findPreference("clockLayout");
        if (!isAdded() || pref == null) {
            return;
        }
        String[] valueArray = getResources().getStringArray(R.array.clockLayoutValues);
        String[] stringArray = getResources().getStringArray(R.array.clockLayout);
        for (int i=0; i< valueArray.length; i++) {
            String v = valueArray[i];
            if (Integer.parseInt(v) == settings.clockLayout) {
                pref.setSummary(stringArray[i]);
                return;
            }
        }
    }

    private void setupBatteryTimeoutPreference() {
        Preference pref = findPreference("batteryTimeout");
        if (!isAdded() || pref == null) {
            return;
        }
        String[] valueArray = getResources().getStringArray(R.array.batteryTimeoutValues);
        String[] stringArray = getResources().getStringArray(R.array.batteryTimeout);
        for (int i=0; i< valueArray.length; i++) {
            String v = valueArray[i];
            if (Integer.parseInt(v) == settings.batteryTimeout) {
                pref.setSummary(stringArray[i]);
                return;
            }
        }
    }

    private void setupWeatherProviderPreference() {
        Preference pref = findPreference("weatherProvider");
        if (!isAdded() || pref == null) {
            return;
        }
        Preference prefAttribution = findPreference("weatherProviderAttribution");
        if (settings.getWeatherProviderString().equals("0")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://openweathermap.org"));
            prefAttribution.setIntent(intent);
            prefAttribution.setTitle("Powered by OpenWeatherMap");
            prefAttribution.setSummary("https://openweathermap.org");
        }
        else if (settings.getWeatherProviderString().equals("1")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://darksky.net/poweredby/"));
            prefAttribution.setIntent(intent);
            prefAttribution.setTitle("Powered by Dark Sky");
            prefAttribution.setSummary("https://darksky.net/poweredby/");
        }

        String[] valueArray = getResources().getStringArray(R.array.weatherProviderValues);
        String[] stringArray = getResources().getStringArray(R.array.weatherProvider);
        for (int i=0; i< valueArray.length; i++) {
            String v = valueArray[i];

            if (v.equals(settings.getWeatherProviderString())) {
                pref.setSummary(stringArray[i]);
                return;
            }
        }
    }

    private void setupBrightnessControls(SharedPreferences prefs) {
        if (!isAdded() ) return;
        Preference brightnessOffset = findPreference("brightness_offset");
        if (brightnessOffset == null) {
            return;
        }
        boolean on = prefs.getBoolean("autoBrightness", false);
        String title = getString(R.string.brightness);
        if (on) {
            title = getString(R.string.brightness_offset);
        }
        brightnessOffset.setTitle(title);
        PreferenceCategory category = findPreference("category_brightness");

        removePreference("maxBrightnessBattery");
        removePreference("nightModeBrightnessInt");
        if (on) {
            float nightModeBrightness = prefs.getFloat("nightModeBrightness", 0.01f);
            SharedPreferences.Editor prefEditor = prefs.edit();
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
            removePreference("maxBrightness");
            removePreference("minBrightness");

            float nightModeBrightness = prefs.getFloat("nightModeBrightness", 0.01f);
            SharedPreferences.Editor prefEditor = prefs.edit();
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
        boolean on = selection.equals("3");

        enablePreference("chooseBackgroundImage", on);
        enablePreference("hideBackgroundImage", on);
    }

    private void setupNightModePreferences(SharedPreferences prefs) {
        int nightModeActivationMode = Integer.parseInt(prefs.getString("nightModeActivationMode", "1"));
        Log.i(TAG, "setupNightModePreferences " + String.valueOf(nightModeActivationMode));
        enablePreference("nightmode_timerange",
                         nightModeActivationMode == Settings.NIGHT_MODE_ACTIVATION_SCHEDULED);
        // TODO WHY ?
        enablePreference("ambientNoiseDetection",
                         nightModeActivationMode == Settings.NIGHT_MODE_ACTIVATION_AUTOMATIC);
    }

    private void initUseDeviceLockPreference() {
        SwitchPreferenceCompat pref = (SwitchPreferenceCompat) findPreference("useDeviceLock");
        if (pref == null) {
            return;
        }

        DevicePolicyManager mgr = (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName cn = new ComponentName(mContext, AdminReceiver.class);
        if (pref.isChecked() && !mgr.isAdminActive(cn) ) {
            pref.setChecked(false);
        }
    }

    private void setupStandByService(SharedPreferences sharedPreferences) {
        if (!isAdded() ) return;
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
                newState, PackageManager.DONT_KILL_APP
        );

        if (on) {
            ScreenWatcherService.start(mContext);
        }
        conditionallyShowSnackBar(sharedPreferences);
    }

    private boolean isAutostartActivated(SharedPreferences sharedPreferences) {
        return (
                sharedPreferences.getBoolean("handle_power", false)
                        || sharedPreferences.getBoolean("standbyEnabledWhileDisconnected", false)
                        || sharedPreferences.getBoolean("scheduledAutoStartEnabled", false)
                        || sharedPreferences.getBoolean("autostartForNotifications", false)
        );
    }

    private boolean hasCanDrawOverlaysPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return (android.provider.Settings.canDrawOverlays(mContext));
        }
        return true;
    }

    void requestCanDrawOverlaysPermission() {
        startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
    }

    private void setupAlarmClock(SharedPreferences sharedPreferences) {
        if (!isAdded() ) return;
        boolean on = sharedPreferences.getBoolean("useInternalAlarm", false);

        if (!on) {
            WakeUpReceiver.cancelAlarm(mContext);
        }
    }


    private void setupNotificationAccessPermission(SharedPreferences sharedPreferences, String preferenceKey) {
        if (!isAdded() ) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
        boolean on = sharedPreferences.getBoolean(preferenceKey, false);
        NotificationManager notificationManager =
            (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (on && !notificationManager.isNotificationPolicyAccessGranted() ) {

            Intent intent =
                new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);

            startActivity(intent);
        }
    }

    private void setupDeviceAdministratorPermissions(SharedPreferences sharedPreferences) {
        if (!isAdded() ) return;
        boolean on = sharedPreferences.getBoolean("useDeviceLock", false);
        if (on) {
            DevicePolicyManager mgr = (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName cn = new ComponentName(mContext, AdminReceiver.class);
            if ( !mgr.isAdminActive(cn)) {
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
        if ( mgr.isAdminActive(cn)) {
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
        prefEditor.putFloat("scaleClockLandscape", -1.f);
        prefEditor.putFloat("scaleClockPortrait", -1.f);
        prefEditor.putFloat("scaleClock", -1.f);
        prefEditor.apply();
    }

    private void enablePreference(String key, boolean on) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setEnabled(on);
        }
    }

    private void showPreference(String key) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setVisible(true);
        }
    }

    private void removePreference(String key) {
        Preference preference = findPreference(key);
        removePreference(preference);
    }

    private void removePreference(Preference preference) {
        PreferenceGroup parent = getParent(getPreferenceScreen(), preference);
        if ( parent != null ) {
            parent.removePreference(preference);
        }
    }

    private PreferenceGroup getParent(PreferenceGroup root, Preference preference) {
        for (int i = 0; i < root.getPreferenceCount(); i++) {
            Preference p = root.getPreference(i);
            if (p == preference) {
                return root;
            }
            if (PreferenceGroup.class.isInstance(p)) {
                PreferenceGroup parent = getParent((PreferenceGroup)p, preference);
                if (parent != null) {
                    return parent;
                }
            }
        }
        return null;
    }

    private void activatePurchasesIfDebuggable() {
        if (Utility.isEmulator()) {
            purchased_donation = true;
            purchased_actions = true;
            purchased_web_radio = true;
            storeWeatherDataPurchase(true, true);
            togglePurchasePreferences();
        }
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof CityIDPreference) {
            DialogFragment dialogFragment = CityIdDialogFragment.newInstance(preference.getKey());
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getFragmentManager(), null);
        } else super.onDisplayPreferenceDialog(preference);
    }

    private void setupDaydreamPreferences() {
        enablePreference("autostart",  !Utility.isConfiguredAsDaydream(mContext) );
        Preference pref = findPreference("autostart");
        boolean on = pref.isEnabled();
        String summary = on ? "" : getString(R.string.autostart_message_disabled);
        pref.setSummary(summary);
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
    private void conditionallyShowSnackBar(SharedPreferences settings){
        if (settings == null) {
            settings = mContext.getSharedPreferences(PREFS_KEY, 0);
        }
        if (isAutostartActivated(settings) && !hasCanDrawOverlaysPermission()) {
            View view = getActivity().findViewById(android.R.id.content);
            snackbar = Snackbar.make(view, R.string.permission_request_autostart, Snackbar.LENGTH_INDEFINITE);
            int color = getRandomMaterialColor();
            int textColor = getContrastColor(color);
            View snackbarView = snackbar.getView();
            snackbarView.setBackgroundColor(color);
            snackbar.setActionTextColor(textColor);

            TextView tv = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
            tv.setTextColor(textColor);

            snackbar.setAction(android.R.string.ok, new CanDrawOverlaysPermissionListener());
            snackbar.show();
        } else {
            dismissSnackBar();
        }
    }
    private void conditionallyShowSnackBarPurchase(SharedPreferences settings){
        if (settings == null) {
            settings = mContext.getSharedPreferences(PREFS_KEY, 0);
        }
        Log.d(TAG, "days: " + Utility.getDaysSinceFirstInstall(mContext));
        long lastTimeShown = settings.getLong("puchaseSnackBarTimestamp", 0L);
        long timeSinceShown = System.currentTimeMillis() - lastTimeShown;
        Log.i(TAG, "timeSinceShown: " + Utility.getDaysSinceFirstInstall(mContext));
        long daysInstalled = Utility.getDaysSinceFirstInstall(mContext);
        if (
                purchased_donation || purchased_pro
                        || (purchased_web_radio && purchased_weather_data)
                        || (snackbar != null && snackbar.isShown())
                        || !hasCanDrawOverlaysPermission()
                        || daysInstalled < 7
                        || timeSinceShown < 60000 * 60 * 24 * 7
        ) {
            return;
        }
        View view = getActivity().findViewById(android.R.id.content);
        snackbar = Snackbar.make(view, R.string.buy_upgrade_summary, Snackbar.LENGTH_INDEFINITE);
        int color = getRandomMaterialColor();
        int textColor = getContrastColor(color);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(color);
        snackbar.setActionTextColor(textColor);

        TextView tv = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setTextColor(textColor);

        snackbar.setAction(android.R.string.ok, new BuyUpgradeListener());
        snackbar.setDuration(10000);
        snackbar.show();
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putLong("puchaseSnackBarTimestamp", System.currentTimeMillis());
        prefEditor.apply();
    }

    void dismissSnackBar() {
        if (snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
            snackbar = null;
        }
    }

    private int getRandomMaterialColor() {
        int[] colors = getResources().getIntArray(R.array.materialColors);
        return colors[new Random().nextInt(colors.length)];
    }

    private int getContrastColor(int color) {
        double y = (299 * Color.red(color) + 587 * Color.green(color) + 114 * Color.blue(color)) / 1000;
        return y >= 128 ? Color.BLACK : Color.WHITE;
    }


    public class CanDrawOverlaysPermissionListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (isAdded()) {
                requestCanDrawOverlaysPermission();
            }
        }
    }

    public class BuyUpgradeListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (isAdded()) {
                purchaseIntent(ITEM_PRO, REQUEST_CODE_PURCHASE_PRO);
            }
        }
    }
}
