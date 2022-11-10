package com.firebirdberlin.nightdream;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference.SummaryProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.firebirdberlin.HueApi.models.HueViewModel;
import com.firebirdberlin.nightdream.ui.HueConnectPreferenceFragmentCompat;
import com.firebirdberlin.nightdream.ui.HueConnectPreference;

public class SmartHomePreferenceFragment extends PreferenceFragmentCompat {
    public static final String PREFS_KEY = "NightDream preferences";
    public static final String TAG = "SmartHomePrefFragment";

    Settings settings = null;

    SharedPreferences.OnSharedPreferenceChangeListener prefChangedListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    Log.d(TAG, "onSharedPreferenceChanged: " + key);
                    switch (key) {
                        case "switchHue":
                            connectToHueBridge(sharedPreferences);
                            break;
                    }
                }
            };


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(PREFS_KEY);
        setPreferencesFromResource(R.xml.preferences_smarthome, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        settings = new Settings(getContext());
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        prefs.registerOnSharedPreferenceChangeListener(prefChangedListener);

        EditTextPreference passwordPreference = findPreference("smart_home_avm_password");
        if (passwordPreference != null) {
            passwordPreference.setSummaryProvider(new SummaryProvider() {
                @Override
                public CharSequence provideSummary(Preference preference) {
                    String text = "";
                    for (int i = 0; i < ((EditTextPreference) preference).getText().toString().length(); i++) {
                        text += "*";
                    }
                    return text;
                }
            });
            passwordPreference.setOnBindEditTextListener(
                    new EditTextPreference.OnBindEditTextListener() {
                        @Override
                        public void onBindEditText(@NonNull final EditText editText) {
                            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        }
                    }
            );
        }

        connectToHueBridge(prefs);
    }

    private void showPreference(String key, boolean visible) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setVisible(visible);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        getParentFragmentManager().setFragmentResultListener("hueKey", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                SharedPreferences prefs = getPreferenceManager().getSharedPreferences();

                // We use a String here, but any type that can be put in a Bundle is supported
                String hueKey = bundle.getString("hueKey");
                Log.d(TAG, "onCreate() setFragmentResultListener: " + hueKey);

                // Do something with the result

                if (hueKey.contains("Error")) {
                    handleHueError("connectHue", hueKey);
                } else {
                    Log.d(TAG, "onCreate() Key found");
                    //Key found
                    if (prefs != null) {
                        testHueKey(prefs.getString("hueBridgeIP", ""), hueKey);
                    }
                }
            }
        });
    }

    private void connectToHueBridge(SharedPreferences prefs) {
        Log.d(TAG, "connectToHueBridge()");
        Preference switchHuePreference = findPreference("switchHue");
        Preference hueKeyPreference = findPreference("connectHue");

        if (switchHuePreference != null) {
            boolean on = prefs.getBoolean("switchHue", false);
            Log.d(TAG, "connectToHueBridge() enable:" + on);

            if (on) {
                //return no Network connection
                if (getContext() != null && !Utility.hasNetworkConnection(getContext())) {
                    Log.e(TAG, "connectToHueBridge() no Network Connection");
                    handleHueError("switchHue", "No Network connection");
                    return;
                }

                switchHuePreference.setSummary("suche nach Hue Bridge");
                Log.d(TAG, "connectToHueBridge() prefs hueBridgeIP: " + prefs.getString("hueBridgeIP", "No IP"));
                String hueBridgeIP = prefs.getString("hueBridgeIP", "");

                if (hueBridgeIP.isEmpty() || !HueViewModel.testHueIP(hueBridgeIP)) {
                    Log.d(TAG, "connectToHueBridge() no working hueBridgeIP in prefs");
                    HueViewModel.observeIP(getContext(), getViewLifecycleOwner(), bridgeIP -> {
                        Log.d(TAG, "connectToHueBridge() found hueBridgeIP: " + bridgeIP);

                        //Save IP
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("hueBridgeIP", bridgeIP);
                        editor.apply();

                        boolean showIp = prefs.getBoolean("switchHue", false);
                        if (showIp && !bridgeIP.isEmpty()) {
                            if (bridgeIP.contains("Error")) {
                                handleHueError("switchHue", bridgeIP);
                            } else {
                                switchHuePreference.setSummary(bridgeIP);
                                enablePreference("connectHue", true);

                                String hueKey = prefs.getString("hueKey", "");
                                if (hueKey.isEmpty()) {
                                    Log.d(TAG, "connectToHueBridge() empty hueKey in prefs");
                                    if (hueKeyPreference != null) {
                                        hueKeyPreference.setSummary("not connected");
                                    }
                                    showHueDialog(prefs, "connectHue");
                                } else {
                                    Log.d(TAG, "connectToHueBridge() found hueKey in prefs: " + hueKey);
                                    testHueKey(bridgeIP, hueKey);
                                }
                            }
                        }
                    });
                } else {
                    //bridge IP found in prefs
                    Log.d(TAG, "connectToHueBridge() hueBridgeIP found in prefs: "+hueBridgeIP);
                    switchHuePreference.setSummary(hueBridgeIP);
                    enablePreference("connectHue", true);
                    String hueKey = prefs.getString("hueKey", "");
                    if (hueKey.isEmpty()) {
                        Log.d(TAG, "connectToHueBridge() empty hueKey in prefs");
                        if (hueKeyPreference != null) {
                            hueKeyPreference.setSummary("not connected");
                        }
                    } else {
                        Log.d(TAG, "connectToHueBridge() found hueKey in prefs: " + hueKey);
                        testHueKey(hueBridgeIP, hueKey);
                    }
                }
            } else {
                switchHuePreference.setSummary("");
                if (hueKeyPreference != null) {
                    hueKeyPreference.setSummary("");
                }
                enablePreference("connectHue", false);
                switchHuePreference.setIcon(R.drawable.ic_hue_bridge_connect);
            }
        }
    }

    private void testHueKey(String hueBridgeIP, String hueKey) {
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        Preference switchHuePreference = findPreference("switchHue");
        Preference switchHueKeyPreference = findPreference("connectHue");

        if (prefs != null) {
            if (HueViewModel.testHueConnection(hueBridgeIP, hueKey)) {
                Log.d(TAG, "onCreate() testHueConnection ok");
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("hueKey", hueKey);
                editor.apply();

                if (switchHuePreference != null) {
                    switchHuePreference.setIcon(R.drawable.ic_hue_bridge);
                }

                if (switchHueKeyPreference != null) {
                    switchHueKeyPreference.setSummary("Connected");
                }
            } else {
                Log.e(TAG, "onCreate() testHueConnection failed");
                handleHueError("connectHue", "Can not connect to Hue Bridge");
            }
        }
    }

    private void showHueDialog(SharedPreferences prefs, String key) {
        Log.d(TAG, "showHueDialog");
        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        Fragment prev = getParentFragmentManager().findFragmentByTag("HueDialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        DialogFragment dialogFragment = HueConnectPreferenceFragmentCompat
                .newInstance(key, prefs.getString("hueBridgeIP", ""));

        dialogFragment.setTargetFragment(this, 0);
        dialogFragment.show(ft, "HueDialog");
    }

    private void handleHueError(String huePreferenceKey, String errorMessage) {
        Log.d(TAG, "handleHueError() : "+errorMessage);
        Preference huePreference = findPreference(huePreferenceKey);
        if (huePreference != null) {
            String[] splitStr = errorMessage.split(":");
            Log.e(TAG, "handleHueError(): " + splitStr[splitStr.length - 1].trim());
            switch (splitStr[splitStr.length - 1].trim()) {
                case "No Network connection":
                case "Host unreachable":
                case "link button not pressed":
                case "Can not connect to Hue Bridge":
                case "No address associated with hostname":
                    huePreference.setSummary(splitStr[splitStr.length - 1].trim());
                    break;
                default:
                    huePreference.setSummary("Unknown Error");
                    break;
            }
        }
    }

    private void enablePreference(String key, boolean on) {
        Preference preference = findPreference(key);
        if (preference != null) {
            Log.d(TAG, "enablePreference(): " + preference.getKey());
            preference.setEnabled(on);
        } else {
            Log.w(TAG, "WARNING: preference " + key + " not found.");
        }
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof HueConnectPreference) {
            Log.d(TAG, "onDisplayPreferenceDialog() preference instanceof HueConnectPreference");
            showHueDialog(getPreferenceManager().getSharedPreferences(), preference.getKey());
        } else super.onDisplayPreferenceDialog(preference);
    }
}
