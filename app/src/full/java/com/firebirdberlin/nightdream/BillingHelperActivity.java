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
import android.content.pm.PackageManager;
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
import java.util.List;

public abstract class BillingHelperActivity extends AppCompatActivity {
    static final String TAG = "BillingHelperActivity";
    List<ProductDetails> productDetails;
    private BillingClient mBillingClient;
    private PurchaseManager purchaseManager = null;
    private final PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchaseList) -> {
        Log.d(TAG, "onPurchasesUpdated()");
        int responseCode = billingResult.getResponseCode();
        if (responseCode == BillingClient.BillingResponseCode.OK && purchaseList != null) {
            for (Purchase purchase : purchaseList) {
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

    public boolean isPurchased(String sku) {
        return purchaseManager.isPurchased(sku);
    }

    public void showPurchaseDialog() {
        Log.i(TAG, "showPurchaseDialog()");
        // if (isPurchased(PurchaseManager.ITEM_DONATION)) return;
        List<CharSequence> entries = new ArrayList<>();
        final List<Integer> values = new ArrayList<>();

        boolean purchased_weather_data = isPurchased(PurchaseManager.ITEM_WEATHER_DATA);
        boolean purchased_web_radio = isPurchased(PurchaseManager.ITEM_WEB_RADIO);
        boolean purchased_actions = isPurchased(PurchaseManager.ITEM_ACTIONS);
        boolean purchased_pro = isPurchased(PurchaseManager.ITEM_PRO);
        boolean purchased_donation = isPurchased(PurchaseManager.ITEM_DONATION);
        boolean purchased_subscription = isPurchased(PurchaseManager.ITEM_ONE_YEAR_SUBSCRIPTION); // Check for subscription
        Log.i(TAG, String.format("purchased_subscription = %s", purchased_subscription));


//        purchased_pro = (purchased_pro || (purchased_weather_data && purchased_web_radio && purchased_actions));
//        if (!purchased_subscription) {
//            if (!purchased_pro) {
//                entries.add(getProductWithPrice(R.string.product_name_pro, PurchaseManager.ITEM_PRO));
//                values.add(PurchaseManager.PRODUCT_ID_PRO);
//            }
//        }

        if (!purchased_donation) {
            entries.add(getProductWithPrice(R.string.product_name_donation, PurchaseManager.ITEM_DONATION));
            values.add(PurchaseManager.PRODUCT_ID_DONATION);
        }
        if (entries.isEmpty()) {
            return;
        }

        runOnUiThread(() -> new AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle(getResources().getString(R.string.buy))
                .setItems(
                        entries.toArray(new CharSequence[0]),
                        (dialogInterface, which) -> {
                            Log.i(TAG, String.format("selected %d", which));
                            int selected = values.get(which);
                            switch (selected) {
                                case PurchaseManager.PRODUCT_ID_DONATION:
                                    launchBillingFlow(PurchaseManager.ITEM_DONATION);
                                    break;
                                case PurchaseManager.PRODUCT_ID_PRO:
                                    launchBillingFlow(PurchaseManager.ITEM_PRO);
                                    break;
                                case PurchaseManager.PRODUCT_ID_ONE_YEAR_SUBSCRIPTION:
                                    launchBillingFlow(PurchaseManager.ITEM_ONE_YEAR_SUBSCRIPTION);
                                    break;
                            }
                        })
                .setNeutralButton(android.R.string.cancel, null)
                .show()
        );
    }

    public void showSubscriptionDialog() {
        Log.i(TAG, "showSubscriptionDialog()");
        // if (isPurchased(PurchaseManager.ITEM_DONATION)) return;
        boolean purchased_weather_data = isPurchased(PurchaseManager.ITEM_WEATHER_DATA);
        boolean purchased_web_radio = isPurchased(PurchaseManager.ITEM_WEB_RADIO);
        boolean purchased_actions = isPurchased(PurchaseManager.ITEM_ACTIONS);
        boolean purchased_pro = isPurchased(PurchaseManager.ITEM_PRO);
        boolean purchased_subscription = isPurchased(PurchaseManager.ITEM_ONE_YEAR_SUBSCRIPTION);
        Log.i(TAG, String.format("purchased_subscription = %s", purchased_subscription));


        String description = getDetailedSubscriptionDescription(PurchaseManager.ITEM_ONE_YEAR_SUBSCRIPTION);
        purchased_pro = (purchased_pro || (purchased_weather_data && purchased_web_radio && purchased_actions));
        if (purchased_subscription || purchased_pro || description == null) {
            return;
        }

        runOnUiThread(() -> new AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle(getResources().getString(R.string.subscribe))
                .setMessage(description)
                .setNeutralButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok,
                        (dialogInterface, which) -> {
                            Log.i(TAG, String.format("selected %d", which));
                            launchBillingFlow(PurchaseManager.ITEM_ONE_YEAR_SUBSCRIPTION);
                        }
                )
                .show()
        );
    }

    private String getDetailedSubscriptionDescription(String sku) {
        ProductDetails details = getProductDetails(sku);
        if (details == null || !details.getProductType().equals(BillingClient.ProductType.SUBS)) {
            return null;
        }

        List<ProductDetails.SubscriptionOfferDetails> offerDetailsList = details.getSubscriptionOfferDetails();
        if (offerDetailsList == null || offerDetailsList.isEmpty()) {
            return null;
        }
        ProductDetails.SubscriptionOfferDetails offerDetails = offerDetailsList.get(0);
        String productName = details.getName();

        StringBuilder descriptionBuilder = new StringBuilder();
        descriptionBuilder.append(productName).append(":\n\n");

        List<ProductDetails.PricingPhase> pricingPhases = offerDetails.getPricingPhases().getPricingPhaseList();
        int totalPhases = pricingPhases.size();

        for (int i = 0; i < totalPhases; i++) {
            ProductDetails.PricingPhase phase = pricingPhases.get(i);
            String formattedPrice = phase.getFormattedPrice();
            String billingPeriod = phase.getBillingPeriod(); // e.g. "P7D", "P1M", "P1Y"
            int billingCycleCount = phase.getBillingCycleCount();

            String readablePeriod = convertPeriodToReadableString(billingPeriod, billingCycleCount, false);
            String perPeriod = convertPeriodToReadableString(billingPeriod, 1, true);
            String formattedPricePerPeriod = getString(R.string.subscription_price_per_period, formattedPrice, perPeriod);

            descriptionBuilder.append("\t• ");

            // trial phase
            if (phase.getPriceAmountMicros() == 0) {
                String trialDescription = getString(R.string.subscription_free_trial_description, readablePeriod);
                descriptionBuilder.append(trialDescription).append("\n");
            } else {
                // paid phase
                if (billingCycleCount > 0) {
                    if (i > 0) {
                        descriptionBuilder.append(getString(R.string.subscription_prefix_after_period));
                    }
                    String formattedOfferString = getString(R.string.subscription_offer_price_for_period, formattedPricePerPeriod, readablePeriod);
                    descriptionBuilder.append(formattedOfferString).append("\n");
                } else {
                    if (i > 0) {
                        descriptionBuilder.append(getString(R.string.subscription_prefix_subsequently)).append(" ");
                    }
                    descriptionBuilder.append(formattedPricePerPeriod).append("\n");
                }
            }
        }
        descriptionBuilder.append("\n").append(getString(R.string.subscription_renewal_info));

        return descriptionBuilder.toString();
    }
    private String convertPeriodToReadableString(String period, int cycleCount, boolean onlyUnit) {
        String unit;
        // extract the first character
        char periodChar = period.toUpperCase().charAt(period.length() - 1);

        switch (periodChar) {
            case 'D': // days
                unit = (cycleCount == 1) ? getString(R.string.unit_day) : getString(R.string.unit_days);
                break;
            case 'W': // weeks
                unit = (cycleCount == 1) ? getString(R.string.unit_week) : getString(R.string.unit_weeks);
                break;
            case 'M': // months
                unit = (cycleCount == 1) ? getString(R.string.unit_month) : getString(R.string.unit_months);
                break;
            case 'Y': // years
                unit = (cycleCount == 1) ? getString(R.string.unit_year) : getString(R.string.unit_years);
                break;
            default:
                return period;
        }

        if (onlyUnit){
            return unit;
        } else {
            return String.format("%d %s", cycleCount, unit);
        }
    }

private String getProductWithPrice(int resId, String sku) {
        String price = PurchaseManager.getPrice(sku);
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
        purchaseManager = PurchaseManager.getInstance(this);
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
        String productType = (PurchaseManager.ITEM_ONE_YEAR_SUBSCRIPTION.equals(sku)) ? BillingClient.ProductType.SUBS : BillingClient.ProductType.INAPP;
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
        for (String sku : PurchaseManager.fullSkuList) {
            boolean isPurchased = purchaseManager.rawIsPurchased(sku);
            Log.i(TAG, String.format("onPurchasesInitialized(%s, %s)", sku, isPurchased));
        }
    }

    protected void onItemPurchased(String sku) {
        purchaseManager.setPurchased(sku, true);
        showThankYouDialog();
    }

    protected void onItemConsumed(String sku) {
        purchaseManager.setPurchased(sku, false);
    }

    void handlePurchase(final Purchase purchase) {
        Log.d(TAG, "handlePurchase(" + purchase + " )");
        if (purchase == null) {
            return;
        }

        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            List<String> skus = purchase.getProducts();
            for (String sku : skus) {
                if (PurchaseManager.fullSkuList.contains(sku)) { // Check if it's a product we are managing
                    if (sku.equals(PurchaseManager.ITEM_ONE_YEAR_SUBSCRIPTION)) {
                        // Handle Subscription
                        if (!purchase.isAcknowledged()) {
                            acknowledgePurchase(purchase); // Acknowledge subscriptions
                        }
                        purchaseManager.setPurchased(sku, true); // Update cache
                        onItemPurchased(sku); // Your custom logic for purchased items
                        Log.i(TAG, "Subscription purchased: " + sku);
                    } else {
                        // Handle In-App Purchases
                        if (!purchase.isAcknowledged()) {
                            acknowledgePurchase(purchase); // Acknowledge in-app items
                        }
                        purchaseManager.setPurchased(sku, true); // Update cache
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
        for (Purchase purchase : purchaseList) {
            List<String> skus = purchase.getProducts();
            for (String sku : skus) {
                if (PurchaseManager.fullSkuList.contains(sku)) { // Ensure it's a product we care about
                    int state = purchase.getPurchaseState();
                    boolean purchased = (state == Purchase.PurchaseState.PURCHASED);
                    purchaseManager.setPurchased(sku, purchased);
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
                if (PurchaseManager.ITEM_ONE_YEAR_SUBSCRIPTION.equals(productId)) {
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

        purchaseManager.setPurchased(PurchaseManager.ITEM_ONE_YEAR_SUBSCRIPTION, isItemOneYearSubscribed);
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
        for (String sku : PurchaseManager.fullSkuList) {
            if (PurchaseManager.ITEM_ONE_YEAR_SUBSCRIPTION.equals(sku)) {
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
                    PurchaseManager.setPrice(productId, price);
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
                            PurchaseManager.setPrice(productId, price);
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
                    purchaseManager.setPurchased(sku, false);
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
        return (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED);
    }
}
