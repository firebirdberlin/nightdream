package com.firebirdberlin.radiostreamapi;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.AdapterView;
import android.widget.ListView;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.radiostreamapi.models.Country;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

import org.json.JSONException;



public class RadioStreamPreference extends DialogPreference
                                   implements StationRequestTask.AsyncResponse, CountryRequestTask.AsyncResponse {
    private final static String TAG = "NightDream.RadioStreamPreference";
    private Context mContext = null;
    private EditText queryText = null;
    private String selectedStream;
    private ArrayList<RadioStation> stations = new ArrayList<RadioStation>();
    private ArrayList<String> stationTexts = new ArrayList<String>();
    private ListView stationListView;
    private Spinner countrySpinner;
    private TextView noResultsText;
    private ContentLoadingProgressBar spinner;
    private Button searchButton;
    private Map<String, String> countryNameToCodeMap = null;

    public RadioStreamPreference(Context ctx) {
        this(ctx, null);
        mContext = getContext();
    }

    public RadioStreamPreference(Context ctx, AttributeSet attrs) {
        this(ctx, attrs, android.R.attr.dialogPreferenceStyle);
        mContext = getContext();
        setValuesFromXml(attrs);
    }

    public RadioStreamPreference(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
        setValuesFromXml(attrs);
    }

    private void setValuesFromXml(AttributeSet attrs) {
        mContext = getContext();
        setNegativeButtonText(android.R.string.cancel);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setPositiveButton(null, null);
    }

    private static String getAttributeStringValue(AttributeSet attrs, String namespace,
                                                  String name, String defaultValue) {
        String value = attrs.getAttributeValue(namespace, name);
        if (value == null) value = defaultValue;

        return value;
    }

    @Override
    protected View onCreateDialogView() {
        updateDisplayedRadioStationTexts();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, stationTexts);

        LayoutInflater inflater = (LayoutInflater)
            getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View v = inflater.inflate(R.layout.radio_stream_dialog, null);

        final RadioStreamPreference context = this;
        queryText = ((EditText) v.findViewById(R.id.query_string));
        spinner = (ContentLoadingProgressBar) v.findViewById(R.id.progress_bar);
        stationListView = (ListView) v.findViewById(R.id.radio_stream_list_view);
        countrySpinner = (Spinner) v.findViewById(R.id.countrySpinner);
        noResultsText = (TextView) v.findViewById(R.id.no_results);
        noResultsText.setVisibility(View.GONE);

        queryText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                    startSearch();

                    return true;
                }
                return false;
            }
        });

        stationListView.setAdapter(adapter);
        stationListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RadioStation station = stations.get(position);
                //persistString(station.stream);
                persistRadioStation(station);
                setSummary(station.stream);
                notifyChanged();
                getDialog().dismiss();

            }
        });

        countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                //Log.i(TAG, "country changed");
                startSearch();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }

        });
        searchButton = ((Button) v.findViewById(R.id.start_search));
        searchButton.setEnabled(false);
        searchButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startSearch();
            }
        });

        queryText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                searchButton.setEnabled(queryText.getText().length() > 0 );
            }
        });

        initCountrySpinner();

        return v;
    }

    private void startSearch() {

        String query = queryText.getText().toString().trim();
        if (query == null || query.isEmpty()) {
            return;
        }

        spinner.show();
        stationListView.setVisibility(View.GONE);
        noResultsText.setVisibility(View.GONE);

        String country = getSelectedCountry();
        new StationRequestTask(this).execute(query, country);

        InputMethodManager imm =
                (InputMethodManager) queryText.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(queryText.getWindowToken(), 0);
        searchButton.setEnabled(false);
    }

    @Override
    public void onRequestFinished(List<RadioStation> stations){
        this.stations.clear();
        this.stations.addAll(stations);
        updateDisplayedRadioStationTexts();
        //Log.i(TAG, String.format("Request finished with %d entries", this.stations.size()));
        ((ArrayAdapter) stationListView.getAdapter()).notifyDataSetChanged();
        spinner.hide();
        stationListView.setVisibility((stations.size() == 0) ? View.GONE : View.VISIBLE);
        noResultsText.setVisibility((stations.size() == 0) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCountryRequestFinished(List<Country> countries) {
        //Log.i(TAG, "found " + countries.size() + " countries.");
        // sort countries alphabetically
        Collections.sort(countries, new Comparator<Country>() {
            @Override
            public int compare(Country lhs, Country rhs) {
                if (lhs.name == null) {
                    return -1;
                }
                if (rhs.name == null) {
                    return 1;
                }
                return lhs.name.compareTo(rhs.name);
            }
        });

        updateCountryNameToCodeMap(countries);
        updateCountrySpinner(countries);
    }


    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        this.stations.clear();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }

    @Override
    public CharSequence getSummary () {
        RadioStation station = getPersistedRadioStation();
        if (station != null && station.name != null && !station.name.isEmpty()) {

            final boolean hasCountry = (station.countryCode != null);
            final boolean hasBitrate = (station.bitrate > 0);
            String summary;
            if (hasCountry && hasBitrate) {
                summary = String.format("%s (%s, %d kbit/s)", station.name, station.countryCode, station.bitrate);
            } else if (hasBitrate) {
                summary = String.format("%s (%d kbit/s)", station.name, station.bitrate);
            } else if (hasCountry){
                summary = String.format("%s (%s)", station.name, station.countryCode);
            } else {
                summary = String.format("%s", station.name);
            }

            return summary;
        } else {
            return super.getSummary();
        }
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setTitle(getTitle());
        setSummary(getPersistedString((String) defaultValue));
    }

    private void initCountrySpinner() {
        Log.i(TAG, "starting country search");
        new CountryRequestTask(this, mContext).execute();
    }

    private void updateCountryNameToCodeMap(List<Country> countries) {
        countryNameToCodeMap = new HashMap<String, String>();
        for (Country c : countries) {
            countryNameToCodeMap.put(c.name, c.countryCode);
        }

    }

    private void updateCountrySpinner(List<Country> countries) {


        RadioStation persistedRadioStation = getPersistedRadioStation();
        String persistedCountryCode = persistedRadioStation != null ? persistedRadioStation.countryCode : null;
        Log.i(TAG, "found persisted station: " + (persistedRadioStation !=  null ? persistedRadioStation.toString() : "null"));


        List<String> preferredCountryCodes = getPreferredCountryCodes();

        List<String> countryList = new ArrayList<String>();

        // first add empty entry meaning "any country"
        //countryList.add("All countries");
        //empty string until localized
        countryList.add("");

        int selectedItemIndex = -1;

        // add preferred countries first
        if (preferredCountryCodes != null && !preferredCountryCodes.isEmpty()) {

            for (Country c : countries) {
                int numFound = 0;
                for (String preferredCountry : preferredCountryCodes) {
                    if (c.countryCode != null && c.countryCode.equals(preferredCountry)) {
                        countryList.add(c.name);
                        numFound++;

                        // if radio station is already configured selects its country as default
                        if (persistedCountryCode != null && c.countryCode.equals(persistedRadioStation.countryCode)) {
                            selectedItemIndex = countryList.size() - 1;
                        }
                    }
                }

                if (numFound == preferredCountryCodes.size()) {
                    break;
                }
            }
        }


        // now add all countries (including preferred country, so they are duplicates, but no problem)
        for (Country c : countries) {
            countryList.add(c.name);

            // if radio station is already configured selects its country as default
            if (persistedCountryCode != null && c.countryCode.equals(persistedRadioStation.countryCode)) {
                selectedItemIndex = countryList.size() - 1;
            }
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_item, countryList);

        // select as default: first preferred country, and "any country"
        if (selectedItemIndex == -1) {
            selectedItemIndex = !preferredCountryCodes.isEmpty() ? 1 : 0;
        }

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //dataAdapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        countrySpinner.setAdapter(dataAdapter);
        if (selectedItemIndex > -1) {
            countrySpinner.setSelection(selectedItemIndex);
        }
    }

    private List<String> getPreferredCountryCodes() {

        List<String> preferredCountryCodes = new ArrayList<String>();

        try {
            TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                String simCountryCode = tm.getSimCountryIso();
                if (simCountryCode != null && !simCountryCode.isEmpty()) {
                    preferredCountryCodes.add(simCountryCode.toUpperCase());
                }
                //Log.i(TAG, "iso country code is " + locale);
            }
        } catch (Throwable t) {

        }

        String locale = mContext.getResources().getConfiguration().locale.getCountry();
        if (locale != null && !preferredCountryCodes.isEmpty() && !preferredCountryCodes.get(0).equals(locale)) {
            preferredCountryCodes.add(locale);
        }

        //test: french people living in Canada :D
        /*
        preferredCountryCodes.clear();
        preferredCountryCodes.add("CA");
        preferredCountryCodes.add("FR");
        */

        return preferredCountryCodes;
    }

    private String getSelectedCountry() {

        // first item means "all countries", no country restriction
        if (countrySpinner.getSelectedItemPosition() == 0) {
            return null;
        }

        String item = (String) countrySpinner.getSelectedItem();
        if (item != null && !item.isEmpty()) {
            return countryNameToCodeMap.get(item);
        } else {
            return null;
        }

    }

    private boolean isCountrySelected() {
        return (countrySpinner != null && countrySpinner.getSelectedItemPosition() > 0);
    }

    private void updateDisplayedRadioStationTexts() {
        boolean countrySelected = isCountrySelected();
        stationTexts.clear();
        for (RadioStation station : stations) {
            String stationTitle;
            if (countrySelected) {
                stationTitle = getDisplayedRadioStationText(station, false);
            } else {
                stationTitle = getDisplayedRadioStationText(station, true);
            }
            stationTexts.add(stationTitle);
        }

    }

    private String getDisplayedRadioStationText(RadioStation station, boolean displayCountryCode) {
        if (displayCountryCode) {
            return String.format("%s %s (%d kbit/s)", station.countryCode, station.name, station.bitrate);
        } else {
            return String.format("%s (%d kbit/s)", station.name, station.bitrate);
        }
    }

    private String jsonKey() {
        return String.format("%s_json", getKey());
    }

    private void persistRadioStation(RadioStation station) {
        //save stream as separate field
        persistString(station.stream);

        //save complete station as json string
        try {
            String json = station.toJson();
            SharedPreferences prefs = getSharedPreferences();
            SharedPreferences.Editor prefEditor = prefs.edit();
            prefEditor.putString(jsonKey(), json);
            prefEditor.commit();
        } catch (Throwable t) {
            Log.e(TAG, "error converting station to json", t);
        }

    }

    private RadioStation getPersistedRadioStation() {

        SharedPreferences prefs = getSharedPreferences();
        String json = prefs.getString(jsonKey(),  null);
        if (json != null) {
            try {
                RadioStation s = RadioStation.fromJson(json);
                return s;
            } catch (JSONException e) {
                Log.e(TAG, "error converting json to station", e);
            }
        }

        return null;

    }

}
