package com.firebirdberlin.nightdream.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.core.widget.ContentLoadingProgressBar;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.services.RadioStreamService;
import com.firebirdberlin.radiostreamapi.RadioStreamMetadata;
import com.firebirdberlin.radiostreamapi.RadioStreamMetadataRetriever.RadioStreamMetadataListener;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class RadioInfoDialogFragment extends AppCompatDialogFragment {

    public static String TAG = "RadioInfoDialogFragment";

    private RadioInfoDialogListener listener;
    private Context context;
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
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.DialogTheme);
        // Get the layout inflater

        // Warning: must use context of AlertDialog.Builder here so that the changed theme is
        // applied by LayoutInflater in RadioStreamDialog!
        // (AlertDialog.Builder uses a ContextThemeWrapper internally to change the theme for this
        // DialogFragment)
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
                    Log.i(TAG, "open url in browser: " + urlText);
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlText));
                        startActivity(browserIntent);
                    } catch (ActivityNotFoundException e) {
                    }
                }
            }
        });

        RadioStation station = RadioStreamService.getCurrentRadioStation();

        stationTitle = station != null ? station.name : "";

        builder.setTitle(stationTitle)
                .setIcon(R.drawable.ic_radio)
                .setView(view)

                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (listener != null) {
                            listener.onRadioInfoDialogDismissed();
                        }
                    }
                });

        updateMetaData();

        return builder.create();
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
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (listener != null) {
            listener.onRadioInfoDialogDismissed();
        }
    }

    private void updateMetaData() {

        RadioStreamMetadataListener listener = new RadioStreamMetadataListener() {

            @Override
            public void onMetadataRequestStarted() {
                if (!infosUpdated) {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onMetadataAvailable(RadioStreamMetadata metadata) {
                if (!infosUpdated) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateInfo();
                        }
                    });
                }
            }
        };

        RadioStreamService.updateMetaData(listener, context);
    }

    private void updateInfo() {

        progressBar.setVisibility(View.GONE);

        infosUpdated = true;

        RadioStreamMetadata data = RadioStreamService.getCurrentIcecastMetadata();

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

        String kbps = (data != null && data.icyHeaderInfo != null && data.icyHeaderInfo.getBitrate() != null ? (data.icyHeaderInfo.getBitrate() + " kbps") : null);
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
        void onRadioInfoDialogDismissed();
    }

}
