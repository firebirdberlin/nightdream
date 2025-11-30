/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.firebirdberlin.AvmAhaApi;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.firebirdberlin.AvmAhaApi.models.AvmAhaDevice;
import com.firebirdberlin.AvmAhaApi.models.AvmCredentials;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class AvmAhaRequestTask {
    private static final String TAG = "AvmAhaRequestTask";
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 10000;

    private final AsyncResponse delegate;

    private static String session_id = null;
    private static AvmCredentials credentials = null;
    private final List<AvmAhaDevice> deviceList = new ArrayList<>();

    private String errorMessage = null;

    public interface AsyncResponse {
        // void onAhaRequestFinished(); // Method was unused
        void onAhaDeviceListReceived(List<AvmAhaDevice> deviceList);
        void onAhaDeviceStateChanged(AvmAhaDevice device);
        void onAhaConnectionError(String message);
    }

    public AvmAhaRequestTask(AsyncResponse listener, AvmCredentials credentials) {
        this.delegate = listener;
        // Removed static access for instance variable
        AvmAhaRequestTask.credentials = credentials;
    }

    public void fetchDeviceList() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> { //background thread
            if (!sessionIsValid()) login();
            List<AvmAhaDevice> devices = getDeviceList();
            handler.post(() -> { // main thread
                if (errorMessage != null) {
                    delegate.onAhaConnectionError(errorMessage);
                } else {
                    delegate.onAhaDeviceListReceived(devices);
                }
            });
        });
        // No explicit shutdown for this single-use executor, as per common patterns for short-lived tasks.
        // If this task were to be reused, consider a more robust lifecycle management.
    }

    public void setSimpleOnOff(AvmAhaDevice device, String newState) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> { // background thread
            if (!sessionIsValid()) login();
            boolean result = toggleBulb(device.ain, newState);
            Log.i(TAG, "new_state: " + result);
            if (result) {
                device.state = newState;
            }
            handler.post(() -> { // main thread
                delegate.onAhaDeviceStateChanged(device);
            });
        });
        // No explicit shutdown for this single-use executor.
    }

    public void closeSession() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> { // background thread
            if (sessionIsValid()) logout();
        });
        // No explicit shutdown for this single-use executor.
    }


    private boolean sessionIsValid() {
        return !(session_id == null || "0000000000000000".equals(session_id));
    }

    private void login() {
        if (sessionIsValid()) {
            logout();
        }
        URL url = getUrl("login_sid.lua", null);
        HashMap<String, String> response = new HashMap<>();
        InputStream inputStream = request(url);

        if (inputStream == null) {return;}

        getValues(inputStream, response);

        HashMap<String, String> params = new HashMap<>();
        params.put("username", credentials.username);
        params.put("response", credentials.getSecret(response.get("Challenge")));
        url = getUrl("login_sid.lua", params);

        response.clear();

        inputStream = request(url);
        if (inputStream == null) {return;}

        getValues(inputStream, response);

        // Use static session_id directly
        session_id = response.get("SID");
        if (session_id == null || "0000000000000000".equals(session_id)) {
            errorMessage = "Authentication error";
        }
    }

    private void logout() {
        HashMap<String, String> params = new HashMap<>();
        params.put("logout", "1");
        params.put("sid", session_id); // Use static session_id directly

        URL url = getUrl("login_sid.lua", params);
        HashMap<String, String> response = new HashMap<>(); // Corrected raw type warning
        InputStream inputStream = request(url);
        getValues(inputStream, response);
        session_id = null;
    }

    private List<AvmAhaDevice> getDeviceList() {

        HashMap<String, String> params = new HashMap<>();
        params.put("sid", session_id); // Use static session_id directly
        params.put("switchcmd", "getdevicelistinfos");

        URL url = getUrl("webservices/homeautoswitch.lua", params);
        InputStream inputStream = request(url);
        if (inputStream == null) {
            return null;
        }
        return parseDeviceList(inputStream);
    }

    private boolean toggleBulb(String ain, String newState) {
        HashMap<String, String> params = new HashMap<>();
        params.put("switchcmd", "setsimpleonoff");
        params.put("sid", session_id); // Use static session_id directly
        params.put("ain", ain);
        params.put("onoff", newState);

        URL url = getUrl("webservices/homeautoswitch.lua", params);
        InputStream inputStream = request(url);
        if (inputStream == null) return false;
        String responseText = getResponseText(inputStream);
        Log.d(TAG, "Response: '" + strip(responseText) + "'");

        return newState.equals(strip(responseText));
    }

    private String strip(String input) {
        // Input is checked for nullity at the call site or handled by the caller.
        // The warning "Value 'input' is always 'null'" was likely a static analysis artifact
        // or referred to a context not present in the current method's scope.
        // Assuming 'input' can be non-null based on usage.
        if (input == null) return null;
        return input.replace(" ", "").replace("\n", "");
    }

    // Private method 'toggleSwitch' is unused and has been removed.
    // Private method 'getSwitchState' is unused and has been removed.

    private URL getUrl(String endpoint, HashMap<String, String> params) {

        if (credentials == null || credentials.host == null || credentials.host.isEmpty() ) {
            return null;
        }
        String[] separated = endpoint.split("/");

        Uri.Builder builder = Uri.parse(credentials.host).buildUpon();
        for (String path : separated) {
            if (!path.isEmpty()) { // Avoid adding empty paths
                 builder.appendPath(path);
            }
        }

        if (params != null) {
            for(HashMap.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                builder = builder.appendQueryParameter(key, value);
            }
        }

        String url = builder.build().toString();
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Malformed URL: " + url, e);
            errorMessage = "Invalid host configuration"; // Set a more specific error message
            return null;
        }
    }

    private InputStream request(URL url) {
        if (url == null) {
            return null;
        }
        Log.d(TAG, "request(" + url.toString() + ")");
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", "NightClock/com.firebirdberlin.nightdream");
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);

            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, String.valueOf(responseCode));
            if (responseCode == HttpURLConnection.HTTP_OK) { // Use constant for HTTP OK
                return urlConnection.getInputStream();
            }
            urlConnection.disconnect();
        } catch (SocketTimeoutException e) {
            errorMessage = "Connection timed out";
            Log.e(TAG, "Http Timeout", e);
        } catch (UnknownHostException e) {
            errorMessage = "Unknown host: " + url.getHost();
            Log.e(TAG, "Unknown host", e);
        } catch (ConnectException e) {
            errorMessage = "Connection refused";
            Log.e(TAG, "Connect exception", e);
        } catch (IOException e) { // Catch broader IOException for network issues
            errorMessage = "Network error: " + e.getMessage();
            Log.e(TAG, "Network error", e);
        } catch (Exception e) {
            errorMessage = "An unexpected error occurred: " + e.getMessage();
            Log.e(TAG, "Unexpected error", e);
            // Removed printStackTrace() as requested.
        }
        return null;
    }

    private String getResponseText(InputStream inputStream) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) { // Use try-with-resources
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            Log.e(TAG, "Error reading response stream", e);
            errorMessage = "Error reading response";
            return "";
        }
    }

    private boolean getValues(InputStream inputStream, HashMap<String, String> map) {
        if (inputStream == null) {
            return false;
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(new InputStreamReader(inputStream));
            int eventType = xpp.getEventType();
            String key = null;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                // Removed empty if block for START_DOCUMENT
                if (eventType == XmlPullParser.START_TAG) {
                    key = xpp.getName();
                } else if (eventType == XmlPullParser.END_TAG) {
                    key = null;
                } else if (eventType == XmlPullParser.TEXT) {
                    if (key != null) { // Ensure key is not null before putting into map
                        map.put(key, xpp.getText());
                    }
                }
                eventType = xpp.next();
            }
            return true;
        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "Error parsing XML", e);
            errorMessage = "Error parsing response";
            return false;
        }
    }

    private List<AvmAhaDevice> parseDeviceList(InputStream inputStream) {
        deviceList.clear();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(new InputStreamReader(inputStream));
            int eventType = xpp.getEventType();
            AvmAhaDevice device = null;
            String tag = null;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    tag = xpp.getName();
                    if ("device".equals(xpp.getName()) ) {
                        device = new AvmAhaDevice();
                        for (int i = 0; i < xpp.getAttributeCount(); i++) {
                            String name = xpp.getAttributeName(i);
                            String value = xpp.getAttributeValue(i);
                            Log.d(TAG, name + " = " + value);
                            switch (name) {
                                case "identifier":
                                    device.ain = value;
                                    break;
                                case "manufacturer":
                                    device.manufacturer = value;
                                    break;
                                case "productname":
                                    device.productname = value;
                                    break;
                                case "functionbitmask":
                                    // Removed redundant 'bitmask' variable.
                                    device.functionbitmask = Integer.parseInt(value);
                                    break;
                            }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if ("device".equals(xpp.getName()) && device != null) {
                        Log.d(TAG, "adding device " + device.toString());
                        deviceList.add(device);
                        device = null;
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    if (device != null && tag != null) {
                        switch (tag) {
                            case "name":
                                device.name = xpp.getText();
                                break;
                            case "state":
                                device.state = xpp.getText();
                                break;
                            case "present":
                                device.present = xpp.getText();
                                break;
                            case "level":
                                device.level = xpp.getText();
                                break;
                        }
                    }
                }
                eventType = xpp.next();
            }
            Log.d(TAG, "found " + deviceList.size() + " devices");
            return deviceList;
        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "Error parsing device list", e);
            errorMessage = "Error parsing device list";
            return null;
        }
    }
}
