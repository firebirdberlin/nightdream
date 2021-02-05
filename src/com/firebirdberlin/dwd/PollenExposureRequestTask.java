package com.firebirdberlin.dwd;

import android.content.Context;
import android.os.AsyncTask;

import com.firebirdberlin.nightdream.PollenExposure;

public class PollenExposureRequestTask extends AsyncTask<String, Void, PollenExposure> {

    private final AsyncResponse delegate;
    HttpReader httpReader;

    public PollenExposureRequestTask(AsyncResponse listener, final Context context) {
        this.delegate = listener;
        httpReader = new HttpReader(context, "pollen.json");
    }

    @Override
    protected PollenExposure doInBackground(String... postalCode) {
        String postCode = postalCode[0];

        String url = "https://opendata.dwd.de/climate_environment/health/alerts/s31fg.json";
        PollenExposure pollen = new PollenExposure();
        pollen.setPostCode(postCode);

        // read data (either from the url or from the cache)
        String result = httpReader.readUrl(url, false);
        if (result != null && !result.isEmpty() && postCode.length() > 2) {
            pollen.parse(result, postCode);

            // check if there's an update pending
            long nextUpdate = pollen.getNextUpdate();
            long now = System.currentTimeMillis();
            if (nextUpdate > -1L && nextUpdate < now) {
                result = httpReader.readUrl(url, true);
                if (result != null && !result.isEmpty() && postCode.length() > 2) {
                    pollen.parse(result, postCode);
                }
            }
        }
        return pollen;
    }

    public interface AsyncResponse {
        void onRequestFinished(PollenExposure result);
    }

    @Override
    protected void onPostExecute(PollenExposure result) {
        delegate.onRequestFinished(result);
    }
}
