package com.firebirdberlin.radiostreamapi;

import java.util.List;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.radiostreamapi.StationRequestTask;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

public class RadioStreamPreference extends DialogPreference
                                   implements StationRequestTask.AsyncResponse {
    private final static String TAG = "NightDream.RadioStreamPreference";
    private Context mContext = null;
    private EditText queryText = null;
    private String selectedStream;
    private ArrayList<RadioStation> stations = new ArrayList<RadioStation>();
    private ArrayAdapter<RadioStation> adapter;
    private ListView stationListView;

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
        adapter = new ArrayAdapter<RadioStation>(mContext, android.R.layout.simple_list_item_1, stations);
        LayoutInflater inflater = (LayoutInflater)
            getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View v = inflater.inflate(R.layout.radio_stream_dialog, null);

        final RadioStreamPreference context = this;
        queryText = ((EditText) v.findViewById(R.id.query_string));

        queryText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = v.getText().toString();
                    new StationRequestTask(context).execute(query);

                    InputMethodManager imm =
                        (InputMethodManager) v.getContext()
                                              .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        stationListView = (ListView) v.findViewById(R.id.radio_stream_list_view);
        stationListView.setAdapter(adapter);
        stationListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RadioStation station = (RadioStation) parent.getItemAtPosition(position);
                persistString(station.stream);
                setSummary(station.stream);
                //invalidate();
                notifyChanged();
                getDialog().dismiss();

            }
        });

        return v;
    }

    @Override
    public void onRequestFinished(List<RadioStation> stations){
        this.stations.clear();
        this.stations.addAll(stations);
        Log.i(TAG, String.format("Request finished with %d entries", this.stations.size()));
        ((ArrayAdapter) stationListView.getAdapter()).notifyDataSetChanged();
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
