package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class AlarmsPreferenceActivity extends AppCompatActivity {

    public static void start(Context context) {
        Intent intent = new Intent(context, AlarmsPreferenceActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.PreferencesTheme);
        super.onCreate(savedInstanceState);
        actionBarSetup();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new AlarmsPreferenceFragment())
                .commit();
    }

    private void actionBarSetup() {
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.preferences);
        }
    }


}
