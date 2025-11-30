/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
                    Log.d(TAG, "handleOnBackPressed: Landscape mode. Finishing activity.");
                    finish();
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
                autostartPref.setTitle(getString(R.string.handle_power)); // Corrected string resource
                onPreferenceStartFragment(fragment, autostartPref);
            }
        } else {
            Log.d(TAG, "onCreate: savedInstanceState not null. Restoring state.");
            // Activity is being recreated (e.g., rotation)
            rootKey = savedInstanceState.getString(ROOT_KEY, "");
            currentTitle = savedInstanceState.getCharSequence(KEY_TITLE);
            Log.d(TAG, "onCreate: Restored rootKey = " + rootKey + ", currentTitle = " + currentTitle);

            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction fT = fm.beginTransaction();

            Log.d(TAG, "onCreate: Handling restoration for orientation: " + (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? "PORTRAIT" : "LANDSCAPE"));

            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                // In portrait, the main frame should display the active preference (or main if rootKey is empty)
                PreferencesFragment newFragment = new PreferencesFragment();
                Bundle data = new Bundle();
                data.putString("rootKey", rootKey); // Use the restored rootKey for portrait
                newFragment.setArguments(data);
                fT.replace(R.id.main_frame, newFragment, FRAGMENT_TAG_1);
                fragment = newFragment; // Update reference

                // ONLY add to back stack if a sub-preference was active
                if (!rootKey.isEmpty()) {
                    Log.d(TAG, "onCreate: Portrait restoration with active rootKey: " + rootKey + ". Adding to back stack.");
                    fT.addToBackStack(null);
                } else if (fm.getBackStackEntryCount() > 0) {
                    // If rootKey is empty and there's a back stack, clear it.
                    Log.d(TAG, "onCreate: Portrait restoration with empty rootKey. Clearing back stack.");
                    fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }

            } else { // Landscape
                // Always add/replace the master fragment to the left pane
                PreferencesFragment masterFragment = new PreferencesFragment();
                Bundle masterData = new Bundle();
                masterData.putString("rootKey", ""); // Master fragment always has empty rootKey
                masterFragment.setArguments(masterData);
                fT.replace(R.id.preferences_menu, masterFragment, FRAGMENT_TAG_1);
                fragment = masterFragment; // Update reference

                // If a detail pane was active, re-add it to R.id.details
                if (!rootKey.isEmpty()) {
                    Log.d(TAG, "onCreate: Landscape restoration. Re-opening detail fragment with rootKey: " + rootKey);
                    PreferencesFragment detailFragment = new PreferencesFragment();
                    Bundle detailData = new Bundle();
                    detailData.putString("rootKey", rootKey);
                    detailFragment.setArguments(detailData);
                    fT.replace(R.id.details, detailFragment, FRAGMENT_TAG_2);
                    fragment2 = detailFragment; // Update reference
                } else { // If rootKey is empty (no sub-preference was active), open autostart by default
                    Log.d(TAG, "onCreate: Landscape restoration with empty rootKey. Opening autostart by default.");
                    Preference autostartPref = new Preference(this);
                    autostartPref.setKey("autostart");
                    autostartPref.setTitle(getString(R.string.handle_power)); // Corrected string resource
                    onPreferenceStartFragment(fragment, autostartPref);
                }
            }

            Log.d(TAG, "onCreate: Committing fragment transaction during restoration.");
            fT.commitAllowingStateLoss();
            getSupportFragmentManager().executePendingTransactions();
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
            Log.d(TAG, "setupInitialFragments: Landscape, replacing R.id.preferences_menu");
            // Only add the master fragment to the left pane
            fT.replace(R.id.preferences_menu, fragment, FRAGMENT_TAG_1);
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

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (backStackEntryCount == 0) {
                rootKey = "";
                currentTitle = getString(R.string.preferences);
                onBackPressedCallback.setEnabled(false);

                Log.d(TAG, "onBackStackChanged: Back stack empty. Scheduling root fragment recreation.");
                // Use post to ensure this runs after the current transaction finishes
                new Handler(Looper.getMainLooper()).post(() -> {
                    Log.d(TAG, "Executing deferred fragment transaction for root.");
                    // It's a good practice to re-check the back stack entry count here
                    // because the state might have changed again by the time this runs.
                    if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                        FragmentManager currentFm = getSupportFragmentManager(); // Get FM again to be safe
                        FragmentTransaction fT = currentFm.beginTransaction();
                        PreferencesFragment mainFragment = new PreferencesFragment();
                        Bundle data = new Bundle();
                        data.putString("rootKey", "");
                        mainFragment.setArguments(data);

                        fT.replace(R.id.main_frame, mainFragment, FRAGMENT_TAG_1);
                        // No addToBackStack needed here as we are restoring the root
                        fT.commitAllowingStateLoss(); // Use commitAllowingStateLoss as before
                        currentFm.executePendingTransactions(); // Execute immediately after commit
                        this.fragment = mainFragment; // Update the reference
                    } else {
                        Log.d(TAG, "Deferred fragment transaction skipped: back stack is no longer empty.");
                    }
                });
            } else {
                // A sub-preference screen is active.
                // We rely on onPreferenceStartFragment to set currentTitle.
                onBackPressedCallback.setEnabled(true);
            }
        } else { // landscape
            finish();
        }
        initTitleBar(); // Update action bar with potentially new currentTitle
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
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
            runOnUiThread(() -> fragment.onPurchasesInitialized());
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
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, Preference pref) {
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