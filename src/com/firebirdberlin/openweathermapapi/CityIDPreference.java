package com.firebirdberlin.openweathermapapi;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
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
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.openweathermapapi.CityRequestTask;
import com.firebirdberlin.openweathermapapi.models.City;

public class CityIDPreference extends DialogPreference
                              implements CityRequestTask.AsyncResponse {
    private final static String TAG = "NightDream.CityIDPreference";
    private Context mContext = null;
    private EditText queryText = null;
    private String selectedStream;
    private ArrayList<City> cities = new ArrayList<City>();
    private ArrayAdapter<City> adapter;
    private ListView cityListView;
    private TextView noResultsText;
    private ContentLoadingProgressBar spinner;
    private Button searchButton;

    public CityIDPreference(Context ctx) {
        this(ctx, null);
        mContext = getContext();
    }

    public CityIDPreference(Context ctx, AttributeSet attrs) {
        this(ctx, attrs, android.R.attr.dialogPreferenceStyle);
        mContext = getContext();
        setValuesFromXml(attrs);
    }

    public CityIDPreference(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
        setValuesFromXml(attrs);
    }

    private void setValuesFromXml(AttributeSet attrs) {
        mContext = getContext();
        //setPositiveButtonText(android.R.string.ok);
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
        adapter = new ArrayAdapter<City>(mContext, android.R.layout.simple_list_item_1, cities);
        LayoutInflater inflater = (LayoutInflater)
            getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View v = inflater.inflate(R.layout.city_id_search_dialog, null);

        final CityIDPreference context = this;
        queryText = ((EditText) v.findViewById(R.id.query_string));
        spinner = (ContentLoadingProgressBar) v.findViewById(R.id.progress_bar);
        cityListView = (ListView) v.findViewById(R.id.radio_stream_list_view);
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

        cityListView.setAdapter(adapter);
        cityListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                City city = (City) parent.getItemAtPosition(position);
                String cityid = String.format("%d", city.id);
                persistString(cityid);
                setSummary(cityid);
                notifyChanged();
                getDialog().dismiss();

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


        return v;
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
        //Log.i(TAG, String.format("Request finished with %d entries", this.cities.size()));
        ((ArrayAdapter) cityListView.getAdapter()).notifyDataSetChanged();
        spinner.hide();
        cityListView.setVisibility((cities.size() == 0) ? View.GONE : View.VISIBLE);
        noResultsText.setVisibility((cities.size() == 0) ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        this.cities.clear();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setTitle(getTitle());
        setSummary(getPersistedString((String) defaultValue));
    }
}
