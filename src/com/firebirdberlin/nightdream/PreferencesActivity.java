package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PreferencesActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
        .replace(android.R.id.content, new PreferencesFragment())
        .commit();
    }

    public static void start(Context context) {
        Intent myIntent = new Intent(context, PreferencesActivity.class);
        context.startActivity(myIntent);
    }
}
