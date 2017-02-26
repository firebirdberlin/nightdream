package com.firebirdberlin.nightdream;

import java.util.ArrayList;

import org.json.JSONObject;
import org.json.JSONException;

import com.android.vending.billing.IInAppBillingService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.pm.PackageManager;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import de.firebirdberlin.preference.InlineSeekBarPreference;
import com.firebirdberlin.nightdream.receivers.WakeUpReceiver;
import com.firebirdberlin.nightdream.services.RadioStreamService;

public class PreferencesFragment extends PreferenceFragment {
    public static final String TAG = "NightDream.PreferencesFragment";
    public static final String ITEM_DONATION = "donation";
    public static final String ITEM_WEATHER_DATA = "weather_data";
    public static final String ITEM_WEB_RADIO = "web_radio";
    public static final int REQUEST_CODE_PURCHASE_DONATION = 1001;
    public static final int REQUEST_CODE_PURCHASE_WEATHER = 1002;
    public static final int REQUEST_CODE_PURCHASE_WEB_RADIO = 1003;
    public static final String PREFS_KEY = "NightDream preferences";
    private static int RESULT_LOAD_IMAGE = 1;
    private final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2;
    private final int PERMISSIONS_REQUEST_RECORD_AUDIO = 3;
    private static int RESULT_LOAD_IMAGE_KITKAT = 4;
    private final int PERMISSIONS_REQUEST_ACCESS_LOCATION = 5;
    private Settings settings = null;
    private Context mContext = null;

