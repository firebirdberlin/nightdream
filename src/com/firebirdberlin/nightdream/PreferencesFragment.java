package com.firebirdberlin.nightdream;

import android.app.Activity;
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
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.MediaStore;
import android.widget.Toast;
import de.firebirdberlin.preference.InlineSeekBarPreference;


public class PreferencesFragment extends PreferenceFragment {
    public static final String TAG = "PreferencesFragment";
    public static final String PREFS_KEY = "NightDream preferences";
    private static int RESULT_LOAD_IMAGE = 1;
    private Settings settings = null;
    private Context mContext = null;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;

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
                selectBackgroundImage();
                return true;
            }
        });

        Preference prefHandlePower = (Preference) findPreference("handle_power");
        Preference prefAllowScreenOff = (Preference) findPreference("allow_screen_off");

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


        Preference goToDonation = (Preference) findPreference("openDonationPage");
        goToDonation.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                openDonationPage();
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == Activity.RESULT_OK && null != data){

            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getActivity().getContentResolver()
                                         .query(selectedImage,filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            if (picturePath != null){
                settings.setBackgroundImage(picturePath);
            } else {
                Toast.makeText(getActivity(), "Could locate image !", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void selectBackgroundImage() {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");

        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");
        String msg = getString(R.string.background_image_select);
        Intent chooserIntent = Intent.createChooser(getIntent, msg);
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

        startActivityForResult(chooserIntent, RESULT_LOAD_IMAGE);
    }

    private void openDonationPage() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=5PX9XVHHE6XP8"));
        startActivity(browserIntent);
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
