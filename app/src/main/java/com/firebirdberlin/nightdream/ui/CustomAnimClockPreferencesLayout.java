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

package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;


public class CustomAnimClockPreferencesLayout extends LinearLayout {

    AppCompatActivity activity = null;
    private OnConfigChangedListener mListener = null;
    private Settings settings = null;

    public CustomAnimClockPreferencesLayout(
            Context context, Settings settings, AppCompatActivity activity
    ) {
        super(context);
        this.settings = settings;
        this.activity = activity;
        init(context);
    }

    public CustomAnimClockPreferencesLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(final Context context) {

        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View child = inflater.inflate(R.layout.custom_digital_anim_clock_preferences_layout, null);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(child, lp);

        settings.setFontUri("file:///android_asset/fonts/roboto_thin.ttf", "Roboto Thin", 9);

        boolean showSeconds =
                settings.getShowSeconds(ClockLayout.LAYOUT_ID_DIGITAL_ANIMATED);
        SwitchCompat switchShowSeconds = child.findViewById(R.id.switch_show_seconds);
        switchShowSeconds.setChecked(showSeconds);
        switchShowSeconds.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            settings.setShowSeconds(isChecked, ClockLayout.LAYOUT_ID_DIGITAL_ANIMATED);
            if (mListener != null) {
                mListener.onConfigChanged();
            }
        });
    }

    public void setOnConfigChangedListener(OnConfigChangedListener listener) {
        this.mListener = listener;
    }

    public interface OnConfigChangedListener {
        void onConfigChanged();

        void onPurchaseRequested();
    }
}
