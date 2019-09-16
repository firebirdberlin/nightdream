package com.firebirdberlin.openweathermapapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
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
import android.widget.TextView;

import androidx.core.widget.ContentLoadingProgressBar;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceViewHolder;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.openweathermapapi.models.City;

import java.util.ArrayList;
import java.util.List;

public class CityIDPreference extends DialogPreference
                              implements CityRequestTask.AsyncResponse {
    private final static String TAG = "NightDream.CityIDPreference";
    private static final String NAMESPACE = "owmapi";
    private Context mContext = null;
    private EditText queryText = null;
    private ArrayList<City> cities = new ArrayList<City>();
    private ArrayAdapter<City> adapter;
    private ListView cityListView;
    private TextView noResultsText;
    private ContentLoadingProgressBar spinner;
    private Button searchButton;
    private String textSummary = "";

    public CityIDPreference(Context ctx) {
        this(ctx, null);
        mContext = getContext();
    }

    public CityIDPreference(Context ctx, AttributeSet attrs) {
        this(ctx, attrs, android.R.attr.dialogPreferenceStyle);
        setValuesFromXml(attrs);
    }

    public CityIDPreference(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
        setValuesFromXml(attrs);
    }

    private static String getAttributeStringValue(AttributeSet attrs, String namespace,
                                                  String name, String defaultValue) {
        String value = attrs.getAttributeValue(namespace, name);
        if (value == null) value = defaultValue;

        return value;
    }

    private void setValuesFromXml(AttributeSet attrs) {
        mContext = getContext();
        Resources res = mContext.getResources();
        String resClear = getAttributeStringValue(attrs, NAMESPACE, "textClear", null);
        int identifier = res.getIdentifier(resClear, null, mContext.getPackageName());
        if (identifier != 0 ) {
            String label = res.getString(identifier);
            setPositiveButtonText(label);
        } else {
            setPositiveButtonText(android.R.string.cancel);
        }

        String resSummary = getAttributeStringValue(attrs, NAMESPACE, "textSummary", "");
        int resSummaryID = res.getIdentifier(resSummary, null, mContext.getPackageName());
        if (resSummaryID != 0 ) {
            textSummary = res.getString(resSummaryID);
        }
    }

    /*
    @Override
    protected void onPrepareDialogBuilder(android.app.AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setNegativeButton(null, null);
    }
*/
    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        setNegativeButtonText(null);
        adapter = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, cities);
        LayoutInflater inflater = (LayoutInflater)
            getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View v = inflater.inflate(R.layout.city_id_search_dialog, null);

        queryText = (v.findViewById(R.id.query_string));
        spinner = v.findViewById(R.id.progress_bar);
        cityListView = v.findViewById(R.id.radio_stream_list_view);
        noResultsText = v.findViewById(R.id.no_results);
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

        cityListView.setAdapter(adapter);
        cityListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                City city = (City) parent.getItemAtPosition(position);
                persist(city);
                //getDialog().dismiss();

            }
        });

        searchButton = (v.findViewById(R.id.start_search));
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

    }

    private void startSearch() {
        spinner.show();
        cityListView.setVisibility(View.GONE);
        noResultsText.setVisibility(View.GONE);
        String query = queryText.getText().toString().trim();
        new CityRequestTask(this).execute(query);

        InputMethodManager imm =
                (InputMethodManager) queryText.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(queryText.getWindowToken(), 0);
        searchButton.setEnabled(false);
    }

    @Override
    public void onRequestFinished(List<City> cities){
        this.cities.clear();
        this.cities.addAll(cities);
        ((ArrayAdapter) cityListView.getAdapter()).notifyDataSetChanged();
        spinner.hide();
        cityListView.setVisibility((cities.size() == 0) ? View.GONE : View.VISIBLE);
        noResultsText.setVisibility((cities.size() == 0) ? View.VISIBLE : View.GONE);
    }


    // TODO
    /*@Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        this.cities.clear();
        // the positive button is used to clear the contents.
        if (positiveResult) {
            persist(null);
        }
    }*/

    private void persist(City city) {
        if (city == null) {

            persistString("");
            SharedPreferences prefs = getSharedPreferences();
            SharedPreferences.Editor prefEditor = prefs.edit();
            String keyName = String.format("%s_name", getKey());
            String keyJson = String.format("%s_json", getKey());
            prefEditor.putString(keyName, "");
            prefEditor.putString(keyJson, "");
            prefEditor.apply();
            setSummary(getSummaryText("", ""));
            notifyChanged();
        } else {
            String cityId = String.format("%d", city.id);
            persistString(cityId);

            SharedPreferences prefs = getSharedPreferences();
            SharedPreferences.Editor prefEditor = prefs.edit();
            String keyName = String.format("%s_name", getKey());
            String keyJson = String.format("%s_json", getKey());
            prefEditor.putString(keyName, city.name);
            prefEditor.putString(keyJson, city.toJson());
            prefEditor.apply();

            setSummary(getSummaryText(cityId, city.name));
            notifyChanged();
        }
    }
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setTitle(getTitle());

        String def = (String) defaultValue;
        String name = getCityName();

        setSummary(getSummaryText(getPersistedString(def), name));
    }

    private String getCityName() {
        SharedPreferences prefs = getSharedPreferences();
        String keyName = getKeyForCityName();
        return prefs.getString(keyName, "");
    }

    private String getKeyForCityName() {
        return String.format("%s_name", getKey());
    }

    private String getSummaryText(String id, String cityName) {
        if (cityName.isEmpty() ) {
            return String.format("%s\n%s", id, textSummary);
        }
        return String.format("%s (%s)\n%s", cityName, id, textSummary);
    }
}
