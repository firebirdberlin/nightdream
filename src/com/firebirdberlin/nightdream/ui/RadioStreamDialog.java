package com.firebirdberlin.nightdream.ui;


import android.content.Context;
import android.content.res.Resources;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.radiostreamapi.CountryRequestTask;
import com.firebirdberlin.radiostreamapi.RadioStreamPreference;
import com.firebirdberlin.radiostreamapi.StationRequestTask;
import com.firebirdberlin.radiostreamapi.models.Country;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RadioStreamDialog implements StationRequestTask.AsyncResponse,
        CountryRequestTask.AsyncResponse {

    private final static String TAG = "RadioStreamDialog";

    private final Context context;

    // this dialog instance must not be used repeatedly, otherwise it wont reflect a changed station
    private final RadioStation persistedRadioStation;

    private EditText queryText = null;
    private ArrayList<RadioStation> stations = new ArrayList<>();
    private ArrayList<String> stationTexts = new ArrayList<>();
    private ListView stationListView;
    private Spinner countrySpinner;
    private TextView noResultsText;
    private TextView noDataConnectionText;
    private ContentLoadingProgressBar spinner;
    private Button searchButton;
    private Map<String, String> countryNameToCodeMap = null;

    private final RadioStreamManualInputDialog manualInputDialog = new RadioStreamManualInputDialog();

    public RadioStreamDialog(Context context, RadioStation persistedRadioStation) {
        this.context = context;
        this.persistedRadioStation = persistedRadioStation;
    }

    private Context getContext() {
        return context;
    }

    public View createDialogView(final RadioStreamDialogListener radioStreamDialogListener) {
        updateDisplayedRadioStationTexts();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, stationTexts);

        LayoutInflater inflater = (LayoutInflater)
                getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View v = inflater.inflate(R.layout.radio_stream_dialog, null);

        queryText = ((EditText) v.findViewById(R.id.query_string));
        spinner = (ContentLoadingProgressBar) v.findViewById(R.id.progress_bar);
        stationListView = (ListView) v.findViewById(R.id.radio_stream_list_view);
        countrySpinner = (Spinner) v.findViewById(R.id.countrySpinner);
        noResultsText = (TextView) v.findViewById(R.id.no_results);
        noResultsText.setVisibility(View.GONE);
        Button directInputHintText = (Button) v.findViewById(R.id.direct_input_hint);
        directInputHintText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showManualInputDialog(radioStreamDialogListener);
            }
        });

        noDataConnectionText = (TextView) v.findViewById(R.id.no_data_connection);
        noDataConnectionText.setVisibility(View.GONE);
        String lang = Locale.getDefault().getLanguage();
        if ("de".equals(lang) || "en".equals(lang)) {
            // For German and English display this text in parentheses, otherwise the default
            // message_no_data_connection is displayed
            try {
                noDataConnectionText.setText(String.format("(%s)",
                        context.getResources().getString(R.string.message_no_data_connection)));
            } catch (Resources.NotFoundException e) {
                e.printStackTrace();
            }
        }

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
                /*
                persistRadioStation(station);
                //TODO setSummary still needed?
                //setSummary(station.stream);
                notifyChanged();
                getDialog().dismiss();
                */

                //TODO setSummary still needed?
                radioStreamDialogListener.onRadioStreamSelected(station);
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
        if (query.isEmpty()) {
            return;
        }

        spinner.show();
        stationListView.setVisibility(View.GONE);
        noResultsText.setVisibility(View.GONE);
        noDataConnectionText.setVisibility(View.GONE);

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
        ((ArrayAdapter) stationListView.getAdapter()).notifyDataSetChanged();
        spinner.hide();
        stationListView.setVisibility((stations.size() == 0) ? View.GONE : View.VISIBLE);
        noResultsText.setVisibility((stations.size() == 0) ? View.VISIBLE : View.GONE);
        if (stations.size() == 0 && !Utility.hasNetworkConnection(context)) {
            noDataConnectionText.setVisibility(View.VISIBLE);
        } else {
            noDataConnectionText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCountryRequestFinished(List<Country> countries) {
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
        //RadioStation persistedRadioStation = getPersistedRadioStation();
        updateCountrySpinner(countries, persistedRadioStation);
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

    private void initCountrySpinner() {
        Log.i(TAG, "starting country search");
        new CountryRequestTask(this, context).execute();
    }

    private void updateCountryNameToCodeMap(List<Country> countries) {
        countryNameToCodeMap = new HashMap<>();
        for (Country c : countries) {
            countryNameToCodeMap.put(c.name, c.countryCode);
        }

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

    private boolean isCountrySelected() {
        return (countrySpinner != null && countrySpinner.getSelectedItemPosition() > 0);
    }

    private String getDisplayedRadioStationText(RadioStation station, boolean displayCountryCode) {
        String countryCode = (displayCountryCode) ? String.format("%s ", station.countryCode) : "";
        String streamOffline = (station.isOnline) ? ""
                : String.format(" - %s",
                context.getResources().getString(R.string.radio_stream_offline));
        return String.format("%s%s (%d kbit/s)%s",
                countryCode, station.name, station.bitrate, streamOffline);
    }

    private void updateCountrySpinner(List<Country> countries, RadioStation persistedRadioStation) {


        //RadioStation persistedRadioStation = getPersistedRadioStation();
        String persistedCountryCode = persistedRadioStation != null ? persistedRadioStation.countryCode : null;
        Log.i(TAG, "found persisted station: " + (persistedRadioStation !=  null ? persistedRadioStation.toString() : "null"));


        List<String> preferredCountryCodes = getPreferredCountryCodes();

        List<String> countryList = new ArrayList<>();

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

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, countryList);

        // select as default: first preferred country, and "any country"
        if (selectedItemIndex == -1 && !countryList.isEmpty()) {
            selectedItemIndex = (preferredCountryCodes != null && !preferredCountryCodes.isEmpty() && countryList.size() > 1) ? 1 : 0;
        }

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countrySpinner.setAdapter(dataAdapter);
        if (selectedItemIndex > -1) {
            countrySpinner.setSelection(selectedItemIndex);
        }
    }

    private List<String> getPreferredCountryCodes() {

        List<String> preferredCountryCodes = new ArrayList<>();

        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                String simCountryCode = tm.getSimCountryIso();
                if (simCountryCode != null && !simCountryCode.isEmpty()) {
                    preferredCountryCodes.add(simCountryCode.toUpperCase());
                }
                //Log.i(TAG, "iso country code is " + locale);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        String locale = context.getResources().getConfiguration().locale.getCountry();
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

    public void clearLastSearchResult() {
        this.stations.clear();
    }

    public void showManualInputDialog(final RadioStreamDialogListener radioStreamDialogListener) {

        final RadioStreamDialogListener manualDialogListener = new RadioStreamDialogListener() {
            public void onRadioStreamSelected(RadioStation station) {
                //dialog is already closed, now also finish parent dialog (RadioStreamPreference)
                radioStreamDialogListener.onRadioStreamSelected(station);
            }
        };

        manualInputDialog.showDialog(getContext(), persistedRadioStation, manualDialogListener);
    }

}
