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
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PurchaseManager {
    public static final String ITEM_ONE_YEAR_SUBSCRIPTION = "oneyear";
    public static final String ITEM_WEATHER_DATA = "weather_data";
    public static final String ITEM_DONATION = "donation";
    public static final String ITEM_WEB_RADIO = "web_radio";
    public static final String ITEM_PRO = "pro";
    public static final String ITEM_ACTIONS = "actions";
    private static final String TAG = PurchaseManager.class.getSimpleName();

    public static final int PRODUCT_ID_DONATION = 2;
    public static final int PRODUCT_ID_PRO = 3;
    public static final int PRODUCT_ID_ONE_YEAR_SUBSCRIPTION = 5;

    public static List<String> fullSkuList = new ArrayList<>(
            Arrays.asList(
                    ITEM_DONATION, ITEM_PRO, ITEM_WEATHER_DATA,
                    ITEM_ACTIONS, ITEM_WEB_RADIO, ITEM_ONE_YEAR_SUBSCRIPTION
            )
    );
    private static PurchaseManager instance;
    private final SharedPreferences preferences;
    Map<String, Boolean> purchases = getDefaultPurchaseMap();

    public static void setPrice(String productId, String price) {
        prices.put(productId, price);
    }

    public static String getPrice(String productId) {
        return prices.get(productId);
    }

    private static final HashMap<String, String> prices = new HashMap<>();

    private PurchaseManager(Context context) {
        preferences = Settings.getDefaultSharedPreferences(context);
        if (preferences != null) { // initialize from cache
            for (String sku : fullSkuList) {
                boolean isPurchased = preferences.getBoolean(
                        String.format(Locale.US, "purchased_%s", sku), false);
                purchases.put(sku, isPurchased);
            }
        }
    }

    public static synchronized PurchaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new PurchaseManager(context);
        }
        return instance;
    }

    static HashMap<String, Boolean> getDefaultPurchaseMap() {
        HashMap<String, Boolean> def = new HashMap<>();
        for (String sku : fullSkuList) {
            def.put(sku, false);
        }
        return def;
    }

    public boolean isPurchased(String sku) {
        // Check if it's a debuggable build, if so, bypass the check and return true.
        if ("noGms".equalsIgnoreCase(BuildConfig.FLAVOR) || BuildConfig.DEBUG || Utility.isEmulator()) {
            return true;
        }

        if (purchases != null) {
            boolean hasDonation = Boolean.TRUE.equals(purchases.get(ITEM_DONATION));
            boolean hasSubscription = Boolean.TRUE.equals(purchases.get(ITEM_ONE_YEAR_SUBSCRIPTION));
            boolean hasRawPro = Boolean.TRUE.equals(purchases.get(ITEM_PRO));

            // Grandfather rule: Owning all 3 legacy features grants PRO entitlement
            boolean hasLegacyAllThree = Boolean.TRUE.equals(purchases.get(ITEM_WEATHER_DATA))
                    && Boolean.TRUE.equals(purchases.get(ITEM_WEB_RADIO))
                    && Boolean.TRUE.equals(purchases.get(ITEM_ACTIONS));

            boolean hasProAccess = hasDonation || hasSubscription || hasRawPro || hasLegacyAllThree;

            switch (sku) {
                case ITEM_DONATION:
                    return hasDonation;
                case ITEM_ONE_YEAR_SUBSCRIPTION:
                    return hasSubscription;
                case ITEM_PRO:
                    return hasProAccess;
                case ITEM_WEATHER_DATA:
                    return hasProAccess || Boolean.TRUE.equals(purchases.get(ITEM_WEATHER_DATA));
                case ITEM_WEB_RADIO:
                    return hasProAccess || Boolean.TRUE.equals(purchases.get(ITEM_WEB_RADIO));
                case ITEM_ACTIONS:
                    return hasProAccess || Boolean.TRUE.equals(purchases.get(ITEM_ACTIONS));
            }
        }
        return false;
    }

    public boolean rawIsPurchased(String sku) {
        return Boolean.TRUE.equals(purchases.get(sku));
    }

    void setPurchased(String sku, boolean value) {
        Log.i(TAG, String.format(Locale.US, "setPurchased(%s, %s)", sku, value));
        purchases.put(sku, value);
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(String.format(Locale.US, "purchased_%s", sku), value);
            editor.apply();
        }
    }
}
