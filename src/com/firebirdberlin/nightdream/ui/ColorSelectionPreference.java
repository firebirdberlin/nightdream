package com.firebirdberlin.nightdream.ui;

import android.app.AlertDialog;
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
import com.rarepebble.colorpicker.ColorPickerView;

public class ColorSelectionPreference extends Preference
        implements View.OnClickListener {
    private final Context context;
    private ColorPrefWidgetView primaryColorView = null;
    private ColorPrefWidgetView secondaryColorView = null;
    private ColorPrefWidgetView primaryColorNightView = null;
    private ColorPrefWidgetView secondaryColorNightView = null;

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

        showDialog(v);
    }

    private void showDialog(View v) {
        if (v instanceof ColorPrefWidgetView) {
            ColorPrefWidgetView button = (ColorPrefWidgetView) v;

            final ColorPickerView picker = new ColorPickerView(getContext());
            picker.showHex(false);
            picker.showAlpha(false);
            picker.showPreview(true);

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(null).setView(picker);

            picker.setColor(button.getColor());
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                final int color = picker.getColor();
                if (callChangeListener(color)) {
                    putInt(button.getTag().toString(), color);
                    notifyChanged();
                }
            });

            builder.show();
        }
    }

    public void putInt(String key, int value) {
        SharedPreferences settings = context.getSharedPreferences(Settings.PREFS_KEY, 0);
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putInt(key, value);
        prefEditor.apply();
    }

}
