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
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.firebirdberlin.nightdream.ui.SmartHomeDeviceLayout;
import com.firebirdberlin.AvmAhaApi.AvmAhaRequestTask;
import com.firebirdberlin.AvmAhaApi.models.AvmAhaDevice;
import com.firebirdberlin.AvmAhaApi.models.AvmCredentials;

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
    private Snackbar snackbar;
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
                scrollView.addView(layout);
            } else {
                layout.update(device);
            }
        }
        scrollView.invalidate();
    }

    public void onDeviceStateChangeRequest(AvmAhaDevice device, String newState) {
        Log.d(TAG, "onDeviceStateChangeRequest " + device.toString());
        if (isPurchased(BillingHelperActivity.ITEM_ACTIONS)) {
            new AvmAhaRequestTask(this, credentials).setSimpleOnOff(device, newState);
        } else {
            // reset the switch state to the current device state
            SmartHomeDeviceLayout layout = layoutHashMap.get(device.ain);
            if (layout != null) {
                layout.update(device);
            }
            showPurchaseDialog();
        }
    }

    public void onAhaDeviceStateChanged(AvmAhaDevice device) {
        Log.d(TAG, "onDeviceStateChanged " + device.toString());

        SmartHomeDeviceLayout layout = layoutHashMap.get(device.ain);
        if (layout != null) {
            layout.update(device);
        }
    }
    public void onAhaConnectionError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onPurchasesInitialized() {
        super.onPurchasesInitialized();
        conditionallyShowSnackBar();
    }

    @Override
    protected void onItemPurchased(String sku) {
        super.onItemPurchased(sku);
        conditionallyShowSnackBar();
    }

    private void conditionallyShowSnackBar() {
        if (!isPurchased(BillingHelperActivity.ITEM_ACTIONS)) {
            View view = findViewById(android.R.id.content);
            snackbar = Snackbar.make(
                    view, R.string.smart_home_purchase_request, Snackbar.LENGTH_INDEFINITE
            );
            int color = Utility.getRandomMaterialColor(this);
            int textColor = Utility.getContrastColor(color);
            View snackbarView = snackbar.getView();
            snackbarView.setBackgroundColor(color);
            snackbar.setActionTextColor(textColor);

            TextView tv = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
            tv.setTextColor(textColor);

            snackbar.setAction(android.R.string.ok, new RequestPurchaseListener());
            snackbar.show();
        } else {
            dismissSnackBar();
        }
    }

    public class RequestPurchaseListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            showPurchaseDialog();
        }
    }
    void dismissSnackBar() {
        if (snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
            snackbar = null;
        }
    }
}
