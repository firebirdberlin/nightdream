package com.firebirdberlin.nightdream.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.services.RadioStreamService;
import com.firebirdberlin.radiostreamapi.models.FavoriteRadioStations;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

import java.util.ArrayList;
import java.util.List;

public class WebRadioStationButtonsLayout extends LinearLayout {

    public static String TAG ="WebRadioStationButtonsLayout";
    private static int NUM_BUTTONS = 5;
    ColorFilter defaultColorFilter;
    ColorFilter accentColorFilter;
    private Context context;

    private List<Button> stationSelectButtons;
    private Settings settings;
    private Integer activeStationIndex;
    private int accentColor;
    private int textColor;

    public WebRadioStationButtonsLayout(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public WebRadioStationButtonsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public void setCustomColor(int accentColor, int textColor) {

        this.accentColor = accentColor;
        this.textColor = textColor;

        accentColorFilter = new LightingColorFilter(accentColor, 1);
        defaultColorFilter = new LightingColorFilter(textColor, 1);

        updateButtonColors();
    }

    private void init() {

        settings = new Settings(context);

        stationSelectButtons = new ArrayList<>();

        for (int i = 0; i < NUM_BUTTONS; i++) {

            Button btn = new Button(context);
            btn.setText(String.valueOf(i + 1));
            btn.setTag(i);

            int widthDP = Utility.pixelsToDp(context, 40);
            int heightDP = Utility.pixelsToDp(context, 35);
            int margin = Utility.pixelsToDp(context, 5);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(widthDP, heightDP);
            lp.setMargins(margin, 2, margin, margin);
            btn.setLayoutParams(lp);
            btn.setPadding(0, 0, 0, 0);
            btn.setBackgroundResource(R.drawable.webradio_station_button);
            btn.setTextSize(16);

            btn.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(final View v) {
                    int stationIndex = (int) v.getTag();
                    showRadioStreamDialog(stationIndex);
                    return true;
                }
            });

            btn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(final View v) {
                    int index = (int) v.getTag();
                    if (activeStationIndex != null && index == activeStationIndex.intValue()) {
                        stopRadioStream();
                    } else {
                        startRadioStreamOrShowDialog(index);
                    }

                }
            });

            addView(btn);

            stationSelectButtons.add(btn);
        }


    }

    private void updateButtonColors() {
        if (stationSelectButtons == null) return;

        for (Button b : stationSelectButtons) {
            int tag = (int) b.getTag();
            int color = (activeStationIndex != null && activeStationIndex.intValue() == tag)
                    ? accentColor : textColor;
            b.setTextColor(color);
            Drawable border = b.getBackground();
            border.setColorFilter((color == accentColor) ? accentColorFilter : defaultColorFilter);
        }
    }

    private void startRadioStreamOrShowDialog(final int stationIndex) {

        FavoriteRadioStations stations = settings.getFavoriteRadioStations();
        RadioStation station = null;
        if (stations != null) {
            station = stations.get(stationIndex);
            //Log.i(TAG, "found stations");
        }
        if (station != null) {
            //start radio stream
            //Todo add active radio station as parameter
            final NightDreamActivity nightDreamActivity = (NightDreamActivity) getContext();
            nightDreamActivity.toggleRadioStreamState(stationIndex, true);

        } else {
            showRadioStreamDialog(stationIndex);
        }
    }

    private void stopRadioStream() {
        if ( RadioStreamService.streamingMode == RadioStreamService.StreamingMode.RADIO ) {
            RadioStreamService.stop(context);
        }
    }

    private void showRadioStreamDialog(final int stationIndex) {
        RadioStreamDialogListener listener = new RadioStreamDialogListener() {
            @Override
            public void onRadioStreamSelected(RadioStation station) {
                Toast.makeText(getContext(), "Saved radio station #" + stationIndex + ": " + station.name, Toast.LENGTH_LONG).show();

                // update station in settings
                /*
                FavoriteRadioStations stations = settings.getFavoriteRadioStations();
                stations.set(stationIndex, station);
                settings.setFavoriteRadioStations(stations);
                */
                settings.setPersistentFavoriteRadioStation(station, stationIndex);

                NightDreamActivity nightDreamActivity = (NightDreamActivity) getContext();
                nightDreamActivity.hideSystemUI();

                setActiveStation(stationIndex);

                /*
                try {
                    Log.i(TAG, "saved stations: " + stations.toJson());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                */

            }

            @Override
            public void onCancel() {
                NightDreamActivity nightDreamActivity = (NightDreamActivity) getContext();
                nightDreamActivity.hideSystemUI();
            }
        };

        FavoriteRadioStations stations = settings.getFavoriteRadioStations();
        RadioStation station = stations.get(stationIndex);
        RadioStreamDialogFragment.showDialog((Activity)getContext(), stationIndex, station, listener);
    }

    public void setActiveStation(int stationIndex) {
        activeStationIndex = stationIndex > -1 ? stationIndex : null;
        //highlight active button
        updateButtonColors();
    }

    public void clearActiveStation() {
        activeStationIndex = null;
        //highlight active button
        updateButtonColors();
    }
}