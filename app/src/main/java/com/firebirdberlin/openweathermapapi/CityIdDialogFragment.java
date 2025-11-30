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

package com.firebirdberlin.openweathermapapi;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
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
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.openweathermapapi.models.City;

import java.util.ArrayList;
import java.util.List;

public class CityIdDialogFragment extends PreferenceDialogFragmentCompat {
    private String TAG = getClass().getSimpleName();

    CityIDPreference preference;
    private EditText queryText = null;
    private ArrayList<City> cities = new ArrayList<City>();
    private ArrayAdapter<City> adapter;
    private ListView cityListView;
    private TextView noResultsText;
    private ContentLoadingProgressBar spinner;
    private Button searchButton;

    public static CityIdDialogFragment newInstance(String key) {
        final CityIdDialogFragment fragment = new CityIdDialogFragment();
        final Bundle bundle = new Bundle(1);
        bundle.putString(ARG_KEY, key);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        preference = (CityIDPreference) getPreference();
        builder.setNegativeButton(null, null);
        builder.setPositiveButton(preference.getPositiveButtonText(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                preference.persist(null);
            }
        });
    }

    @Override
    protected View onCreateDialogView(Context context) {
        //return super.onCreateDialogView(context);
        adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, cities);
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
                Log.d(TAG, city.toString());

                preference.persist(city);
                getDialog().dismiss();

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

        return v;
    }

    private void startSearch() {
        spinner.show();
        cityListView.setVisibility(View.GONE);
        noResultsText.setVisibility(View.GONE);
        String query = queryText.getText().toString().trim();

        CityRequestManager.findCities(getContext(), query, new CityRequestManager.AsyncResponse() {
            @Override
            public void onRequestFinished(List<City> citiesList) {
                cities.clear();
                cities.addAll(citiesList);
                ((ArrayAdapter<?>) cityListView.getAdapter()).notifyDataSetChanged();
                spinner.hide();
                cityListView.setVisibility((citiesList.isEmpty()) ? View.GONE : View.VISIBLE);
                noResultsText.setVisibility((citiesList.isEmpty()) ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onRequestError(Exception exception) {
                // This code runs on the main thread.
                // Show an error message to the user.
                Log.e("MyActivity", "City search failed: ", exception);
                // Example: Toast.makeText(MyActivity.this, "Error: " + exception.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        InputMethodManager imm =
                (InputMethodManager) queryText.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(queryText.getWindowToken(), 0);
        searchButton.setEnabled(false);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {

    }
}