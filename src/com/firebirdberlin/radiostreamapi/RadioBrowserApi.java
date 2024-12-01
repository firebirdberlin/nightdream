package com.firebirdberlin.radiostreamapi;

import android.net.Uri;
import android.util.Log;

import com.firebirdberlin.radiostreamapi.models.Country;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class RadioBrowserApi {

    private static String TAG = "RadioBrowserApi";

    private static final String COUNTRIES_CACHE_FILE = "radioBrowserCountries.json";

    private static int MAX_NUM_RESULTS = 100;
    private static int READ_TIMEOUT = 10000;
    private static int CONNECT_TIMEOUT = 10000;

    private static String getRandomServer() {
        Log.d(TAG, "getRandomServer()");
        List<String> servers = new ArrayList<>();

        try {
            // add all round robin servers one by one to select them separately
            InetAddress[] list = InetAddress.getAllByName("all.api.radio-browser.info");
            for (InetAddress item : list) {
                servers.add(item.getCanonicalHostName());
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        if (servers.size() > 0) {
            return servers.get(new Random().nextInt(servers.size()));
        }
        else {
            return "";
        }
    }

    public static List<RadioStation> fetchStations(String queryString, String countryCode) {
        List<RadioStation> stationList = new ArrayList<RadioStation>();
        int responseCode = 0;
        String response = "";
        String responseText = "";

        try {
            String urlstring = getStationSearchPath();
            Log.d(TAG, "Requesting " + urlstring);
            URL url = new URL(urlstring);

            // this should escape the queryString properly and guarantee a valid json string
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", queryString);
            jsonObject.put("limit", MAX_NUM_RESULTS);
            if (countryCode != null && !countryCode.isEmpty()) {
                jsonObject.put("countrycode", countryCode);
            }
            String postDataString = jsonObject.toString();
            byte[] postDataBytes = postDataString.getBytes(Charset.forName("UTF-8"));
            int postDataLength = postDataBytes.length;

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);
            //enable post request
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("charset", "utf-8");
            urlConnection.setRequestProperty("User-Agent", "NightDream/2.8 - com.firebirdberlin.nightdream");
            urlConnection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
            urlConnection.getOutputStream().write(postDataBytes);

            response = urlConnection.getResponseMessage();
            responseCode = urlConnection.getResponseCode();
            if ( responseCode == 200 ) {
                responseText = getResponseText(urlConnection.getInputStream());
            }
            urlConnection.disconnect();
        }
        catch (SocketTimeoutException e) {
            Log.e(TAG, "Http Timeout");
            return stationList;
        }
        catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            e.printStackTrace();
        }

        Log.i(TAG, " >> response " + response);
        if (responseCode == 200) {
            Log.i(TAG, " >> responseText " + responseText);
            try {
                JSONArray jsonArray = new JSONArray(responseText);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonStation = jsonArray.getJSONObject(i);
                    RadioStation station = new RadioStation();
                    station.uuid = jsonStation.getString("stationuuid");
                    station.name = jsonStation.getString("name");
                    station.favIcon = jsonStation.getString("favicon");
                    station.countryCode = jsonStation.getString("countrycode");
                    station.stream = jsonStation.getString("url");
                    station.bitrate = jsonStation.getLong("bitrate");
                    station.isOnline = jsonStation.getLong("lastcheckok") == 1L;
                    if (station.isOnline) {
                        stationList.add(station);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, " >> responseCode " + String.valueOf(responseCode));
        }
        return stationList;
    }

    private static String getStationSearchPath() {
        Uri.Builder builder = getPathBuilder("stations");
        builder.appendPath("search");
        return builder.build().toString();
    }

    private static String getCountriesPath() {
        Uri.Builder builder = getPathBuilder("countrycodes");
        return builder.build().toString();
    }

    private static Uri.Builder getPathBuilder(String endpoint) {
        Log.d(TAG, "getPathBuilder()");
        String server = getRandomServer();
        Log.d(TAG, server);
        return Uri.parse("https://" + server).buildUpon()
                .appendPath("json")
                .appendPath(endpoint);
    }

    private static String getResponseText(InputStream inputStream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line+"\n");
        }
        br.close();
        return sb.toString();
    }

    public static List<Country> fetchCountries(File cacheDir) {

        List<Country> countries;
        int responseCode = 0;
        String response = "";
        String responseText = "";

        // first try to read from cache
        try {
            countries = readCountriesFromCache(cacheDir);
            if (countries != null && countries.size() > 0) {
                Log.i(TAG, "read countries from cache");
                return countries;
            }
        } catch (Throwable t) {
            Log.e(TAG, "error reading countries from cache: " + t.getMessage());
        }
        countries = new ArrayList<>();

        String urlstring = getCountriesPath();
        Log.i(TAG, "requesting " + urlstring);
        try {
            URL url = new URL(urlstring);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);
            response = urlConnection.getResponseMessage();
            responseCode = urlConnection.getResponseCode();
            if ( responseCode == 200 ) {
                responseText = getResponseText(urlConnection.getInputStream());
            }
            urlConnection.disconnect();
        }
        catch (SocketTimeoutException e) {
            Log.e(TAG, "Http Timeout");
            return countries;
        }
        catch (UnknownHostException e) {
            Log.e(TAG, "Unknown host");
            return countries;
        }
        catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e), e);
            e.printStackTrace();
        }

        Log.i(TAG, " >> response " + response);
        if (responseCode == 200) {
            Log.i(TAG, " >> responseText " + responseText);
            try {
                countries = decodeCountriesJsonResponse(responseText);

                if (countries.size() > 0) {
                    // retrieved countries successfully, thus save it to cache (just the response text)
                    try {
                        writeCountryResponseToCache(responseText, cacheDir);
                    } catch (IOException e) {
                        e.printStackTrace();
                        //TODO
                        // 1. no caching possible for some reason, so disable fetchCountries at all to save api requests
                        // 2. fall back to a static list of countries provided within the app to save api requests
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                //TODO resilience (count errors and if error threshold is reached (API service broken), disable fetchCountries for some time to save api requests)
            }

        } else {
            Log.w(TAG, " >> responseCode " + String.valueOf(responseCode));
        }
        return countries;
    }

    private static List<Country> decodeCountriesJsonResponse(String responseText) throws JSONException {
        List<Country> countries = new ArrayList<>();

        JSONArray jsonArray = new JSONArray(responseText);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonCountry = jsonArray.getJSONObject(i);

            Country country = new Country();
            country.name = jsonCountry.getString("name");
            country.countryCode = country.name; // there is no proper name
            country.region = null;
            country.subRegion = null;
            country.setNameFromIsoCode();
            countries.add(country);
        }

        return countries;
    }

    private static File getCountryCacheFile(File cacheDir) {
        return new File(cacheDir, COUNTRIES_CACHE_FILE);
    }

    private static Date getCacheExpireDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date result = cal.getTime();
        return result;
    }

    private static List<Country> readCountriesFromCache(File cacheDir) throws JSONException {
        String responseText = readCountryResponseFromCache(cacheDir);
        if (responseText != null) {
            List<Country> countries = decodeCountriesJsonResponse(responseText);
            return countries;
        }
        return null;
    }

    private static String readCountryResponseFromCache(File cacheDir){
        File cacheFile = getCountryCacheFile(cacheDir);
        String response = null;

        if (cacheFile.exists()) {

            // check if cache file expired
            Date modifiedDate = new Date(cacheFile.lastModified());
            Date expireDate = getCacheExpireDate();
            long timeDiff = modifiedDate.getTime() - expireDate.getTime();
            Log.i(TAG, "cache file will expire in " + timeDiff + " millis");
            boolean expired = modifiedDate.before(expireDate);
            if (expired) {
                Log.i(TAG, "cache file expired");
                return null;
            }

            InputStream is = null;

            try {
                is = new FileInputStream(cacheFile);
                response = getResponseText(is);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // nothing to do here
                    }
                }
            }
        }
        return response;
    }

    private static void writeCountryResponseToCache(String responseText, File cacheDir) throws IOException {
        BufferedWriter out = null;
        try {
            File cacheFile = getCountryCacheFile(cacheDir);
            Log.i(TAG, "write country response to cache file " + cacheFile.getAbsolutePath());
            //truncates file if already exists
            out = new BufferedWriter(new FileWriter(cacheFile));
            out.write(responseText);
            out.close();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {}
            }
        }
    }
}
