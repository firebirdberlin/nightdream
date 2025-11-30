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
