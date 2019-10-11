package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;


public class PreferencesActivity extends BillingHelperActivity
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
        initTitleBar();
        fragment = new PreferencesFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit();
    }

    @Override
    protected void onPurchasesInitialized() {
        super.onPurchasesInitialized();
        if (fragment != null) {
            fragment.onPurchasesInitialized();
        }
    }

    @Override
    protected void onItemPurchased(String sku) {
        super.onItemPurchased(sku);
        if (fragment != null) {
            fragment.onPurchasesInitialized();
        }
    }


    @Override
    protected void onItemConsumed(String sku) {
        super.onItemConsumed(sku);
    }

    public void restartFragment() {
        FragmentManager fm = getSupportFragmentManager();

        initTitleBar();
        if (fragment != null) {
            fm.beginTransaction()
                    .remove(fragment)
                    .commit();
            fm.popBackStackImmediate();
        }
        fragment = new PreferencesFragment();

        fm.beginTransaction()
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        initTitleBar();
    }

    void initTitleBar() {
        ActionBar actionBar = getSupportActionBar();
        if(actionBar !=null) {
            actionBar.setTitle(R.string.preferences);
        }
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

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(pref.getTitle());
        }

        return true;
    }
}
