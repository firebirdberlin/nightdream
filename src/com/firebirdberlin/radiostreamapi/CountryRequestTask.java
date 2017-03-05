package com.firebirdberlin.radiostreamapi;

import android.content.Context;
import android.os.AsyncTask;

import com.firebirdberlin.radiostreamapi.models.Country;

import java.util.List;

public class CountryRequestTask extends AsyncTask<Void, Void, List<Country> > {

    public interface AsyncResponse {
        public void onCountryRequestFinished(List<Country> countries);
    }

    private CountryRequestTask.AsyncResponse delegate = null;
    private Context context;

    public CountryRequestTask(CountryRequestTask.AsyncResponse listener, Context context) {
        this.delegate = listener;
        this.context = context;
    }

    @Override
    protected List<Country> doInBackground(Void... params) {
        return DirbleApi.fetchCountries(context.getCacheDir());
    }

    @Override
    protected void onPostExecute(List<Country> countries) {
        delegate.onCountryRequestFinished(countries);
    }

}
