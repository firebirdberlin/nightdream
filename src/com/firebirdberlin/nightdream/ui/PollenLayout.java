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

import com.firebirdberlin.nightdream.databinding.PollenCountBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

    public void setupFromJSON (Context mContext, String result) {
        Log.d(TAG, "setupFromJSON");

        ArrayList<HashMap<String, String>> pollenList;
        ArrayList<Drawable> setupPollenImages = new ArrayList<>();
        pollenList = new ArrayList<>();

        if (result != null) {
            try {
                JSONObject jsonObj = new JSONObject(result);
                JSONArray content = jsonObj.getJSONArray("content");

                for (int i = 0; i < content.length(); i++) {
                    JSONObject c = content.getJSONObject(i);
                    String partregion_name = c.getString("partregion_name");

                    if (partregion_name.equals("Rhein.-Westfäl. Tiefland")) {
                        JSONObject pollen = c.getJSONObject("Pollen");

                        HashMap<String, String> pollenTmp = new HashMap<>();

                        Iterator<String> iter = pollen.keys();
                        while (iter.hasNext()) {
                            String key = iter.next();
                            try {
                                String herb = null;

                                switch (key){
                                    case "Ambrosia": herb = "ambrosia"; break;
                                    case "Beifuss": herb = "mugwort"; break;
                                    case "Birke": herb = "birch"; break;
                                    case "Erle": herb = "alder"; break;
                                    case "Esche": herb = "ash"; break;
                                    case "Graeser": herb = "grass"; break;
                                    case "Hasel": herb = "hazelnut"; break;
                                    case "Roggen": herb = "rye"; break;
                                }

                                JSONObject forecast = pollen.getJSONObject(key);
                                pollenTmp.put(herb, forecast.getString("today"));
                            } catch (JSONException e) {
                                Log.e(TAG, "Json pollen error: " + e.getMessage());
                            }
                        }
                        pollenList.add(pollenTmp);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "Json parsing error: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "Couldn't get json from server.");
        }

        Log.d(TAG, "pollen: "+pollenList);

        String [] color = new String [7];
        color[0] = "#FF006400";
        color[1] = "#FF008000";
        color[2] = "#FFADFF2F";
        color[3] = "#FFFFD700";
        color[4] = "#FFFF6347";
        color[5] = "#FFFF0000";
        color[6] = "#FFDC143C";

        for (HashMap<String, String> map : pollenList) {
            for (Map.Entry<String, String> entrySet : map.entrySet()) {
                String icon="ic_" + entrySet.getKey();
                int resID = mContext.getResources().getIdentifier(icon, "drawable",  mContext.getPackageName());

                if (resID != 0) {
                    Drawable herb = ContextCompat.getDrawable(mContext, resID);

                    if (herb != null) {
                        if (pollenList.get(0).get(entrySet.getKey()) != null) {
                            herb.setColorFilter(Color.parseColor(color[Integer.parseInt(pollenList.get(0).get(entrySet.getKey()))]), PorterDuff.Mode.SRC_ATOP);
                            setupPollenImages.add(herb);
                        }
                    }
                }
            }
        }

        this.pollenImages.setValue(setupPollenImages);
    }

}
