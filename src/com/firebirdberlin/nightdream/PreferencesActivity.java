package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;


public class PreferencesActivity extends PreferenceActivity {
    PreferencesFragment fragment = null;

    public static void start(Context context) {
            Intent intent = new Intent(context, PreferencesActivity.class);
            context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.PreferencesTheme);

        fragment = new PreferencesFragment();
        Intent intent = getIntent();
        if (intent.hasExtra("shallShowPurchaseDialog")) {
            boolean showDialog = intent.getBooleanExtra("shallShowPurchaseDialog", false);
            if (showDialog) {
                fragment.setShowPurchaseDialog();
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

    public void showPurchaseDialog() {
        if (fragment != null) {
            fragment.showPurchaseDialog();
        }
    }
}
