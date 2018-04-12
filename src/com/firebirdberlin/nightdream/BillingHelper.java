package com.firebirdberlin.nightdream;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BillingHelper {
    static final String TAG = "BillingHelper";
   public static final String ITEM_DONATION = "donation";
   public static final String ITEM_WEATHER_DATA = "weather_data";
   public static final String ITEM_WEB_RADIO = "web_radio";
   public static final String ITEM_PRO = "pro";

   private final Map<String, Boolean> purchases = createMap();
   IInAppBillingService mService;
   Context context;

   public BillingHelper(Context context, IInAppBillingService mService) {
      this.context = context;
      this.mService = mService;
   }

   private Map<String, Boolean> createMap() {
      Map<String,Boolean> map = new HashMap<>();
      map.put(ITEM_DONATION, false);
      map.put(ITEM_WEATHER_DATA, false);
      map.put(ITEM_WEB_RADIO, false);
      map.put(ITEM_PRO, false);
      return map;
   }

   public Map<String, Boolean> getPurchases() {
      for (String key : purchases.keySet()) {
         purchases.put(key, false);
      }
      if (mService == null) {
         return purchases;
      }

      Bundle ownedItems;
      try {
         ownedItems = mService.getPurchases(3, context.getPackageName(), "inapp", null);
      } catch (RemoteException e) {
         return purchases;
      }


      if (ownedItems == null) {
          return purchases;
      }

      int response = ownedItems.getInt("RESPONSE_CODE");
      if (response == 0) {
         ArrayList<String> ownedSkus =
                 ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
         ArrayList<String> purchaseDataList =
                 ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
         ArrayList<String> signatureList =
                 ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
         String continuationToken = ownedItems.getString("INAPP_CONTINUATION_TOKEN");

         for (int i = 0; i < purchaseDataList.size(); ++i) {
            String purchaseData = purchaseDataList.get(i);
            String signature = signatureList.get(i);
            String sku = ownedSkus.get(i);
            if ( purchases.containsKey(sku) ) {
               purchases.put(sku, true);
            }

            // do something with this purchase information
            // e.g. display the updated list of products owned by user
            // or consume the purchase
//                try {
//                    JSONObject o = new JSONObject(purchaseData);
//                    String purchaseToken = o.getString("purchaseToken");
//                    mService.consumePurchase(3, getActivity().getPackageName(), purchaseToken);
//                }
//                catch (Exception e) {
//                    e.printStackTrace();
//                }
         }

         // if continuationToken != null, call getPurchases again
         // and pass in the token to retrieve more items
      }

      if ( purchases.get(ITEM_DONATION) == true ) {
         purchases.put(ITEM_PRO, true);
         purchases.put(ITEM_WEATHER_DATA, true);
         purchases.put(ITEM_WEB_RADIO, true);
      }

      if ( purchases.get(ITEM_PRO) == true ) {
         purchases.put(ITEM_WEATHER_DATA, true);
         purchases.put(ITEM_WEB_RADIO, true);
      }
      return purchases;
   }

   public boolean isPurchased(String sku) {
       if (purchases.containsKey(sku)){
           return purchases.get(sku);
       }
       return false;
   }

}
