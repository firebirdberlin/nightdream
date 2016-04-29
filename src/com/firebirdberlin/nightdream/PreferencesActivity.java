package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.MediaStore;
import android.provider.Settings.System;
import android.widget.Toast;


public class PreferencesActivity extends PreferenceActivity {
    public static final String PREFS_KEY = "NightDream preferences";
    private static int RESULT_LOAD_IMAGE = 1;
    private static int RESULT_LOAD_ALARM_SOUND = 2;
    private Settings settings = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new Settings(this);

        getPreferenceManager().setSharedPreferencesName(PREFS_KEY);

        addPreferencesFromResource(R.layout.preferences);

        Preference goToSettings = (Preference) findPreference("startNotificationService");
        goToSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                if (Build.VERSION.SDK_INT < 18)	{
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
                Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getIntent.setType("image/*");

                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("image/*");
                String msg = getString(R.string.background_image_select);
                Intent chooserIntent = Intent.createChooser(getIntent, msg);
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

                startActivityForResult(chooserIntent, RESULT_LOAD_IMAGE);
                return true;
            }
        });

        Preference chooseAlarmSound = (Preference) findPreference("chooseAlarmSound");
        chooseAlarmSound.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                String msg = getString(R.string.select_alarm_sound);
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, msg);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(settings.AlarmToneUri));
                startActivityForResult(intent, RESULT_LOAD_ALARM_SOUND);
                return true;
            }
        });

        Preference prefHandlePower = (Preference) findPreference("handle_power");
        Preference prefAllowScreenOff = (Preference) findPreference("allow_screen_off");

        boolean enabled = Utility.isDaydreamEnabled(this);
        prefAllowScreenOff.setEnabled( ! enabled );

        if ( Utility.getLightSensor(this) == null ) {
            PreferenceScreen colorScreen = (PreferenceScreen) findPreference("colors_screen");
            Preference autoBrightness = (Preference) findPreference("autoBrightness");
            colorScreen.removePreference(autoBrightness);
        }

        final Context context = this;
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

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data){

            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(selectedImage,filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            if (picturePath != null){
                settings.setBackgroundImage(picturePath);
            } else {
                Toast.makeText(this, "Could locate image !", Toast.LENGTH_LONG).show();
            }
        }
        else
        if (requestCode == RESULT_LOAD_ALARM_SOUND && resultCode == RESULT_OK && null != data){
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            String uri_string = (uri == null) ? "" : uri.toString();
            settings.setAlarmToneUri(uri_string);
            Toast.makeText(this, uri_string, Toast.LENGTH_LONG).show();
        }
    }

    private void openDonationPage() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=5PX9XVHHE6XP8"));
        startActivity(browserIntent);
    }

    public static void start(Context context) {
        Intent myIntent = new Intent(context, PreferencesActivity.class);
        context.startActivity(myIntent);
    }
}
