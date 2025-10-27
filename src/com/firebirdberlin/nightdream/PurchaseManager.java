package com.firebirdberlin.nightdream;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
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

public class PurchaseManager {
    public static final String ITEM_ONE_YEAR_SUBSCRIPTION = "oneyear";
    public static final String ITEM_WEATHER_DATA = "weather_data";
    public static final String ITEM_DONATION = "donation";
    public static final String ITEM_WEB_RADIO = "web_radio";
    public static final String ITEM_PRO = "pro";
    public static final String ITEM_ACTIONS = "actions";
    static final String TAG = "PurchaseManager";
//    private static final int PRODUCT_ID_WEATHER_DATA = 0;  #noqa
//    private static final int PRODUCT_ID_WEB_RADIO = 1;
    private static final int PRODUCT_ID_DONATION = 2;
    private static final int PRODUCT_ID_PRO = 3;
    //    private static final int PRODUCT_ID_ACTIONS = 4;
    private static final int PRODUCT_ID_ONE_YEAR_SUBSCRIPTION = 5;

    static ImmutableList<QueryProductDetailsParams.Product> productList = ImmutableList.of(
            buildProduct(ITEM_DONATION),
            buildProduct(ITEM_PRO),
            buildProduct(ITEM_ACTIONS),
            buildProduct(ITEM_WEB_RADIO),
            buildProduct(ITEM_WEATHER_DATA),
            buildSubscriptionProduct(ITEM_ONE_YEAR_SUBSCRIPTION)
    );
    static List<String> fullSkuList = new ArrayList<>(
            Arrays.asList(
                    ITEM_DONATION, ITEM_PRO, ITEM_WEATHER_DATA,
                    ITEM_ACTIONS, ITEM_WEB_RADIO, ITEM_ONE_YEAR_SUBSCRIPTION
            )
    );
    private static PurchaseManager instance;
    private final SharedPreferences preferences;
    Map<String, Boolean> purchases = getDefaultPurchaseMap();
    HashMap<String, String> prices = new HashMap<>();
    List<ProductDetails> productDetails;
    private BillingClient mBillingClient;
    private final PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
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
            // This is handled by the Activity
        } else {
            Log.d(TAG, "Other code: " + responseCode);
            // Handle any other error codes.
        }
    };
    private final Context context;

    private PurchaseManager(Context context) {
        this.context = context.getApplicationContext();
        preferences = Settings.getDefaultSharedPreferences(this.context);
        if (preferences != null) { // initialize from cache
            for (String sku : fullSkuList) {
                boolean isPurchased = preferences.getBoolean(String.format("purchased_%s", sku), false);
                purchases.put(sku, isPurchased);
            }
        }
        initBillingClient();
    }

    public static synchronized PurchaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new PurchaseManager(context);
        }
        return instance;
    }

    static QueryProductDetailsParams.Product buildProduct(String sku) {
        return QueryProductDetailsParams.Product.newBuilder()
                .setProductId(sku)
                .setProductType(BillingClient.ProductType.INAPP)
                .build();
    }

    static QueryProductDetailsParams.Product buildSubscriptionProduct(String sku) {
        return QueryProductDetailsParams.Product.newBuilder()
                .setProductId(sku)
                .setProductType(BillingClient.ProductType.SUBS) // Use SUBS for subscriptions
                .build();
    }

    static HashMap<String, Boolean> getDefaultPurchaseMap() {
        HashMap<String, Boolean> def = new HashMap<>();
        for (String sku : fullSkuList) {
            def.put(sku, false);
        }
        return def;
    }

    private void initBillingClient() {
        PendingPurchasesParams pendingParams = PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()   // or .enableAllPurchases()
                .build();

        mBillingClient = BillingClient.newBuilder(context)
                .enablePendingPurchases(pendingParams)
                .setListener(purchasesUpdatedListener)
                .build();

        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "onBillingSetupFinished()");
                    // The BillingClient is ready. You can query purchases here.
                    queryProductDetails();
                    queryPurchases();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });
    }

    void queryProductDetails() {
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        mBillingClient.queryProductDetailsAsync(
                params,
                (billingResult, prodDetailsList) -> {
                    productDetails = prodDetailsList.getProductDetailsList();
                    Log.i(TAG, "Product details list size: " + productDetails.size());
                    // Process the result
                    for (ProductDetails details : productDetails) {
                        String sku = details.getProductId();
                        String price = "";
                        if (details.getOneTimePurchaseOfferDetails() != null) {
                            price = details.getOneTimePurchaseOfferDetails().getFormattedPrice();
                        } else if (details.getSubscriptionOfferDetails() != null && !details.getSubscriptionOfferDetails().isEmpty()) {
                            price = details.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice();
                        }
                        prices.put(sku, price);
                    }
                }
        );
    }

    void queryPurchases() {
        if (mBillingClient != null && mBillingClient.isReady()) {
            QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build();
            mBillingClient.queryPurchasesAsync(
                params, (billingResult, purchasesList) -> {
                    Log.d(TAG, "queryPurchases() INAPP");
                    for (Purchase p : purchasesList) {
                        handlePurchase(p);
                    }
                }
            );

            params = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build();
            mBillingClient.queryPurchasesAsync(
                params, (billingResult, purchasesList) -> {
                    Log.d(TAG, "queryPurchases() SUBS");

                    for (Purchase p : purchasesList) {
                        handlePurchase(p);
                    }
                }
            );
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            for (String sku : purchase.getProducts()) {
                if (!isPurchased(sku)) {
                    setPurchased(sku, true);
                }
            }
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
                });
            }
        }
    }

    public boolean isPurchased(String sku) {
        if (Utility.isEmulator()) {
            return true;
        }
        if (purchases != null) {
            switch (sku) {
                case ITEM_DONATION:
                    return Boolean.TRUE.equals(purchases.get(ITEM_DONATION));
                case ITEM_ONE_YEAR_SUBSCRIPTION:
                    return Boolean.TRUE.equals(purchases.get(ITEM_ONE_YEAR_SUBSCRIPTION));
                case ITEM_PRO:
                    return (
                            Boolean.TRUE.equals(purchases.get(ITEM_DONATION))
                                    || Boolean.TRUE.equals(purchases.get(ITEM_ONE_YEAR_SUBSCRIPTION))
                                    || Boolean.TRUE.equals(purchases.get(ITEM_PRO))
                    );
                case ITEM_WEATHER_DATA:
                    return (
                            Boolean.TRUE.equals(purchases.get(ITEM_DONATION))
                                    || Boolean.TRUE.equals(purchases.get(ITEM_ONE_YEAR_SUBSCRIPTION))
                                    || Boolean.TRUE.equals(purchases.get(ITEM_PRO))
                                    || Boolean.TRUE.equals(purchases.get(ITEM_WEATHER_DATA))
                    );
                case ITEM_WEB_RADIO:
                    return (
                            Boolean.TRUE.equals(purchases.get(ITEM_DONATION))
                                    || Boolean.TRUE.equals(purchases.get(ITEM_ONE_YEAR_SUBSCRIPTION))
                                    || Boolean.TRUE.equals(purchases.get(ITEM_PRO))
                                    || Boolean.TRUE.equals(purchases.get(ITEM_WEB_RADIO))
                    );
                case ITEM_ACTIONS:
                    return (
                            Boolean.TRUE.equals(purchases.get(ITEM_DONATION))
                                    || Boolean.TRUE.equals(purchases.get(ITEM_ONE_YEAR_SUBSCRIPTION))
                                    || Boolean.TRUE.equals(purchases.get(ITEM_PRO))
                                    || Boolean.TRUE.equals(purchases.get(ITEM_ACTIONS))
                    );
            }
        }
        return false;
    }

    void setPurchased(String sku, boolean value) {
        purchases.put(sku, value);
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(String.format("purchased_%s", sku), value);
            editor.apply();
        }
    }

    public void showPurchaseDialog(Activity activity) {
        Log.i(TAG, "showPurchaseDialog()");
        // if (isPurchased(ITEM_DONATION)) return;
        List<CharSequence> entries = new ArrayList<>();
        final List<Integer> values = new ArrayList<>();

        boolean purchased_weather_data = isPurchased(ITEM_WEATHER_DATA);
        boolean purchased_web_radio = isPurchased(ITEM_WEB_RADIO);
        boolean purchased_actions = isPurchased(ITEM_ACTIONS);
        boolean purchased_pro = isPurchased(ITEM_PRO);
        boolean purchased_donation = isPurchased(ITEM_DONATION);
        boolean purchased_subscription = isPurchased(ITEM_ONE_YEAR_SUBSCRIPTION); // Check for subscription
        Log.i(TAG, String.format("purchased_subscription = %s", purchased_subscription));


        purchased_pro = (purchased_pro || (purchased_weather_data && purchased_web_radio && purchased_actions));
        if (!purchased_subscription) {
            if (!purchased_pro) {
//                entries.add(getProductWithPrice(prices, R.string.product_name_subscription, ITEM_ONE_YEAR_SUBSCRIPTION));
//                TODO Switch to subscription model in a later step
//                values.add(PRODUCT_ID_ONE_YEAR_SUBSCRIPTION);
                entries.add(getProductWithPrice(prices, R.string.product_name_pro, ITEM_PRO));
                values.add(PRODUCT_ID_PRO);
            }
        }

        if (!purchased_donation) {
            entries.add(getProductWithPrice(prices, R.string.product_name_donation, ITEM_DONATION));
            values.add(PRODUCT_ID_DONATION);
        }

        activity.runOnUiThread(() -> new AlertDialog.Builder(activity, R.style.DialogTheme)
                .setTitle(context.getResources().getString(R.string.buy))
                .setItems(
                        entries.toArray(new CharSequence[0]),
                        (dialogInterface, which) -> {
                            Log.i(TAG, String.format("selected %d", which));
                            int selected = values.get(which);
                            switch (selected) {
                                case PRODUCT_ID_DONATION:
                                    launchBillingFlow(activity, ITEM_DONATION);
                                    break;
                                case PRODUCT_ID_PRO:
                                    launchBillingFlow(activity, ITEM_PRO);
                                    break;
                                case PRODUCT_ID_ONE_YEAR_SUBSCRIPTION:
                                    launchBillingFlow(activity, ITEM_ONE_YEAR_SUBSCRIPTION);
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
            return String.format("%s (%s)", context.getResources().getString(resId), price);
        }
        return context.getResources().getString(resId);
    }

    public void launchBillingFlow(Activity activity, String sku) {
        Log.d(TAG, "launchBillingFlow(" + sku + ")");
        String productType = (ITEM_ONE_YEAR_SUBSCRIPTION.equals(sku)) ? BillingClient.ProductType.SUBS : BillingClient.ProductType.INAPP;
        ProductDetails details = getProductDetails(sku);
        if (details == null) {
            Log.e(TAG, "Product details not found for sku: " + sku);
            return;
        }

        ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                ImmutableList.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(details)
                                .build()
                );

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        BillingResult billingResult = mBillingClient.launchBillingFlow(activity, billingFlowParams);
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Toast.makeText(context, R.string.dialog_message_already_owned, Toast.LENGTH_LONG).show();
        }
    }

    private ProductDetails getProductDetails(String sku) {
        if (productDetails != null) {
            for (ProductDetails details : productDetails) {
                if (details.getProductId().equals(sku)) {
                    return details;
                }
            }
        }
        return null;
    }

    public void updateAllPurchases() {
        if (mBillingClient != null && mBillingClient.isReady()) {
            queryPurchases();
        }
    }

    public void endConnection() {
        if (mBillingClient != null) {
            mBillingClient.endConnection();
            mBillingClient = null;
        }
    }
}
