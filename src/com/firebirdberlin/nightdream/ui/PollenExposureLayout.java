package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.firebirdberlin.nightdream.PollenExposure;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.databinding.PollenExposureBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PollenExposureLayout extends ViewModel {
    private final static String TAG = "PollenLayout";
    private final MutableLiveData<ArrayList<Drawable>> pollenImages = new MediatorLiveData<>();
    private final PollenExposureBinding pollenExposureBinding;

    private int[] colorResId = new int[]{
            R.color.grey,
            R.color.material_light_green,
            R.color.material_yellow,
            R.color.material_amber,
            R.color.material_orange,
            R.color.material_purple,
            R.color.material_red
    };

    public PollenExposureLayout(ConstraintLayout pollenContainer) {
        LayoutInflater inflater = LayoutInflater.from(pollenContainer.getContext());
        pollenExposureBinding = PollenExposureBinding.inflate(inflater, pollenContainer, false);
        pollenExposureBinding.setModel(this);
    }

    public View getView() {
        return pollenExposureBinding.getRoot();
    }

    public LiveData<ArrayList<Drawable>> getPollenImages() {
        return pollenImages;
    }

    public void setupFromObject(Context mContext, PollenExposure result) {
        Log.d(TAG, "setupFromObject");

        ArrayList<Drawable> setupPollenImages = new ArrayList<>();

        Resources res = mContext.getResources();
        for (Map<String, String> map : result.getPollenList()) {
            for (Map.Entry<String, String> entrySet : map.entrySet()) {
                String icon = "ic_" + entrySet.getKey();
                String value = entrySet.getValue();
                int resID = res.getIdentifier(icon, "drawable", mContext.getPackageName());

                if (resID == 0) {
                    continue;
                }

                Drawable herbDrawable = ContextCompat.getDrawable(mContext, resID);

                if (herbDrawable != null && value != null) {
                    // the index may be "0-1", "1-2"
                    value = value.substring(value.length() - 1);
                    int index = Integer.parseInt(value);
                    index = Math.max(0, index);
                    index = Math.min(6, index);
                    int col = res.getColor(colorResId[index]);
                    herbDrawable.setColorFilter(col, PorterDuff.Mode.SRC_ATOP);
                    setupPollenImages.add(herbDrawable);
                }
            }
        }

        this.pollenImages.setValue(setupPollenImages);
    }
}
