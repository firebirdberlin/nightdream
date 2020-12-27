package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;

import com.firebirdberlin.nightdream.HttpReader;
import com.firebirdberlin.nightdream.Pollen;
import com.firebirdberlin.nightdream.databinding.PollenCountBinding;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PollenCount extends AsyncTask<String, Void, String> {

    private static String TAG = "PollenCount";
    Pollen pollen;
    private WeakReference<Context> mContext;
    private WeakReference<ConstraintLayout> pollenContainer;

    public PollenCount(Context mContext, ConstraintLayout pollenContainer, Pollen pollen) {
        this.mContext = new WeakReference<>(mContext);
        this.pollenContainer = new WeakReference<>(pollenContainer);
        this.pollen = pollen;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... arg0) {
        Log.d(TAG, "doInBackground");
        String jsonStr = "";
        Date nextUpdate = new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24)); //default set to yesterday

        if ( pollen.getNextUpdate() != null)
        {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            try {
                nextUpdate = dateFormat.parse(pollen.getNextUpdate());
            } catch (Exception ex) {
                Log.e(TAG, ex.toString());
            }
        }

        if (new Date().after(nextUpdate)) {
            Log.d(TAG, "Downloading pollen data");

            HttpReader hr = new HttpReader();
            jsonStr = hr.readUrl("https://opendata.dwd.de/climate_environment/health/alerts/s31fg.json");

            Log.d(TAG, "Response from pollen url: " + jsonStr);
        }

        return jsonStr;
    }

    @Override
    protected void onPostExecute(String result) {

        if (!result.isEmpty()) {
            pollen.setupPollen(result);
        }

        View boundView = pollenContainer.get().getChildAt(0);
        PollenCountBinding pollencountBinding = DataBindingUtil.getBinding(boundView);

        if (pollen.getPollenList() != null) {
            if (pollencountBinding != null) {
                pollencountBinding.getModel().setupFromObject(mContext.get(), pollen);
                pollencountBinding.invalidateAll();
            } else {
                PollenLayout pollenLayout = new PollenLayout(pollenContainer.get());
                pollenContainer.get().removeAllViews();
                pollenContainer.get().addView(pollenLayout.getView());
                pollenLayout.setupFromObject(mContext.get(), pollen);
            }
        }
    }

}
