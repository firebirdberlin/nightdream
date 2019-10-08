package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.services.RadioStreamService;
import com.firebirdberlin.radiostreamapi.models.FavoriteRadioStations;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

public class WebRadioStationButtonsLayout extends LinearLayout {

    public static String TAG = "WebRadioStationButtons";
    ColorFilter defaultColorFilter;
    ColorFilter accentColorFilter;
    FavoriteRadioStations stations;
    private Context context;
    private Settings settings;
    private Integer activeStationIndex;
    private int accentColor;
    private int textColor;
    private boolean showSmallButtons = false;
    private OnClickListener buttonOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            int index = (int) v.getTag();
            if (activeStationIndex != null && index == activeStationIndex) {
                stopRadioStream();
            } else {
                startRadioStreamOrShowDialog(index);
            }
        }
    };
    private OnLongClickListener buttonOnLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(final View v) {
            int stationIndex = (int) v.getTag();
            showRadioStreamDialog(stationIndex);
            return true;
        }
    };

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

        updateButtonState();
    }

    private void init() {

        settings = new Settings(context);
        stations = settings.getFavoriteRadioStations();

        Utility utility = new Utility(getContext());
        Point displaySize = utility.getDisplaySize();
        showSmallButtons = (displaySize.x <= 480);
        final int buttonWidthPixels = (showSmallButtons ? 35 : 40);

        final int maxNumButtons = FavoriteRadioStations.getMaxNumEntries();
        for (int i = 0; i < maxNumButtons; i++) {

            Button btn = new Button(context);
            btn.setText(String.valueOf(i + 1));
            btn.setTag(i);

            int widthDP = Utility.pixelsToDp(context, buttonWidthPixels);
            int heightDP = Utility.pixelsToDp(context, 30);
            int margin = Utility.pixelsToDp(context, 5);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(widthDP, heightDP);
            lp.setMargins(margin, 2, margin, margin);
            btn.setLayoutParams(lp);
            btn.setPadding(0, 0, 0, 0);
            btn.setBackgroundResource(R.drawable.webradio_station_button);
            btn.setTextSize(16);

            btn.setOnLongClickListener(buttonOnLongClickListener);
            btn.setOnClickListener(buttonOnClickListener);

            addView(btn);
        }
    }

    @Override
    public void setClickable(boolean clickable) {
        super.setClickable(clickable);
        for (int i = 0; i < getChildCount(); i++) {
            Button b = (Button) getChildAt(i);
            b.setClickable(clickable);
            b.setLongClickable(clickable);
        }
    }

    private void updateButtonState() {
        final int lastButtonInUseIndex = lastButtonInUseIndex();
        for (int i = 0; i < getChildCount(); i++) {
            Button b = (Button) getChildAt(i);
            b.setVisibility(i <= lastButtonInUseIndex + 1 ? VISIBLE : GONE);

            final boolean active = (activeStationIndex != null && activeStationIndex == i);
            int color = active ? accentColor : textColor;

            if (active) {
                // draw stop button: empty text, use shape containing additional "stop" shape
                b.setText("");
                b.setBackgroundResource(
                        showSmallButtons
                                ? R.drawable.webradio_station_stop_button_small
                                : R.drawable.webradio_station_stop_button
                );
            } else {
                b.setText(String.valueOf(i + 1));
                b.setBackgroundResource(R.drawable.webradio_station_button);
            }

            Drawable border = b.getBackground();
            if (stations != null && stations.get(i) == null) {
                border.setAlpha(125);
                color = setAlpha(color, 125);
                b.setText(i <= lastButtonInUseIndex ? String.valueOf(i + 1) : "+");
            } else {
                border.setAlpha(255);
                color = setAlpha(color, 255);
            }
            b.setTextColor(color);
            border.setColorFilter((color == accentColor) ? accentColorFilter : defaultColorFilter);
        }
    }

    private int lastButtonInUseIndex() {
        int lastIndex = -1;
        for (int i = 0; i < getChildCount(); i++) {
            if (stations != null && stations.get(i) != null) {
                lastIndex = i;
            }
        }
        return lastIndex;
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
        }
        if (station != null) {
            //start radio stream
            toggleRadioStreamState(stationIndex);

        } else {
            showRadioStreamDialog(stationIndex);
        }
    }

    private void toggleRadioStreamState(final int radioStationIndex) {
        boolean wasAlreadyPlaying = false;
        if (RadioStreamService.streamingMode == RadioStreamService.StreamingMode.RADIO) {
            RadioStreamService.stop(context);
            wasAlreadyPlaying = true;
        }

        if (Utility.hasNetworkConnection(context)) {
            // is stream was already playing before, don't ask again? (but what if user switched from wifi to 3g since stream start?)
            if (Utility.hasFastNetworkConnection(context) || wasAlreadyPlaying) {
                RadioStreamService.startStream(context, radioStationIndex);
            } else {
                new AlertDialog.Builder(context, R.style.DialogTheme)
                        .setTitle(R.string.message_mobile_data_connection)
                        .setMessage(R.string.message_mobile_data_connection_confirmation)
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                hideSystemUI();
                            }
                        })
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
                if (settings != null) {
                    settings.persistFavoriteRadioStation(station, stationIndex);
                    stations = settings.getFavoriteRadioStations();
                }
                toggleRadioStreamState(stationIndex);
                hideSystemUI();
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
                if (settings != null) {
                    settings.deleteFavoriteRadioStation(stationIndex);
                    stations = settings.getFavoriteRadioStations();
                }
                updateButtonState();
                hideSystemUI();

            }
        };

        RadioStation station = stations.get(stationIndex);
        String preferredCountry = null;
        if (station != null) {
            preferredCountry = station.countryCode;
        } else {
            for (int i = 0; i < stations.numAvailableStations(); i++) {
                RadioStation s = stations.get(i);
                preferredCountry = s.countryCode;
           }
        }
        RadioStreamDialogFragment.showDialog((AppCompatActivity)getContext(), stationIndex, station, preferredCountry, listener);
    }

    private void hideSystemUI() {
        Utility.hideSystemUI(getContext());
    }

    public void setActiveStation(int stationIndex) {
        activeStationIndex = stationIndex > -1 ? stationIndex : null;
        updateButtonState();
    }

    public void clearActiveStation() {
        activeStationIndex = null;
        updateButtonState();
    }
}
