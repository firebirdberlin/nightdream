package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;


public class CustomDigitalClockPreferencesLayout extends LinearLayout {

    private OnConfigChangedListener mListener = null;
    private Settings settings = null;
    private boolean isPurchased = false;
    AppCompatActivity activity = null;

    public CustomDigitalClockPreferencesLayout(
            Context context, Settings settings, AppCompatActivity activity
    ) {
        super(context);
        this.settings = settings;
        this.activity = activity;
        init(context);
    }

    public CustomDigitalClockPreferencesLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(final Context context) {

        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View child = inflater.inflate(R.layout.custom_digital_clock_preferences_layout, null);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
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
        glowRadius.setProgress(settings.getGlowRadius(ClockLayout.LAYOUT_ID_DIGITAL));
        glowRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                settings.setGlowRadius(progress, ClockLayout.LAYOUT_ID_DIGITAL);
                if (mListener != null) {
                    mListener.onConfigChanged();
                }
            }
        });
        TextView fontButton = child.findViewById(R.id.typeface_preference);
        String fontButtonText = fontButton.getText().toString();
        fontButtonText = String.format(
                "%s: %s", fontButtonText, settings.getFontName(ClockLayout.LAYOUT_ID_DIGITAL)
        );
        fontButton.setText(fontButtonText);
        fontButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (activity == null) {
                    return;
                }

                FragmentManager fm = activity.getSupportFragmentManager();
                ManageFontsDialogFragment dialog = new ManageFontsDialogFragment();
                dialog.setIsPurchased(isPurchased);
                dialog.setSelectedUri(settings.getFontUri(ClockLayout.LAYOUT_ID_DIGITAL));
                dialog.setDefaultFonts("roboto_regular.ttf", "roboto_light.ttf",
                        "roboto_thin.ttf", "7_segment_digital.ttf",
                        "dancingscript_regular.ttf");
                dialog.setOnFontSelectedListener(new ManageFontsDialogFragment.ManageFontsDialogListener() {
                    @Override
                    public void onFontSelected(Uri uri, String name) {
                        settings.setFontUri(uri.toString(), name, ClockLayout.LAYOUT_ID_DIGITAL);
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

        final TextView decorationStylePreference = child.findViewById(R.id.decoration_preference);

        String[] textures = context.getResources().getStringArray(R.array.textures);
        String title = context.getString(R.string.decoration);
        decorationStylePreference.setText(
                String.format("%s: %s",
                        title,
                        textures[settings.getTextureId(ClockLayout.LAYOUT_ID_DIGITAL)]
                )
        );
        decorationStylePreference.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.decoration)
                        .setItems(R.array.textures, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                settings.setTextureId(which, ClockLayout.LAYOUT_ID_DIGITAL);
                                if (mListener != null) {
                                    mListener.onConfigChanged();
                                }
                            }
                        });
                builder.show();
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
