package com.firebirdberlin.nightdream;

import java.util.ArrayList;

import org.json.JSONObject;
import org.json.JSONException;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceActivity;
import android.os.Build;

import com.android.vending.billing.IInAppBillingService;

public class PreferencesActivity extends PreferenceActivity {
    public static final String ITEM_DONATION = "donation";
    public static final String ITEM_WEATHER_DATA = "weather_data";
    public static final int REQUEST_CODE_PURCHASE_DONATION = 1001;
    public static final int REQUEST_CODE_PURCHASE_WEATHER = 1002;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
        .replace(android.R.id.content, new PreferencesFragment())
        .commit();
    }

    public static void start(Context context) {
        if (Build.VERSION.SDK_INT > 10) {
            Intent myIntent = new Intent(context, PreferencesActivity.class);
            context.startActivity(myIntent);
        } else {
            PreferencesActivityv9.start(context);
        }
    }


    IInAppBillingService mService;
    public boolean purchased_donation = false;
    public boolean purchased_weather_data = false;

    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
                IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
            getPurchases();
        }
    };

    private void getPurchases() {
        if (mService == null) {
            return;
        }

        Bundle ownedItems = null;
        try {
            ownedItems = mService.getPurchases(3, getPackageName(), "inapp", null);
        } catch (RemoteException e) {
            return;
        }

        if (ownedItems == null) return;

        int response = ownedItems.getInt("RESPONSE_CODE");
        if (response == 0) {
            ArrayList<String> ownedSkus =
                ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
            ArrayList<String>  purchaseDataList =
                ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
            ArrayList<String>  signatureList =
                ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
            String continuationToken =
                ownedItems.getString("INAPP_CONTINUATION_TOKEN");

            for (int i = 0; i < purchaseDataList.size(); ++i) {
                String purchaseData = purchaseDataList.get(i);
                String signature = signatureList.get(i);
                String sku = ownedSkus.get(i);

                if (sku.equals(ITEM_DONATION)) {
                    purchased_donation = true;
                    purchased_weather_data = true;
                }
                if (sku.equals(ITEM_WEATHER_DATA)) {
                    purchased_weather_data = true;
                }

                // do something with this purchase information
                // e.g. display the updated list of products owned by user
            }

            // if continuationToken != null, call getPurchases again
            // and pass in the token to retrieve more items
        }

    }

    public void purchaseIntent(String sku, int REQUEST_CODE) {
        if (mService == null) return;
        try {
            String developerPayload = "abcdefghijklmnopqrstuvwxyz";
            Bundle buyIntentBundle = mService.getBuyIntent(3, getPackageName(),
                    sku, "inapp",developerPayload);
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            startIntentSenderForResult(pendingIntent.getIntentSender(),
                    REQUEST_CODE, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                    Integer.valueOf(0));
        } catch (RemoteException e1) {
            return;
        } catch (SendIntentException e2) {
            return;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PURCHASE_DONATION ||
                requestCode == REQUEST_CODE_PURCHASE_WEATHER) {
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            if (resultCode == RESULT_OK) {
                try {
                    JSONObject jo = new JSONObject(purchaseData);
                    String sku = jo.getString("productId");
                    if (sku.equals(ITEM_DONATION) ) {
                        purchased_donation = true;
                        purchased_weather_data = true;
                        showThankYouDialog();
                    } else
                    if (sku.equals(ITEM_WEATHER_DATA) ) {
                        purchased_weather_data = true;
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void showThankYouDialog() {
        new AlertDialog.Builder(this)
            .setTitle(getResources().getString(R.string.dialog_title_thank_you))
            .setMessage(R.string.dialog_message_thank_you)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }


}
