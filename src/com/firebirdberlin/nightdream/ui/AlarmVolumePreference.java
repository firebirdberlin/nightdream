package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.firebirdberlin.nightdream.R;

public class AlarmVolumePreference extends Preference implements OnSeekBarChangeListener {
    private static final String ANDROIDNS = "http://schemas.android.com/apk/res/android";
    private static final String SEEKBAR = "http://schemas.android.com/apk/lib/android";
    private static final int DEFAULT_VALUE = 50;

    private AudioManager audioManager;
    private int stream = AudioManager.STREAM_ALARM;
    private int maxValue = 100;
    private int currentValue;

    private SeekBar seekBar;
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
        stream = attrs.getAttributeIntValue(ANDROIDNS, "stream", AudioManager.STREAM_ALARM);
        audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        maxValue = audioManager.getStreamMaxVolume(stream);
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
                ViewGroup summaryParent2 = (ViewGroup) summaryParent;
                layoutInflater.inflate(R.layout.alarm_volume_preference, summaryParent2);

                icon = (ImageView) summaryParent2.findViewById(R.id.icon);
                seekBar = (SeekBar) summaryParent2.findViewById(R.id.seekBar);
                seekBar.setMax(maxValue);
                seekBar.setOnSeekBarChangeListener(this);
            }
        }
        setupIcon();
        updateView();
    }

    protected void updateView() {
        if (seekBar != null) seekBar.setProgress(currentValue);
    }

    //region OnSeekBarChangeListener interface
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

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        notifyChanged();
    }
    //endregion

    private void setupIcon() {
        if ( currentValue == 0 ) {
            icon.setImageResource(R.drawable.ic_no_audio);
            icon.setColorFilter( Color.GRAY, PorterDuff.Mode.SRC_ATOP );
        } else {
            icon.setImageResource(R.drawable.ic_audio);
            icon.setColorFilter( Color.WHITE, PorterDuff.Mode.SRC_ATOP );
        }
        icon.invalidate();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray ta, int index) {
        int defaultValue = ta.getInt(index, DEFAULT_VALUE);
        return defaultValue;
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            currentValue = getPersistedInt(currentValue);
        }
        else {
            int temp = 0;
            if (defaultValue instanceof Integer) temp = (Integer) defaultValue;

            persistInt(temp);
            currentValue = temp;
        }
    }

    private static String getAttributeStringValue(AttributeSet attrs, String namespace,
                                                  String name, String defaultValue) {
        String value = attrs.getAttributeValue(namespace, name);
        if (value == null) value = defaultValue;

        return value;
    }

    private void setMax(int value){
        if (seekBar != null) {
            seekBar.setMax(value);
            seekBar.invalidate();
        }
    }
}
