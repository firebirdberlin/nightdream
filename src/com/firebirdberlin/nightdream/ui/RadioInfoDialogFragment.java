package com.firebirdberlin.nightdream.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.services.RadioStreamService;
import com.firebirdberlin.radiostreamapi.IcecastMetadata;
import com.firebirdberlin.radiostreamapi.models.RadioStation;


public class RadioInfoDialogFragment extends DialogFragment {

    public static String TAG = "RadioInfoDialogFragment";

    private RadioInfoDialogListener listener;
    private Context context;

    private WebRadioLayout.NightDreamBroadcastReceiver broadcastReceiver = null;

    private String stationTitle;

    private TextView stationNameTextView;
    private TextView metaTitleTextView;
    private TextView urlTextView;
    private TextView kbpsTextView;
    private TextView genreTextView;

    private ContentLoadingProgressBar progressBar;

    final private Handler handler = new Handler();

    // dont update infos twice if multiple broadcasts occur
    private boolean infosUpdated = false;

    public RadioInfoDialogFragment() {
        // Required empty public constructor
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.DialogTheme);
        // Get the layout inflater

        // Warning: must use context of AlertDialog.Builder here so that the changed theme is applied by LayoutInflater in RadioStreamDialog!
        // (AlertDialog.Builder uses a ContextThemeWrapper internally to change the theme for this DialogFragment)
        LayoutInflater inflater = (LayoutInflater)
                builder.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.radio_info_dialog, null);

        stationNameTextView = (TextView) view.findViewById(R.id.stationNameText);
        metaTitleTextView = (TextView) view.findViewById(R.id.metaTitleText);
        urlTextView = (TextView) view.findViewById(R.id.urlText);
        kbpsTextView = (TextView) view.findViewById(R.id.kbpsText);
        genreTextView = (TextView) view.findViewById(R.id.genreText);
        progressBar = (ContentLoadingProgressBar) view.findViewById(R.id.progress_bar);

        urlTextView.setPaintFlags(urlTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        urlTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String urlText = urlTextView.getText().toString();
                if (urlText != null && !urlText.isEmpty()) {
                    urlText = urlText.trim();
                    if (!urlText.startsWith("http://") && !urlText.startsWith("https://")) {
                        urlText = "http://" + urlText;
                    }
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlText));
                        startActivity(browserIntent);
                    } catch (ActivityNotFoundException e) {
                    }
                }
            }
        });

        RadioStation station = RadioStreamService.getCurrentRadioStation();

        if (station == null) {
            getDialog().cancel();
        }

        stationTitle = station.name;

        builder.setTitle(stationTitle)
                .setIcon(R.drawable.ic_radio)
                .setView(view)

                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (listener != null) {
                            listener.onRadioInfoDialogClosed();
                        }
                    }
                });

        startMetaDataUpdate();
        registerBroadcastReceiver(); // register here, after all view elements are ready

        final Dialog dialog = builder.create();

        // detect back button and also call onCancel listener so systemUI can be hidden afterwards
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey (DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK &&
                        event.getAction() == KeyEvent.ACTION_UP &&
                        !event.isCanceled()) {

                    dialog.cancel();

                    if (listener != null) {
                        listener.onRadioInfoDialogClosed();
                    }

                    return true;
                }
                return false;
            }
        });

        return dialog;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.context = activity;

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        if (context instanceof RadioInfoDialogListener) {
            listener = (RadioInfoDialogListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
        unregisterBroadcastReceiver();
    }

    private NightDreamBroadcastReceiver registerBroadcastReceiver() {
        //Log.i(TAG, "registerBroadcastReceiver");
        unregisterBroadcastReceiver();
        NightDreamBroadcastReceiver receiver = new NightDreamBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.ACTION_RADIO_STREAM_META_DATA_REQUEST_STARTED);
        filter.addAction(Config.ACTION_RADIO_STREAM_META_DATA_AVAILABLE);
        context.registerReceiver(receiver, filter);
        return receiver;
    }

    private void unregisterBroadcastReceiver() {
        //Log.i(TAG, "unregisterBroadcastReceiver");
        try {
            if (broadcastReceiver != null) {
                context.unregisterReceiver(broadcastReceiver);
            }

        } catch (IllegalArgumentException ignored) {
        }
    }

    private void startMetaDataUpdate() {
        boolean forcedUpdate = false; // get from cache
        RadioStreamService.updateMetaData(context, forcedUpdate);
    }

    private void updateInfo() {

        progressBar.setVisibility(View.GONE);

        infosUpdated = true;

        IcecastMetadata data = RadioStreamService.getCurrentIcecastMetadata();

        String metaTitle = (data != null && data.streamTitle != null && !data.streamTitle.isEmpty() ? data.streamTitle : null);
        if (metaTitle != null) {
            metaTitleTextView.setVisibility(View.VISIBLE);
            metaTitleTextView.setText(metaTitle);
        } else {
            metaTitleTextView.setVisibility(View.GONE);
        }

        // show name only if different from title
        String name = (data != null && data.icyHeaderInfo != null && data.icyHeaderInfo.getName() != null ? data.icyHeaderInfo.getName() : null);
        if (name != null && !name.equals(stationTitle)
                && (metaTitle == null || !metaTitle.equals(name))) {
            stationNameTextView.setVisibility(View.VISIBLE);
            stationNameTextView.setText(name);
        } else {
            urlTextView.setVisibility(View.GONE);
        }

        //TODO show a link, open in stock browser on click
        String url = (data != null && data.icyHeaderInfo != null && data.icyHeaderInfo.getUrl() != null ? data.icyHeaderInfo.getUrl() : null);
        if (url != null) {
            urlTextView.setVisibility(View.VISIBLE);
            urlTextView.setText(url.toLowerCase());
        } else {
            urlTextView.setVisibility(View.GONE);
        }

        String kbps = (data != null && data.icyHeaderInfo != null && data.icyHeaderInfo.getBitrate() != null ? (data.icyHeaderInfo.getBitrate().intValue() + " kbps") : null);
        if (kbps != null) {
            kbpsTextView.setVisibility(View.VISIBLE);
            kbpsTextView.setText(kbps);
        } else {
            kbpsTextView.setVisibility(View.GONE);
        }


        String genre = (data != null && data.icyHeaderInfo != null && data.icyHeaderInfo.getGenre() != null ? data.icyHeaderInfo.getGenre() : null);
        if (genre != null) {
            genreTextView.setVisibility(View.VISIBLE);
            genreTextView.setText(genre);
        } else {
            genreTextView.setVisibility(View.GONE);
        }
    }

    public interface RadioInfoDialogListener {
        void onRadioInfoDialogClosed();
    }

    class NightDreamBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (Config.ACTION_RADIO_STREAM_META_DATA_REQUEST_STARTED.equals(action)) {
                Log.i(TAG, "ACTION_RADIO_STREAM_META_DATA_REQUEST_STARTED");
                if (!infosUpdated) {
                    progressBar.setVisibility(View.VISIBLE);
                }
            } else if (Config.ACTION_RADIO_STREAM_META_DATA_AVAILABLE.equals(action)) {
                Log.i(TAG, "ACTION_RADIO_STREAM_META_DATA_AVAILABLE");
                if (!infosUpdated) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateInfo();
                        }
                    });

                }
            }
        }
    }


}
