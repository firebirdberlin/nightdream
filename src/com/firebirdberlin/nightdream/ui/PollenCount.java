package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;

import com.firebirdberlin.nightdream.HttpReader;
import com.firebirdberlin.nightdream.databinding.PollenCountBinding;

import java.lang.ref.WeakReference;

public class PollenCount extends AsyncTask<String, Void, String> {

    private static String TAG = "PollenCount";
    private WeakReference<Context> mContext;
    private WeakReference<ConstraintLayout> pollenContainer;

    public PollenCount(Context mContext, ConstraintLayout pollenContainer) {
        this.mContext = new WeakReference<>(mContext);
        this.pollenContainer = new WeakReference<>(pollenContainer);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(TAG, "Downloading pollen data");
        Toast.makeText(mContext.get(),"Downloading Pollen Data",Toast.LENGTH_LONG).show();
    }

    @Override
    protected String doInBackground(String... arg0) {
        String jsonStr;

        HttpReader hr = new HttpReader();
        jsonStr = hr.readUrl("https://opendata.dwd.de/climate_environment/health/alerts/s31fg.json");

        Log.d(TAG, "Response from pollen url: " + jsonStr);

        return jsonStr;
    }

    @Override
    protected void onPostExecute(String result) {
        View boundView = pollenContainer.get().getChildAt(0);
        PollenCountBinding pollencountBinding = DataBindingUtil.getBinding(boundView);

        if (pollencountBinding != null) {
            pollencountBinding.getModel().setupFromJSON(mContext.get(), result);
            pollencountBinding.invalidateAll();
        }else {
            PollenLayout pollenLayout = new PollenLayout(pollenContainer.get());
            pollenContainer.get().removeAllViews();
            pollenContainer.get().addView(pollenLayout.getView());
            pollenLayout.setupFromJSON(mContext.get(), result);
        }
    }

}
