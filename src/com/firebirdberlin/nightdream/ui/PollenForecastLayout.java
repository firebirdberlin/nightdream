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
    private TextView pollenStressLevel= null;

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
        pollenStressLevel =  findViewById(R.id.pollenStressLevel);
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
        Log.d(TAG, "pollenkey: "+pollenStress);
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
