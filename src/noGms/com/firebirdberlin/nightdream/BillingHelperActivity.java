package com.firebirdberlin.nightdream;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public abstract class BillingHelperActivity extends AppCompatActivity {
    static final String TAG = "BillingHelperActivity";
    private PurchaseManager purchaseManager = null;


    public boolean isPurchased(String sku) {
        return purchaseManager.isPurchased(sku);
    }

    public void showPurchaseDialog() {
    }

    public void showSubscriptionDialog() {
    }


    private void updateAllPurchases() {
        queryPurchases();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        purchaseManager = PurchaseManager.getInstance(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void launchBillingFlow(String sku) {}


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

    void queryPurchases() {
        onPurchasesInitialized();
    }


    public void showThankYouDialog() {
    }

    public void showPurchasePendingDialog() {
    }

    public boolean hasPermission(String permission) {
        return (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED);
    }
}
