package com.firebirdberlin.nightdream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

public class NightDreamSettingsActivity extends Activity {
    public static final String PREFS_KEY = "NightDream preferences";
    public final static int BACKGROUND_BLACK = 1;
    public final static int BACKGROUND_GRADIENT = 2;
    public final static int BACKGROUND_IMAGE = 3;
    private int clockColor;
    private ImageView bgimage;
    private int background_mode;
    private boolean startedByActivity = false;
    SharedPreferences settings;

    private static int RESULT_LOAD_IMAGE = 1;
    private static final int PICK_FROM_GALLERY = 2;
    private Button btn_choose_bgimage;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null){
            if ( intent.hasExtra("NightDreamActivity") ){
                startedByActivity = extras.getBoolean("NightDreamActivity");
            }
        }

        settings = getSharedPreferences(PREFS_KEY, 0);
        boolean animate = settings.getBoolean("showDate", true);
        boolean handle_power = settings.getBoolean("handle_power", false);
        boolean allow_screen_off = settings.getBoolean("allow_screen_off", false);
        boolean ambientNoise = settings.getBoolean("ambientNoiseDetection", false);
        int sensitivity = settings.getInt("NoiseSensitivity", 4);
        background_mode = settings.getInt("BackgroundMode", BACKGROUND_BLACK);
        clockColor = settings.getInt("clockColor", Color.parseColor("#33B5E5"));

        final Button choose_color = (Button) findViewById(R.id.btn_choose_color);
        btn_choose_bgimage = (Button) findViewById(R.id.btn_choose_bgimage);

        final CheckBox cbSilence = (CheckBox) findViewById(R.id.checkbox_mute_ringer);
        cbSilence.setChecked(settings.getBoolean("Night.muteRinger", true));
        cbSilence.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                {
                    SharedPreferences.Editor prefEditor = settings.edit();
                    prefEditor.putBoolean("Night.muteRinger", isChecked);
                    prefEditor.commit();
                }

            });

        if (startedByActivity == false){
            final View switch_handle_power = (View) findViewById(R.id.llHandlePower);
            switch_handle_power.setVisibility(View.GONE);
            final View switch_allow_screen_off = (View) findViewById(R.id.llAllowScreenOff);
            switch_allow_screen_off.setVisibility(View.GONE);
        } else {

            final CompoundButton switch_handle_power     = (CompoundButton) findViewById(R.id.switch_handle_power);
            switch_handle_power.setChecked(handle_power);
            switch_handle_power.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(final CompoundButton buttonView,
                        final boolean isChecked) {
                    SharedPreferences.Editor prefEditor = settings.edit();
                    prefEditor.putBoolean("handle_power", isChecked);
                    prefEditor.commit();
                }
            });

            final     CompoundButton switch_allow_screen_off     = (CompoundButton) findViewById(R.id.switch_allow_screen_off);
            switch_allow_screen_off.setChecked(allow_screen_off);
            switch_allow_screen_off.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(final CompoundButton buttonView,
                        final boolean isChecked) {
                    SharedPreferences.Editor prefEditor = settings.edit();
                    prefEditor.putBoolean("allow_screen_off", isChecked);
                    prefEditor.commit();
                }
            });


        }

        final SeekBar sbSensitivity =(SeekBar) findViewById(R.id.sbNoiseSensitivity);
        sbSensitivity.setProgress(sensitivity);
        sbSensitivity.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    SharedPreferences.Editor prefEditor = settings.edit();
                    prefEditor.putInt("NoiseSensitivity", seekBar.getProgress());
                    prefEditor.commit();
                }



                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {

                }
        });

        final CompoundButton switch_ambient_noise = (CompoundButton) findViewById(R.id.switch_ambient_noise);
        switch_ambient_noise.setChecked(ambientNoise);
        switch_ambient_noise.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView,
                    final boolean isChecked) {
                SharedPreferences.Editor prefEditor = settings.edit();
                prefEditor.putBoolean("ambientNoiseDetection", isChecked);
                prefEditor.commit();
            }
        });

        final CompoundButton toggle = (CompoundButton) findViewById(R.id.toggle_animate_button);
        toggle.setChecked(animate);
        toggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView,
                    final boolean isChecked) {
                SharedPreferences.Editor prefEditor = settings.edit();
                prefEditor.putBoolean("showDate", isChecked);
                prefEditor.commit();
            }
        });

        final RadioButton radio_bg_black    = (RadioButton) findViewById(R.id.radio_bg_black);
        final RadioButton radio_bg_gradient    = (RadioButton) findViewById(R.id.radio_bg_gradient);
        final RadioButton radio_bg_image    = (RadioButton) findViewById(R.id.radio_bg_image);