    IInAppBillingService mService;
    public boolean purchased_donation = false;
    public boolean purchased_weather_data = false;
    public boolean purchased_web_radio = false;

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
    public void onDestroy() {
        super.onDestroy();
        // unbind the in-app billing service
        if (mService != null) {
            getActivity().unbindService(mServiceConn);
        }
    }

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
        }
    };

    private void getPurchases() {
        if ( Utility.isDebuggable(mContext) ) {
            purchased_donation = true;
            purchased_weather_data = true;
            purchased_web_radio = true;
            togglePurchasePreferences();
            return;
        }
        if (mService == null || getActivity() == null) {
            return;
        }

        Bundle ownedItems = null;
        try {
            ownedItems = mService.getPurchases(3, getActivity().getPackageName(), "inapp", null);
        } catch (RemoteException e) {
            return;
        }

        if (ownedItems == null) return;

        int response = ownedItems.getInt("RESPONSE_CODE");
        if (response == 0) {
            ArrayList<String> ownedSkus =
                ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
            ArrayList<String>  purchaseDataList =
                ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
            ArrayList<String>  signatureList =
                ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
            String continuationToken =
                ownedItems.getString("INAPP_CONTINUATION_TOKEN");

            for (int i = 0; i < purchaseDataList.size(); ++i) {
                String purchaseData = purchaseDataList.get(i);
                String signature = signatureList.get(i);
                String sku = ownedSkus.get(i);

                if (sku.equals(ITEM_DONATION)) {
                    purchased_donation = true;
                    purchased_weather_data = true;
                    purchased_web_radio = true;
                }
                if (sku.equals(ITEM_WEATHER_DATA)) {
                    purchased_weather_data = true;
                }
                if (sku.equals(ITEM_WEB_RADIO)) {
                    purchased_web_radio = true;
                }

                // do something with this purchase information
                // e.g. display the updated list of products owned by user
                // or consume the purchase
                //try {
                    //JSONObject o = new JSONObject(purchaseData);
                    //String purchaseToken = o.getString("purchaseToken");
                    //mService.consumePurchase(3, getActivity().getPackageName(), purchaseToken);
                //}
                //catch (Exception e) {
                    //e.printStackTrace();
                //}
            }

            togglePurchasePreferences();

            // if continuationToken != null, call getPurchases again
            // and pass in the token to retrieve more items
        }

    }

    private void togglePurchasePreferences() {
        Preference donationPreference = (Preference) findPreference("donation_play");
        Preference purchaseWeatherDataPreference = (Preference) findPreference("purchaseWeatherData");
        Preference purchaseWebRadioPreference = (Preference) findPreference("purchaseWebRadio");
        Preference purchaseWebRadioUIPreference = (Preference) findPreference("purchaseWebRadioUI");
        Preference enableWeatherDataPreference = (Preference) findPreference("showWeather");
        Preference useRadioAlarmClockPreference = (Preference) findPreference("useRadioAlarmClock");

        enableWeatherDataPreference.setEnabled(purchased_weather_data);
        useRadioAlarmClockPreference.setEnabled(purchased_web_radio);

        if (purchased_donation) {
            PreferenceCategory category = (PreferenceCategory) findPreference("donation_screen");
            if (category != null) {
                category.removePreference(donationPreference);
            }
        }

        if (purchased_weather_data) {
            PreferenceScreen screen = (PreferenceScreen) findPreference("weather_screen");
            if (screen != null) {
                screen.removePreference(purchaseWeatherDataPreference);
            }
        }

        if (purchased_web_radio) {
            PreferenceCategory categoryRadio = (PreferenceCategory) findPreference("category_radio_stream");
            if (categoryRadio != null) {
                categoryRadio.removePreference(purchaseWebRadioPreference);
            }
            PreferenceScreen screenRadio = (PreferenceScreen) findPreference("radio_screen");
            if (screenRadio != null) {
                screenRadio.removePreference(purchaseWebRadioUIPreference);
            }
        }
    }

    public void purchaseIntent(String sku, int REQUEST_CODE) {
        if (mService == null) return;
        try {
            String developerPayload = "abcdefghijklmnopqrstuvwxyz";
            Bundle buyIntentBundle = mService.getBuyIntent(3, getActivity().getPackageName(),
                                                           sku, "inapp",developerPayload);
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            getActivity().startIntentSenderForResult(pendingIntent.getIntentSender(),
                    REQUEST_CODE, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                    Integer.valueOf(0));
        } catch (RemoteException e1) {
            return;
        } catch (SendIntentException e2) {
            return;
        }
    }

    public void showThankYouDialog() {
        new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.dialog_title_thank_you))
            .setMessage(R.string.dialog_message_thank_you)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(PREFS_KEY);
        addPreferencesFromResource(R.layout.preferences);
        init();
    }

    private void init() {
        final Context context = mContext;
        settings = new Settings(mContext);

        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        prefs.registerOnSharedPreferenceChangeListener(prefChangedListener);

        setupBrightnessControls(prefs);
        setupBackgroundImageControls(prefs);

        Preference goToSettings = (Preference) findPreference("startNotificationService");
        goToSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                if (Build.VERSION.SDK_INT < 18){
                    Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivityForResult(intent, 0);
                } else {
                    Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                    startActivityForResult(intent, 0);
                }
                return true;
            }
        });

        Preference chooseImage = (Preference) findPreference("chooseBackgroundImage");
        chooseImage.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                checkPermissionAndSelectBackgroundImage();
                return true;
            }
        });

        Preference donationPreference = (Preference) findPreference("donation_play");
        donationPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                purchaseIntent(ITEM_DONATION, REQUEST_CODE_PURCHASE_DONATION);
                return true;
            }
        });

        Preference purchaseWeatherDataPreference = (Preference) findPreference("purchaseWeatherData");
        purchaseWeatherDataPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                purchaseIntent(ITEM_WEATHER_DATA, REQUEST_CODE_PURCHASE_WEATHER);
                return true;
            }
        });

        Preference purchaseWebRadioPreference = (Preference) findPreference("purchaseWebRadio");
        purchaseWebRadioPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                purchaseIntent(ITEM_WEB_RADIO, REQUEST_CODE_PURCHASE_WEB_RADIO);
                return true;
            }
        });

        Preference prefHandlePower = (Preference) findPreference("handle_power");
        Preference prefAmbientNoiseDetection = (Preference) findPreference("ambientNoiseDetection");
        Preference prefAmbientNoiseReactivation = (Preference) findPreference("reactivate_screen_on_noise");

        prefAmbientNoiseDetection.setOnPreferenceChangeListener(recordAudioPrefChangeListener);
        prefAmbientNoiseReactivation.setOnPreferenceChangeListener(recordAudioPrefChangeListener);

        Preference prefFetchWeatherData = (Preference) findPreference("showWeather");
        prefFetchWeatherData.setOnPreferenceChangeListener(fetchWeatherDataPrefChangeListener);

        if ( Utility.getLightSensor(context) == null ) {
            PreferenceScreen colorScreen = (PreferenceScreen) findPreference("colors_screen");
            Preference autoBrightness = (Preference) findPreference("autoBrightness");
            colorScreen.removePreference(autoBrightness);
        }

        prefHandlePower.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object new_value) {
                boolean on = Boolean.parseBoolean(new_value.toString());
                Utility.toggleComponentState(context, PowerConnectionReceiver.class, on);
                return true;
            }
        });


        Preference recommendApp = (Preference) findPreference("recommendApp");
        recommendApp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                recommendApp();
                return true;
            }
        });

        Preference resetToDefaults = (Preference) findPreference("reset_to_defaults");
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
                            addPreferencesFromResource(R.layout.preferences);
                            init();
                            togglePurchasePreferences();
                        }
                    }).show();

                return true;
            }
        });

        Preference startAudioStream = (Preference) findPreference("startAudioStream");
        startAudioStream.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                if (! RadioStreamService.isRunning) {
                    RadioStreamService.startStream(context);
                } else {
                    RadioStreamService.stop(context);
                }
                return true;
            }
        });
    }

    Preference.OnPreferenceChangeListener recordAudioPrefChangeListener =
        new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object new_value) {
                boolean on = Boolean.parseBoolean(new_value.toString());
                if (on && ! hasPermission(Manifest.permission.RECORD_AUDIO) ) {
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
                if (on && ! hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                       PERMISSIONS_REQUEST_ACCESS_LOCATION);
                }
                return true;
            }
        };

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
                    requestCode == REQUEST_CODE_PURCHASE_WEATHER ||
                    requestCode == REQUEST_CODE_PURCHASE_WEB_RADIO )) {
            Log.i(TAG, "Purchase request for " + String.valueOf(requestCode));
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
            Log.i(TAG, purchaseData);

            try {
                JSONObject jo = new JSONObject(purchaseData);
                String sku = jo.getString("productId");
                if (sku.equals(ITEM_DONATION) ) {
                    purchased_donation = true;
                    purchased_weather_data = true;
                    purchased_web_radio = true;
                    showThankYouDialog();
                } else
                if (sku.equals(ITEM_WEATHER_DATA) ) {
                    purchased_weather_data = true;
                } else
                if (sku.equals(ITEM_WEB_RADIO) ) {
                    purchased_web_radio = true;
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
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
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
        if (Build.VERSION.SDK_INT >= 23 ) {
            return ( getActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED );
        }
        return true;
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= 23 ) {
            return ( getActivity().checkSelfPermission(permission)
                    == PackageManager.PERMISSION_GRANTED );
        }
        return true;
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

                    SwitchPreference prefAmbientNoiseDetection = (SwitchPreference) findPreference("ambientNoiseDetection");
                    CheckBoxPreference prefAmbientNoiseReactivation = (CheckBoxPreference) findPreference("reactivate_screen_on_noise");
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

                    SwitchPreference prefShowWeather = (SwitchPreference) findPreference("showWeather");
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

    private void setupBrightnessControls(SharedPreferences prefs) {
        if (!isAdded() ) return;
        Preference brightnessOffset = (Preference) findPreference("brightness_offset");
        boolean on = prefs.getBoolean("autoBrightness", false);
        String title = getString(R.string.brightness);
        if (on) {
            title = getString(R.string.brightness_offset);
        }
        brightnessOffset.setTitle(title);
    }

    private void setupBackgroundImageControls(SharedPreferences prefs) {
        String selection = prefs.getString("backgroundMode", "1");
        boolean on = selection.equals("3");

        Preference chooseImage = (Preference) findPreference("chooseBackgroundImage");
        Preference hideImage = (Preference) findPreference("hideBackgroundImage");
        chooseImage.setEnabled(on);
        hideImage.setEnabled(on);
    }

    SharedPreferences.OnSharedPreferenceChangeListener prefChangedListener =
        new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("brightness_offset")) {
                    int offsetInt = sharedPreferences.getInt("brightness_offset", 0);
                    settings.setBrightnessOffset(offsetInt/100.f);
                } else
                if (key.equals("autoBrightness")) {
                    InlineSeekBarPreference pref = (InlineSeekBarPreference) findPreference("brightness_offset");
                    // reset the brightness level
                    settings.setBrightnessOffset(0.8f);
                    pref.setProgress(80);
                    setupBrightnessControls(sharedPreferences);
                } else
                if (key.equals("backgroundMode")) {
                    setupBackgroundImageControls(sharedPreferences);
                }
            }
        };
}
