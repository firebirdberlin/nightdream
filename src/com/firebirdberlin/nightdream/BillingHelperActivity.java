package com.firebirdberlin.nightdream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BillingHelperActivity extends Activity {
    static final String TAG = "BillingActivity";


    public static final int REQUEST_CODE_PURCHASE_DONATION = 1001;
    public static final int REQUEST_CODE_PURCHASE_WEATHER = 1002;
    public static final int REQUEST_CODE_PURCHASE_WEB_RADIO = 1003;
    public static final int REQUEST_CODE_PURCHASE_PRO = 1004;
    private static final int PRODUCT_ID_WEATHER_DATA = 0;
    private static final int PRODUCT_ID_WEB_RADIO = 1;
    private static final int PRODUCT_ID_DONATION = 2;
    private static final int PRODUCT_ID_PRO = 3;

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

    public void showPurchaseDialog() {
        Log.i(TAG, "showPurchaseDialog()");
        if (isPurchased(BillingHelper.ITEM_DONATION)) return;
        List<CharSequence> entries = new ArrayList<>();
        final List<Integer> values = new ArrayList<>();
        HashMap<String, String> prices = getPrices();

        boolean purchased_weather_data = isPurchased(BillingHelper.ITEM_WEATHER_DATA);
        boolean purchased_web_radio = isPurchased(BillingHelper.ITEM_WEB_RADIO);
        boolean purchased_pro = isPurchased(BillingHelper.ITEM_PRO);
        boolean purchased_donation = isPurchased(BillingHelper.ITEM_DONATION);

        if (!purchased_weather_data) {
            entries.add(getProductWithPrice(prices, R.string.product_name_weather,
                    BillingHelper.ITEM_WEATHER_DATA));
            values.add(PRODUCT_ID_WEATHER_DATA);
        }

        if (!purchased_web_radio) {
            entries.add(getProductWithPrice(prices, R.string.product_name_webradio,
                    BillingHelper.ITEM_WEB_RADIO));
            values.add(PRODUCT_ID_WEB_RADIO);
        }

        if (!purchased_pro && !purchased_weather_data && !purchased_web_radio) {
            entries.add(getProductWithPrice(prices, R.string.product_name_pro,
                    BillingHelper.ITEM_PRO));
            values.add(PRODUCT_ID_PRO);
        }

        if (!purchased_donation) {
            entries.add(getProductWithPrice(prices, R.string.product_name_donation,
                    BillingHelper.ITEM_DONATION));
            values.add(PRODUCT_ID_DONATION);
        }

        new AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle(getResources().getString(R.string.buy))
                .setItems(
                        entries.toArray(new CharSequence[entries.size()]),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                Log.i(TAG, String.format("selected %d", which));
                                int selected = values.get(which);
                                switch (selected) {
                                    case PRODUCT_ID_WEATHER_DATA:
                                        purchaseIntent(BillingHelper.ITEM_WEATHER_DATA, REQUEST_CODE_PURCHASE_WEATHER);
                                        break;
                                    case PRODUCT_ID_WEB_RADIO:
                                        purchaseIntent(BillingHelper.ITEM_WEB_RADIO, REQUEST_CODE_PURCHASE_WEB_RADIO);
                                        break;
                                    case PRODUCT_ID_DONATION:
                                        purchaseIntent(BillingHelper.ITEM_DONATION, REQUEST_CODE_PURCHASE_DONATION);
                                        break;
                                    case PRODUCT_ID_PRO:
                                        purchaseIntent(BillingHelper.ITEM_PRO, REQUEST_CODE_PURCHASE_PRO);
                                        break;
                                }
                            }
                        })
                .setNeutralButton(android.R.string.cancel, null)
                .show();
    }

    public void purchaseIntent(String sku, int REQUEST_CODE) {
        if (mService == null) return;
        try {
            String developerPayload = "abcdefghijklmnopqrstuvwxyz";
            Bundle buyIntentBundle = mService.getBuyIntent(
                    3, getPackageName(),
                    sku, "inapp", developerPayload
            );
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            startIntentSenderForResult(pendingIntent.getIntentSender(),
                    REQUEST_CODE, new Intent(), 0, 0, 0);
        } catch (RemoteException | IntentSender.SendIntentException | NullPointerException ignored) {
        }
    }

    private String getProductWithPrice(HashMap<String, String> prices, int resId, String sku) {
        String price = prices.get(sku);
        if (price != null) {
            return String.format("%s (%s)", getResources().getString(resId), price);
        }
        return getResources().getString(resId);
    }

    private HashMap<String, String> getPrices() {
        HashMap<String, String> map = new HashMap<>();
        if (mService == null) return map;

        ArrayList<String> skuList = new ArrayList<String>();
        skuList.add(BillingHelper.ITEM_WEATHER_DATA);
        skuList.add(BillingHelper.ITEM_WEB_RADIO);
        skuList.add(BillingHelper.ITEM_DONATION);
        skuList.add(BillingHelper.ITEM_PRO);
        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList("ITEM_ID_LIST", skuList);

        Bundle skuDetails;
        try {
            skuDetails = mService.getSkuDetails(3, getPackageName(), "inapp", querySkus);
        } catch (RemoteException e) {
            e.printStackTrace();
            return map;
        }
        final int BILLING_RESPONSE_RESULT_OK = 0;
        int response = skuDetails.getInt("RESPONSE_CODE");
        if (response == BILLING_RESPONSE_RESULT_OK) {
            ArrayList<String> responseList
                    = skuDetails.getStringArrayList("DETAILS_LIST");

            for (String thisResponse : responseList) {
                try {
                    JSONObject object = new JSONObject(thisResponse);
                    String sku = object.getString("productId");
                    String price = object.getString("price");
                    map.put(sku, price);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return map;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK &&
                (requestCode == REQUEST_CODE_PURCHASE_DONATION ||
                        requestCode == REQUEST_CODE_PURCHASE_PRO ||
                        requestCode == REQUEST_CODE_PURCHASE_WEATHER ||
                        requestCode == REQUEST_CODE_PURCHASE_WEB_RADIO)) {
            Log.i(TAG, "Purchase request for " + String.valueOf(requestCode));
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
            Log.i(TAG, purchaseData);

            // update all purchases
            if (billingHelper != null) {
                purchases = billingHelper.getPurchases();
            }
            try {
                JSONObject jo = new JSONObject(purchaseData);
                String sku = jo.getString("productId");
                if (purchases.containsKey(sku)) {
                    showThankYouDialog();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void showThankYouDialog() {
        new AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle(getResources().getString(R.string.dialog_title_thank_you))
                .setMessage(R.string.dialog_message_thank_you)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
