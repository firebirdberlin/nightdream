package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.firebirdberlin.nightdream.ui.PollenForecastLayout;
import com.firebirdberlin.openweathermapapi.models.City;

import java.lang.reflect.Field;
import java.util.Map;

public class WeatherForecastTabPollen extends Fragment {

    final static String TAG = "ForecastTabPollen";
    private final int[] colorResId = new int[]{
            R.color.grey,
            R.color.material_light_green,
            R.color.material_yellow,
            R.color.material_amber,
            R.color.material_orange,
            R.color.material_purple,
            R.color.material_red
    };
    private LinearLayout scrollViewLayout = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_weather_forecast_scrollview, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        scrollViewLayout = view.findViewById(R.id.scroll_view_layout);
    }

    public void onRequestFinished(City city, PollenExposure result) {
        Log.d(TAG, "addPollen - onRequestFinished");
        scrollViewLayout.removeAllViews();
        Context context = getContext();
        if (context == null) {
            return;
        }

        if (city == null) {
            Log.d(TAG, "addPollen: city = null -> return");
            return;
        }
        Utility.GeoCoder geoCoder = new Utility.GeoCoder(context, city.lat, city.lon);
        String countryCode = geoCoder.getCountryCode();
        String postCode = geoCoder.getPostalCode();
        if (!"DE".equals(countryCode) || postCode.isEmpty()) {
            Log.d(TAG, "addPollen - countryCode/postCode return");
            Log.d(TAG, "addPollen - countryCode: "+countryCode);
            Log.d(TAG, "addPollen - postCode: "+postCode);

            TextView textView = new TextView(context);
            textView.setGravity(Gravity.CENTER);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
            textView.setPadding(10, 10, 10, 10);
            textView.setText(getString(R.string.showPollenNoData));
            scrollViewLayout.addView(textView);
        } else {
            Resources res = context.getResources();
            for (Map<String, String> map : result.getPollenList()) {
                for (Map.Entry<String, String> entrySet : map.entrySet()) {
                    String icon = "ic_" + entrySet.getKey();
                    String value = entrySet.getValue();
                    int resID = res.getIdentifier(icon, "drawable", context.getPackageName());

                    if (resID == 0) {
                        continue;
                    }

                    Drawable herbDrawable = ContextCompat.getDrawable(context, resID);

                    int pollenKeyId = 0;
                    String pollenKey = "pollen_" + entrySet.getKey();
                    Log.d(TAG, "pollenkey: " + pollenKey);
                    Class resString = R.string.class;
                    Field field;
                    try {
                        field = resString.getField(pollenKey);
                        pollenKeyId = field.getInt(null);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "pollenKeyId: " + pollenKeyId);

                    if (herbDrawable != null && value != null) {
                        // the index may be "0-1", "1-2"
                        value = value.substring(value.length() - 1);
                        int index = Integer.parseInt(value);
                        index = Math.max(0, index);
                        index = Math.min(6, index);
                        int col = res.getColor(colorResId[index]);
                        herbDrawable.setColorFilter(col, PorterDuff.Mode.SRC_ATOP);
                        ImageView herb = new ImageView(context);
                        herb.setImageDrawable(herbDrawable);

                        PollenForecastLayout pollenLayout = new PollenForecastLayout(context);
                        pollenLayout.setImage(herbDrawable);
                        pollenLayout.setPollenText(context.getString(pollenKeyId));
                        pollenLayout.setPollenStressLevel(index);

                        scrollViewLayout.addView(pollenLayout);
                    }
                }
            }
        }
    }
}
