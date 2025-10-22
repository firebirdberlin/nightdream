package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class PreferencesActivity extends BillingHelperActivity
        implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    public static final String TAG = "PreferencesActivity";
    private static final String ROOT_KEY = "rootKey";
    private static final String KEY_TITLE = "title";
    private static final String FRAGMENT_TAG_1 = "f1";
    private static final String FRAGMENT_TAG_2 = "f2";

    PreferencesFragment fragment = null;
    PreferencesFragment fragment2 = null;
    String rootKey = "";
    private OnBackPressedCallback onBackPressedCallback;
    private CharSequence currentTitle;

    public static void start(Context context) {
        Intent intent = new Intent(context, PreferencesActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Log.d(TAG, "onPostResume()");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setTheme(R.style.PreferencesTheme);

        // Set the appropriate content view initially.
        // The activity will be recreated on rotation, so setContentView will be called again then.
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.preferences_layout);
        } else {
            setContentView(R.layout.preferences_layout_land);
        }

        if (savedInstanceState != null) {
            rootKey = savedInstanceState.getString(ROOT_KEY, "");
            currentTitle = savedInstanceState.getCharSequence(KEY_TITLE);
            // Fragments are automatically restored by FragmentManager, just retrieve references
            fragment = (PreferencesFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_1);
            fragment2 = (PreferencesFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_2);
        } else {
            currentTitle = getString(R.string.preferences);
            // Only add fragments if it's the very first creation, not recreation after rotation
            initRootFragments();
        }

        initTitleBar();

        onBackPressedCallback = new OnBackPressedCallback(rootKey != null && !rootKey.isEmpty()) {
            @Override
            public void handleOnBackPressed() {
                Log.d(TAG, "handleOnBackPressed");

                FragmentManager fm = getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                } else {
                    rootKey = "";
                    currentTitle = getString(R.string.preferences);
                    initTitleBar();
                    setEnabled(false);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);

        getSupportFragmentManager().addOnBackStackChangedListener(this::onBackStackChanged);
    }

    // New method for initial fragment setup
    private void initRootFragments() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fT = fm.beginTransaction();

        // Initialize fragment (main or left pane)
        fragment = new PreferencesFragment();
        Bundle data = new Bundle();
        data.putString("rootKey", rootKey);
        fragment.setArguments(data);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            fT.replace(R.id.main_frame, fragment, FRAGMENT_TAG_1);
        } else { // Landscape
            // Initialize fragment2 (right pane)
            fragment2 = new PreferencesFragment();
            fT.replace(R.id.right, fragment, FRAGMENT_TAG_1);
            fT.replace(R.id.details, fragment2, FRAGMENT_TAG_2);
        }
        fT.commitAllowingStateLoss();
        fm.executePendingTransactions();
    }


    private void onBackStackChanged() {
        Log.d(TAG, "onBackStackChanged: Back stack entry count = " + getSupportFragmentManager().getBackStackEntryCount());
        FragmentManager fm = getSupportFragmentManager();
        int backStackEntryCount = fm.getBackStackEntryCount();

        if (backStackEntryCount == 0) {
            rootKey = "";
            currentTitle = getString(R.string.preferences);
            onBackPressedCallback.setEnabled(false);
        } else {
            // A sub-preference screen is active.
            // We rely on onPreferenceStartFragment to set currentTitle.
            onBackPressedCallback.setEnabled(true);
        }
        initTitleBar(); // Update action bar with potentially new currentTitle
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        // No need to call initFragmentIfRequired() here as fragments are handled in onCreate and onPreferenceStartFragment
        // and re-attached by the FragmentManager on activity recreation.
    }

    // initFragmentIfRequired() is no longer needed in this form as fragments are handled differently.
    // private void initFragmentIfRequired() { ... }

    // removeFragment() is no longer directly called in initFragment() or initRootFragments(), keeping for now.
    private void removeFragment(Fragment removeFragment) {
        Log.d(TAG, "removeFragment()");
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (removeFragment != null && fragmentManager.findFragmentById(removeFragment.getId()) != null) {
            fragmentManager.beginTransaction().remove(removeFragment).commitAllowingStateLoss();
        }
    }

    // initFragment() is replaced by initRootFragments() for initial setup and onPreferenceStartFragment for sub-screens.
    // private void initFragment() { ... }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged()");
        super.onConfigurationChanged(newConfig);

        // DO NOT call setContentView() here. Let the system recreate the Activity.
        // The new layout will be inflated by onCreate() on recreation.

        // Fragments will be re-attached by FragmentManager.
        // Just update action bar and back press callback state.
        initTitleBar();
        if (onBackPressedCallback != null) {
            // Re-evaluate enabled state for back button based on new orientation
            onBackPressedCallback.setEnabled(!rootKey.isEmpty() && newConfig.orientation == Configuration.ORIENTATION_PORTRAIT);
        }
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
        for (Fragment activeFragment : getSupportFragmentManager().getFragments()) {
            if (activeFragment instanceof PreferencesFragment) {
                activeFragment.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    void initTitleBar() {
        Log.d(TAG, "initTitleBar");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(currentTitle);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        Log.d(TAG, "onSupportNavigateUp");
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log.d(TAG, "onSaveInstanceState()");
        savedInstanceState.putString(ROOT_KEY, rootKey);
        savedInstanceState.putCharSequence(KEY_TITLE, currentTitle);
    }

    // onRestoreInstanceState is generally not needed if state is handled in onCreate.
    // @Override
    // public void onRestoreInstanceState(Bundle savedInstanceState) { 
    //     super.onRestoreInstanceState(savedInstanceState);
    //     Log.d(TAG, "onRestoreInstanceState");
    // }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        Log.d(TAG, "onPreferenceStartFragment");
        Log.d(TAG, "rootKey:" + pref.getKey());

        rootKey = pref.getKey();
        currentTitle = pref.getTitle();

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fT = fm.beginTransaction();

        PreferencesFragment newFragment = new PreferencesFragment();
        Bundle data = new Bundle();
        data.putString("rootKey", rootKey);
        newFragment.setArguments(data);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            fT.setCustomAnimations(R.anim.move_in_prefs_right, R.anim.move_out_prefs_left, R.anim.move_in_prefs_left, R.anim.move_out_prefs_right)
                    .replace(R.id.main_frame, newFragment, FRAGMENT_TAG_1)
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
            onBackPressedCallback.setEnabled(true);
        } else {
            fT.replace(R.id.right, newFragment, FRAGMENT_TAG_1).commitAllowingStateLoss();
        }

        this.fragment = newFragment;

        initTitleBar();

        return true;
    }
}
