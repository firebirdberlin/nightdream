package com.firebirdberlin.nightdream;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;
import de.greenrobot.event.EventBus;
import java.util.Calendar;


public class NightDreamService extends DreamService implements View.OnClickListener, View.OnTouchListener {

    TextView current;
    AlarmClock histogram;
    ImageView background_image;

    SensorManager sensorManager;
    Sensor lightSensor;
    int mode;
    boolean isDebuggable;

    private float last_ambient;
    private double last_ambient_noise = 32000.;
    private NightDreamUI nightDreamUI = null;
    private Utility utility;
    private NotificationReceiver nReceiver;

    private double NOISE_AMPLITUDE_WAKE  = Config.NOISE_AMPLITUDE_WAKE;
    private double NOISE_AMPLITUDE_SLEEP = Config.NOISE_AMPLITUDE_SLEEP;
    private Settings mySettings = null;

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setContentView(R.layout.main);

        nightDreamUI = new NightDreamUI(this, getWindow(), true);
        mySettings = new Settings(this);
        utility = new Utility(this);
        isDebuggable = utility.isDebuggable();

        setInteractive(true);
        setFullscreen(true);
        setScreenBright(true);

        NOISE_AMPLITUDE_SLEEP *= mySettings.sensitivity;
        NOISE_AMPLITUDE_WAKE  *= mySettings.sensitivity;

        current = (TextView) findViewById(R.id.current);

        histogram = (AlarmClock) findViewById(R.id.AlarmClock);
        histogram.setUtility(utility);
        histogram.setSettings(mySettings);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (lightSensor == null){
            Toast.makeText(NightDreamService.this, "No Light Sensor!", Toast.LENGTH_LONG).show();
            mode = 2;
            last_ambient = 30.0f;
        }

        nReceiver = registerNotificationReceiver();

        TextClock clock = (TextClock)findViewById(R.id.clock);
        clock.setOnClickListener(this);

        background_image = (ImageView)findViewById(R.id.background_view);
        background_image.setOnTouchListener(this);
    }

    // ignore click on the settings icon
    public void onSettingsClick(View v) {
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();

        nightDreamUI.onStart();
        nightDreamUI.onResume();

        EventBus.getDefault().register(this);

        // ask for active notifications
        if (Build.VERSION.SDK_INT >= 18){
            Intent i = new Intent("com.firebirdberlin.nightdream.NOTIFICATION_LISTENER");
            i.putExtra("command","list");
            sendBroadcast(i);
        }
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        nightDreamUI.onPause();
        nightDreamUI.onStop();
        EventBus.getDefault().unregister(this);
        if (nReceiver != null) {
            unregisterReceiver(nReceiver);
            nReceiver = null;
        }

        //stop notification listener service
        if (Build.VERSION.SDK_INT >= 18){
            Intent i = new Intent("com.firebirdberlin.nightdream.NOTIFICATION_LISTENER");
            i.putExtra("command","release");
            sendBroadcast(i);
        }
    }

    @Override
    public void onDestroy(){
        nightDreamUI.onDestroy();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (isDebuggable)
            Log.d("NightDreamService","onDetachedFromWindow() called.");
    }

    private NotificationReceiver registerNotificationReceiver(){
        NotificationReceiver receiver = new NotificationReceiver(getWindow());
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.firebirdberlin.nightdream.NOTIFICATION_LISTENER");
        registerReceiver(receiver,filter);
        return receiver;
    }

    public boolean onTouch(View view, MotionEvent e) {
        return nightDreamUI.onTouch(view, e, last_ambient);
    }


    @Override
    public void onClick(View v) {
        if (utility.AlarmRunning() == true) histogram.stopAlarm();

        if (v instanceof TextView){
            nightDreamUI.onClockClicked(last_ambient);
        }

        if (lightSensor == null){ // in the emulator
            switch (mode){
                case 0:
                    SwitchModes(18.f);
                    break;
                case 1:
                    SwitchModes(39.f);
                    break;
                case 2:
                    SwitchModes(50.f);
                    break;
                case 3:
                    SwitchModes(4.0f);
                    break;
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        nightDreamUI.onConfigurationChanged();
    }

    private void SwitchModes(float light_value){
        int current_mode = mode;
        mode = nightDreamUI.determineScreenMode(current_mode, light_value, last_ambient_noise);

        nightDreamUI.switchModes(light_value, last_ambient_noise);

        setScreenBright(mode >= 2);
    }

    public void onEvent(OnNewLightSensorValue event){
        if (isDebuggable)
            current.setText(String.valueOf(event.value) + " lux, n=" + String.valueOf(event.n));
        last_ambient = event.value;
        SwitchModes(event.value);
    }

    public void onEvent(OnLightSensorValueTimeout event){
        if (isDebuggable)
            current.setText("Static for 15s: " + String.valueOf(event.value) + " lux.");
        last_ambient = event.value;
        SwitchModes(event.value);
    }

    public void onEvent(OnNewAmbientNoiseValue event) {
        last_ambient_noise = event.value;
        if (isDebuggable){
            current.setText("Ambient noise " + String.valueOf(last_ambient_noise));
        }
        SwitchModes(last_ambient);
    }
}
