package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BillingHelperActivity
        extends AppCompatActivity
        implements PurchasesUpdatedListener {
    public static final String ITEM_WEATHER_DATA = "weather_data";
    public static final String ITEM_DONATION = "donation";
    public static final String ITEM_WEB_RADIO = "web_radio";
    public static final String ITEM_PRO = "pro";
    public static final String ITEM_ACTIONS = "actions";
    static final String TAG = "BillingHelperActivity";
    private static final int PRODUCT_ID_WEATHER_DATA = 0;
    private static final int PRODUCT_ID_WEB_RADIO = 1;
    private static final int PRODUCT_ID_DONATION = 2;
    private static final int PRODUCT_ID_PRO = 3;
    private static final int PRODUCT_ID_ACTIONS = 4;
    static ImmutableList<QueryProductDetailsParams.Product> productList = ImmutableList.of(
            buildProduct(ITEM_DONATION),
            buildProduct(ITEM_PRO),
            buildProduct(ITEM_ACTIONS),
            buildProduct(ITEM_WEB_RADIO),
            buildProduct(ITEM_WEATHER_DATA)
    );
    static List<String> fullSkuList = new ArrayList<>(
            Arrays.asList(
                    ITEM_DONATION, ITEM_PRO, ITEM_WEATHER_DATA,
                    ITEM_ACTIONS, ITEM_WEB_RADIO
            )
    );
    Map<String, Boolean> purchases = getDefaultPurchaseMap();
    HashMap<String, String> prices = new HashMap<>();
    List<ProductDetails> productDetails;
    SharedPreferences preferences;
    private BillingClient mBillingClient;

    static QueryProductDetailsParams.Product buildProduct(String sku) {
        return QueryProductDetailsParams.Product.newBuilder()
                .setProductId(sku)
                .setProductType(BillingClient.ProductType.INAPP)
                .build();
    }

    static HashMap<String, Boolean> getDefaultPurchaseMap() {
        HashMap<String, Boolean> def = new HashMap<>();
        for (String sku : fullSkuList) {
            def.put(sku, false);
        }
        return def;
    }

    public boolean isPurchased(String sku) {
        if (Utility.isEmulator()) {
            return true;
        }
        if (purchases != null) {
            switch (sku) {
                case ITEM_DONATION:
                    return Boolean.TRUE.equals(purchases.get(ITEM_DONATION));
                case ITEM_PRO:
                    return (Boolean.TRUE.equals(purchases.get(ITEM_DONATION))
                            || Boolean.TRUE.equals(purchases.get(ITEM_PRO)));
                case ITEM_WEATHER_DATA:
                    return (Boolean.TRUE.equals(purchases.get(ITEM_DONATION))
                            || Boolean.TRUE.equals(purchases.get(ITEM_PRO))
                            || Boolean.TRUE.equals(purchases.get(ITEM_WEATHER_DATA)));
                case ITEM_WEB_RADIO:
                    return (Boolean.TRUE.equals(purchases.get(ITEM_DONATION))
                            || Boolean.TRUE.equals(purchases.get(ITEM_PRO))
                            || Boolean.TRUE.equals(purchases.get(ITEM_WEB_RADIO)));
                case ITEM_ACTIONS:
                    return (Boolean.TRUE.equals(purchases.get(ITEM_DONATION))
                            || Boolean.TRUE.equals(purchases.get(ITEM_PRO))
                            || Boolean.TRUE.equals(purchases.get(ITEM_ACTIONS)));
            }
        }
        return false;
    }

    public void showPurchaseDialog() {
        Log.i(TAG, "showPurchaseDialog()");
        if (isPurchased(ITEM_DONATION)) return;
        List<CharSequence> entries = new ArrayList<>();
        final List<Integer> values = new ArrayList<>();

        boolean purchased_weather_data = isPurchased(ITEM_WEATHER_DATA);
        boolean purchased_web_radio = isPurchased(ITEM_WEB_RADIO);
        boolean purchased_actions = isPurchased(ITEM_ACTIONS);
        boolean purchased_pro = isPurchased(ITEM_PRO);
        boolean purchased_donation = isPurchased(ITEM_DONATION);

        if (!purchased_pro && !purchased_weather_data && !purchased_web_radio && !purchased_actions) {
            entries.add(getProductWithPrice(prices, R.string.product_name_pro, ITEM_PRO));
            values.add(PRODUCT_ID_PRO);
        }

        // Stop selling packages and only offer packages in case one of the packages has already been purchased
        if (!purchased_pro && (purchased_weather_data || purchased_web_radio || purchased_actions)) {
            if (!purchased_weather_data) {
                entries.add(getProductWithPrice(prices, R.string.product_name_weather, ITEM_WEATHER_DATA));
                values.add(PRODUCT_ID_WEATHER_DATA);
            }

            if (!purchased_web_radio) {
                entries.add(getProductWithPrice(prices, R.string.product_name_webradio, ITEM_WEB_RADIO));
                values.add(PRODUCT_ID_WEB_RADIO);
            }

            if (!purchased_actions) {
                entries.add(getProductWithPrice(prices, R.string.product_name_actions, ITEM_ACTIONS));
                values.add(PRODUCT_ID_ACTIONS);
            }
        }

        if (!purchased_donation) {
            entries.add(getProductWithPrice(prices, R.string.product_name_donation, ITEM_DONATION));
            values.add(PRODUCT_ID_DONATION);
        }

        runOnUiThread(() -> new AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle(getResources().getString(R.string.buy))
                .setItems(
                        entries.toArray(new CharSequence[0]),
                        (dialogInterface, which) -> {
                            Log.i(TAG, String.format("selected %d", which));
                            int selected = values.get(which);
                            switch (selected) {
                                case PRODUCT_ID_WEATHER_DATA:
                                    launchBillingFlow(ITEM_WEATHER_DATA);
                                    break;
                                case PRODUCT_ID_WEB_RADIO:
                                    launchBillingFlow(ITEM_WEB_RADIO);
                                    break;
                                case PRODUCT_ID_ACTIONS:
                                    launchBillingFlow(ITEM_ACTIONS);
                                    break;
                                case PRODUCT_ID_DONATION:
                                    launchBillingFlow(ITEM_DONATION);
                                    break;
                                case PRODUCT_ID_PRO:
                                    launchBillingFlow(ITEM_PRO);
                                    break;
                            }
                        })
                .setNeutralButton(android.R.string.cancel, null)
                .show()
        );
    }

    private String getProductWithPrice(HashMap<String, String> prices, int resId, String sku) {
        String price = prices.get(sku);
        if (price != null) {
            return String.format("%s (%s)", getResources().getString(resId), price);
        }
        return getResources().getString(resId);
    }

    private void updateAllPurchases() {
        // TODO
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = Settings.getDefaultSharedPreferences(this);
        if (preferences != null) { // initialize from cache
            for (String sku : fullSkuList) {
                boolean isPurchased = preferences.getBoolean(String.format("purchased_%s", sku), false);
                purchases.put(sku, isPurchased);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        initBillingClient();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAllPurchases();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBillingClient != null) {
            mBillingClient.endConnection();
            mBillingClient = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void launchBillingFlow(String sku) {
        ProductDetails details = getProductDetails(sku);
        if (details == null) return;
        // Set the parameters for the offer that will be presented
        // in the billing flow creating separate productDetailsParamsList variable
        ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                ImmutableList.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()
                                //.setOfferToken(offerToken).build()
                );

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        // Launch the billing flow
        mBillingClient.launchBillingFlow(this, billingFlowParams);
    }

    protected void onPurchasesInitialized() {
    }

    protected void onItemPurchased(String sku) {
        setPurchased(sku, true);
        showThankYouDialog();
    }

    void setPurchased(String sku, boolean isPurchased) {
        if (preferences == null) {
            return;
        }
        SharedPreferences.Editor edit = preferences.edit();
        edit.putBoolean(String.format("purchased_%s", sku), isPurchased);
        edit.apply();
    }

    protected void onItemConsumed(String sku) {
        setPurchased(sku, false);
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        Log.d(TAG, "onPurchasesUpdated()");
        int responseCode = billingResult.getResponseCode();
        if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Log.d(TAG, "User Canceled" + responseCode);
        } else if (responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Toast.makeText(this, R.string.dialog_message_already_owned, Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "Other code" + responseCode);
            // Handle any other error codes.
        }

    }

    void handlePurchase(final Purchase purchase) {
        if (purchase == null) {
            return;
        }
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            // Acknowledge purchase and grant the item to the user
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
                    Log.i(TAG, "onAcknowledgePurchaseResponse: " + billingResult.getResponseCode());
                    Log.i(TAG, billingResult.getDebugMessage());
                    if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        return;
                    }
                    List<String> skus = purchase.getProducts();
                    int state = purchase.getPurchaseState();
                    boolean purchased = (state == Purchase.PurchaseState.PURCHASED);
                    for (String sku : skus) {
                        Log.d(TAG, String.format("purchased %s = %s (%d)", sku, purchased, state));
                        purchases.put(sku, purchased);
                        onItemPurchased(sku);
                    }
                });
            }
        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            // Here you can confirm to the user that they've started the pending
            // purchase, and to complete it, they should follow instructions that
            // are given to them. You can also choose to remind the user in the
            // future to complete the purchase if you detect that it is still
            // pending.
            showPurchasePendingDialog();
        }
    }

    void initBillingClient() {
        mBillingClient = BillingClient
                .newBuilder(this)
                .setListener(this)
                .enablePendingPurchases()
                .build();

        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                Log.i(TAG, "onBillingSetupFinished");
                try {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        // The BillingClient is ready. You can query purchases here.
                        querySkuDetails();
                        queryPurchases();
                    }
                } catch (IllegalStateException e) {
                    // if the Activity is closed immediately this operation is no longer permitted
                    mBillingClient = null;
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.i(TAG, "onBillingServiceDisconnected");
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });
    }

    void queryPurchases() {
        mBillingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                (result, purchaseList) -> {
                    int responseCode = result.getResponseCode();
                    if (responseCode != BillingClient.BillingResponseCode.OK) {
                        return;
                    }
                    for (String sku : fullSkuList) {
                        purchases.put(sku, false);
                    }
                    for (Purchase purchase: purchaseList) {
                        List<String> skus = purchase.getProducts();
                        for (String sku : skus) {
                            int state = purchase.getPurchaseState();
                            boolean purchased = (state == Purchase.PurchaseState.PURCHASED);
                            purchases.put(sku, purchased);
                            Log.i(TAG, String.format("purchased %s = %s", sku, purchased));
                            // ATTENTION only activate temporarily
                            // consumePurchase(purchase);
                        }
                    }

                    // store in the cache
                    for (String sku : fullSkuList) {
                        boolean isPurchased = Boolean.TRUE.equals(purchases.get(sku));
                        setPurchased(sku, isPurchased);
                    }

                    onPurchasesInitialized();
                });
    }


    void querySkuDetails() {
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        mBillingClient.queryProductDetailsAsync(
                params,
                (result, productDetailsList) -> {
                    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        this.productDetails = productDetailsList;
                        prices.clear();
                        for (ProductDetails details : productDetailsList) {
                            String sku = details.getProductId();
                            ProductDetails.OneTimePurchaseOfferDetails oneTimePurchaseOfferDetails = details.getOneTimePurchaseOfferDetails();
                            if (oneTimePurchaseOfferDetails != null) {
                                String price = oneTimePurchaseOfferDetails.getFormattedPrice();
                                Log.i(TAG, String.format("price %s = %s", sku, price));
                                prices.put(sku, price);
                            }
                        }
                    }
                }
        );
    }

    void consumePurchase(Purchase purchase) {
        final List<String> skus = purchase.getProducts();
        String token = purchase.getPurchaseToken();
        ConsumeParams consumeParams = ConsumeParams.newBuilder().setPurchaseToken(token).build();
        mBillingClient.consumeAsync(consumeParams, (billingResult, purchaseToken) -> {
            Log.d(TAG, "onConsumeResponse: " + billingResult.getDebugMessage());
            int response = billingResult.getResponseCode();
            if (response == BillingClient.BillingResponseCode.OK) {
                for (String sku : skus) {
                    purchases.put(sku, false);
                    onItemConsumed(sku);
                }
            }
        });
    }


    public void showThankYouDialog() {
        final Context self = this ;
        runOnUiThread(() -> new AlertDialog.Builder(self, R.style.DialogTheme)
                .setTitle(getResources().getString(R.string.dialog_title_thank_you))
                .setMessage(R.string.dialog_message_thank_you)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        );
        Log.d(TAG, "showThankYouDialog()");
    }

    public void showPurchasePendingDialog() {
        final Context self = this;
        runOnUiThread(() -> new AlertDialog.Builder(self, R.style.DialogTheme)
                .setTitle(getResources().getString(R.string.dialog_title_thank_you))
                .setMessage(R.string.dialog_message_pending_purchase)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        );
    }

    ProductDetails getProductDetails(String sku) {
        if (this.productDetails != null) {
            for (ProductDetails details : productDetails) {
                if (sku.equals(details.getProductId())) {
                    return details;
                }
            }
        }
        return null;
    }

    public boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return (ContextCompat.checkSelfPermission(this, permission)
                    == PackageManager.PERMISSION_GRANTED);
        }
        return true;
    }
}
