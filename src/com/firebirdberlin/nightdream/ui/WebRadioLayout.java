package com.firebirdberlin.nightdream.ui;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.services.RadioStreamService;
import com.firebirdberlin.radiostreamapi.RadioStreamMetadata;
import com.firebirdberlin.radiostreamapi.RadioStreamMetadataRetriever.RadioStreamMetadataListener;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

public class WebRadioLayout extends RelativeLayout {

    public static String TAG ="WebRadioLayout";

    public boolean locked = false;
    private Context context;
    private TextView textView;
    private ImageView buttonSleepTimer;
    private ImageView volumeMutedIndicator;
    private boolean showConnectingHint = false;
    private boolean showMetaInfoOnNextUpdate = true;
    private ProgressBar spinner;
    private WebRadioStationButtonsLayout webRadioButtons;
    private NightDreamBroadcastReceiver broadcastReceiver = null;
    private AudioVolumeContentObserver audioVolumeContentObserver = null;
    private UserInteractionObserver userInteractionObserver;
    final private Handler handler = new Handler();

    public WebRadioLayout(Context context) {
        super(context);
        this.context = context;
    }

    public WebRadioLayout(final Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        setBackgroundResource(R.drawable.webradiopanelborder);

        int padding = Utility.dpToPx(context, 6.f);
        int paddingLarge = Utility.dpToPx(context, 40.f);

        initTextView(padding, paddingLarge);
        initButtonSleepTimer(padding);
        initSpinner(padding);
        initWebRadioButtons(attrs);

        initVolumeMutedIndicator(padding);

        addView(textView);
        addView(buttonSleepTimer);
        addView(spinner);
        addView(volumeMutedIndicator);
        addView(webRadioButtons);

        updateText();
    }

