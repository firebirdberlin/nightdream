package com.firebirdberlin.nightdream.ui;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.mediarouter.app.MediaRouteButton;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.services.RadioStreamService;
import com.firebirdberlin.radiostreamapi.RadioStreamMetadata;
import com.firebirdberlin.radiostreamapi.RadioStreamMetadataRetriever.RadioStreamMetadataListener;
import com.firebirdberlin.radiostreamapi.models.FavoriteRadioStations;
import com.firebirdberlin.radiostreamapi.models.RadioStation;
import com.google.android.gms.cast.framework.CastButtonFactory;

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
    private NightDreamBroadcastReceiver broadcastReceiver = null;
    private AudioVolumeContentObserver audioVolumeContentObserver = null;
    private UserInteractionObserver userInteractionObserver;
    final private Handler handler = new Handler();
    private int currentVolumme = -1;
    private int accentColor = -1;
    private int textColor = -1;
    private Settings settings;
    FavoriteRadioStations stations;
    private boolean showSmallButtons = false;
    private Integer activeStationIndex;
    private MediaRouteButton mMediaRouteButton;

    public WebRadioLayout(Context context) {
        super(context);
        this.context = context;
    }

    public WebRadioLayout(final Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        Point displaySize = Utility.getDisplaySize(context);
        showSmallButtons = (displaySize.x <= 480);

        settings = new Settings(context);
        stations = settings.getFavoriteRadioStations();

        inflate(context,R.layout.webradio_panel,this);
        volumeMutedIndicator = findViewById(R.id.web_radio_no_audio);
        spinner = findViewById(R.id.web_radio_progress_bar);
        textView = findViewById(R.id.web_radio_text_view);
        buttonSleepTimer = findViewById(R.id.web_radio_sleep_timer);

        mMediaRouteButton = (MediaRouteButton) findViewById(R.id.web_radio_media_route_button);
        CastButtonFactory.setUpMediaRouteButton(context.getApplicationContext(), mMediaRouteButton);

        initButtons();
        initVolumeMutedIndicator();
        initSpinner();
        initTextView();
        initButtonSleepTimer();

        startLastActiveRadioStream();
        updateText();
    }

    private void initTextView() {

        textView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (locked) return;
                notifyUserInteraction();
                updateMetaData();
            }
        });
        textView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (locked) return false;
                notifyUserInteraction();
                showInfoDialog();
                return true;
            }
        });
    }

    private void initButtonSleepTimer() {
        buttonSleepTimer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showSleepTimerDialog();
            }
        });
    }

    private void initSpinner() {
        spinner.setVisibility(View.VISIBLE);
    }

    private void initVolumeMutedIndicator() {
        volumeMutedIndicator.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (audio == null) {
                    return;
                }
                int newVolume = 0;
                if (isStreamMuted()) {
                    int maxVol = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    newVolume = (currentVolumme > -1) ? currentVolumme : Math.round((float) maxVol * 0.1f);
                    currentVolumme = -1;
                } else {
                    currentVolumme = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                }
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, AudioManager.FLAG_SHOW_UI);
                updateVolumeMutedIndicator();
                notifyUserInteraction();
            }
        });

        updateVolumeMutedIndicator();
    }

    public void initButtons () {
        Log.d(TAG, "initButtons");
        final int maxNumButtons = FavoriteRadioStations.getMaxNumEntries();

        for (int i = 0; i < maxNumButtons; i++) {
            int id = getResources().getIdentifier("web_radio_button"+(i+1), "id", context.getPackageName());
            Button btn = (Button) findViewById(id); // get the element

            btn.setOnLongClickListener(buttonOnLongClickListener);
            btn.setOnClickListener(buttonOnClickListener);
        }
    }

    private OnClickListener buttonOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            int index = Integer.parseInt((String) v.getTag());
            Log.d(TAG,"buttonOnClickListener: "+index);

            if (activeStationIndex != null && index == activeStationIndex) {
                stopRadioStream();
                activeStationIndex = null;
                updateButtonState();
            } else {
                Settings.setLastActiveRadioStation(context, index);
                startRadioStreamOrShowDialog(index);
            }
        }
    };

    private OnLongClickListener buttonOnLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(final View v) {
            int stationIndex = Integer.parseInt((String) v.getTag());
            showRadioStreamDialog(stationIndex);
            return true;
        }
    };

    public void startLastActiveRadioStream() {
        if (RadioStreamService.isRunning) return;
        if (stations == null) {
            return;
        }

        if (stations.numAvailableStations() == 0) {
            showRadioStreamDialog(0);
            return;
        }

        int stationIndex = Settings.getLastActiveRadioStation(context);
        //start radio stream
        if (stations.get(stationIndex) != null) {
            toggleRadioStreamState(stationIndex);
        }
    }

    private void startRadioStreamOrShowDialog(final int stationIndex) {
        RadioStation station = null;
        if (stations != null) {
            station = stations.get(stationIndex);
        }
        if (station != null) {
            //start radio stream
            toggleRadioStreamState(stationIndex);
        } else {
            showRadioStreamDialog(stationIndex);
        }
    }

    private void showRadioStreamDialog(final int stationIndex) {
        RadioStreamDialogListener listener = new RadioStreamDialogListener() {
            @Override
            public void onRadioStreamSelected(RadioStation station) {
                // update station in settings
                if (settings != null) {
                    settings.persistFavoriteRadioStation(station, stationIndex);
                    stations = settings.getFavoriteRadioStations();
                }
                toggleRadioStreamState(stationIndex);
                hideSystemUI();
            }

            @Override
            public void onCancel() {
                hideSystemUI();
            }

            @Override
            public void onDelete(int stationIndex) {
                Log.i(TAG, "delete");
                if (activeStationIndex != null && stationIndex == activeStationIndex) {
                    stopRadioStream();
                }
                if (settings != null) {
                    settings.deleteFavoriteRadioStation(stationIndex);
                    stations = settings.getFavoriteRadioStations();
                }
                updateButtonState();
                hideSystemUI();
            }
        };

        RadioStation station = stations.get(stationIndex);
        String preferredCountry = null;
        if (station != null) {
            preferredCountry = station.countryCode;
        } else {
            for (int i = 0; i < stations.numAvailableStations(); i++) {
                RadioStation s = stations.get(i);
                if (s == null) {
                    continue;
                }
                preferredCountry = s.countryCode;
            }
        }
        RadioStreamDialogFragment.showDialog(
                (AppCompatActivity) getContext(), stationIndex, station, preferredCountry, listener
        );
    }

    private void hideSystemUI() {
        Utility.hideSystemUI(getContext());
    }

    private int lastButtonInUseIndex() {
        int lastIndex = -1;
        final int maxNumButtons = FavoriteRadioStations.getMaxNumEntries();
        for (int i = 0; i < maxNumButtons; i++) {
            if (stations != null && stations.get(i) != null) {
                lastIndex = i;
            }
        }
        return lastIndex;
    }

    public int setAlpha(int color, int alpha) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    private void updateButtonState() {
        final int lastButtonInUseIndex = lastButtonInUseIndex();

        final int maxNumButtons = FavoriteRadioStations.getMaxNumEntries();
        for (int i = 0; i < maxNumButtons; i++) {
            int id = getResources().getIdentifier("web_radio_button"+(i+1), "id", context.getPackageName());
            Button b = findViewById(id); // get the element
            b.setVisibility(i <= lastButtonInUseIndex + 1 ? VISIBLE : GONE);

            final boolean active = (activeStationIndex != null && activeStationIndex == i);
            int color = active ? accentColor : textColor;

            if (active) {
                // draw stop button: empty text, use shape containing additional "stop" shape
                b.setText("");
                b.setBackgroundResource(
                        showSmallButtons
                                ? R.drawable.webradio_station_stop_button_small
                                : R.drawable.webradio_station_stop_button
                );
            } else {
                b.setText(String.valueOf(i + 1));
                b.setBackgroundResource(R.drawable.webradio_station_button);
            }

            Drawable border = b.getBackground();
            if (stations != null && stations.get(i) == null) {
                border.setAlpha(125);
                color = setAlpha(color, 125);
                b.setText(i <= lastButtonInUseIndex ? String.valueOf(i + 1) : "+");
            } else {
                border.setAlpha(255);
                color = setAlpha(color, 255);
            }
            b.setTextColor(color);
            border.setColorFilter((color == accentColor) ? new LightingColorFilter(accentColor, 1) :  new LightingColorFilter(textColor, 1));
        }
    }

    private void stopRadioStream() {
        if ( RadioStreamService.streamingMode == RadioStreamService.StreamingMode.RADIO ) {
            RadioStreamService.stop(context);
        }
    }

    public void setActiveStation(int stationIndex) {
        activeStationIndex = stationIndex > -1 ? stationIndex : null;
        updateButtonState();
    }

    private void toggleRadioStreamState(final int radioStationIndex) {
        boolean wasAlreadyPlaying = false;
        if (RadioStreamService.streamingMode == RadioStreamService.StreamingMode.RADIO) {
            RadioStreamService.stop(context);
            wasAlreadyPlaying = true;
        }

        if (Utility.hasNetworkConnection(context)) {
            // is stream was already playing before, don't ask again? (but what if user switched from wifi to 3g since stream start?)
            if (Utility.hasFastNetworkConnection(context) || wasAlreadyPlaying) {
                RadioStreamService.startStream(context, radioStationIndex);
            } else {
                new AlertDialog.Builder(context, R.style.DialogTheme)
                        .setTitle(R.string.message_mobile_data_connection)
                        .setMessage(R.string.message_mobile_data_connection_confirmation)
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                hideSystemUI();
                            }
                        })
                        .setIcon(R.drawable.ic_attention)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                RadioStreamService.startStream(context, radioStationIndex);
                                hideSystemUI();
                            }
                        })
                        .show();
            }

        } else { // no network connection
            Toast.makeText(context, R.string.message_no_data_connection, Toast.LENGTH_LONG).show();
        }
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
        this.accentColor = accentColor;
        this.textColor = textColor;
        Drawable bg;

        bg = ResourcesCompat.getDrawable(getResources(), R.drawable.webradiopanelborder, null);
        if (bg != null) {
            bg.setColorFilter(accentColor, PorterDuff.Mode.MULTIPLY);
            setBackground(bg);
        }

        buttonSleepTimer.setColorFilter(
                RadioStreamService.isSleepTimeSet() ? accentColor : textColor,
                PorterDuff.Mode.SRC_ATOP
        );
        volumeMutedIndicator.setColorFilter(
                isStreamMuted() ? accentColor : textColor,
                PorterDuff.Mode.SRC_ATOP
        );
        textView.setTextColor(textColor);

        updateButtonState();

        if (spinner != null) {
            Drawable drawable = spinner.getIndeterminateDrawable();
            if (drawable != null) {
                drawable.setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP);
            }
        }
    }

    private boolean isStreamMuted() {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audio == null) {
            return false;
        }

        /*
        if (Build.VERSION.SDK_INT >= 23) {
            return audio.isStreamMute(AudioManager.STREAM_MUSIC);
        }
        */
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        return currentVolume == 0;
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
                setActiveStation(radioStationIndex);
            }

            if (showMetaInfoOnNextUpdate) {
                // display current meta info when panel was hidden and becomes visible
                updateMetaData();
            }
        } else {
            textView.setText("");
        }
        if (spinner != null) {
            spinner.setVisibility(showConnectingHint ? View.VISIBLE : View.GONE);
        }
        updateVolumeMutedIndicator();
        buttonSleepTimer.setVisibility(RadioStreamService.isRunning ? VISIBLE : INVISIBLE);
        volumeMutedIndicator.setVisibility(RadioStreamService.isRunning ? VISIBLE : INVISIBLE);
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

        Log.i(TAG, "setClickable: " + clickable);
        super.setClickable(clickable);

        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            view.setClickable(clickable);
        }
        textView.setClickable(clickable);
    }

    private void updateVolumeMutedIndicator() {
        updateVolumeMutedIndicatorVisibility();
    }

    private void updateVolumeMutedIndicatorVisibility() {
        Log.d(TAG, "updateVolumeMutedIndicatorVisibility: "+volumeMutedIndicator );
        if (volumeMutedIndicator != null) {
            volumeMutedIndicator.setColorFilter(
                    isStreamMuted() ? accentColor : textColor,
                    PorterDuff.Mode.SRC_ATOP
            );
        }
        invalidate();
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
        private boolean wasMuted = false;

        AudioVolumeContentObserver(Context context, Handler handler) {
            super(handler);
            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audio != null) {
                previousVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            }
            wasMuted = previousVolume == 0;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            Log.d(TAG, "volume changed");
            if ( wasMuted != isStreamMuted() ) {
                updateVolumeMutedIndicatorVisibility();
            }
            wasMuted = isStreamMuted();
        }
    }
}
