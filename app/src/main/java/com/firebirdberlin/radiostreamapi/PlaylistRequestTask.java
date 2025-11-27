package com.firebirdberlin.radiostreamapi;

import android.os.AsyncTask;

import com.firebirdberlin.radiostreamapi.models.PlaylistInfo;

public class PlaylistRequestTask extends AsyncTask<String, Void, PlaylistInfo> {

    private AsyncResponse delegate;

    public interface AsyncResponse {
        void onPlaylistRequestFinished(PlaylistInfo result);
    }

    public PlaylistRequestTask(AsyncResponse listener) {
        this.delegate = listener;
    }

    @Override
    protected PlaylistInfo doInBackground(String... query) {
        String playlistUrl = query[0];
        PlaylistParser parser = new PlaylistParser();
        return parser.parsePlaylistUrl(playlistUrl);
    }

    @Override
    protected void onPostExecute(PlaylistInfo result) {
        delegate.onPlaylistRequestFinished(result);
    }
}
