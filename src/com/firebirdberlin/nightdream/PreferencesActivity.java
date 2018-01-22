package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;


public class PreferencesActivity extends PreferenceActivity {
    public static final int PREFERENCES_SCREEN_WEB_RADIO_INDEX = 5;

    PreferencesFragment fragment = null;

    public static void start(Context context) {
        if (Build.VERSION.SDK_INT > 10) {
            Intent intent = new Intent(context, PreferencesActivity.class);
            context.startActivity(intent);
        } else {
            PreferencesActivityv9.start(context);
        }
    }

    public static void start(Context context, int preferenceScreenIndex) {
        if (Build.VERSION.SDK_INT > 10) {
            Intent intent = new Intent(context, PreferencesActivity.class);
            intent.putExtra("preferenceScreenIndex", preferenceScreenIndex);
            context.startActivity(intent);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.PreferencesTheme);

        fragment = new PreferencesFragment();
        Intent intent = getIntent();
        if ( intent.hasExtra("preferenceScreenIndex") ) {
            int index = intent.getIntExtra("preferenceScreenIndex", -1);
            if ( index > -1 ) {
                fragment.setInitialScreenIndex(index);
            }
        }

        getFragmentManager()
            .beginTransaction()
            .replace(android.R.id.content, fragment)
            .commit();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // seems to be an Android bug. We have to forward onActivityResult manually for in app
        // billing requests.
        if (requestCode > 1000) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }
}
