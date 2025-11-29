package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

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
    public static final int PRODUCT_ID_DONATION = 2;
    public static final int PRODUCT_ID_PRO = 3;
    //    private static final int PRODUCT_ID_ACTIONS = 4;
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

    private static HashMap<String, String> prices = new HashMap<>();

    private PurchaseManager(Context context) {
        preferences = Settings.getDefaultSharedPreferences(context);
        if (preferences != null) { // initialize from cache
            for (String sku : fullSkuList) {
                boolean isPurchased = preferences.getBoolean(String.format("purchased_%s", sku), false);
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
        if (BuildConfig.FLAVOR == "noGms") {
            return true;
        }
        if (BuildConfig.DEBUG) {
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

    public boolean rawIsPurchased(String sku) {
        return Boolean.TRUE.equals(purchases.get(sku));
    }

    void setPurchased(String sku, boolean value) {
        Log.i(TAG, String.format("setPurchased(%s, %s)", sku, value));
        purchases.put(sku, value);
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(String.format("purchased_%s", sku), value);
            editor.apply();
        }
    }
}
