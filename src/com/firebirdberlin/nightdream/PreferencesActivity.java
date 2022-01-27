package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class PreferencesActivity extends BillingHelperActivity
        implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    public static final String TAG = "PreferencesActivity";
    PreferencesFragment fragment = new PreferencesFragment();
    PreferencesFragment fragment2 = null;
    Handler handler = new Handler();
    String rootKey = "";

    public static void start(Context context) {
        Intent intent = new Intent(context, PreferencesActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(init);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setTheme(R.style.PreferencesTheme);
        initTitleBar();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        handler.removeCallbacks(init);
        handler.postDelayed(init, 300);
    }

    private final Runnable init = () -> initFragment();

    public void initFragment() {
        Log.i(TAG, "initFragment()");
        handler.removeCallbacks(init);
        FragmentManager fm = getSupportFragmentManager();

        FragmentTransaction fT = fm.beginTransaction();
        if (fragment != null) {
            fT.remove(fragment);
        }
        fragment = new PreferencesFragment();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.preferences_layout);
            if (fragment2 != null) {
                fT.remove(fragment2);
                fragment2 = null;
            }

            Bundle data = new Bundle();
            data.putString("rootKey", rootKey);
            fragment.setArguments(data);
            if (Utility.isEmpty(rootKey)) {
                fT.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
            }
            fT.replace(R.id.main_frame, fragment);
        } else {
            setContentView(R.layout.preferences_layout_land);

            if (Utility.isEmpty(rootKey)) {
                fT.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
                rootKey = "autostart";
            }
            Bundle data = new Bundle();
            data.putString("rootKey", rootKey);
            fragment.setArguments(data);
            fragment2 = new PreferencesFragment();

            fT.replace(R.id.right, fragment);
            fT.replace(R.id.details, fragment2);
        }
        fT.commit();
        fm.executePendingTransactions();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        handler.removeCallbacks(init);
        handler.post(init);
    }

    @Override
    protected void onPurchasesInitialized() {
        Log.d(TAG, "onPurchasesInitialized: " + fragment);
        super.onPurchasesInitialized();
        if (fragment != null) {
            fragment.onPurchasesInitialized();
        }
        Settings.storeWeatherDataPurchase(
                this,
                isPurchased(BillingHelperActivity.ITEM_WEATHER_DATA),
                isPurchased(BillingHelperActivity.ITEM_DONATION)
        );

        handler.removeCallbacks(init);
        handler.postDelayed(init, 200);
    }

    @Override
    protected void onItemPurchased(String sku) {
        Log.d(TAG, "onItemPurchased");
        super.onItemPurchased(sku);
        if (fragment != null) {
            runOnUiThread(() -> {
                fragment.onPurchasesInitialized();
                Settings.storeWeatherDataPurchase(
                        this,
                        isPurchased(BillingHelperActivity.ITEM_WEATHER_DATA),
                        isPurchased(BillingHelperActivity.ITEM_DONATION)
                );
                handler.removeCallbacks(init);
                handler.postDelayed(init, 200);
            }
        );
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
                    .replace(R.id.main_frame, fragment)
                    .addToBackStack(null)
                    .commit();
            initTitleBar();
        } else {
            finish();
        }
    }

    void initTitleBar() {
        Log.d(TAG, "initTitleBar");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.preferences);
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
        FragmentManager fm = getSupportFragmentManager();

        FragmentTransaction fT = fm.beginTransaction();
        if (fragment != null) {
            fT.remove(fragment);
        }
        fragment = new PreferencesFragment();
        Bundle data = new Bundle();
        data.putString("rootKey", rootKey);
        fragment.setArguments(data);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            fT.setCustomAnimations(R.anim.move_in_prefs_right, R.anim.move_out_prefs_left, R.anim.move_in_prefs_left, R.anim.move_out_prefs_right)
                    .replace(R.id.main_frame, fragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            fT.replace(R.id.right, fragment).commit();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(pref.getTitle());
        }

        return true;
    }
}
