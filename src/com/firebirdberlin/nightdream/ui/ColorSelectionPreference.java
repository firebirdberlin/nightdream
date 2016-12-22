package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.ui.ColorPrefWidgetView;

public class ColorSelectionPreference extends Preference
                                      implements View.OnClickListener {
    //private LinearLayout colorSelectionLayout = null;
    private ColorPrefWidgetView primaryColorView = null;
    private ColorPrefWidgetView secondaryColorView = null;
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

                primaryColorView.setOnClickListener(this);
                secondaryColorView.setOnClickListener(this);
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
        //colorSelectionLayout.invalidate();

    }

    @Override
    public void onClick(View v) {
        Log.d("NightDream", "click");
    }
}
