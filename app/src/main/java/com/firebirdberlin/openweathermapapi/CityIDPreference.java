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
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;

import com.firebirdberlin.openweathermapapi.models.City;


public class CityIDPreference extends DialogPreference {
    private final static String TAG = "CityIDPreference";
    private static final String NAMESPACE = "owmapi";
    private Context mContext = null;
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

    protected void persist(City city) {
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
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        super.onSetInitialValue(defaultValue);
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
