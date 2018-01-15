package com.firebirdberlin.nightdream.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.receivers.RadioStreamSleepTimeReceiver;
import com.firebirdberlin.nightdream.services.RadioStreamService;
import com.firebirdberlin.radiostreamapi.models.FavoriteRadioStations;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

import java.util.ArrayList;
import java.util.List;

public class WebRadioStationButtonsLayout extends LinearLayout {

    public static String TAG ="WebRadioStationButtonsLayout";

    private static int NUM_BUTTONS = 5;
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

        updateButtonColors();
    }

    private void init() {

        settings = new Settings(context);

        stationSelectButtons = new ArrayList<>();

        for (int i = 0; i < NUM_BUTTONS; i++) {

            Button btn = new Button(context);
            btn.setText(String.valueOf(i + 1));
            //btn1.setTextColor(Color.WHITE);

            int widthDP = Utility.pixelsToDp(context, 25);
            int heightDP = Utility.pixelsToDp(context, 20);
            int margin = Utility.pixelsToDp(context, 5);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(widthDP, heightDP);
            lp.setMargins(margin, margin, margin, margin);
            btn.setLayoutParams(lp);
            btn.setPadding(0, 0, 0, 0);
            btn.setBackgroundResource(R.drawable.border);
            btn.setTextSize(10);
            final int stationIndex = i;
            btn.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(final View v) {
                    showRadioStreamDialog(stationIndex);
                    return true;
                }
            });

            btn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(final View v) {

                    int i = getButtonIndex(v);
                    //Log.i(TAG, "pressed button: " + i + " active=" + (activeStationIndex != null ? activeStationIndex.intValue() : "null")) ;
                    if (activeStationIndex != null && i == activeStationIndex.intValue()) {
                        stopRadioStream();
                    } else {
                        startRadioStreamOrShowDialog(stationIndex);
                    }

                }
            });

            addView(btn);

            stationSelectButtons.add(btn);
        }


    }

    private void updateButtonColors() {
        if (stationSelectButtons != null) {
            int i = 0;
            for (Button b : stationSelectButtons) {

                int color;
                int stroke;
                if (activeStationIndex != null && activeStationIndex.intValue() == i) {
                    color = accentColor;
                    stroke = 2;
                } else {
                    color = textColor;
                    stroke = 1;
                }

                b.setTextColor(color);
                GradientDrawable drawable = (GradientDrawable) b.getBackground();
                drawable.setStroke(stroke, color);
                i++;
            }
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

    private int getButtonIndex(View button) {

        if (stationSelectButtons != null) {
            return stationSelectButtons.indexOf(button);
        } else {
            return -1;
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
