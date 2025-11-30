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

package com.firebirdberlin.nightdream;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.service.dreams.DreamService;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class NightDreamService extends DreamService {

    final Context context = this;
    Handler handler = new Handler();
    Runnable startDelayed = new Runnable() {
        @Override
        public void run() {
            Utility.turnScreenOn(context);
            handler.removeCallbacks(startDelayed);
            NightDreamActivity.start(context);
            finish();
        }
    };

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setScreenBright(true);
        setFullscreen(true);
    }

    public void onDreamingStarted() {
        handler.postDelayed(startDelayed, 5000);
    }

    public void onDreamingStopped() {
        handler.removeCallbacks(startDelayed);
    }
}