    private void initTextView(int padding, int paddingLarge) {
        textView = new TextView(context);
        textView.setId(R.id.web_radio_text_view); // id for placing spinner LEFT_OF this view
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setSingleLine(true);
        textView.setPadding(paddingLarge, padding, paddingLarge, padding);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        textView.setLayoutParams(lp);

        textView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (locked) return;
                notifyUserInteraction();
                showInfoDialog();
            }
        });
        textView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (locked) return false;
                notifyUserInteraction();
                updateMetaData();
                return true;
            }
        });
    }

    private void initButtonSleepTimer(int padding) {
        buttonSleepTimer = new ImageView(context);
        buttonSleepTimer.setImageResource(R.drawable.ic_nightmode);

        buttonSleepTimer.setPadding(padding, padding, padding, padding);
        buttonSleepTimer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showSleepTimerDialog();
            }
        });
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        buttonSleepTimer.setLayoutParams(lp);
    }

    private void initSpinner(int padding) {
        spinner = new ProgressBar(context, null, android.R.attr.progressBarStyleSmall);
        spinner.setPadding(0, padding, 0, padding);
        spinner.setVisibility(View.VISIBLE);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.LEFT_OF, textView.getId());
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        spinner.setLayoutParams(lp);
    }

    private void initWebRadioButtons(AttributeSet attrs) {
        webRadioButtons = new WebRadioStationButtonsLayout(context, attrs);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        webRadioButtons.setLayoutParams(lp);
    }

    private void initVolumeMutedIndicator(int padding) {
        volumeMutedIndicator = new ImageView(context);
        volumeMutedIndicator.setImageResource(R.drawable.ic_no_audio);
        volumeMutedIndicator.setPadding(padding, padding, padding, padding);

        RelativeLayout.LayoutParams lp =
                new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        volumeMutedIndicator.setLayoutParams(lp);

        volumeMutedIndicator.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (audio == null) {
                    return;
                }
                int maxVol = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                audio.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        Math.round((float) maxVol * 0.1f),
                        AudioManager.FLAG_SHOW_UI
                );
            }
        });

        updateVolumeMutedIndicator();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        broadcastReceiver = registerBroadcastReceiver();
        registerAudioVolumeContentObserver();
    }

    private NightDreamBroadcastReceiver registerBroadcastReceiver() {
        NightDreamBroadcastReceiver receiver = new NightDreamBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.ACTION_RADIO_STREAM_STARTED);
        filter.addAction(Config.ACTION_RADIO_STREAM_STOPPED);
        filter.addAction(Config.ACTION_RADIO_STREAM_READY_FOR_PLAYBACK);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);
        return receiver;
    }

    private void registerAudioVolumeContentObserver() {
        audioVolumeContentObserver = new AudioVolumeContentObserver(context, new Handler());
        context.getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, audioVolumeContentObserver );
    }

    private void unregisterAudioVolumeContentObserver() {

        if (audioVolumeContentObserver != null) {
            context.getContentResolver().unregisterContentObserver(audioVolumeContentObserver);
            audioVolumeContentObserver = null;
        }

    }

    public void setUserInteractionObserver(UserInteractionObserver o) {
        userInteractionObserver = o;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        try {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver);
            unregisterAudioVolumeContentObserver();
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void setCustomColor(int accentColor, int textColor) {
        Drawable bg = getBackground();
        bg.setColorFilter( accentColor, PorterDuff.Mode.MULTIPLY );

        buttonSleepTimer.setColorFilter(
                RadioStreamService.isSleepTimeSet() ? accentColor : textColor,
                PorterDuff.Mode.SRC_ATOP
        );
        volumeMutedIndicator.setColorFilter(
                textColor,
                PorterDuff.Mode.SRC_ATOP
        );
        textView.setTextColor(textColor);

        if (webRadioButtons != null) {
            webRadioButtons.setCustomColor(accentColor, textColor);
        }
    }

    private void showSleepTimerDialog() {
        FragmentManager fm = ((AppCompatActivity) getContext()).getSupportFragmentManager();
        SleepTimerDialogFragment dialog = new SleepTimerDialogFragment();
        dialog.show(fm, "sleep_timer");
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    protected void hide() {
        showMetaInfoOnNextUpdate = true;
    }

    protected void showMetaInfoOnNextUpdate() {
        showMetaInfoOnNextUpdate = true;
    }

    protected void updateText() {
        if (textView == null) return;

        int radioStationIndex = RadioStreamService.getCurrentRadioStationIndex();
        RadioStation station = RadioStreamService.getCurrentRadioStation();

        if (RadioStreamService.streamingMode == RadioStreamService.StreamingMode.RADIO) {

            if (radioStationIndex >= 0 && station != null) {
                textView.setText(station.name);
                webRadioButtons.setActiveStation(radioStationIndex);
            }

            if (showMetaInfoOnNextUpdate) {
                // display current meta info when panel was hidden and becomes visible
                updateMetaData();
            }
        } else {
            textView.setText("");
            webRadioButtons.clearActiveStation();
        }
        if (spinner != null) {
            spinner.setVisibility(showConnectingHint ? View.VISIBLE : View.GONE);
        }
        webRadioButtons.invalidate();
        updateVolumeMutedIndicator();
        buttonSleepTimer.setVisibility(RadioStreamService.isRunning ? VISIBLE : INVISIBLE);
        showMetaInfoOnNextUpdate = false;
    }



    private Runnable resetDefaultText = new Runnable() {
        @Override
        public void run() {
            updateText();
        }
    };

    private Runnable hideConnectingHint = new Runnable() {
        @Override
        public void run() {
            setShowConnectingHint(false);
        }
    };

    private void updateMetaData() {
        if (RadioStreamService.getCurrentRadioStation() == null) {
            return;
        }

        RadioStreamMetadataListener listener = new RadioStreamMetadataListener() {
            @Override
            public void onMetadataRequestStarted() {
                setShowConnectingHint(true, true);
            }

            @Override
            public void onMetadataAvailable(RadioStreamMetadata metadata) {
                String streamTitle = (metadata != null ? metadata.streamTitle : null);
                showMetaTitle(streamTitle);
            }
        };

        RadioStreamService.updateMetaData(listener, context);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void showInfoDialog() {
        // dialog not available while stream is still loading or stopped
        if (!RadioStreamService.isReadyForPlayback() ||
                RadioStreamService.getCurrentRadioStation() == null) {
            return;
        }

        FragmentManager fm = ((AppCompatActivity) getContext()).getSupportFragmentManager();
        RadioInfoDialogFragment dialog = new RadioInfoDialogFragment();
        dialog.show(fm, "radio info");
    }

    private void showMetaTitle(final String metaTitle) {
        Log.i(TAG, "showMetaTitle");
        setShowConnectingHint(false);
        if (metaTitle != null && !metaTitle.isEmpty()) {

            // use handler.post to update text, otherwise updated text doesn't show up sometimes
            // (e.g. on update after panel becomes visible)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    textView.setText(metaTitle);
                }
            });

            // switch back to radio name after x seconds
            handler.removeCallbacks(resetDefaultText);
            handler.postDelayed(resetDefaultText, 5000);
        }
    }

    private void setShowConnectingHint(boolean showConnectingHint) {
        setShowConnectingHint(showConnectingHint, false);
    }

    private void setShowConnectingHint(boolean showConnectingHint, boolean autoHide) {
        this.showConnectingHint = showConnectingHint;
        spinner.setVisibility(showConnectingHint ? View.VISIBLE : View.GONE);
        invalidate();

        if (autoHide) {// hide animation after 10s if anything goes wrong
            handler.postDelayed(hideConnectingHint, 10000);
        }
    }

    @Override
    public void setClickable(boolean clickable) {

        Log.i(TAG, "setClickable: " + String.valueOf(clickable));
        super.setClickable(clickable);

        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            view.setClickable(clickable);
        }
        textView.setClickable(clickable);
    }

    private void updateVolumeMutedIndicator() {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audio == null) {
            return;
        }
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);

        boolean muted = false;
        if (Build.VERSION.SDK_INT >= 23) {
            muted = audio.isStreamMute(AudioManager.STREAM_MUSIC);
        }

        updateVolumeMutedIndicatorVisibility(currentVolume, muted);
    }

    private void updateVolumeMutedIndicatorVisibility(int currentVolume, boolean muted) {
        if (volumeMutedIndicator != null) {
            volumeMutedIndicator.setVisibility(currentVolume > 0 && !muted ? GONE : VISIBLE);
        }
    }

    private void notifyUserInteraction() {
        if (userInteractionObserver != null) {
            userInteractionObserver.notifyAction();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        notifyUserInteraction();
        return false;
    }

    class NightDreamBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Config.ACTION_RADIO_STREAM_STARTED.equals(action)) {
                setShowConnectingHint(true);
                updateText();
            } else if (Config.ACTION_RADIO_STREAM_READY_FOR_PLAYBACK.equals(action)) {
                setShowConnectingHint(false);
                updateMetaData();
                updateText();
            } else if (Config.ACTION_RADIO_STREAM_STOPPED.equals(action)) {
                updateText();
                setShowConnectingHint(false);
            }
        }
    }

    class AudioVolumeContentObserver extends ContentObserver {

        private int previousVolume = -1;
        private boolean previousMuted = false;

        AudioVolumeContentObserver(Context context, Handler handler) {
            super(handler);
            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audio != null) {
                previousVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            }
        }

        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audio == null) {
                return;
            }
            int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);

            boolean muted = false;
            if (Build.VERSION.SDK_INT >= 23) {
                muted = audio.isStreamMute(AudioManager.STREAM_MUSIC);
            }


            if (previousVolume != currentVolume || muted != previousMuted) {
                previousVolume = currentVolume;
                previousMuted = muted;

                Log.i(TAG, "volume/muted changed");

                updateVolumeMutedIndicatorVisibility(currentVolume, muted);
            }
        }
    }
}
