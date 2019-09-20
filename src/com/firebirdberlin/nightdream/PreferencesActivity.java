package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;


public class PreferencesActivity extends AppCompatActivity
                                 implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
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

        getSupportFragmentManager()
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
    public boolean isPurchased(String sku) {
        if (fragment == null) {
            return false;
        }
        switch (sku) {
            case PreferencesFragment.ITEM_ACTIONS:
                return fragment.purchased_actions;
            case PreferencesFragment.ITEM_DONATION:
                return fragment.purchased_donation;
            case PreferencesFragment.ITEM_PRO:
                return fragment.purchased_pro;
            case PreferencesFragment.ITEM_WEATHER_DATA:
                return fragment.purchased_weather_data;
            case PreferencesFragment.ITEM_WEB_RADIO:
                return fragment.purchased_web_radio;
        }
        return false;
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        Log.d("Pref", pref.getKey());
        // Instantiate the new Fragment
        final Fragment fragment =
                Fragment.instantiate(this, pref.getFragment(), pref.getExtras());
        fragment.setTargetFragment(caller, 0);
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.getKey());
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }
}
