package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.graphics.Color;
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

import com.firebirdberlin.nightdream.Pollen;
import com.firebirdberlin.nightdream.databinding.PollenCountBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PollenLayout extends ViewModel {
    private static String TAG = "PollenLayout";
    private PollenCountBinding pollencountBinding;
    private final MutableLiveData<ArrayList<Drawable>> pollenImages = new MediatorLiveData<>();

    public PollenLayout(ConstraintLayout pollenContainer) {
        LayoutInflater inflater = LayoutInflater.from(pollenContainer.getContext());
        pollencountBinding = PollenCountBinding.inflate(inflater, pollenContainer, false);
        pollencountBinding.setModel(this);
    }

    public View getView() {
        return pollencountBinding.getRoot();
    }

    public LiveData<ArrayList<Drawable>> getPollenImages() {
        return pollenImages;
    }

    public void setupFromObject (Context mContext, Pollen result) {
        Log.d(TAG, "setupFromObject");

        ArrayList<Drawable> setupPollenImages = new ArrayList<>();

        String [] color = new String [7];
        color[0] = "#FF006400";
        color[1] = "#FF008000";
        color[2] = "#FFADFF2F";
        color[3] = "#FFFFD700";
        color[4] = "#FFFF6347";
        color[5] = "#FFFF0000";
        color[6] = "#FFDC143C";

        for (HashMap<String, String> map : result.getPollenList()) {
            for (Map.Entry<String, String> entrySet : map.entrySet()) {
                String icon="ic_" + entrySet.getKey();
                int resID = mContext.getResources().getIdentifier(icon, "drawable",  mContext.getPackageName());

                if (resID != 0) {
                    Drawable herb = ContextCompat.getDrawable(mContext, resID);

                    if (herb != null) {
                        if (result.getPollenList().get(0).get(entrySet.getKey()) != null) {
                            herb.setColorFilter(Color.parseColor(color[Integer.parseInt(result.getPollenList().get(0).get(entrySet.getKey()))]), PorterDuff.Mode.SRC_ATOP);
                            setupPollenImages.add(herb);
                        }
                    }
                }
            }
        }

        this.pollenImages.setValue(setupPollenImages);

    }

}
