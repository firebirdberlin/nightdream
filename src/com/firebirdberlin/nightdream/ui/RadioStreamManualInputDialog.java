package com.firebirdberlin.nightdream.ui;


import android.app.AlertDialog;
import android.content.Context;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.radiostreamapi.PlaylistParser;
import com.firebirdberlin.radiostreamapi.PlaylistRequestTask;
import com.firebirdberlin.radiostreamapi.StreamURLAvailabilityCheckTask;
import com.firebirdberlin.radiostreamapi.models.PlaylistInfo;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

import java.net.MalformedURLException;
import java.net.URL;

public class RadioStreamManualInputDialog {

    public void showDialog(final Context context, RadioStation persistedRadioStation, final RadioStreamDialogListener listener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DialogTheme);

        builder.setTitle(R.string.radio_stream_manual_input_hint);

        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.radio_stream_manual_input_dialog, null);

        final EditText inputUrl = (EditText) v.findViewById(R.id.radio_stream_manual_input_url);
        final EditText inputDescription = (EditText) v.findViewById(R.id.radio_stream_manual_input_description);
        final ContentLoadingProgressBar progressSpinner = (ContentLoadingProgressBar) v.findViewById(R.id.radio_stream_manual_input_progress_bar);

        //RadioStation persistedRadioStation = getPersistedRadioStation();
        if (persistedRadioStation != null) {
            inputUrl.setText(persistedRadioStation.stream);
            inputDescription.setText(persistedRadioStation.name);
        }

        // test plain stream url + description
        //inputUrl.setText("http://rbb-radioberlin-live.cast.addradio.de/rbb/radioberlin/live/mp3/128/stream.mp3");
        //inputDescription.setText("Radio Berlin 88,8");
        // test of faulty stream url (connection timeout)
        //inputUrl.setText("http://pub8.rockradio.com:80/rr_classicrock");
        //inputDescription.setText("Rockradio timeout test");
        // test ashx json
        //inputUrl.setText("https://opml.radiotime.com/Tune.ashx?audience=%3BVZ_Altice%3Ball%3B&id=s96162&render=json&listenId=1523703277&formats=mp3,aac,ogg,flash,html&type=station&serial=a9bbfdc9-1157-46a2-87a8-f2909d897169&partnerId=RadioTime&version=2.29&itemUrlScheme=secure&build=2.29.0&reqAttempt=1");
        //inputDescription.setText("ashx json test");
        // test ashx plain
        //inputUrl.setText("https://opml.radiotime.com/Tune.ashx?audience=%3BVZ_Altice%3Ball%3B&id=s96162&listenId=1523703277&formats=mp3,aac,ogg,flash,html&type=station&serial=a9bbfdc9-1157-46a2-87a8-f2909d897169&partnerId=RadioTime&version=2.29&itemUrlScheme=secure&build=2.29.0&reqAttempt=1");
        //inputDescription.setText("ashx plain test");

        // test playlist
        //inputUrl.setText("http://www.radioberlin.de/live.m3u");
        //inputUrl.setText("http://www.radioberlin.de/live.pls");

        final TextView invalidUrlMessage = (TextView) v.findViewById(R.id.invalid_url);
        invalidUrlMessage.setVisibility(View.GONE);

        // hide error message when url is edited
        inputUrl.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                invalidUrlMessage.setVisibility(View.GONE);
            }
        });

        inputUrl.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    return;
                }
                // when url input field looses focus and is a playlist url (m3u, pls), start the validation,
                // retrieve stream description and put it into the description field
                final String urlString = inputUrl.getText().toString();
                final String description = inputDescription.getText().toString();

                if (!description.isEmpty()) {
                    return;
                }

                boolean networkConnection = Utility.hasNetworkConnection(context);
                final URL url = validateUrlInput(urlString);
                if (networkConnection && url != null && PlaylistParser.isPlaylistUrl(url)) {
                    progressSpinner.setVisibility(View.VISIBLE);

                    PlaylistRequestTask.AsyncResponse playListResponseListener = new PlaylistRequestTask.AsyncResponse() {
                        @Override
                        public void onPlaylistRequestFinished(PlaylistInfo result) {
                            progressSpinner.setVisibility(View.GONE);

                            if (result.valid) {
                                String stationName = getStationName(description, result.description, url);
                                inputDescription.setText(stationName);
                            } else {
                                showUrlErrorMessage(invalidUrlMessage);
                            }
                        }
                    };
                    new PlaylistRequestTask(playListResponseListener).execute(urlString);
                }
            }
        });

        builder.setView(v);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNeutralButton(android.R.string.cancel, null);

        final AlertDialog manualInputDialog = builder.create();
        // set OK button click handler here, so closing of the dialog can be prevented if invalid url was entered
        manualInputDialog.show();
        manualInputDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String urlString = inputUrl.getText().toString();
                final String description = inputDescription.getText().toString();

                final URL url = validateUrlInput(urlString);
                if (url == null) {
                    showUrlErrorMessage(invalidUrlMessage);
                    return;
                }

                boolean networkConnection = Utility.hasNetworkConnection(context);
                if (!networkConnection) {
                    invalidUrlMessage.setText(R.string.message_no_data_connection);
                    invalidUrlMessage.setVisibility(View.VISIBLE);
                    return;
                }

                progressSpinner.setVisibility(View.VISIBLE);

                if (PlaylistParser.isPlaylistUrl(url)) {
                    PlaylistRequestTask.AsyncResponse playListResponseListener = new PlaylistRequestTask.AsyncResponse() {
                        @Override
                        public void onPlaylistRequestFinished(PlaylistInfo result) {
                            progressSpinner.setVisibility(View.GONE);

                            final URL resultStreamUrl = (result != null ? validateUrlInput(result.streamUrl) : null);
                            if (result != null && result.valid && resultStreamUrl != null) {
                                String stationName = getStationName(description, result.description, resultStreamUrl);
                                int bitrate = (result.bitrateHint != null ? result.bitrateHint : 0);
                                persistAndDismissDialog(manualInputDialog, listener, stationName, urlString, bitrate);
                            } else {
                                showUrlErrorMessage(invalidUrlMessage);
                            }

                        }
                    };
                    new PlaylistRequestTask(playListResponseListener).execute(urlString);
                } else { // its a plain stream url
                    StreamURLAvailabilityCheckTask.AsyncResponse streamCheckResponseListener = new StreamURLAvailabilityCheckTask.AsyncResponse() {
                        @Override
                        public void onRequestFinished(Boolean result) {
                            progressSpinner.setVisibility(View.GONE);
                            if (result != null && result) {
                                String name = (!description.isEmpty() ? description : url.getHost());
                                persistAndDismissDialog(manualInputDialog, listener, name, urlString, 0);
                            } else {
                                showUrlErrorMessage(invalidUrlMessage);
                            }
                        }
                    };
                    new StreamURLAvailabilityCheckTask(streamCheckResponseListener).execute(urlString);
                }

            }

        });
    }

    private URL validateUrlInput(String urlString) {
        if ( urlString == null || urlString.isEmpty() ) {
            return null;
        }
        try {
            return new URL(urlString);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private String getStationName(String manualInput, String autoInput, URL streamURL) {
        if (manualInput != null && !manualInput.trim().isEmpty()) {
            return manualInput.trim();
        } else if (autoInput != null && !autoInput.trim().isEmpty()) {
            return autoInput.trim();
        } else {
            return streamURL.getHost();
        }
    }

    private void showUrlErrorMessage(TextView invalidUrlMessage) {
        invalidUrlMessage.setText(R.string.radio_stream_unreachable_url);
        invalidUrlMessage.setVisibility(View.VISIBLE);
    }

    private RadioStation createRadioStation(String name, String streamUrl, int bitrate) {
        RadioStation station = new RadioStation();
        station.isUserDefinedStreamUrl = true;
        station.isOnline = true;
        station.name = name;
        station.stream = streamUrl;
        station.bitrate = bitrate;
        station.countryCode = ""; // empty string, otherwise invalid json
        station.isUserDefinedStreamUrl = true;
        return station;
    }

    private void persistAndDismissDialog(final AlertDialog dialog, RadioStreamDialogListener listener, String name, String urlString, int bitrate) {
        RadioStation station = createRadioStation(name, urlString, bitrate);

        // close dialog
        dialog.dismiss();

        listener.onRadioStreamSelected(station);
    }

}
