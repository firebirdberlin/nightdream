package com.firebirdberlin.nightdream.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebirdberlin.nightdream.NightDreamActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.receivers.RadioStreamSleepTimeReceiver;
import com.firebirdberlin.nightdream.services.RadioStreamService;
import com.firebirdberlin.radiostreamapi.models.FavoriteRadioStations;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class WebRadioLayout extends RelativeLayout {

    public static String TAG ="WebRadioLayout";

    public boolean locked = false;
    private Context context;
    private TextView textView;
    private ImageView buttonSleepTimer;
    private boolean showConnectingHint = false;
    private ProgressBar spinner;
    private List<Button> stationSelectButtons;
    private Settings settings;

    public WebRadioLayout(Context context) {
        super(context);
        this.context = context;
    }

    public WebRadioLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        setBackgroundResource(R.drawable.webradiopanelborder);

        settings = new Settings(context);

        textView = new TextView(context);
        textView.setId(R.id.web_radio_text_view); // id for placing spinner LEFT_OF this view
        textView.setEllipsize(TextUtils.TruncateAt.END);
        int padding = Utility.dpToPx(context, 6.f);
        textView.setPadding(padding, padding, padding, padding);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

        buttonSleepTimer = new ImageView(context);
        buttonSleepTimer.setImageResource(R.drawable.ic_nightmode);

        padding = Utility.dpToPx(context, 6.f);
        buttonSleepTimer.setPadding(padding, padding, padding, padding);
        buttonSleepTimer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fm = ((Activity) getContext()).getFragmentManager();
                SleepTimerDialogFragment dialog = new SleepTimerDialogFragment();
                dialog.show(fm, "sleep_timer");
            }
        });
        RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        lp2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        //lp2.addRule(RelativeLayout.CENTER_IN_PARENT);

        spinner = new ProgressBar(context, null, android.R.attr.progressBarStyleSmall);
        spinner.setPadding(0, padding, 0, padding);
        spinner.setVisibility(View.VISIBLE);

        RelativeLayout.LayoutParams lp3 = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp3.addRule(RelativeLayout.LEFT_OF, textView.getId());
        lp3.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        addView(textView, lp);
        addView(buttonSleepTimer, lp2);
        addView(spinner, lp3);
        addStationButtons();
    }

    private void addStationButtons() {

        // station preset buttons
        LinearLayout buttonContainer = new LinearLayout(context);
        //buttonContainer.setBackgroundResource(R.drawable.border);
        //buttonContainer.setPadding(0, 5, 0, 0);
        RelativeLayout.LayoutParams lp4 = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp4.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        lp4.addRule(RelativeLayout.CENTER_IN_PARENT);

        stationSelectButtons = new ArrayList<>();

        for (int i = 0; i < 5; i++) {

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
                    startRadioStreamOrShowDialog(stationIndex);
                }
            });

            buttonContainer.addView(btn);

            stationSelectButtons.add(btn);
        }

        addView(buttonContainer, lp4);
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

    public void setCustomColor(int accentColor, int textColor) {
        Drawable bg = getBackground();
        bg.setColorFilter( accentColor, PorterDuff.Mode.MULTIPLY );

        buttonSleepTimer.setColorFilter(
                RadioStreamSleepTimeReceiver.isSleepTimeSet() ? accentColor : textColor,
                PorterDuff.Mode.SRC_ATOP
        );
        textView.setTextColor(textColor);

        if (stationSelectButtons != null) {
            for (Button b : stationSelectButtons) {
                b.setTextColor(textColor);
                GradientDrawable drawable = (GradientDrawable) b.getBackground();
                drawable.setStroke(1, textColor);
            }
        }
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    protected void setText(Bundle extras) {

        //Log.i(TAG, "setText extras=" + (extras != null ? "set" : "null"));

        if (textView == null) return;
        if (RadioStreamService.streamingMode == RadioStreamService.StreamingMode.RADIO) {

            RadioStation station;
            // extras must be provided to update the station name
            if (extras != null) {
                int radioStationIndex = extras.getInt(RadioStreamService.EXTRA_RADIO_STATION_INDEX, 0);
                //Log.i(TAG, "extras != null, index=" + radioStationIndex);
                station = settings.getFavoriteRadioStation(radioStationIndex);
                textView.setText(station.name);
            }
            /*
            else {
                //Log.i(TAG, "extras = null");
                station = RadioStreamService.getCurrentRadioStation(context);
                textView.setText(station.name);
            }
            */

        } else {
            textView.setText("");
        }
        if (spinner != null) {
            spinner.setVisibility(showConnectingHint ? View.VISIBLE : View.GONE);
        }
    }

    protected void setShowConnectingHint(boolean showConnectingHint) {
        this.showConnectingHint = showConnectingHint;
        spinner.setVisibility(showConnectingHint ? View.VISIBLE : View.GONE);
        invalidate();
        //Log.i(TAG, "setShowConnectingHint " + showConnectingHint);
    }

    @Override
    public void setClickable(boolean clickable) {
        super.setClickable(clickable);
        buttonSleepTimer.setClickable(clickable);
    }
}
