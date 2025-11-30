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
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.firebirdberlin.nightdream.R;

import com.firebirdberlin.AvmAhaApi.models.AvmAhaDevice;
import com.firebirdberlin.nightdream.SmartHomeActivity;


public class SmartHomeDeviceLayout extends LinearLayout {

    private static final String TAG = "SmartHomeDeviceLayout";
    private final Context context;
    private AvmAhaDevice device = null;

    private RelativeLayout mainLayout = null;
    private TextView textViewModelName = null;
    private SwitchCompat switchState = null;

    public SmartHomeDeviceLayout(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public SmartHomeDeviceLayout(Context context, AvmAhaDevice device) {
        super(context);
        this.context = context;
        this.device = device;
        init();
    }

    public SmartHomeDeviceLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View child = inflater.inflate(R.layout.avm_aha_device_layout, null);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(child, lp);
        mainLayout = findViewById(R.id.mainLayout);
        textViewModelName = findViewById(R.id.textViewModelName);
        switchState = findViewById(R.id.onoffswitch);

        update();
    }

    public void update(AvmAhaDevice device) {
        if (this.device.ain == device.ain) {
            this.device = device;
            update();
            invalidate();
        }
    }

    private void update() {
        switchState.setOnCheckedChangeListener(null);
        switchState.setChecked(device.isOn());
        switchState.setEnabled(device.isPresent());
        switchState.setText(device.name);
        textViewModelName.setEnabled(device.isPresent());
        textViewModelName.setText(device.productname);
        SwitchCompat.OnCheckedChangeListener checkedChangeListener = (compoundButton, isChecked) -> {
            String newState = isChecked ? AvmAhaDevice.STATE_ON : AvmAhaDevice.STATE_OFF;
            ((SmartHomeActivity) context).onDeviceStateChangeRequest(device, newState);
        };
        switchState.setOnCheckedChangeListener(checkedChangeListener);
    }
}
