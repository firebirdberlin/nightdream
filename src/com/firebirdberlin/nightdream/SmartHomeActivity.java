package com.firebirdberlin.nightdream;

import android.Manifest;
import android.animation.LayoutTransition;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.AlarmClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.firebirdberlin.nightdream.ui.SmartHomeDeviceLayout;

import com.firebirdberlin.AvmAhaApi.AvmAhaRequestTask;
import com.firebirdberlin.AvmAhaApi.models.AvmAhaDevice;
import com.firebirdberlin.AvmAhaApi.models.AvmCredentials;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class SmartHomeActivity
        extends BillingHelperActivity
        implements AvmAhaRequestTask.AsyncResponse {
    static final String TAG = "SmartHomeActivity";

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }

    final int MENU_ITEM_PREFERENCES = 3000;
    private final HashMap<String, SmartHomeDeviceLayout> layoutHashMap = new HashMap<>();
    private LinearLayout scrollView = null;
    private List<AvmAhaDevice> entries = null;
    private Settings settings = null;
    AvmCredentials credentials = null;

    public static void start(Context context) {
        Intent intent = new Intent(context, SmartHomeActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_smart_home);
        setTheme(R.style.AlarmClockActivityTheme);

        scrollView = findViewById(R.id.scroll_view);

        // https://www.youtube.com/watch?v=55wLsaWpQ4g
        LayoutTransition layoutTransition = scrollView.getLayoutTransition();
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // return super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, MENU_ITEM_PREFERENCES, Menu.NONE, getString(R.string.preferences))
                .setIcon(R.drawable.ic_settings)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //return super.onOptionsItemSelected(item);

        if (item.getItemId() == MENU_ITEM_PREFERENCES) {
            SmartHomePreferenceActivity.start(this);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume()");
        super.onResume();

        init();
    }

    @Override
    protected void onPause() {
        super.onPause();
        new AvmAhaRequestTask(this, credentials).closeSession();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void init() {
        update();
    }


    private void update() {
        settings = new Settings(this);
        credentials = new AvmCredentials(
                settings.getString("smart_home_avm_host"),
                settings.getString("smart_home_avm_username"),
                settings.getString("smart_home_avm_password")
        );
        new AvmAhaRequestTask(this, credentials).fetchDeviceList();
        //Collections.sort(entries, (obj1, obj2) -> obj1.getCalendar().compareTo(obj2.getCalendar()));
    }

    public void onAhaRequestFinished() {
        Log.d(TAG, "onAhaRequestFinished");
    }
    public void onAhaDeviceListReceived(List<AvmAhaDevice> deviceList) {
        scrollView.removeAllViews();
        if (deviceList == null) {
            return;
        }
        Log.d(TAG, "onAhaDeviceListReceived: " + deviceList.size() );

        for(AvmAhaDevice device : deviceList) {
            if (!device.isSwitchable()) continue;

            SmartHomeDeviceLayout layout = layoutHashMap.get(device.ain);

            if (layout == null) {
                layout = new SmartHomeDeviceLayout(this, device);
                layoutHashMap.put(device.ain, layout);
            }
            scrollView.addView(layout);
        }
        scrollView.invalidate();
    }

    public void onDeviceStateChangeRequest(AvmAhaDevice device, String newState) {
        Log.d(TAG, "onDeviceStateChangeRequest " + device.toString());
        new AvmAhaRequestTask(this, credentials).setSimpleOnOff(device, newState);
    }

    public void onAhaDeviceStateChanged(AvmAhaDevice device) {
        Log.d(TAG, "onDeviceStateChanged " + device.toString());

        SmartHomeDeviceLayout layout = layoutHashMap.get(device.ain);
        if (layout != null) {
            layout.update(device);
        }
    }
}
