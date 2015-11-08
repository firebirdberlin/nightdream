package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.MediaStore;
import android.widget.Toast;


public class PreferencesActivity extends PreferenceActivity {
    public static final String PREFS_KEY = "NightDream preferences";
    private static int RESULT_LOAD_IMAGE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, RESULT_LOAD_IMAGE);
                return true;
            }
        });

        Preference prefHandlePower = (Preference) findPreference("handle_power");
        Preference prefAllowScreenOff = (Preference) findPreference("allow_screen_off");

        boolean enabled = Utility.isDaydreamEnabled(this);
        final Context context = this;
        prefAllowScreenOff.setEnabled( ! enabled );
        prefHandlePower.setEnabled( ! enabled );
        prefHandlePower.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object new_value) {
                boolean on = Boolean.parseBoolean(new_value.toString());
                Utility.toggleComponentState(context, PowerConnectionReceiver.class, on);
                Utility.toggleComponentState(context, PowerDisconnectionReceiver.class, on);
                return true;
            }
        });
    }

    // an image was selected
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
                SharedPreferences settings = getSharedPreferences(PREFS_KEY, 0);
                SharedPreferences.Editor prefEditor = settings.edit();
                prefEditor.putString("BackgroundImage", picturePath);
                prefEditor.commit();
            } else {
                Toast.makeText(this, "Could locate image !", Toast.LENGTH_LONG).show();
            }
        }
    }

    public static void start(Context context) {
        Intent myIntent = new Intent(context, PreferencesActivity.class);
        context.startActivity(myIntent);
    }
}
