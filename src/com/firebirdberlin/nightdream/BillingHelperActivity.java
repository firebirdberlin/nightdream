package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
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

public abstract class BillingHelperActivity
        extends AppCompatActivity
{
    public static final String ITEM_ONE_YEAR_SUBSCRIPTION = "oneyear";
    public static final String ITEM_WEATHER_DATA = "weather_data";
    public static final String ITEM_DONATION = "donation";
    public static final String ITEM_WEB_RADIO = "web_radio";
    public static final String ITEM_PRO = "pro";
    public static final String ITEM_ACTIONS = "actions";
    static final String TAG = "BillingHelperActivity";
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
    Map<String, Boolean> purchases = getDefaultPurchaseMap();
    HashMap<String, String> prices = new HashMap<>();
    List<ProductDetails> productDetails;
    SharedPreferences preferences;
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
            Toast.makeText(this, R.string.dialog_message_already_owned, Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "Other code: " + responseCode);
            // Handle any other error codes.
        }
    };

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

    public void showPurchaseDialog() {
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

        runOnUiThread(() -> new AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle(getResources().getString(R.string.buy))
                .setItems(
                        entries.toArray(new CharSequence[0]),
                        (dialogInterface, which) -> {
                            Log.i(TAG, String.format("selected %d", which));
                            int selected = values.get(which);
                            switch (selected) {
                                case PRODUCT_ID_DONATION:
                                    launchBillingFlow(ITEM_DONATION);
                                    break;
                                case PRODUCT_ID_PRO:
                                    launchBillingFlow(ITEM_PRO);
                                    break;
                                case PRODUCT_ID_ONE_YEAR_SUBSCRIPTION:
                                    launchBillingFlow(ITEM_ONE_YEAR_SUBSCRIPTION);
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
        if (mBillingClient != null && mBillingClient.isReady()) {
            queryPurchases();
        }
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
        Log.d(TAG, "launchBillingFlow(" + sku + ")");
        String productType = (ITEM_ONE_YEAR_SUBSCRIPTION.equals(sku)) ? BillingClient.ProductType.SUBS : BillingClient.ProductType.INAPP;
        ProductDetails details = getProductDetails(sku);
        if (details == null) {
            Log.e(TAG, "Product details not found for sku: " + sku);
            return;
        }

        // Ensure the product type matches the details' type if possible
        if (!details.getProductType().equals(productType)) {
            Log.w(TAG, "Product type mismatch for sku: " + sku + ". Expected: " + productType + ", Found: " + details.getProductType());
            // You might want to handle this more robustly, e.g., re-query or show an error
            // For now, we'll proceed with the details found, assuming it's correct.
        }

        if (productType.equals(BillingClient.ProductType.SUBS)) {

            ProductDetails.SubscriptionOfferDetails offer = details.getSubscriptionOfferDetails().get(0);

            BillingFlowParams.ProductDetailsParams params =
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .setOfferToken(offer.getOfferToken()) // REQUIRED for SUBS
                            .build();

            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(ImmutableList.of(params))
                    .build();
            Log.i(TAG, "SUBS: launchBillingFlow( "+ flowParams +" )");
            mBillingClient.launchBillingFlow(this, flowParams);

        } else {
            ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                    ImmutableList.of(
                            BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()
                    );

            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build();

            // Launch the billing flowo
            Log.i(TAG, "launchBillingFlow( " + billingFlowParams + " )");
            mBillingClient.launchBillingFlow(this, billingFlowParams);
        }
    }


    protected void onPurchasesInitialized() {
        for (String sku : fullSkuList) {
            boolean isPurchased = Boolean.TRUE.equals(purchases.get(sku));
            Log.i(TAG, String.format("onPurchasesInitialized(%s, %s)", sku, isPurchased));
        }
    }

    protected void onItemPurchased(String sku) {
        setPurchased(sku, true);
        showThankYouDialog();
    }

    void setPurchased(String sku, boolean isPurchased) {
        if (preferences == null) {
            return;
        }
        Log.i(TAG, String.format("setPurchased(%s, %s)", sku, isPurchased));
        SharedPreferences.Editor edit = preferences.edit();
        edit.putBoolean(String.format("purchased_%s", sku), isPurchased);
        edit.apply();
    }

    protected void onItemConsumed(String sku) {
        setPurchased(sku, false);
    }

    void handlePurchase(final Purchase purchase) {
        Log.d(TAG, "handlePurchase(" + purchase + " )");
        if (purchase == null) {
            return;
        }

        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            List<String> skus = purchase.getProducts();
            for (String sku : skus) {
                if (fullSkuList.contains(sku)) { // Check if it's a product we are managing
                    if (sku.equals(ITEM_ONE_YEAR_SUBSCRIPTION)) {
                        // Handle Subscription
                        if (!purchase.isAcknowledged()) {
                            acknowledgePurchase(purchase); // Acknowledge subscriptions
                        }
                        purchases.put(sku, true);
                        setPurchased(sku, true); // Update cache
                        onItemPurchased(sku); // Your custom logic for purchased items
                        Log.i(TAG, "Subscription purchased: " + sku);
                    } else {
                        // Handle In-App Purchases
                        if (!purchase.isAcknowledged()) {
                            acknowledgePurchase(purchase); // Acknowledge in-app items
                        }
                        purchases.put(sku, true);
                        setPurchased(sku, true); // Update cache
                        onItemPurchased(sku); // Your custom logic for purchased items
                        Log.i(TAG, "In-app item purchased: " + sku);
                    }
                }
            }
        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            // Handle pending purchases (applies to both INAPP and SUBS)
            showPurchasePendingDialog();
        }
    }

    void initBillingClient() {
        // Build the PendingPurchasesParams object
        PendingPurchasesParams pendingParams = PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()   // or .enableAllPurchases()
                .build();

        mBillingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases(pendingParams)
                .setListener(purchasesUpdatedListener)
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
        // Query for INAPP purchases
        mBillingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                this::handleInAppPurchasesResult
        );

        // Query for SUBS purchases
        mBillingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                this::handleSubscriptionPurchasesResult
        );
    }

    private void handleInAppPurchasesResult(BillingResult result, List<Purchase> purchaseList) {
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Error querying in-app purchases: " + result.getDebugMessage());
            return;
        }
        for (String sku : fullSkuList) {
            // Only reset if it's not a subscription, to avoid overriding subscription status
            if (!sku.equals(ITEM_ONE_YEAR_SUBSCRIPTION)) {
                purchases.put(sku, false);
            }
        }
        for (Purchase purchase : purchaseList) {
            List<String> skus = purchase.getProducts();
            for (String sku : skus) {
                if (fullSkuList.contains(sku)) { // Ensure it's a product we care about
                    int state = purchase.getPurchaseState();
                    boolean purchased = (state == Purchase.PurchaseState.PURCHASED);
                    purchases.put(sku, purchased);
                    Log.i(TAG, String.format("In-App purchased %s = %s", sku, purchased));
                    // For in-app items, you might acknowledge or consume here if not handled in handlePurchase
                    if (!purchase.isAcknowledged()) {
                        acknowledgePurchase(purchase); // Helper method to acknowledge
                    }
//                    ATTENTION only activate temporarily
//                    consumePurchase(purchase);
                }
            }
        }
        // Store in cache for in-app items
        for (String sku : fullSkuList) {
            if (!sku.equals(ITEM_ONE_YEAR_SUBSCRIPTION)) {
                boolean isPurchased = Boolean.TRUE.equals(purchases.get(sku));
                setPurchased(sku, isPurchased);
            }
        }
//        onPurchasesInitialized(); // Call this after all purchases are processed
    }

    private void handleSubscriptionPurchasesResult(BillingResult billingResult, List<Purchase> activeSubscriptions) {
        if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Error querying subscriptions: " + billingResult.getDebugMessage());
            return;
        }
        Log.i(TAG, "handleSubscriptionPurchasesResult with len(activeSubscriptions) = " + activeSubscriptions.size());
        boolean isItemOneYearSubscribed = false;
        // this goes through all ACTIVE subscriptions
        for (Purchase purchase : activeSubscriptions) {
            List<String> productIds = purchase.getProducts();
            for (String productId : productIds) {
                Log.i(TAG, "handleSubscriptionPurchasesResult: " + productId);
                if (ITEM_ONE_YEAR_SUBSCRIPTION.equals(productId)) {
                    int purchaseState = purchase.getPurchaseState();
                    isItemOneYearSubscribed = (purchaseState == Purchase.PurchaseState.PURCHASED);
                    Log.i(TAG, String.format("Subscription purchased %s = %s", productId, isItemOneYearSubscribed));
                    // Subscriptions need to be acknowledged, but not consumed.
                    if (!purchase.isAcknowledged()) {
                        acknowledgePurchase(purchase); // Helper method to acknowledge
                    }
                }
            }
        }

        // Update cache for subscription
        purchases.put(ITEM_ONE_YEAR_SUBSCRIPTION, isItemOneYearSubscribed);
        setPurchased(ITEM_ONE_YEAR_SUBSCRIPTION, isItemOneYearSubscribed);
        onPurchasesInitialized(); // Call this after all purchases are processed
    }

    private void acknowledgePurchase(Purchase purchase) {
        if (purchase.isAcknowledged()) {
            return;
        }
        AcknowledgePurchaseParams acknowledgePurchaseParams =
                AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();
        mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
            Log.i(TAG, "onAcknowledgePurchaseResponse: " + billingResult.getResponseCode());
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged successfully.");
                // Optionally, re-verify or update UI after acknowledgment
                // For subscriptions, this acknowledgment is crucial for the purchase to be active.
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: " + billingResult.getDebugMessage());
            }
        });
    }

    void querySkuDetails() {
        // Separate product lists by type
        ImmutableList.Builder<QueryProductDetailsParams.Product> inAppProductsBuilder = ImmutableList.builder();
        ImmutableList.Builder<QueryProductDetailsParams.Product> subscriptionProductsBuilder = ImmutableList.builder();

        // Populate the builders based on your fullSkuList or productList
        // It's better to iterate through your known SKUs and categorize them
        for (String sku : fullSkuList) {
            if (ITEM_ONE_YEAR_SUBSCRIPTION.equals(sku)) {
                subscriptionProductsBuilder.add(buildSubscriptionProduct(sku)); // Use the dedicated builder for subscriptions
            } else {
                inAppProductsBuilder.add(buildProduct(sku)); // Use the standard builder for in-app items
            }
        }

        ImmutableList<QueryProductDetailsParams.Product> inAppProducts = inAppProductsBuilder.build();
        ImmutableList<QueryProductDetailsParams.Product> subscriptionProducts = subscriptionProductsBuilder.build();

        // Query for In-App Products if the list is not empty
        if (!inAppProducts.isEmpty()) {
            QueryProductDetailsParams inAppParams = QueryProductDetailsParams.newBuilder()
                    .setProductList(inAppProducts)
                    .build();

            mBillingClient.queryProductDetailsAsync(inAppParams, (billingResult, productDetailsResult) -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // Process in-app product details
                    processProductDetails(productDetailsResult.getProductDetailsList(), BillingClient.ProductType.INAPP);
                } else {
                    Log.e(TAG, "Failed to query in-app product details: " + billingResult.getDebugMessage());
                }
            });
        }

        // Query for Subscription Products if the list is not empty
        if (!subscriptionProducts.isEmpty()) {
            QueryProductDetailsParams subscriptionParams = QueryProductDetailsParams.newBuilder()
                    .setProductList(subscriptionProducts)
                    .build();

            mBillingClient.queryProductDetailsAsync(subscriptionParams, (billingResult, productDetailsResult) -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // Process subscription product details
                    processProductDetails(productDetailsResult.getProductDetailsList(), BillingClient.ProductType.SUBS);
                } else {
                    Log.e(TAG, "Failed to query subscription product details: " + billingResult.getDebugMessage());
                }
            });
        }
    }

    // Helper method to process product details and update prices
    private void processProductDetails(List<ProductDetails> productDetailsList, @BillingClient.ProductType String productType) {
        if (this.productDetails == null) {
            this.productDetails = new ArrayList<>();
        }
        this.productDetails.addAll(productDetailsList); // Add details to the main list

        for (ProductDetails details : productDetailsList) {
            String productId = details.getProductId();
            // Check if it's a one-time purchase offer for INAPP products
            if (BillingClient.ProductType.INAPP.equals(productType)) {
                ProductDetails.OneTimePurchaseOfferDetails oneTimeOffer = details.getOneTimePurchaseOfferDetails();
                if (oneTimeOffer != null) {
                    String price = oneTimeOffer.getFormattedPrice();
                    Log.i(TAG, String.format("INAPP price %s = %s", productId, price));
                    prices.put(productId, price);
                } else {
                    Log.w(TAG, "No OneTimePurchaseOfferDetails for INAPP product: " + productId);
                }
            } else if (BillingClient.ProductType.SUBS.equals(productType)) {
                // For subscriptions, you might want to get the pricing from subscriptionOfferDetails
                // This part can be more complex as subscriptions can have multiple offers.
                // For simplicity, let's get the first available offer's price if it exists.
                List<ProductDetails.SubscriptionOfferDetails> offerDetailsList = details.getSubscriptionOfferDetails();
                if (offerDetailsList != null && !offerDetailsList.isEmpty()) {
                    // Often, you'll want to pick a specific offerToken for the base plan/specific offer
                    // Here, we'll just take the first one's formatted price for demonstration
                    for(ProductDetails.SubscriptionOfferDetails offerDetails : offerDetailsList) {
                        // You might need to check offerDetails.getBasePlanId() or other criteria
                        // to select the correct pricing if you have multiple base plans/offers.
                        ProductDetails.PricingPhase pricingPhase = offerDetails.getPricingPhases().getPricingPhaseList().get(0);
                        if (pricingPhase != null) {
                            String price = pricingPhase.getFormattedPrice();
                            Log.i(TAG, String.format("SUBS price %s = %s", productId, price));
                            prices.put(productId, price);
                            // Break if you only want the price from the first offer detail found
                            break;
                        }
                    }
                } else {
                    Log.w(TAG, "No SubscriptionOfferDetails for SUBS product: " + productId);
                }
            }
        }
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
            return (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED);
        }
        return true;
    }
}
