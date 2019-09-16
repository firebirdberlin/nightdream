package com.firebirdberlin.nightdream.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.net.Uri;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;


public class CustomDigitalClockPreferencesLayout extends LinearLayout {

    private OnConfigChangedListener mListener = null;
    private Settings settings = null;
    private boolean isPurchased = false;

    public CustomDigitalClockPreferencesLayout(Context context, Settings settings) {
        super(context);
        this.settings = settings;
        init(context);
    }

    public CustomDigitalClockPreferencesLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View child = inflater.inflate(R.layout.custom_digital_clock_preferences_layout, null);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
//        child.setBackgroundResource(R.drawable.border);
        addView(child, lp);

        Switch switchShowDivider = child.findViewById(R.id.switch_show_divider);
        switchShowDivider.setChecked(settings.showDivider);
        switchShowDivider.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                settings.setShowDivider(isChecked);
                if (mListener != null) {
                    mListener.onConfigChanged();
                }
            }
        });

        SeekBar glowRadius = child.findViewById(R.id.glowRadius);
        glowRadius.setProgress(settings.glowRadius);
        glowRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                settings.glowRadius = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                settings.setGlowRadius(progress);
                if (mListener != null) {
                    mListener.onConfigChanged();
                }
            }
        });
        TextView fontButton = (TextView) child.findViewById(R.id.typeface_preference);
        String fontButtonText = fontButton.getText().toString();
        fontButtonText = String.format("%s: %s", fontButtonText, settings.fontName);
        fontButton.setText(fontButtonText);
        fontButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fm = ((Activity) getContext()).getFragmentManager();
                ManageFontsDialogFragment dialog = new ManageFontsDialogFragment();
                dialog.setIsPurchased(isPurchased);
                dialog.setSelectedUri(settings.fontUri);
                dialog.setDefaultFonts("roboto_regular.ttf", "roboto_light.ttf",
                        "roboto_thin.ttf", "7_segment_digital.ttf",
                        "dancingscript_regular.ttf");
                dialog.setOnFontSelectedListener(new ManageFontsDialogFragment.ManageFontsDialogListener() {
                    @Override
                    public void onFontSelected(Uri uri, String name) {
                        settings.setFontUri(uri.toString(), name);
                        if (mListener != null) {
                            mListener.onConfigChanged();
                        }
                    }

                    @Override
                    public void onPurchaseRequested() {
                        if (mListener != null) {
                            mListener.onPurchaseRequested();
                        }
                    }
                });
                dialog.show(fm, "custom fonts");
            }
        });
    }

    public void setIsPurchased(boolean isPurchased) {
        this.isPurchased = isPurchased;
    }

    public void setOnConfigChangedListener(OnConfigChangedListener listener) {
        this.mListener = listener;
    }

    public interface OnConfigChangedListener {
        void onConfigChanged();

        void onPurchaseRequested();
    }
}
