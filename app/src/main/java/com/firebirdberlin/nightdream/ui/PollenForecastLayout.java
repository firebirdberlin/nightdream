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
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.firebirdberlin.nightdream.R;

import java.lang.reflect.Field;

public class PollenForecastLayout extends ConstraintLayout {
    private static final String TAG = "PollenForecastLayout";
    private Context context = null;
    private ImageView pollenImage = null;
    private TextView pollenText = null;
    private TextView pollenStressLevel = null;

    public PollenForecastLayout(@NonNull Context context) {
        super(context);
        this.context = context;
        init();
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View child = inflater.inflate(R.layout.pollen_forecast_layout, null);
        addView(child);

        pollenImage = findViewById(R.id.pollenImage);
        pollenText = findViewById(R.id.pollenText);
        pollenStressLevel = findViewById(R.id.pollenStressLevel);
    }

    public void setImage(Drawable image) {
        pollenImage.setImageDrawable(image);
    }

    public void setPollenText(String text) {
        pollenText.setText(text);
    }

    public void setPollenStressLevel(int index) {
        int pollenStressId = 0;
        String pollenStress = "showPollenIndex" + index;
        Log.d(TAG, "pollenkey: " + pollenStress);
        Class resString = R.string.class;
        Field field = null;
        try {
            field = resString.getField(pollenStress);
            pollenStressId = field.getInt(null);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        pollenStressLevel.setText(context.getString(pollenStressId));
    }

}