//        bgimage                                   = (ImageView) findViewById(R.id.background_thumbnail);

        switch (background_mode){
            case BACKGROUND_BLACK:
                    radio_bg_black.setChecked(true);
                    btn_choose_bgimage.setVisibility(View.INVISIBLE);
                    break;
            case BACKGROUND_GRADIENT:
                    radio_bg_gradient.setChecked(true);
                    btn_choose_bgimage.setVisibility(View.INVISIBLE);
                    break;
            case BACKGROUND_IMAGE:
                    radio_bg_image.setChecked(true);
                    btn_choose_bgimage.setVisibility(View.VISIBLE);
                    break;
        }
    }

    public void buttonClicked(View v){
        if(v.getId() == R.id.buttonAccessibilitySettings){
            if (Build.VERSION.SDK_INT < 18)    {
                Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivityForResult(intent, 0);
            } else {
                Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                startActivityForResult(intent, 0);
            }
        }
    }

    public void chooseColor(View view) {
         Utility utility = new Utility(this);
        Point size = utility.getDisplaySize();
        int radius = (size.x < size.y) ? (int) (.5 *(size.x - 0.05f*size.x)) : (int) (.5*(size.y - 0.05f*size.y));
        new ColorPickerDialog(this, mOnColorChangedListener, clockColor, radius).show();
    }

    ColorPickerDialog.OnColorChangedListener mOnColorChangedListener
        = new ColorPickerDialog.OnColorChangedListener(){

        @Override
        public void colorChanged(int color){
            // do something
            clockColor = color;
            SharedPreferences.Editor prefEditor = settings.edit();
            prefEditor.putInt("clockColor", clockColor);
            prefEditor.commit();
        }
    };

    public void chooseBackgroundImage(View view) {
        Intent in = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                      startActivityForResult(in, RESULT_LOAD_IMAGE);
    }

    // an image was selected
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data){

            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(selectedImage,filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            if (picturePath != null){
                SharedPreferences.Editor prefEditor = settings.edit();
                prefEditor.putString("BackgroundImage", picturePath);
                prefEditor.commit();

            } else {
                Toast.makeText(this, "Could locate image !", Toast.LENGTH_LONG).show();
            }
        }
    }


    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        SharedPreferences.Editor prefEditor = settings.edit();
        btn_choose_bgimage.setVisibility(View.INVISIBLE);
        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_bg_black:
                if (checked){
                    prefEditor.putInt("BackgroundMode", BACKGROUND_BLACK);
                }
                break;
            case R.id.radio_bg_gradient:
                if (checked){
                    prefEditor.putInt("BackgroundMode", BACKGROUND_GRADIENT);
                }
                break;
            case R.id.radio_bg_image:
                if (checked){
                    prefEditor.putInt("BackgroundMode", BACKGROUND_IMAGE);
                    btn_choose_bgimage.setVisibility(View.VISIBLE);
                }
                break;
        }
        prefEditor.commit();
    }

    public static void start(Context context) {
        Intent myIntent = new Intent(context, NightDreamSettingsActivity.class);
        myIntent.putExtra("NightDreamActivity", true); //Optional parameters
        context.startActivity(myIntent);
    }
}
