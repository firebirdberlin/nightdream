package com.firebirdberlin.nightdream.viewmodels;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.models.BatteryValue;

public class BatteryReferenceViewModel extends ViewModel {
    private static String TAG = "BatteryReferenceViewModel";
    private static MutableLiveData<BatteryValue> batteryValue;

    public static boolean updateIfNecessary(Context context, BatteryValue value) {
        BatteryValue reference = BatteryReferenceViewModel.getValue();
        Log.i(TAG, "current: " + value.toJson());
        if (reference != null) {
            Log.i(TAG, "reference: " + reference.toJson());
            Log.i(TAG, "remaining: " + value.getEstimateMillis(reference));
        }
        if (reference == null
                || reference.chargingMethod != value.chargingMethod
                || reference.isAirplaneModeOn != value.isAirplaneModeOn) {
            BatteryReferenceViewModel.set(context, value);
            return true;
        }
        return false;
    }

    public static void set(Context context, BatteryValue value) {
        Log.i(TAG, "setting " + (value != null ? value.toString() : "null"));
        batteryValue.postValue(value);
        Settings.saveBatteryReference(context, value);
    }

    private MutableLiveData<BatteryValue> get(Context context) {
        if (batteryValue == null) {
            batteryValue = new MutableLiveData<>();
            BatteryValue value = Settings.loadBatteryReference(context);
            batteryValue.postValue(value);
        }
        return batteryValue;
    }

    public static BatteryValue getValue() {
        if (batteryValue == null){
            return null;
        }
        else {
            return batteryValue.getValue();
        }
    }

    public static void observe(Context context, @NonNull Observer<BatteryValue> observer) {
        BatteryReferenceViewModel model = new ViewModelProvider(
                (ViewModelStoreOwner) context
        ).get(BatteryReferenceViewModel.class);
        model.get(context).observe((LifecycleOwner) context, observer);
    }
}