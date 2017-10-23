package com.firebirdberlin.nightdream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.ui.AlarmClockLayout;

import java.util.List;

public class SetAlarmClockActivity extends Activity {
    private LinearLayout scrollView = null;
    public static void start(Context context) {
        Intent intent = new Intent(context, SetAlarmClockActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_alarm_clock);
        setTheme(R.style.DialogTheme);

        scrollView = (LinearLayout) findViewById(R.id.scroll_view);
        init();
    }

    private void init() {
        DataSource db = new DataSource(this);
        db.open();
        List<SimpleTime> entries = db.getAlarms();
        db.close();
        for (SimpleTime entry : entries) {
            AlarmClockLayout layout = new AlarmClockLayout(this, entry);
            scrollView.addView(layout);
        }
        scrollView.invalidate();
    }
}
