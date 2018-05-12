package com.firebirdberlin.nightdream;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;

import java.util.Map;

public abstract class BillingHelperActivity extends Activity {
    static final String TAG = "BillingActivity";

    IInAppBillingService mService;
    Map<String, Boolean> purchases;
    BillingHelper billingHelper;

    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "IIAB service disconnected");
            billingHelper = null;
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "IIAB service connected");
            mService = IInAppBillingService.Stub.asInterface(service);
            billingHelper = new BillingHelper(getApplicationContext(), mService);
            purchases = billingHelper.getPurchases();
            if (billingHelper.isPurchased(BillingHelper.ITEM_WEB_RADIO)) {
                Log.i(TAG, "Web Radio is purchased");
            } else {
                Log.i(TAG, "Web Radio is NOT purchased");
            }
        }
    };

    public boolean isPurchased(String sku) {
        Log.i(TAG, "Checking purchase " + sku);
        if (Utility.isDebuggable(this)) {
            return true;
        }
        if (billingHelper == null) {
            return false;
        }
        Log.i(TAG, " => " + String.valueOf(billingHelper.isPurchased(sku)));
        return billingHelper.isPurchased(sku);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // bind the in-app billing service
        Intent serviceIntent =
                new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConn);
    }
}
