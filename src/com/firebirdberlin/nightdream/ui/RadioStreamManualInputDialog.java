package com.firebirdberlin.nightdream.ui;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.ContentLoadingProgressBar;

import com.firebirdberlin.nightdream.HttpStatusCheckTask;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.radiostreamapi.IcyHeaderInfo;
import com.firebirdberlin.radiostreamapi.IcyHeaderReader;
import com.firebirdberlin.radiostreamapi.PlaylistParser;
import com.firebirdberlin.radiostreamapi.PlaylistRequestTask;
import com.firebirdberlin.radiostreamapi.models.PlaylistInfo;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

import java.net.MalformedURLException;
import java.net.URL;

public class RadioStreamManualInputDialog {

    public void showDialog(final Context context, RadioStation persistedRadioStation, final RadioStreamDialogListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DialogTheme);

        builder.setTitle(R.string.radio_stream_manual_input_hint);
        View v = createDialogView(context, persistedRadioStation);
        builder.setView(v);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.w("dismiss", "onclick1");

            }
        });
        builder.setNeutralButton(android.R.string.cancel, null);

        final AlertDialog manualInputDialog = builder.create();
        registerDialogListener(manualInputDialog, v, listener);
        manualInputDialog.show();
    }

    void registerDialogListener(final Dialog dialog, View v, final RadioStreamDialogListener listener) {
        final EditText inputUrl = v.findViewById(R.id.radio_stream_manual_input_url);
        final EditText inputDescription = v.findViewById(R.id.radio_stream_manual_input_description);
        final EditText inputSecondsToMute = v.findViewById(R.id.radio_stream_manual_input_seconds_to_mute);
        final ContentLoadingProgressBar progressSpinner = v.findViewById(R.id.radio_stream_manual_input_progress_bar);
        final TextView invalidUrlMessage = v.findViewById(R.id.invalid_url);

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialogInterface) {
                Button positiveButton = ((AlertDialog) dialogInterface).getButton(DialogInterface.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        Log.w("dismiss", "onCLick()");

                        final String urlString = inputUrl.getText().toString();
                        final String description = inputDescription.getText().toString();
                        final int secondsToMute = Integer.valueOf(inputSecondsToMute.getText().toString());

                        final URL url = validateUrlInput(urlString);
                        if (url == null) {
                            showUrlErrorMessage(invalidUrlMessage);
                            return;
                        }

                        boolean networkConnection = Utility.hasNetworkConnection(dialog.getContext());
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
                                        persistAndDismissDialog(
                                                (AlertDialog) dialog, listener, stationName, urlString, bitrate, secondsToMute
                                        );
                                    } else {
                                        showUrlErrorMessage(invalidUrlMessage);
                                    }

                                }
                            };
                            new PlaylistRequestTask(playListResponseListener).execute(urlString);
                        } else { // its a plain stream url
                            HttpStatusCheckTask.AsyncResponse streamCheckResponseListener = new HttpStatusCheckTask.AsyncResponse() {
                                @Override
                                public void onStatusCheckFinished(HttpStatusCheckTask.HttpStatusCheckResult checkResult) {
                                    progressSpinner.setVisibility(View.GONE);

                                    if (checkResult != null && checkResult.isSuccess()) {

                                        IcyHeaderInfo icyHeaderInfo = IcyHeaderReader.getHeaderInfos(checkResult.responseHeaders);

                                        String name;
                                        if (!description.isEmpty()) {
                                            name = description;
                                        } else if (icyHeaderInfo != null
                                                && icyHeaderInfo.getName() != null
                                                && !icyHeaderInfo.getName().isEmpty()) {
                                            name = icyHeaderInfo.getName();
                                        } else {
                                            name = url.getHost();
                                        }

                                        int bitrate = 0;
                                        if (icyHeaderInfo != null && icyHeaderInfo.getBitrate() != null) {
                                            bitrate = icyHeaderInfo.getBitrate();
                                        }

                                        persistAndDismissDialog(
                                                (AlertDialog) dialog, listener, name, urlString, bitrate, secondsToMute
                                        );
                                    } else {
                                        showUrlErrorMessage(invalidUrlMessage);
                                    }
                                }
                            };
                            new HttpStatusCheckTask(streamCheckResponseListener).execute(urlString);
                        }

                    }

                });

            }
        });
    }

    View createDialogView(final Context context, RadioStation persistedRadioStation) {

        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.radio_stream_manual_input_dialog, null);

        final EditText inputUrl = v.findViewById(R.id.radio_stream_manual_input_url);
        final EditText inputDescription = v.findViewById(R.id.radio_stream_manual_input_description);
        final EditText inputSecondsToMute = v.findViewById(R.id.radio_stream_manual_input_seconds_to_mute);
        final ContentLoadingProgressBar progressSpinner = v.findViewById(R.id.radio_stream_manual_input_progress_bar);

        //RadioStation persistedRadioStation = getPersistedRadioStation();
        if (persistedRadioStation != null) {
            inputUrl.setText(persistedRadioStation.stream);
            inputDescription.setText(persistedRadioStation.name);
            if (persistedRadioStation.muteDelayInMillis > 0) {
                inputSecondsToMute.setText(
                        String.valueOf((int) (persistedRadioStation.muteDelayInMillis / 1000))
                );
            }
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

        //bsp: stream url 302 redirect:
        //inputUrl.setText("http://stream.laut.fm/zeiten");
        //bsp: stream url can contain braces
        //inputUrl.setText("http://opml.radiotime.com/Tune.ashx?id=s136389");

        //bsp: nested m3u inside ashx
        //inputUrl.setText("http://opml.radiotime.com/Tune.ashx?id=s84194");

        // test playlist
        //inputUrl.setText("http://www.radioberlin.de/live.m3u");
        //inputUrl.setText("http://www.radioberlin.de/live.pls");
        //inputUrl.setText("http://www.acks.nl/acksmedia/acksradio/sb/listen192.asx");

        final TextView invalidUrlMessage = v.findViewById(R.id.invalid_url);
        invalidUrlMessage.setVisibility(View.GONE);

        // hide error message when url is edited
        inputUrl.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

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
                if (networkConnection && url != null) {
                    progressSpinner.setVisibility(View.VISIBLE);

                    if (PlaylistParser.isPlaylistUrl(url)) {

                        PlaylistRequestTask.AsyncResponse playListResponseListener = new PlaylistRequestTask.AsyncResponse() {
                            @Override
                            public void onPlaylistRequestFinished(final PlaylistInfo result) {


                                if (result.valid) {

                                    HttpStatusCheckTask.AsyncResponse streamCheckResponseListener = new HttpStatusCheckTask.AsyncResponse() {
                                        @Override
                                        public void onStatusCheckFinished(HttpStatusCheckTask.HttpStatusCheckResult checkResult) {
                                            progressSpinner.setVisibility(View.GONE);

                                            IcyHeaderInfo icyHeaderInfo = (checkResult != null ? IcyHeaderReader.getHeaderInfos(checkResult.responseHeaders) : null);

                                            if (checkResult != null && checkResult.isSuccess()
                                                    && icyHeaderInfo != null
                                                    && icyHeaderInfo.getName() != null
                                                    && !icyHeaderInfo.getName().isEmpty()) {

                                                inputDescription.setText(icyHeaderInfo.getName());
                                            } else {

                                                // if no title is available from icy headers, use host name of stream url as fallback
                                                URL urlOfHostnameFallback;
                                                try {
                                                    urlOfHostnameFallback = new URL(result.streamUrl);
                                                } catch (MalformedURLException e) {
                                                    urlOfHostnameFallback = url;
                                                }

                                                String stationName = getStationName(description, result.description, urlOfHostnameFallback);
                                                inputDescription.setText(stationName);
                                            }
                                        }
                                    };

                                    // step 2: check stream url itself (extracted from playlist) to get the station name from icy headers
                                    new HttpStatusCheckTask(streamCheckResponseListener).execute(result.streamUrl);

                                } else {

                                    progressSpinner.setVisibility(View.GONE);
                                    showUrlErrorMessage(invalidUrlMessage);
                                }
                            }
                        };

                        // step 1: check if url is play list and extract a stream url from it
                        new PlaylistRequestTask(playListResponseListener).execute(urlString);

                    } else { // plain stream url

                        // fill description input field from icy-name header
                        HttpStatusCheckTask.AsyncResponse streamCheckResponseListener = new HttpStatusCheckTask.AsyncResponse() {
                            @Override
                            public void onStatusCheckFinished(HttpStatusCheckTask.HttpStatusCheckResult checkResult) {
                                progressSpinner.setVisibility(View.GONE);

                                IcyHeaderInfo icyHeaderInfo = (checkResult != null ? IcyHeaderReader.getHeaderInfos(checkResult.responseHeaders) : null);

                                if (checkResult != null && checkResult.isSuccess()
                                        && icyHeaderInfo != null
                                        && icyHeaderInfo.getName() != null
                                        && !icyHeaderInfo.getName().isEmpty()) {

                                    inputDescription.setText(icyHeaderInfo.getName());
                                }
                            }
                        };
                        new HttpStatusCheckTask(streamCheckResponseListener).execute(urlString);

                    }
                }
            }
        });
        return v;
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

    private RadioStation createRadioStation(String name, String streamUrl, int bitrate, int secondsToMute) {
        RadioStation station = new RadioStation();
        station.isUserDefinedStreamUrl = true;
        station.isOnline = true;
        station.name = name;
        station.stream = streamUrl;
        station.bitrate = bitrate;
        station.countryCode = ""; // empty string, otherwise invalid json
        station.muteDelayInMillis = 1000L * secondsToMute;
        return station;
    }

    private void persistAndDismissDialog(
            final AlertDialog dialog, RadioStreamDialogListener listener, String name,
            String urlString, int bitrate, int secondsToMute
    ) {
        RadioStation station = createRadioStation(name, urlString, bitrate, secondsToMute);

        // close dialog
        dialog.dismiss();

        listener.onRadioStreamSelected(station);
    }

}
