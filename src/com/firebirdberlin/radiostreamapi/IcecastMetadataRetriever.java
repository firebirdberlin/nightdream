package com.firebirdberlin.radiostreamapi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IcecastMetadataRetriever {

    public static String META_KEY_INTERVAL = "icy-metaint";
    public static String META_KEY_STREAM_TITLE = "StreamTitle";

    private static int READ_TIMEOUT = 10000;
    private static int CONNECT_TIMEOUT = 10000;

    public static IcecastMetadata retrieveMetadata(URL streamUrl) {


        InputStream stream = null;

        String metaTitle = null;
        IcyHeaderInfo headerInfos = null;

        try {

            URLConnection urlConnection = streamUrl.openConnection();

            urlConnection.setRequestProperty("Icy-MetaData", "1");
            urlConnection.setRequestProperty("Connection", "close");
            urlConnection.setRequestProperty("Accept", null);
            urlConnection.setRequestProperty("charset", "utf-8");
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);
            urlConnection.connect();

            int metaDataOffset = 0;
            Map<String, List<String>> headers = urlConnection.getHeaderFields();
            stream = urlConnection.getInputStream();

            // parse infos from icy* headers
            headerInfos = IcyHeaderReader.getHeaderInfos(headers);

            // check if there is meta data embedded in the stream
            if (headers.containsKey(META_KEY_INTERVAL)) {
                metaDataOffset = Integer.parseInt(headers.get(META_KEY_INTERVAL).get(0));
            }

            if (metaDataOffset == 0) {
                // no embedded data, but possible meta keys
                return new IcecastMetadata(headerInfos, null, false);
            }

            String metaDataString = readMetadata(stream, metaDataOffset);
            if (metaDataString != null) {
                Map<String, String> metadata = parseMetadata(metaDataString);

                /*
                for (String key : metadata.keySet()) {
                    Log.i(TAG, key + ":" + metadata.get(key));
                }
                */
                if (metadata != null && !metadata.isEmpty() && metadata.containsKey(IcecastMetadataRetriever.META_KEY_STREAM_TITLE)) {
                    metaTitle = metadata.get(IcecastMetadataRetriever.META_KEY_STREAM_TITLE);
                }
            }

            stream.close();
            stream = null;

        } catch (IOException e) {

        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }

        return new IcecastMetadata(headerInfos, metaTitle, true);
    }

    private static String readMetadata(InputStream stream, int metaDataOffset) throws IOException {

        if (metaDataOffset <= 0) {
            return null;
        }

        int b;
        int count = 0;
        int metaDataLength = 0;
        byte[] dataBuffer = null;
        while ((b = stream.read()) != -1) {
            count++;
            if (count == metaDataOffset + 1) {
                metaDataLength = b * 16;
                if (metaDataLength > 0) {
                    dataBuffer = new byte[metaDataLength];
                }
                break;
            }
        }

        if (metaDataLength <= 0) {
            return null;
        }

        // read meta data
        int i = 0;
        boolean dataComplete = false;
        while ((b = stream.read()) != -1) {

            if (i >= metaDataLength) {
                dataComplete = true;
                break;
            }

            dataBuffer[i] = (byte)b;

            i++;
        }

        if (!dataComplete) {
            return null;
        }

        // assume utf-8 encoded text in byte array
        String text = new String(dataBuffer, "UTF-8");
        return text;
    }

    private static Map<String, String> parseMetadata(String metaString) {
        Map<String, String> metadata = new HashMap();
        String[] metaParts = metaString.split(";");
        Pattern p = Pattern.compile("^([a-zA-Z]+)=\\'([^\\']*)\\'$");
        Matcher m;
        for (int i = 0; i < metaParts.length; i++) {
            m = p.matcher(metaParts[i]);
            if (m.find()) {
                metadata.put((String)m.group(1), (String)m.group(2));
            }
        }

        return metadata;
    }
}
