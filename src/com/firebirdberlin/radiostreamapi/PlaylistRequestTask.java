package com.firebirdberlin.radiostreamapi;

import android.os.AsyncTask;

import com.firebirdberlin.radiostreamapi.models.PlaylistInfo;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

import java.net.URL;
import java.util.List;

public class PlaylistRequestTask extends AsyncTask<String, Void, PlaylistInfo> {

    public interface AsyncResponse {
        public void onRequestFinished(PlaylistInfo result);
    }

    private AsyncResponse delegate = null;

    public PlaylistRequestTask(AsyncResponse listener) {
        this.delegate = listener;
    }

    @Override
    protected PlaylistInfo doInBackground(String... query) {
        String playlistUrl = query[0];
        return PlaylistParser.parsePlaylistUrl(playlistUrl);
    }

    @Override
    protected void onPostExecute(PlaylistInfo result) {
        delegate.onRequestFinished(result);
    }
}
