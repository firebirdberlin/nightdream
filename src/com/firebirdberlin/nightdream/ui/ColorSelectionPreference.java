package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.ui.ColorPrefWidgetView;

import yuku.ambilwarna.AmbilWarnaDialog;

public class ColorSelectionPreference extends Preference
                                      implements View.OnClickListener {
    //private LinearLayout colorSelectionLayout = null;
    private ColorPrefWidgetView primaryColorView = null;
    private ColorPrefWidgetView secondaryColorView = null;
    private ColorPrefWidgetView primaryColorNightView = null;
    private ColorPrefWidgetView secondaryColorNightView = null;
    private View preferenceView = null;
    private Context context = null;

    public ColorSelectionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public ColorSelectionPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
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
                layoutInflater.inflate(R.layout.color_selection_layout, summaryParent2, true);

                //colorSelectionLayout = (LinearLayout) summaryParent2.findViewById(R.id.colorSelectionLayout);
                primaryColorView = (ColorPrefWidgetView) summaryParent2.findViewById(R.id.primaryColor);
                secondaryColorView = (ColorPrefWidgetView) summaryParent2.findViewById(R.id.secondaryColor);

                primaryColorNightView = (ColorPrefWidgetView) summaryParent2.findViewById(R.id.primaryColorNight);
                secondaryColorNightView = (ColorPrefWidgetView) summaryParent2.findViewById(R.id.secondaryColorNight);

                primaryColorView.setOnClickListener(this);
                secondaryColorView.setOnClickListener(this);
                primaryColorNightView.setOnClickListener(this);
                secondaryColorNightView.setOnClickListener(this);
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
        primaryColorView.setColor(settings.clockColor);
        secondaryColorView.setColor(settings.secondaryColor);
        primaryColorNightView.setColor(settings.clockColorNight);
        secondaryColorNightView.setColor(settings.secondaryColorNight);
        //colorSelectionLayout.invalidate();

    }

    @Override
    public void onClick(View v) {
        Log.d("NightDream", "click");
        final ColorPrefWidgetView view = (ColorPrefWidgetView) v;
        int color = view.getColor();

        new AmbilWarnaDialog(getContext(), color, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override public void onOk(AmbilWarnaDialog dialog, int color) {
                //if (!callChangeListener(color)) return; // They don't want the value to be set
                //value = color;
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
                //persistInt(value);
                //notifyChanged();
            }

            @Override public void onCancel(AmbilWarnaDialog dialog) {
                // nothing to do
            }
        }).show();
    }

    public void putInt(String key, int value) {
        SharedPreferences settings = context.getSharedPreferences(Settings.PREFS_KEY, 0);
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putInt(key, value);
        prefEditor.commit();
    }

}
