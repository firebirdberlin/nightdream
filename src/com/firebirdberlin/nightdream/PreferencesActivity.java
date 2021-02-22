package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class PreferencesActivity extends BillingHelperActivity
        implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    public static final String TAG = "PreferencesActivity";
    PreferencesFragment fragment = null;
    PreferencesFragment fragment2 = null;
    String rootKey = "";

    public static void start(Context context) {
        Intent intent = new Intent(context, PreferencesActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setTheme(R.style.PreferencesTheme);
        initTitleBar();

        fragment = new PreferencesFragment();

        initFragment(false);
    }

    public void initFragment(boolean putRootkey) {
        FragmentManager fm = getSupportFragmentManager();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            TextView tv = new TextView(this);
            tv.setText("");
            setContentView(tv);

            if (fragment != null) {
                fm.beginTransaction()
                        .remove(fragment)
                        .commit();
                fm.popBackStackImmediate();
            }

            if (fragment2 != null) {
                fm.beginTransaction()
                        .remove(fragment)
                        .commit();
                fm.popBackStackImmediate();
            }

            if (putRootkey) {
                Bundle data = new Bundle();
                data.putString("rootKey", rootKey);
                fragment.setArguments(data);
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .commit();
        } else {
            if (fragment != null) {
                fm.beginTransaction()
                        .remove(fragment)
                        .commit();
                fm.popBackStackImmediate();
            }

            setContentView(R.layout.preferences_layout_land);

            fragment = new PreferencesFragment();

            if (putRootkey) {
                Bundle data = new Bundle();
                data.putString("rootKey", rootKey);
                fragment.setArguments(data);
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.right, fragment)
                    .commit();

            fragment2 = new PreferencesFragment();

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.details, fragment2)
                    .commit();
        }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        initFragment(true);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged: " + getResources().getConfiguration().orientation);
        super.onConfigurationChanged(newConfig);
        initFragment(true);
    }

    @Override
    protected void onPurchasesInitialized() {
        Log.d(TAG, "onPurchasesInitialized: " + fragment);
        super.onPurchasesInitialized();
        if (fragment != null) {
            fragment.onPurchasesInitialized();
        }
    }

    @Override
    protected void onItemPurchased(String sku) {
        Log.d(TAG, "onItemPurchased");
        super.onItemPurchased(sku);
        if (fragment != null) {
            fragment.onPurchasesInitialized();
        }
    }

    @Override
    protected void onItemConsumed(String sku) {
        super.onItemConsumed(sku);
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
        Log.d(TAG, "onBackPressed");

        if (!rootKey.isEmpty() && (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)) {
            rootKey = "";
            fragment = new PreferencesFragment();

            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.move_in_prefs_left, R.anim.move_out_prefs_right, R.anim.move_in_prefs_right, R.anim.move_out_prefs_left)
                    .replace(android.R.id.content, fragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            finish();
        }
        initTitleBar();
    }

    void initTitleBar() {
        Log.d(TAG, "initTitlebar");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.preferences);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        Log.d(TAG, "onSupportNavigateUp");
        onBackPressed();
        return true;
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        Log.d(TAG, "onPreferenceStartFragment");
        Log.d(TAG, "rootKey:" + pref.getKey());

        //save rootKey for onResume
        rootKey = pref.getKey();

        // Instantiate the new Fragment
        FragmentManager fragmentManager = getSupportFragmentManager();

        final Fragment fragment = fragmentManager.getFragmentFactory().instantiate(
                this.getClassLoader(), pref.getFragment());

        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.getKey());
        fragment.setArguments(args);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.move_in_prefs_right, R.anim.move_out_prefs_left, R.anim.move_in_prefs_left, R.anim.move_out_prefs_right)
                    .replace(android.R.id.content, fragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.right, fragment)
                    .commit();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(pref.getTitle());
        }

        return true;
    }
}
