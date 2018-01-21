package com.firebirdberlin.nightdream.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
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

public class WebRadioStationButtonsLayout extends LinearLayout {

    public static String TAG = "WebRadioStationButtons";
    private static int NUM_BUTTONS = 5;
    ColorFilter defaultColorFilter;
    ColorFilter accentColorFilter;
    FavoriteRadioStations stations;
    private Context context;
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
        stations = settings.getFavoriteRadioStations();

        for (int i = 0; i < NUM_BUTTONS; i++) {

            Button btn = new Button(context);
            btn.setText(String.valueOf(i + 1));
            btn.setTag(i);

            int widthDP = Utility.pixelsToDp(context, 40);
            int heightDP = Utility.pixelsToDp(context, 30);
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
        }
    }

    private void updateButtonColors() {
        for (int i = 0; i < getChildCount(); i++) {
            Button b = (Button) getChildAt(i);
            int tag = (int) b.getTag();
            int color = (activeStationIndex != null && activeStationIndex.intValue() == tag)
                    ? accentColor : textColor;

            Drawable border = b.getBackground();
            if (stations != null && stations.get(i) == null) {
                border.setAlpha(125);
                color = setAlpha(color, 125);
            } else {
                border.setAlpha(255);
                color = setAlpha(color, 255);
            }
            b.setTextColor(color);
            border.setColorFilter((color == accentColor) ? accentColorFilter : defaultColorFilter);
        }
    }

    public int setAlpha(int color, int alpha) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    private void startRadioStreamOrShowDialog(final int stationIndex) {
        RadioStation station = null;
        if (stations != null) {
            station = stations.get(stationIndex);
            //Log.i(TAG, "found stations");
        }
        if (station != null) {
            //start radio stream
            //Todo add active radio station as parameter
            toggleRadioStreamState(stationIndex, true);

        } else {
            showRadioStreamDialog(stationIndex);
        }
    }

    public void toggleRadioStreamState(final int radioStationIndex, boolean restart) {
        boolean wasAlreadyPlaying = false;
        if (RadioStreamService.streamingMode == RadioStreamService.StreamingMode.RADIO) {
            RadioStreamService.stop(context);
            wasAlreadyPlaying = true;
            //todo: improve switching station (restart stream without restarting the service?)
            if (!restart) {
                return;
            }
        }

        if (Utility.hasNetworkConnection(context)) {
            // is stream was already playing before, dont ask again? (but what if user switched from wifi to 3g since stream start?)
            if (Utility.hasFastNetworkConnection(context) || wasAlreadyPlaying) {
                RadioStreamService.startStream(context, radioStationIndex);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                new AlertDialog.Builder(context, R.style.DialogTheme)
                        .setTitle(R.string.message_mobile_data_connection)
                        .setMessage(R.string.message_mobile_data_connection_confirmation)
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(R.drawable.ic_attention)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                RadioStreamService.startStream(context, radioStationIndex);
                                hideSystemUI();
                            }
                        })
                        .show();
            }

        } else { // no network connection
            Toast.makeText(context, R.string.message_no_data_connection, Toast.LENGTH_LONG).show();
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
                // update station in settings
                settings.persistFavoriteRadioStation(station, stationIndex);
                stations = settings.getFavoriteRadioStations();

                hideSystemUI();

                //setActiveStation(stationIndex);
                toggleRadioStreamState(stationIndex, true);
            }

            @Override
            public void onCancel() {
                hideSystemUI();
            }

            @Override
            public void onDelete(int stationIndex) {
                Log.i(TAG, "delete");
                if (activeStationIndex != null && stationIndex == activeStationIndex) {
                    stopRadioStream();
                }
                settings.deleteFavoriteRadioStation(stationIndex);
                stations = settings.getFavoriteRadioStations();
                updateButtonColors();

                hideSystemUI();

            }
        };

        RadioStation station = stations.get(stationIndex);
        RadioStreamDialogFragment.showDialog((Activity)getContext(), stationIndex, station, listener);
    }

    private void hideSystemUI() {
        NightDreamActivity nightDreamActivity = (NightDreamActivity) getContext();
        nightDreamActivity.hideSystemUI();
    }

    public void setActiveStation(int stationIndex) {
        activeStationIndex = stationIndex > -1 ? stationIndex : null;
        updateButtonColors();
    }

    public void clearActiveStation() {
        activeStationIndex = null;
        updateButtonColors();
    }
}
