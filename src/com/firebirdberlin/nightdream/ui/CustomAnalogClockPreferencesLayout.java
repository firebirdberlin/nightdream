package com.firebirdberlin.nightdream.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.models.AnalogClockConfig;

public class CustomAnalogClockPreferencesLayout extends LinearLayout {

    private OnConfigChangedListener mListener = null;
    private boolean isPurchased = false;

    public CustomAnalogClockPreferencesLayout(Context context, AnalogClockConfig.Style preset) {
        super(context);
        init(context, preset);
    }

    private void init(Context context, AnalogClockConfig.Style preset) {
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View child = inflater.inflate(R.layout.custom_analog_clock_preferences_layout, null);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(child, lp);

        final AnalogClockConfig config = new AnalogClockConfig(getContext(), preset);

        TextView fontButton = (TextView) child.findViewById(R.id.typeface_preference);
        String fontButtonText = fontButton.getText().toString();
        String[] segments = config.fontUri.split("/");
        if (segments.length > 0) {
            String name =
                    ManageFontsDialogFragment.getUserFriendlyFileName(segments[segments.length - 1]);

            fontButtonText = String.format("%s: %s", fontButtonText, name);
        }
        fontButton.setText(fontButtonText);

        fontButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fm = ((Activity) getContext()).getFragmentManager();
                ManageFontsDialogFragment dialog = new ManageFontsDialogFragment();
                dialog.setIsPurchased(isPurchased);
                dialog.setSelectedUri(config.fontUri);
                dialog.setDefaultFonts("roboto_regular.ttf", "roboto_light.ttf",
                        "roboto_thin.ttf", "dancingscript_regular.ttf");
                dialog.setOnFontSelectedListener(new ManageFontsDialogFragment.ManageFontsDialogListener() {
                    @Override
                    public void onFontSelected(Uri uri, String name) {
                        config.fontUri = uri.toString();
                        config.save();
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

        TextView digitStylePreference = (TextView) child.findViewById(R.id.digit_style_preference);
        {
            final String[] values = getResources().getStringArray(R.array.numberStyles);
            String text = digitStylePreference.getText().toString();
            text = String.format("%s: %s", text, values[config.digitStyle.getValue()]);
            digitStylePreference.setText(text);
        }
        digitStylePreference.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.numberStyle)
                        .setItems(R.array.numberStyles, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                config.digitStyle = AnalogClockConfig.DigitStyle.fromValue(which);
                                configHasChanged(config);
                                // The 'which' argument contains the index position
                                // of the selected item
                            }
                        });
                builder.show();
            }
        });

        TextView decorationStylePreference = (TextView) child.findViewById(R.id.decoration_preference);
        {
            final String[] values = getResources().getStringArray(R.array.decorationStyles);
            String text = decorationStylePreference.getText().toString();
            text = String.format("%s: %s", text, values[config.decoration.getValue()]);
            decorationStylePreference.setText(text);
        }
        decorationStylePreference.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.decoration_style)
                        .setItems(R.array.decorationStyles, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                config.decoration = AnalogClockConfig.Decoration.fromValue(which);
                                configHasChanged(config);
                                // The 'which' argument contains the index position
                                // of the selected item
                            }
                        });
                builder.show();
            }
        });
    }

    private void configHasChanged(AnalogClockConfig config) {
        config.save();
        if (mListener != null) {
            mListener.onConfigChanged();
        }
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
