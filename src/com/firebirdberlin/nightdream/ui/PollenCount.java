package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;

import com.firebirdberlin.nightdream.HttpReader;
import com.firebirdberlin.nightdream.Pollen;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.databinding.PollenCountBinding;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PollenCount extends AsyncTask<String, Void, String> {

    private static String TAG = "PollenCount";
    Pollen pollen;
    private WeakReference<Context> mContext;
    private WeakReference<ConstraintLayout> pollenContainer;
    private Settings settings;
    private WeatherEntry weather;


    public PollenCount(Context mContext, ConstraintLayout pollenContainer, Pollen pollen) {
        this.mContext = new WeakReference<>(mContext);
        this.pollenContainer = new WeakReference<>(pollenContainer);
        this.pollen = pollen;
        settings = new Settings(mContext);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... arg0) {
        Log.d(TAG, "doInBackground");
        weather = settings.weatherEntry;
        String postCode = "";

        Geocoder geoCoder = new Geocoder(mContext.get(), Locale.getDefault());
        List<Address> address = null;

        try {
            address = geoCoder.getFromLocation(weather.lat, weather.lon, 1);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (address != null && address.size() > 0) {
            postCode = address.get(0).getPostalCode();
            Log.d(TAG, "pollen plz: " + postCode);
        }

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

        if (new Date().after(nextUpdate) || !postCode.equals(pollen.getPostCode())) {
            Log.d(TAG, "Downloading pollen data");

            HttpReader hr = new HttpReader();
            jsonStr = hr.readUrl("https://opendata.dwd.de/climate_environment/health/alerts/s31fg.json");

            pollen.setPostCode(postCode);

            Log.d(TAG, "Response from pollen url: " + jsonStr);
        }

        return jsonStr;
    }

    @Override
    protected void onPostExecute(String result) {

        if (!result.isEmpty()) {
            pollen.setupPollen(result, "Rhein.-Westfäl. Tiefland");
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
