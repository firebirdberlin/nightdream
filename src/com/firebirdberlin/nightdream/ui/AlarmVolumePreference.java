package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.firebirdberlin.nightdream.R;

public class AlarmVolumePreference extends Preference implements OnSeekBarChangeListener {
    private static final String ANDROIDNS = "http://schemas.android.com/apk/res/android";
    private static final int DEFAULT_VALUE = 50;

    private int minValue = 0;
    private int maxValue = 100;
    private int currentValue;

    private AppCompatSeekBar seekBar;
    private ImageView icon;

    public AlarmVolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setValuesFromXml(attrs);
    }

    public AlarmVolumePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setValuesFromXml(attrs);
    }

    private void setValuesFromXml(AttributeSet attrs) {
        int stream = attrs.getAttributeIntValue(ANDROIDNS, "stream", AudioManager.STREAM_ALARM);
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            maxValue = audioManager.getStreamMaxVolume(stream);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                minValue = audioManager.getStreamMinVolume(stream);
            }
        } else {
            maxValue = 7;
            minValue = 0;
        }

    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View ret = holder.itemView;

        View summary = ret.findViewById(android.R.id.summary);
        if (summary != null) {
            ViewParent summaryParent = summary.getParent();
            if (summaryParent instanceof ViewGroup) {
                final LayoutInflater layoutInflater =
                        (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                if (layoutInflater == null) {
                    return;
                }
                ViewGroup summaryParent2 = (ViewGroup) summaryParent;
                View view = summaryParent2.findViewWithTag("custom");
                if (view == null) {
                    layoutInflater.inflate(R.layout.alarm_volume_preference, summaryParent2);
                }

                icon = summaryParent2.findViewById(R.id.icon);
                seekBar = summaryParent2.findViewById(R.id.seekBar);
                seekBar.setMax(maxValue);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    seekBar.setMin(minValue);
                }
                seekBar.setOnSeekBarChangeListener(this);
            }
        }
        setupIcon();
        updateView();
    }

    protected void updateView() {
        if (seekBar != null) seekBar.setProgress(currentValue);
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (progress > maxValue) progress = maxValue;


        // change rejected, revert to the previous value
        if (!callChangeListener(progress)) {
            seekBar.setProgress(currentValue);
            return;
        }
        // change accepted, store it
        currentValue = progress;
        persistInt(progress);
        setupIcon();
    }

    public void onStartTrackingTouch(SeekBar seekBar) {}

    public void onStopTrackingTouch(SeekBar seekBar) {
        notifyChanged();
    }

    private void setupIcon() {
        if (currentValue == minValue) {
            icon.setImageResource(R.drawable.ic_no_audio);
            icon.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP);
        } else {
            icon.setImageResource(R.drawable.ic_audio);
            icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        }
        icon.invalidate();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray ta, int index) {
        return ta.getInt(index, DEFAULT_VALUE);
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        super.onSetInitialValue(defaultValue);

        currentValue = getPersistedInt(DEFAULT_VALUE);
    }
}
