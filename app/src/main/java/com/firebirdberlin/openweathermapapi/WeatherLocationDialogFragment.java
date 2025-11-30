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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.fragment.app.DialogFragment;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.openweathermapapi.models.City;
import com.firebirdberlin.openweathermapapi.CityRequestManager;

import java.util.ArrayList;
import java.util.List;

public class WeatherLocationDialogFragment extends DialogFragment {

    public static String TAG = "WeatherLocationDialogFragment";
    private Context context = null;
    private EditText queryText = null;
    private ArrayList<City> cities = new ArrayList<City>();
    private ArrayAdapter<City> adapter;
    private ListView cityListView;
    private TextView noResultsText;
    private ContentLoadingProgressBar spinner;
    private Button searchButton;
    private String lastQuery = null;


    public interface WeatherLocationDialogListener {
        void onWeatherLocationSelected(City city);
    }

    public WeatherLocationDialogFragment() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

        alertDialogBuilder.setTitle(getString(R.string.weather_city_id_title));
        alertDialogBuilder.setPositiveButton(null, null);
        alertDialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }

        });
        View view = onCreateDialogView(getActivity().getLayoutInflater(), null, null);
        onViewCreated(view, null);

        alertDialogBuilder.setView(view);
        return alertDialogBuilder.create();
    }

    @Nullable
    public View onCreateDialogView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        context = getContext();
        return inflater.inflate(R.layout.city_id_search_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, cities);
        queryText = (v.findViewById(R.id.query_string));
        spinner = v.findViewById(R.id.progress_bar);
        cityListView = v.findViewById(R.id.radio_stream_list_view);
        noResultsText = v.findViewById(R.id.no_results);
        noResultsText.setVisibility(View.GONE);

        searchButton = (v.findViewById(R.id.start_search));
        searchButton.setEnabled(false);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSearch();
            }
        });

        queryText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event == null) {
                    return false;
                }
                if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
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
                Log.d(TAG, "city selected");
                WeatherLocationDialogListener listener = (WeatherLocationDialogListener) getActivity();
                listener.onWeatherLocationSelected(city);
                getDialog().dismiss();

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
                String query = queryText.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchButton.setEnabled(lastQuery == null || !lastQuery.equals(query));
                }
            }
        });

    }

    private void startSearch() {
        if (!searchButton.isEnabled()) {
            return;
        }
        String query = queryText.getText().toString().trim();
        if (lastQuery != null && lastQuery.equals(query)) {
            return;
        }
        lastQuery = query;
        spinner.show();
        cityListView.setVisibility(View.GONE);
        noResultsText.setVisibility(View.GONE);


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
                (InputMethodManager) queryText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(queryText.getWindowToken(), 0);
        searchButton.setEnabled(false);
    }
}
