package com.firebirdberlin.nightdream.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.models.AnalogClockConfig;

public class CustomAnalogClockPreferencesLayout extends LinearLayout {

    private OnConfigChangedListener mListener = null;
    private boolean isPurchased = false;

    public CustomAnalogClockPreferencesLayout(Context context) {
        super(context);
        init(context);
    }

    public CustomAnalogClockPreferencesLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }


    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View child = inflater.inflate(R.layout.custom_analog_clock_preferences_fragment, null);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
//        child.setBackgroundResource(R.drawable.border);
        addView(child, lp);

        final AnalogClockConfig config =
                new AnalogClockConfig(getContext(), AnalogClockConfig.Style.DEFAULT);
        Button fontButton = (Button) child.findViewById(R.id.fontButton);
        fontButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fm = ((Activity) getContext()).getFragmentManager();
                ManageFontsDialogFragment dialog = new ManageFontsDialogFragment();
                dialog.setIsPurchased(isPurchased);
                dialog.setSelectedUri(config.fontUri);
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
