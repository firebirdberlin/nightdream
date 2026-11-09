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

package com.firebirdberlin.nightdream.repositories;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

public class VibrationHandler {
    private static final String TAG = VibrationHandler.class.getSimpleName();

    Vibrator vibrator;

    @SuppressWarnings("deprecation")
    public static boolean hasVibrator(Context context) {
        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vibratorManager != null ? vibratorManager.getDefaultVibrator() : null;
        } else {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
        return vibrator != null && vibrator.hasVibrator();
    }

    @SuppressWarnings("deprecation")
    public VibrationHandler(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vibratorManager != null) {
                vibrator = vibratorManager.getDefaultVibrator();
            }
        }
        if (vibrator == null) {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    @SuppressWarnings("deprecation")
    public void startVibration() {
        Log.i(TAG, "startvibration");
        if (vibrator == null) {
            return;
        }
        long[] pattern = {0, 1000, 100, 1000, 100};
        int[] amplitudes = {0, 255, 0, 128, 0};

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            vibrator.vibrate(pattern, 0);
        } else {
            VibrationEffect effect = VibrationEffect.createWaveform(pattern, amplitudes, 0);
            vibrator.vibrate(effect);
        }
    }

    public void stopVibration() {
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    @SuppressWarnings("deprecation")
    public void startOneShotVibration(long milliseconds) {
        Log.d(TAG, "startOneShotVibration: " + milliseconds + "ms, vibrator=" + vibrator);
        if (vibrator == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                long[] timings = {0, milliseconds};
                int[] amplitudes = {0, 255};
                VibrationEffect effect = VibrationEffect.createWaveform(timings, amplitudes, -1);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                vibrator.vibrate(effect, audioAttributes);
            } else {
                vibrator.vibrate(milliseconds);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to vibrate", e);
        }
    }
}
