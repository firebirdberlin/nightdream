package com.firebirdberlin.nightdream.ui;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

public class ClockLayoutPreviewPreference extends Preference {
    private static PreviewMode previewMode = PreviewMode.DAY;
    private ClockLayout clockLayout = null;
    private TextView textViewPurchaseHint = null;
    private View preferenceView = null;
    private Context context = null;
    public ClockLayoutPreviewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public ClockLayoutPreviewPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    public static void setPreviewMode(PreviewMode previewMode) {
        ClockLayoutPreviewPreference.previewMode = previewMode;
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        preferenceView = super.onCreateView(parent);

        View summary = preferenceView.findViewById(android.R.id.summary);
        if (summary != null) {
            ViewParent summaryParent = summary.getParent();
            if (summaryParent instanceof ViewGroup) {
                final LayoutInflater layoutInflater =
                    (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                ViewGroup summaryParent2 = (ViewGroup) summaryParent;
                layoutInflater.inflate(R.layout.clock_layout_preference, summaryParent2, true);

                RelativeLayout previewContainer = (RelativeLayout) summaryParent2.findViewById(R.id.previewContainer);
                clockLayout = (ClockLayout) summaryParent2.findViewById(R.id.clockLayout);
                textViewPurchaseHint = (TextView) summaryParent2.findViewById(R.id.textViewPurchaseHint);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    LayoutTransition lt = new LayoutTransition();
                    lt.disableTransitionType(LayoutTransition.CHANGING);
                    previewContainer.setLayoutTransition(lt);
                }
            }
        }

        return preferenceView;
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        updateView();
    }

    protected void updateView() {
        Settings settings = new Settings(getContext());
        textViewPurchaseHint.setVisibility(showPurchaseHint(settings) ? View.VISIBLE : View.GONE);
        clockLayout.setBackgroundColor(Color.TRANSPARENT);
        clockLayout.setLayout(settings.getClockLayoutID(true));
        clockLayout.setTypeface(settings.typeface);
        clockLayout.setPrimaryColor(previewMode == PreviewMode.DAY ? settings.clockColor : settings.clockColorNight);
        clockLayout.setSecondaryColor(previewMode == PreviewMode.DAY ? settings.secondaryColor : settings.secondaryColorNight);

        clockLayout.setDateFormat(settings.dateFormat);
        clockLayout.setTimeFormat(settings.timeFormat12h, settings.timeFormat24h);
        clockLayout.showDate(settings.showDate);

        clockLayout.setTemperature(settings.showTemperature, settings.temperatureUnit);
        clockLayout.setWindSpeed(settings.showWindSpeed, settings.speedUnit);
        clockLayout.showWeather(settings.showWeather);

        WeatherEntry entry = getWeatherEntry(settings);
        clockLayout.update(entry);

        Utility utility = new Utility(getContext());
        Point size = utility.getDisplaySize();
        Configuration config = context.getResources().getConfiguration();
        clockLayout.updateLayout(size.x - preferenceView.getPaddingLeft()
                                        - preferenceView.getPaddingRight(),
                                 config);

        clockLayout.requestLayout();
        clockLayout.invalidate();
    }

    private WeatherEntry getWeatherEntry(Settings settings) {
        WeatherEntry entry = settings.weatherEntry;
        if ( entry.timestamp ==  -1L) {
            entry.setFakeData();
        }
        return entry;
    }

    private boolean showPurchaseHint(Settings settings) {
        return (!settings.purchasedWeatherData && settings.getClockLayoutID(true) > 1);
    }

    public enum PreviewMode {DAY, NIGHT}
}
