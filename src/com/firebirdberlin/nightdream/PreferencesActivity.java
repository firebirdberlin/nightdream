package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.os.Build;


public class PreferencesActivity extends PreferenceActivity {
    PreferencesFragment fragment = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            setTheme(R.style.PreferencesTheme);
        }
        fragment = new PreferencesFragment();
        getFragmentManager()
            .beginTransaction()
            .replace(android.R.id.content, fragment)
            .commit();
    }

    public static void start(Context context) {
        if (Build.VERSION.SDK_INT > 10) {
            Intent myIntent = new Intent(context, PreferencesActivity.class);
            context.startActivity(myIntent);
        } else {
            PreferencesActivityv9.start(context);
        }
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
