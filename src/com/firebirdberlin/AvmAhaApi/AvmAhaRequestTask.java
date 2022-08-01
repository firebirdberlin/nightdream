package com.firebirdberlin.AvmAhaApi;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.firebirdberlin.HttpReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.firebirdberlin.AvmAhaApi.models.AvmCredentials;
import com.firebirdberlin.AvmAhaApi.models.AvmAhaDevice;


public class AvmAhaRequestTask {
    private static String TAG = "AvmAhaRequestTask";
    private static int CONNECT_TIMEOUT = 10000;
    private static int READ_TIMEOUT = 10000;

    private static String AIN = "11630 0145542";
    private static String AIN_BULB = "13077 0044328-1";
    private final AsyncResponse delegate;

    private static String session_id = null;
    private static AvmCredentials credentials = null;
    private List<AvmAhaDevice> deviceList = new ArrayList<AvmAhaDevice>();

    public interface AsyncResponse {
        void onAhaRequestFinished();
        void onAhaDeviceListReceived(List<AvmAhaDevice> deviceList);
        void onAhaDeviceStateChanged(AvmAhaDevice device);
    }

    public AvmAhaRequestTask(AsyncResponse listener, AvmCredentials credentials) {
        this.delegate = listener;
        this.credentials = credentials;
    }

    public void fetchDeviceList() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> { //background thread
            if (!sessionIsValid()) login();
            List<AvmAhaDevice> devices = getDeviceList();
            handler.post(() -> { //like onPostExecute()
                delegate.onAhaDeviceListReceived(devices);
            });
        });
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
    }

    public void closeSession() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> { // background thread
            if (sessionIsValid()) logout();
        });
    }


    private boolean sessionIsValid() {
        return !(session_id == null || "0000000000000000".equals(session_id));
    }

    private boolean login() {
        if (sessionIsValid()) {
            logout();
        }
        URL url = getUrl("login_sid.lua", null);
        HashMap<String, String> response = new HashMap<>();
        InputStream inputStream = request(url);

        if (inputStream == null) {return false;}

        getValues(inputStream, response);

        HashMap<String, String> params = new HashMap<>();
        params.put("username", credentials.username);
        params.put("response", credentials.getSecret(response.get("Challenge")));
        url = getUrl("login_sid.lua", params);

        response.clear();

        inputStream = request(url);
        if (inputStream == null) {return false;}

        getValues(inputStream, response);

        this.session_id = response.get("SID");
        if (session_id == null || "0000000000000000".equals(session_id)) {
            return false;
        }
        return true;
    }

    private void logout() {
        HashMap<String, String> params = new HashMap<>();
        params.put("logout", "1");
        params.put("sid", this.session_id);

        URL url = getUrl("login_sid.lua", params);
        HashMap<String, String> response = new HashMap();
        InputStream inputStream = request(url);
        getValues(inputStream, response);
        this.session_id = null;
    }

    private List<AvmAhaDevice> getDeviceList() {

        HashMap<String, String> params = new HashMap();
        params.put("sid", session_id);
        params.put("ain", AIN);
        params.put("switchcmd", "getdevicelistinfos");

        URL url = getUrl("webservices/homeautoswitch.lua", params);
        InputStream inputStream = request(url);
        if (inputStream == null) {
            return null;
        }
        return parseDeviceList(inputStream);
    }

    private boolean toggleBulb(String ain, String newState) {
        HashMap<String, String> params = new HashMap();
        params.put("switchcmd", "setsimpleonoff");
        params.put("sid", session_id);
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
        if (input == null) return input;
        return input.replace(" ", "").replace("\n", "");
    }

    private boolean toggleSwitch(String ain, String newState) {
        HashMap<String, String> params = new HashMap();
        params.put("sid", session_id);
        params.put("ain", ain);
        switch (newState) {
            case AvmAhaDevice.STATE_ON:
                params.put("switchcmd", "setswitchon");
                break;
            case AvmAhaDevice.STATE_OFF:
                params.put("switchcmd", "setswitchoff");
                break;
            case AvmAhaDevice.STATE_TOGGLE:
                params.put("switchcmd", "setswitchtoggle");
                break;
        }

        URL url = getUrl("webservices/homeautoswitch.lua", params);
        HashMap<String, String> response = new HashMap();
        InputStream inputStream = request(url);
        String responseText = getResponseText(inputStream);
        Log.d(TAG, responseText);

        return newState.equals(strip(responseText));
    }

    private void getSwitchState(String ain) {
        // ?ain=<ain>&switchcmd=<cmd>&sid=<sid>
        HashMap<String, String> params = new HashMap();
        params.put("switchcmd", "getswitchstate");
        params.put("sid", session_id);
        params.put("ain", ain);
        URL url = getUrl("webservices/homeautoswitch.lua", params);
        InputStream inputStream = request(url);
        if (inputStream == null) return;
        String responseText = getResponseText(inputStream);
        Log.d(TAG, responseText);
    }

    private URL getUrl(String endpoint, HashMap<String, String> params) {

        String[] separated = endpoint.split("/");

        Uri.Builder builder = Uri.parse(credentials.host).buildUpon();
        for (String path : separated) {
                builder.appendPath(path);
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
            return null;
        }
    }

    private InputStream request(URL url) {
        Log.d(TAG, "request(" + url.toString() + ")");
        String responseText = "";
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", "NightClock/com.firebirdberlin.nightdream");
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);

            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, String.valueOf(responseCode));
            if (responseCode == 200) {
                return urlConnection.getInputStream();
            }
            urlConnection.disconnect();
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Http Timeout");
        } catch (UnknownHostException e) {
            Log.e(TAG, "Unknown host");
        } catch (ConnectException e) {
            Log.e(TAG, "Connect exception");
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e), e);
            e.printStackTrace();
        }
        return null;
    }

    private String getResponseText(InputStream inputStream) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            return sb.toString();
        } catch (IOException e) {
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
                if (eventType == XmlPullParser.START_DOCUMENT) {
                    //Log.d(TAG, "Start document");
                } else if (eventType == XmlPullParser.START_TAG) {
                    key = xpp.getName();
                    //Log.d(TAG, "Start tag " + xpp.getName());
                } else if (eventType == XmlPullParser.END_TAG) {
                    key = null;
                    //Log.d(TAG, "End tag " + xpp.getName());
                } else if (eventType == XmlPullParser.TEXT) {
                    map.put(key, xpp.getText());
                    //Log.d(TAG, "Text " + xpp.getText());
                }
                eventType = xpp.next();
            }
            //Log.d(TAG, "End document");
            return true;
        } catch (XmlPullParserException | IOException e) {
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
                                    int bitmask = Integer.parseInt(value);
                                    device.functionbitmask = bitmask;
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
            return null;
        }
    }
}
