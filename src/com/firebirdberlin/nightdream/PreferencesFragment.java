package com.firebirdberlin.nightdream;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.SwitchPreference;
import android.provider.MediaStore;
import android.widget.Toast;
import de.firebirdberlin.preference.InlineSeekBarPreference;


public class PreferencesFragment extends PreferenceFragment {
    public static final String TAG = "PreferencesFragment";
    public static final String PREFS_KEY = "NightDream preferences";
    private static int RESULT_LOAD_IMAGE = 1;
    private final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2;
    private final int PERMISSIONS_REQUEST_RECORD_AUDIO = 3;
    private static int RESULT_LOAD_IMAGE_KITKAT = 4;
    private Settings settings = null;
    private Context mContext = null;

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
        final Context context = mContext;
        settings = new Settings(mContext);

        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        prefs.registerOnSharedPreferenceChangeListener(prefChangedListener);

        setupBrightnessControls(prefs);

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
                PreferencesActivity activity = (PreferencesActivity) getActivity();
                activity.purchaseIntent(PreferencesActivity.ITEM_DONATION,
                                        PreferencesActivity.REQUEST_CODE_PURCHASE_DONATION);
                return true;
            }
        });

        Preference prefHandlePower = (Preference) findPreference("handle_power");
        Preference prefAmbientNoiseDetection = (Preference) findPreference("ambientNoiseDetection");
        Preference prefAmbientNoiseReactivation = (Preference) findPreference("reactivate_screen_on_noise");

        prefAmbientNoiseDetection.setOnPreferenceChangeListener(recordAudioPrefChangeListener);
        prefAmbientNoiseReactivation.setOnPreferenceChangeListener(recordAudioPrefChangeListener);

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
                settings.clear();
                getPreferenceScreen().removeAll();
                addPreferencesFromResource(R.layout.preferences);
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
                }
            }
        };
}
