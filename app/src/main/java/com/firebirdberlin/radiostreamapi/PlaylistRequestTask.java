/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
