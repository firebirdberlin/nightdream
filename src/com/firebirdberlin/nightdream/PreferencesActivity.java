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
    private static final String FRAGMENT_TAG_1 = "f1"; // For master/single pane fragment
    private static final String FRAGMENT_TAG_2 = "f2"; // For detail pane fragment in landscape

    PreferencesFragment fragment = null; // Master/single pane fragment
    PreferencesFragment fragment2 = null; // Detail pane fragment in landscape
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
        Log.d(TAG, "onCreate: savedInstanceState is " + (savedInstanceState == null ? "null" : "not null"));
        super.onCreate(savedInstanceState);
        setTheme(R.style.PreferencesTheme);

        setContentView(R.layout.preferences_layout);

        // Initialize onBackPressedCallback early
        onBackPressedCallback = new OnBackPressedCallback(isBackEnabled()) {
            @Override
            public void handleOnBackPressed() {
                Log.d(TAG, "handleOnBackPressed");
                FragmentManager fm = getSupportFragmentManager();

                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    if (!rootKey.isEmpty()) { // A sub-preference is active in landscape (detail pane)
                        // Remove the current detail fragment
                        if (fragment2 != null) {
                            fm.beginTransaction().remove(fragment2).commitAllowingStateLoss();
                            fragment2 = null;
                        }
                        rootKey = ""; // Clear the rootKey, showing no sub-preference
                        currentTitle = getString(R.string.preferences); // Reset title to main preferences
                        initTitleBar();
                        setEnabled(false); // Disable callback as no sub-preference is active in detail pane
                    } else {
                        // No sub-preference in detail pane, perform default back action (exit activity)
                        finish();
                    }
                } else { // Portrait mode
                    if (fm.getBackStackEntryCount() > 0) {
                        fm.popBackStack();
                    } else {
                        rootKey = "";
                        currentTitle = getString(R.string.preferences);
                        initTitleBar();
                        setEnabled(false);
                    }
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
        Log.d(TAG, "onCreate: onBackPressedCallback initialized");

        if (savedInstanceState == null) {
            Log.d(TAG, "onCreate: savedInstanceState is null. Initial setup.");
            // Activity is creating for the first time, not rotating
            currentTitle = getString(R.string.preferences);
            setupInitialFragments(); // Sets up the master fragment

            // Programmatically open the "autostart" preference in the detail pane on initial landscape launch
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Log.d(TAG, "onCreate: Landscape initial setup. Opening autostart.");
                Preference autostartPref = new Preference(this);
                autostartPref.setKey("autostart");
                autostartPref.setTitle(getString(R.string.handle_power));
                onPreferenceStartFragment(null, autostartPref);
            }
        } else {
            Log.d(TAG, "onCreate: savedInstanceState not null. Restoring state.");
            // Activity is being recreated (e.g., rotation)
            rootKey = savedInstanceState.getString(ROOT_KEY, "");
            currentTitle = savedInstanceState.getCharSequence(KEY_TITLE);
            Log.d(TAG, "onCreate: Restored rootKey = " + rootKey + ", currentTitle = " + currentTitle);

            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction fT = fm.beginTransaction();
            boolean needCommit = false;

            Log.d(TAG, "onCreate: Handling restoration for orientation: " + (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? "PORTRAIT" : "LANDSCAPE"));

            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                // In portrait, the main frame should display the active preference (or main if rootKey is empty)
                PreferencesFragment newFragment = new PreferencesFragment();
                Bundle data = new Bundle();
                data.putString("rootKey", rootKey); // Use the restored rootKey for portrait
                newFragment.setArguments(data);
                fT.replace(R.id.main_frame, newFragment, FRAGMENT_TAG_1);
                fragment = newFragment; // Update reference
                needCommit = true;

                // Clear back stack in portrait as landscape doesn't use it for detail
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }
            } else { // Landscape
                // Always add/replace the master fragment to the left pane
                PreferencesFragment masterFragment = new PreferencesFragment();
                Bundle masterData = new Bundle();
                masterData.putString("rootKey", ""); // Master fragment always has empty rootKey
                masterFragment.setArguments(masterData);
                fT.replace(R.id.right, masterFragment, FRAGMENT_TAG_1);
                fragment = masterFragment; // Update reference
                needCommit = true;

                // If a detail pane was active, re-add it to R.id.details
                if (!rootKey.isEmpty()) {
                    Log.d(TAG, "onCreate: Landscape restoration. Re-opening detail fragment with rootKey: " + rootKey);
                    PreferencesFragment detailFragment = new PreferencesFragment();
                    Bundle detailData = new Bundle();
                    detailData.putString("rootKey", rootKey);
                    detailFragment.setArguments(detailData);
                    fT.replace(R.id.details, detailFragment, FRAGMENT_TAG_2);
                    fragment2 = detailFragment; // Update reference
                    needCommit = true;
                } else { // If rootKey is empty (no sub-preference was active), open autostart by default
                    Log.d(TAG, "onCreate: Landscape restoration with empty rootKey. Opening autostart by default.");
                    Preference autostartPref = new Preference(this);
                    autostartPref.setKey("autostart");
                    autostartPref.setTitle(getString(R.string.autostart));
                    onPreferenceStartFragment(null, autostartPref);
                }
            }

            if (needCommit) {
                Log.d(TAG, "onCreate: Committing fragment transaction during restoration.");
                fT.commitAllowingStateLoss();
                getSupportFragmentManager().executePendingTransactions();
            }
        }

        initTitleBar();

        getSupportFragmentManager().addOnBackStackChangedListener(this::onBackStackChanged);
    }

    private boolean isBackEnabled() {
        Log.d(TAG, "isBackEnabled: current orientation is " + (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? "PORTRAIT" : "LANDSCAPE"));
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            return getSupportFragmentManager().getBackStackEntryCount() > 0;
        } else { // Landscape
            return !rootKey.isEmpty();
        }
    }

    private void setupInitialFragments() {
        Log.d(TAG, "setupInitialFragments");
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fT = fm.beginTransaction();

        fragment = new PreferencesFragment();
        Bundle data = new Bundle();
        data.putString("rootKey", ""); // rootKey will be "" for initial root preferences
        fragment.setArguments(data);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d(TAG, "setupInitialFragments: Portrait, replacing R.id.main_frame");
            fT.replace(R.id.main_frame, fragment, FRAGMENT_TAG_1);
        } else { // Landscape
            Log.d(TAG, "setupInitialFragments: Landscape, replacing R.id.right");
            // Only add the master fragment to the left pane
            fT.replace(R.id.right, fragment, FRAGMENT_TAG_1);
            // The details pane (R.id.details) remains empty initially.
            // fragment2 will be null, and will be set when onPreferenceStartFragment is called.
        }
        fT.commitAllowingStateLoss();
        fm.executePendingTransactions();
    }


    private void onBackStackChanged() {
        Log.d(TAG, "onBackStackChanged: Back stack entry count = " + getSupportFragmentManager().getBackStackEntryCount());
        FragmentManager fm = getSupportFragmentManager();
        int backStackEntryCount = fm.getBackStackEntryCount();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // In landscape, back stack is not used for detail pane navigation,
            // so we rely on rootKey to determine back button state.
            if (rootKey.isEmpty()) {
                onBackPressedCallback.setEnabled(false);
                currentTitle = getString(R.string.preferences);
            } else {
                onBackPressedCallback.setEnabled(true);
                // currentTitle is already set in onPreferenceStartFragment
            }
        }

        else { // Portrait
            if (backStackEntryCount == 0) {
                rootKey = "";
                currentTitle = getString(R.string.preferences);
                onBackPressedCallback.setEnabled(false);
            } else {
                // A sub-preference screen is active.
                // We rely on onPreferenceStartFragment to set currentTitle.
                onBackPressedCallback.setEnabled(true);
            }
        }
        initTitleBar(); // Update action bar with potentially new currentTitle
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    private void removeFragment(Fragment removeFragment) {
        Log.d(TAG, "removeFragment()");
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (removeFragment != null && fragmentManager.findFragmentById(removeFragment.getId()) != null) {
            fragmentManager.beginTransaction().remove(removeFragment).commitAllowingStateLoss();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged()");
        super.onConfigurationChanged(newConfig);

        // The system recreates the Activity, so onCreate will be called.
        // We don't need to do much here other than update UI elements if necessary.
        // The fragments will be re-attached by the FragmentManager.
        // We need to ensure the title bar is updated and the back callback is enabled/disabled correctly.
        initTitleBar();
        if (onBackPressedCallback != null) {
            onBackPressedCallback.setEnabled(isBackEnabled());
        }
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
            runOnUiThread(() -> {
                fragment.onPurchasesInitialized();
            });
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

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        Log.d(TAG, "onPreferenceStartFragment: pref key = " + pref.getKey());
        Log.d(TAG, "onPreferenceStartFragment: current rootKey = " + rootKey);

        rootKey = pref.getKey();
        currentTitle = pref.getTitle();

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fT = fm.beginTransaction();

        PreferencesFragment newFragment = new PreferencesFragment();
        Bundle data = new Bundle();
        data.putString("rootKey", rootKey);
        newFragment.setArguments(data);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d(TAG, "onPreferenceStartFragment: Portrait. Replacing R.id.main_frame");
            fT.setCustomAnimations(R.anim.move_in_prefs_right, R.anim.move_out_prefs_left, R.anim.move_in_prefs_left, R.anim.move_out_prefs_right)
                    .replace(R.id.main_frame, newFragment, FRAGMENT_TAG_1)
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
            onBackPressedCallback.setEnabled(true);
            this.fragment = newFragment; // Update fragment reference for portrait
        } else { // Landscape
            Log.d(TAG, "onPreferenceStartFragment: Landscape. Replacing R.id.details");
            // Replace the details pane (R.id.details) with the new sub-preference fragment
            fT.replace(R.id.details, newFragment, FRAGMENT_TAG_2) // Use FRAGMENT_TAG_2 for the details pane
                    .commitAllowingStateLoss();
            this.fragment2 = newFragment; // Keep track of the current detail fragment
            onBackPressedCallback.setEnabled(true); // Enable back button if detail pane has content
        }

        initTitleBar();

        return true;
    }
}