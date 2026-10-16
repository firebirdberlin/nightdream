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

import android.os.Handler;
import android.os.Looper;

import com.firebirdberlin.radiostreamapi.models.PlaylistInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaylistRequestTask {

    private AsyncResponse delegate;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean cancelled = false;

    public interface AsyncResponse {
        void onPlaylistRequestFinished(PlaylistInfo result);
    }

    public PlaylistRequestTask(AsyncResponse listener) {
        this.delegate = listener;
    }

    public void execute(String playlistUrl) {
        executor.execute(() -> {
            if (cancelled) return;
            PlaylistParser parser = new PlaylistParser();
            PlaylistInfo result = parser.parsePlaylistUrl(playlistUrl);
            mainHandler.post(() -> {
                if (!cancelled && delegate != null) {
                    delegate.onPlaylistRequestFinished(result);
                }
            });
        });
    }

    public void cancel(boolean mayInterruptIfRunning) {
        this.cancelled = true;
    }
}
