package com.firebirdberlin.radiostreamapi;

import android.os.AsyncTask;

import java.util.List;

import com.firebirdberlin.radiostreamapi.DirbleApi;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

public class StationRequestTask extends AsyncTask<String, Void, List<RadioStation> > {

    public interface AsyncResponse {
        public void onRequestFinished(List<RadioStation> stations);
    }

    private AsyncResponse delegate = null;

    public StationRequestTask(AsyncResponse listener) {
        this.delegate = listener;
    }

    @Override
    protected List<RadioStation> doInBackground(String... query) {
        String q = query[0];
        String countryCode = query.length > 1 ? query[1] : null;

        return RadioBrowserApi.fetchStations(q, countryCode);
        //return DirbleApi.fetchStations(q, countryCode);
    }

    @Override
    protected void onPostExecute(List<RadioStation> stations) {
        delegate.onRequestFinished(stations);
    }
}
