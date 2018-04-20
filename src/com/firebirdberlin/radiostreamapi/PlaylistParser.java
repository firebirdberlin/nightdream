package com.firebirdberlin.radiostreamapi;

import android.util.Log;

import com.firebirdberlin.radiostreamapi.models.PlaylistInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaylistParser {

    private static final String M3U_EXT_FILE_HEADER = "#EXTM3U";
    private static final String M3U_EXT_INFO_PREFIX = "#EXTINF:";
    private static final String PLS_FILE_HEADER = "[playlist]";
    private static final String PLS_FILE_FILE1_PREFIX = "File1=";
    private static final String PLS_FILE_TITLE1_PREFIX = "Title1=";
    private static final Integer[] USUAL_BITRATES = new Integer[] { 64, 96, 128, 192, 256};

    private static final String ASHX_CONTENT_TYPE_PLAIN = "audio/x-mpegurl";
    private static final String ASHX_CONTENT_TYPE_JSON = "application/json";
    private static final Pattern URL_PATTERN =
            Pattern.compile("https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");


    private static String TAG = "NightDream.PlaylistParser";
    private static int READ_TIMEOUT = 3000;
    private static int CONNECT_TIMEOUT = 3000;

    public static boolean isPlaylistUrl(URL url) {
        return (getPlaylistFormat(url.getPath()) != null);
    }

    public static boolean isPlaylistUrl(String url) {
        return (getPlaylistFormat(url) != null);
    }

    public static PlaylistInfo parsePlaylistUrl(String playlistUrl) {

        HttpURLConnection urlConnection = null;
        try {

            URL url = new URL(playlistUrl);
            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setRequestMethod("GET");
            urlConnection.setReadTimeout(READ_TIMEOUT);
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);

            urlConnection.setDoOutput(true);

            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            String contentType = urlConnection.getContentType();
            if ( responseCode == 200 ) {
                List<String> lines = getResponseLines(url.openStream());
                if (lines == null || lines.isEmpty()) {
                    return erroneousPlaylist(PlaylistInfo.Error.INVALID_CONTENT);
                }
                PlaylistInfo.Format format = getPlaylistFormat(playlistUrl);
                if (format == null) {
                    return erroneousPlaylist(PlaylistInfo.Error.UNSUPPORTED_FORMAT);
                }
                if (format == PlaylistInfo.Format.M3U) {
                    return parseM3U(lines);
                } else if (format == PlaylistInfo.Format.PLS) {
                    return parsePLS(lines);
                } else if (format == PlaylistInfo.Format.ASHX && isASHXContentType(contentType)) {
                    return parseASHX(lines);
                } else {
                    return erroneousPlaylist(PlaylistInfo.Error.UNSUPPORTED_FORMAT);
                }
            } else {
                Log.e(TAG, "status code " + responseCode);
            }

        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Http Timeout");
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
           if (urlConnection != null) {
               urlConnection.disconnect();
           }
        }

        return erroneousPlaylist(PlaylistInfo.Error.UNREACHABLE_URL);
    }

    public static boolean checkStreamURLAvailability(String urlString) {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setReadTimeout(READ_TIMEOUT);
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);

            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, "status code " + responseCode);
            return ( responseCode == 200 );
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return false;
    }

    private static PlaylistInfo erroneousPlaylist(PlaylistInfo.Error error) {
        PlaylistInfo p = new PlaylistInfo();
        p.valid = false;
        p.error = error;
        return p;
    }

    private static List<String> getResponseLines(InputStream inputStream) throws IOException {
        List<String> lines = new ArrayList<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = br.readLine()) != null) {
            lines.add(line);
        }
        br.close();
        return lines;
    }

    private static PlaylistInfo.Format getPlaylistFormat(String url) {
        String sLower = url.toLowerCase();
        if (sLower.endsWith(".m3u")) {
            return PlaylistInfo.Format.M3U;
        } else if (sLower.endsWith(".pls")) {
            return PlaylistInfo.Format.PLS;
        } else if (sLower.contains(".ashx")) {
            return PlaylistInfo.Format.ASHX;
        }
        return null;
    }

    private static PlaylistInfo parseM3U(List<String> lines) {

        if (lines.isEmpty()) {
            return null;
        }

        boolean extendedFormat = lines.get(0).startsWith(M3U_EXT_FILE_HEADER);
        if (extendedFormat) {
            int i = 0;
            for (String line : lines) {
                //find first occurrence of a pair of lines, where the first line starts with #EXTINF and the second with a valid stream url
                if (line.startsWith(M3U_EXT_INFO_PREFIX)) {
                    if (i < lines.size() - 1) {
                        String urlString = lines.get(i + 1);
                        if (isValidUrl(urlString)) {
                            PlaylistInfo p = new PlaylistInfo();
                            p.streamUrl = urlString;
                            p.format = PlaylistInfo.Format.M3U;

                            String description = m3UExtInfoDescription(line);
                            if (description != null && !description.isEmpty()) {
                                p.description = description;
                            }

                            p.bitrateHint = bitrateFromUrl(urlString);

                            return p;
                        }
                    }
                }
                i++;
            }
        } else {
            // find first line beginning with http:// or https:// and use this as stream url
            for (String line : lines) {
                if (isValidUrl(line)) {
                    PlaylistInfo p = new PlaylistInfo();
                    p.streamUrl = line;
                    p.format = PlaylistInfo.Format.M3U;
                    return p;
                }
            }
        }

        return null;
    }

    private static boolean isValidUrl(String s) {
        if (s.startsWith("http://") || s.startsWith("https://")) {
            try {
                String urlString = s.trim();
                new URL(urlString);
               return true;
            } catch (MalformedURLException ignored) {

            }
        }
        return false;
    }

    /**
     * find bitrate hints in a url
     */
    private static Integer bitrateFromUrl(String url) {
        for (Integer bitrate : USUAL_BITRATES) {
            if (url.indexOf("/" + String.valueOf(bitrate) + "/") > 0) {
                return bitrate;
            }
        }
        return null;
    }

    private static String m3UExtInfoDescription(String line) {
        if (line.length() > M3U_EXT_INFO_PREFIX.length()) {
            //get part after first comma, skipping runtime, which is usually -1 for streams
            int offsetDescription = line.indexOf(',');
            if (offsetDescription > 0 && line.length() > offsetDescription + 1) {
                return line.substring(offsetDescription + 1);
            }
        }
        return null;
    }

    private static PlaylistInfo parsePLS(List<String> lines) {

        if (lines.isEmpty()) {
            return null;
        }

        boolean valid = lines.get(0).startsWith(PLS_FILE_HEADER);
        if (valid) {

            String streamUrl = null;
            String streamTitle = null;
            Integer bitrateHint = null;
            for (String line : lines) {
                //find File 1 and Title1
                if (line.startsWith(PLS_FILE_FILE1_PREFIX)) {
                    if (line.length() > PLS_FILE_FILE1_PREFIX.length()) {
                        String urlString = line.substring(PLS_FILE_FILE1_PREFIX.length());
                        if (isValidUrl(urlString)) {
                            streamUrl = urlString;
                            bitrateHint = bitrateFromUrl(urlString);
                        }
                    }
                }

                if (line.startsWith(PLS_FILE_TITLE1_PREFIX)) {
                    if (line.length() > PLS_FILE_TITLE1_PREFIX.length()) {
                        String title = line.substring(PLS_FILE_TITLE1_PREFIX.length()).trim();
                        if (!title.isEmpty()) {
                            streamTitle = title;
                        }
                    }
                }
            }

            if (streamUrl != null) {
                PlaylistInfo p = new PlaylistInfo();
                p.streamUrl = streamUrl;
                p.format = PlaylistInfo.Format.PLS;

                if (streamTitle != null) {
                    p.description = streamTitle;
                }
                p.bitrateHint = bitrateHint;

                return p;
            }
        }

        return null;
    }

    private static boolean isASHXContentType(String contentType) {
        return (contentType != null &&
                (contentType.toLowerCase().contains(ASHX_CONTENT_TYPE_PLAIN) ||
                        contentType.toLowerCase().contains(ASHX_CONTENT_TYPE_JSON)));
    }

    private static PlaylistInfo parseASHX(List<String> lines) {

        String protoHttp = "http://";
        String protoHttps = "https://";

        String streamUrl = null;
        for (String line : lines) {
            if (line.contains(protoHttp) || line.contains(protoHttps)) {
                Matcher matcher = URL_PATTERN.matcher(line);
                if (matcher.find()) {
                    streamUrl = matcher.group();
                    break;
                }
            }
        }

        if (streamUrl == null || streamUrl.isEmpty()) {
            return null;
        }

        PlaylistInfo p = new PlaylistInfo();
        p.streamUrl = streamUrl;
        p.format = PlaylistInfo.Format.ASHX;
        return p;
    }
}
