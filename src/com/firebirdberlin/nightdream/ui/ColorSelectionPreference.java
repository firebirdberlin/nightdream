package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;

import yuku.ambilwarna.AmbilWarnaDialog;

public class ColorSelectionPreference extends Preference
                                      implements View.OnClickListener {
    private ColorPrefWidgetView primaryColorView = null;
    private ColorPrefWidgetView secondaryColorView = null;
    private ColorPrefWidgetView primaryColorNightView = null;
    private ColorPrefWidgetView secondaryColorNightView = null;
    private Context context;

    public ColorSelectionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public ColorSelectionPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View preferenceView = holder.itemView;

        View summary = preferenceView.findViewById(android.R.id.summary);
        if (summary != null) {
            ViewParent summaryParent = summary.getParent();
            if (summaryParent instanceof ViewGroup) {
                final LayoutInflater layoutInflater =
                    (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                ViewGroup summaryParent2 = (ViewGroup) summaryParent;
                View customView = summaryParent2.findViewWithTag("custom");
                if (customView == null) {
                    layoutInflater.inflate(R.layout.color_selection_layout, summaryParent2, true);
                }

                primaryColorView = summaryParent2.findViewById(R.id.primaryColor);
                secondaryColorView = summaryParent2.findViewById(R.id.secondaryColor);

                primaryColorNightView = summaryParent2.findViewById(R.id.primaryColorNight);
                secondaryColorNightView = summaryParent2.findViewById(R.id.secondaryColorNight);

                primaryColorView.setOnClickListener(this);
                secondaryColorView.setOnClickListener(this);
                primaryColorNightView.setOnClickListener(this);
                secondaryColorNightView.setOnClickListener(this);
                View iconDay = summaryParent2.findViewById(R.id.iconDay);
                View iconNight = summaryParent2.findViewById(R.id.iconNight);
                iconDay.setOnClickListener(this);
                iconNight.setOnClickListener(this);
            }
        }

        updateView();
    }

    protected void updateView() {
        Settings settings = new Settings(getContext());
        primaryColorView.setColor(settings.clockColor);
        secondaryColorView.setColor(settings.secondaryColor);
        primaryColorNightView.setColor(settings.clockColorNight);
        secondaryColorNightView.setColor(settings.secondaryColorNight);
    }

    @Override
    public void onClick(View v) {
        Log.d("NightDream", "click");

        if (R.id.iconDay == v.getId()) {
            Log.d(getClass().getSimpleName(), "day click");
            ClockLayoutPreviewPreference.setPreviewMode(ClockLayoutPreviewPreference.PreviewMode.DAY);

            putInt("colorPreviewMode", 0);
            notifyChanged();
            return;
        } else if (R.id.iconNight == v.getId()) {
            ClockLayoutPreviewPreference.setPreviewMode(ClockLayoutPreviewPreference.PreviewMode.NIGHT);
            putInt("colorPreviewMode", 1);
            notifyChanged();
            return;
        }

        if (! (v instanceof ColorPrefWidgetView)) {
            return;
        }
        final ColorPrefWidgetView view = (ColorPrefWidgetView) v;
        int color = view.getColor();

        AmbilWarnaDialog dialog = new AmbilWarnaDialog(getContext(), color, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override public void onOk(AmbilWarnaDialog dialog, int color) {
                view.setColor(color);
                view.invalidate();
                if (view.equals(primaryColorView)) {
                    putInt("clockColor", color);
                } else
                if (view.equals(primaryColorNightView)) {
                    putInt("primaryColorNight", color);
                } else
                if (view.equals(secondaryColorView)) {
                    putInt("secondaryColor", color);
                } else
                if (view.equals(secondaryColorNightView)) {
                    putInt("secondaryColorNight", color);
                }
                notifyChanged();
            }

            @Override public void onCancel(AmbilWarnaDialog dialog) {
                // nothing to do
            }
        });

        dialog.setQuickColor1(primaryColorView.getColor());
        dialog.setQuickColor2(secondaryColorView.getColor());
        dialog.setQuickColor3(primaryColorNightView.getColor());
        dialog.setQuickColor4(secondaryColorNightView.getColor());
        dialog.show();
    }

    public void putInt(String key, int value) {
        SharedPreferences settings = context.getSharedPreferences(Settings.PREFS_KEY, 0);
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putInt(key, value);
        prefEditor.apply();
    }

}
