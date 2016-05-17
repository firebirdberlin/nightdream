package com.firebirdberlin.nightdream;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.animation.AlphaAnimation;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import static android.text.format.DateFormat.getBestDateTimePattern;

public class NightDreamUI {
    private static String TAG ="NightDreamActivity";

    final private Handler handler = new Handler();
    private int mode = 2;
    private BatteryStats battery;
    private boolean isDebuggable;
    private Context mContext;
    private Drawable bgshape;
    private Drawable bgblack;
    private AlarmClock alarmClock;
    private ImageView background_image;
    private ImageView settingsIcon;
    private ImageView callIcon, gmailIcon, twitterIcon, whatsappIcon;
    private LightSensorEventListener lightSensorEventListener = null;
    private LinearLayout clockLayout;
    private LinearLayout notificationbar;
    private Settings settings = null;
    private SoundMeter soundmeter;
    private TextView batteryView;
    private TextView clock, date;
    private TextView gmailNumber, twitterNumber, whatsappNumber;
    private Utility utility;
    private View divider;
    private View rootView;
    private Window window;
    private mAudioManager AudioManage;

    private int dim_offset_init_x = 0;
    private int dim_offset_curr_x = 0;
    public boolean setDimOffset = false;
    private boolean daydreamMode = false;

    public NightDreamUI(Context context, Window window) {
        mContext = context;
        this.window = window;
        rootView = window.getDecorView().findViewById(android.R.id.content);
        background_image = (ImageView) rootView.findViewById(R.id.background_view);
        batteryView = (TextView) rootView.findViewById(R.id.battery);
        clockLayout = (LinearLayout) rootView.findViewById(R.id.clockLayout);
        clock = (TextView) rootView.findViewById(R.id.clock);
        date = (TextView) rootView.findViewById(R.id.date);
        divider = (View) rootView.findViewById(R.id.divider);
        alarmClock = (AlarmClock) rootView.findViewById(R.id.AlarmClock);
        notificationbar = (LinearLayout) rootView.findViewById(R.id.notificationbar);
        settingsIcon = (ImageView) rootView.findViewById(R.id.settings_icon);

        callIcon = (ImageView) rootView.findViewById(R.id.call_icon);
        gmailIcon = (ImageView) rootView.findViewById(R.id.gmail_icon);
        whatsappIcon = (ImageView) rootView.findViewById(R.id.whatsapp_icon);
        twitterIcon = (ImageView) rootView.findViewById(R.id.twitter_icon);

        gmailNumber = (TextView) rootView.findViewById(R.id.gmail_number);
        whatsappNumber = (TextView) rootView.findViewById(R.id.whatsapp_number);
        twitterNumber = (TextView) rootView.findViewById(R.id.twitter_number);

        // prepare zoom-in effect
        // API level 11
        if (Build.VERSION.SDK_INT >= 12){
            clockLayout.setScaleX(.1f);
            clockLayout.setScaleY(.1f);
        }

        utility = new Utility(context);
        battery = new BatteryStats(context);
        settings = new Settings(context);
        AudioManage = new mAudioManager(context);

        checkForReviewRequest();
        isDebuggable = utility.isDebuggable();
    }

    public NightDreamUI(Context context, Window window, boolean daydreamMode) {
        this(context, window);
        this.daydreamMode = daydreamMode;
    }

    public void onStart() {
        lightSensorEventListener = new LightSensorEventListener(mContext);
        lightSensorEventListener.register();

        if (! daydreamMode) setAlpha(settingsIcon, .5f, 100);
        updateBatteryView();
        if (Build.VERSION.SDK_INT >= 12){
            handler.postDelayed(zoomIn, 500);
        }
        handler.postDelayed(moveAround, 30000);
        handler.postDelayed(hideAlarmClock, 20000);
    }

    public void onResume() {
        settings.reload();
        if (settings.showDate){
            showDate();
        } else {
            hideDate();
        }

        setColor();

        if (settings.ambientNoiseDetection == true){
            soundmeter = new SoundMeter(isDebuggable);
        } else {
            soundmeter = null;
        }
    }

    void setColor() {
        clock.setTextColor(settings.clockColor);
        date.setTextColor(settings.secondaryColor);
        batteryView.setTextColor(settings.secondaryColor);
        gmailNumber.setTextColor(settings.secondaryColor);
        twitterNumber.setTextColor(settings.secondaryColor);
        whatsappNumber.setTextColor(settings.secondaryColor);
        settingsIcon.setColorFilter( settings.secondaryColor, PorterDuff.Mode.MULTIPLY );
        callIcon.setColorFilter( settings.secondaryColor, PorterDuff.Mode.MULTIPLY );
        gmailIcon.setColorFilter( settings.secondaryColor, PorterDuff.Mode.MULTIPLY );
        twitterIcon.setColorFilter( settings.secondaryColor, PorterDuff.Mode.MULTIPLY );
        whatsappIcon.setColorFilter( settings.secondaryColor, PorterDuff.Mode.MULTIPLY );
        alarmClock.setCustomColor(settings.clockColor, settings.secondaryColor);
        alarmClock.setClickable(true);

        bgblack = new ColorDrawable(Color.parseColor("#000000"));
        bgshape = bgblack;
        switch (settings.background_mode){
            case Settings.BACKGROUND_BLACK: {
                bgshape = bgblack;
                break;
            }
            case Settings.BACKGROUND_GRADIENT: {
                bgshape = mContext.getResources().getDrawable(R.drawable.background_gradient);
                break;
            }
            case Settings.BACKGROUND_IMAGE: {
                bgshape = loadBackGroundImage();
                break;
            }
        }

        background_image.setImageDrawable(bgshape);

    }

    public void onPause() {

    }

    public void onStop() {
        lightSensorEventListener.unregister();
        removeCallbacks(moveAround);
        removeCallbacks(hideAlarmClock);
        removeCallbacks(zoomIn);
        removeCallbacks(ClickOut);
        if (soundmeter != null){
            soundmeter.stopMeasurement();
            soundmeter = null;
        }

    }

    public void onDestroy() {
    }

    private void removeCallbacks(Runnable runnable) {
        if (handler == null) return;
        if (runnable == null) return;

        handler.removeCallbacks(runnable);
    }


    public void onConfigurationChanged() {
        Point d = utility.getDisplaySize();
        setDesiredClockWidth((int)(0.6 * d.x));
        removeCallbacks(moveAround);
        handler.postDelayed(moveAround, 2000);
    }

    public void showDate() {
        clockLayout.setBackgroundColor(Color.parseColor("#44000000"));
        date.setVisibility(View.VISIBLE);
        divider.setVisibility(View.VISIBLE);

        if (Build.VERSION.SDK_INT >= 17){
            // note that format string kk is implemented incorrectly in API <= 17
            // from API level 18 on, we can set the system default
            TextClock tclock = (TextClock) rootView.findViewById(R.id.clock);
            TextClock tdate  = (TextClock) rootView.findViewById(R.id.date);
            if (Build.VERSION.SDK_INT >= 18){
                String tlocalPattern24 = getBestDateTimePattern(Locale.getDefault(), "HH:mm");
                String tlocalPattern12 = getBestDateTimePattern(Locale.getDefault(), "hh:mm a");
                String localPatternDate = getBestDateTimePattern(Locale.getDefault(), "EEEEddLLLL");

                tclock.setFormat12Hour(tlocalPattern12);
                tclock.setFormat24Hour(tlocalPattern24);
                tdate.setFormat12Hour(localPatternDate);
                tdate.setFormat24Hour(localPatternDate);
            } else {
                DateFormat formatter = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
                String pattern       = ((SimpleDateFormat)formatter).toPattern();
                String localPattern  = ((SimpleDateFormat)formatter).toLocalizedPattern();
                tdate.setFormat12Hour(localPattern);
                tdate.setFormat24Hour(localPattern);
            }
        }
    }

    public void hideDate() {
        date.setVisibility(View.INVISIBLE);
        divider.setVisibility(View.INVISIBLE);
        clockLayout.setBackgroundColor(Color.parseColor("#00000000"));
    }

    public void setDesiredClockWidth(int desiredWidth){
        String text = clock.getText().toString();
        clock.setTextSize(TypedValue.COMPLEX_UNIT_PX, 1);
        int size = 1;
        do{
            float textWidth = clock.getPaint().measureText(text);

            if (textWidth < desiredWidth) {
                clock.setTextSize(++size);
            } else {
                clock.setTextSize(--size);
                break;
            }
        } while(true);
    }

    public void updateBatteryView() {
        float percentage = battery.getPercentage();
        if (battery.isCharging()) {
            if (percentage < 95.){
                long est = battery.getEstimateMillis()/1000; // estimated seconds
                formatBatteryEstimate(percentage, est);
            }  else if (percentage < 98.) {
                batteryView.setText(String.format("%02d %%", (int) percentage));
            } else {
                batteryView.setText(""); // nothing, if fully charged
            }
        } else { // not charging
            long est = battery.getDischargingEstimateMillis()/1000; // estimated seconds
            formatBatteryEstimate(percentage, est);
        }
    }

    private void formatBatteryEstimate(float percentage, long est) {
        Log.i(TAG, String.valueOf(est));
        if (est > 0){
            long h = est / 3600;
            long m  = ( est % 3600 ) / 60;
            batteryView.setText(String.format("%02d %% -- %02d:%02d",
                                              (int) percentage, (int) h, (int) m));
        } else {
            batteryView.setText(String.format("%02d %%", (int) percentage));
        }
    }

    public void powerConnected() {
        battery = new BatteryStats(mContext);
    }

    public void updateClockPosition() {
        Random random = new Random();
        Point size = utility.getDisplaySize();
        int w = size.x;
        int h = size.y;
        if (Build.VERSION.SDK_INT < 12) {
            // make back panel fully transparent
            clockLayout.setBackgroundColor(Color.parseColor("#00000000"));
            // random x position
            int rxpos = (w - clockLayout.getWidth()) / 2;
            int rypos = (h - clockLayout.getHeight()) / 2; // API level 1

            rxpos = to_range(rxpos, 1, w);
            int i1 = random.nextInt(2 * rxpos);

            // random y position
            rypos = to_range(rypos, 1, h);
            int i2 = 90 + random.nextInt(2 * rypos);

            clockLayout.setPadding(i1, i2, 0, 0);
            clockLayout.invalidate();
        } else {
            // random x position
            int rxpos = w - (int) (clockLayout.getWidth() * clockLayout.getScaleX()); // API level 11
            int i1 = 0;
            if (rxpos > 0) i1 = random.nextInt(rxpos);

            // random y position
            int rypos = h - (int) (clockLayout.getHeight() * clockLayout.getScaleY())-150-90; // API level 11
            // lower 150 px is reserved for alarm clockLayout
            // upper 90 px is for battery stats
            int i2 = 90;
            if (rypos > 0) i2 = 90 + random.nextInt(rypos);

            clockLayout.animate().setDuration(10000).x(i1).y(i2); // api level 12
        }
    }

    private float LIGHT_VALUE_DARK = 4.2f;
    private float LIGHT_VALUE_BRIGHT = 40.0f;
    private float LIGHT_VALUE_DAYLIGHT = 300.0f;

    private int to_range(int value, int min, int max){
        if (value > max) return max;
        if (value < min) return min;
        return value;
    }

    private float to_range(float value, float min, float max){
        if (value > max) return max;
        if (value < min) return min;
        return value;
    }

    public void dimScreen(int millis, float light_value, float add_brightness){
        LIGHT_VALUE_DARK = settings.minIlluminance;
        float v = 0.f;
        float brightness = 0.f;
        if (settings.autoBrightness && Utility.getLightSensor(mContext) != null) {
            float luminance_offset = LIGHT_VALUE_BRIGHT * add_brightness;
            if (light_value > LIGHT_VALUE_BRIGHT && add_brightness > 0.f) {
                luminance_offset = LIGHT_VALUE_DAYLIGHT * add_brightness;
            }
            v = (light_value + luminance_offset - LIGHT_VALUE_DARK)/(LIGHT_VALUE_BRIGHT - LIGHT_VALUE_DARK);
            v = 0.3f + 0.7f * v;

            brightness = (light_value + luminance_offset - LIGHT_VALUE_BRIGHT)/(LIGHT_VALUE_DAYLIGHT - LIGHT_VALUE_BRIGHT);
        } else {
            v = 1.f + add_brightness;
            brightness = add_brightness;

            if ( daydreamMode ) {
                v *= 0.5f;
                brightness = 0.f;
            }
        }

        v = to_range(v, 0.05f, 1.f);
        brightness = to_range(brightness, 0.01f, 1.f);
        // On some screens (as the Galaxy S2) a value of 0 means the screen is completely dark.
        // Therefore a minimum value must be set to preserve the visibility of the clock.

        Log.d(TAG, "light value : " + String.valueOf(light_value));
        Log.d(TAG, "a : " + String.valueOf(v) + " | b : " + String.valueOf(brightness));

        setBrightness(brightness);

        setAlpha(clockLayout, v, millis);
        if ( alarmClock.isClickable() ) {
            setAlpha(alarmClock, v, millis);
            v = to_range(v, 0.6f, 1.f);
            setAlpha(batteryView, v, millis);
            if (! daydreamMode) setAlpha(settingsIcon, v, millis);
        }

        if ( mode == 0 ) {
            setAlpha(notificationbar, 0.0f, millis);
        } else {
            // increase minimum alpha value for the notification bar
            v = to_range(v, 0.6f, 1.f);
            setAlpha(notificationbar, v, millis);
        }

        if ( light_value + 0.2f < settings.minIlluminance ) {
            settings.setMinIlluminance(light_value + 0.2f);
        }
    }

    private void setBrightness(float value) {
        LayoutParams layout = window.getAttributes();
        layout.screenBrightness = value;
        layout.buttonBrightness = 0.f;
        window.setAttributes(layout);
        hideSoftButtons();
    }

    private void hideSoftButtons() {
        if (Build.VERSION.SDK_INT >= 14){
            clockLayout.setSystemUiVisibility(View. SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    }

    public int determineScreenMode(int current_mode, float light_value, double last_ambient_noise){

        LIGHT_VALUE_DARK = settings.minIlluminance;
        double ambient_noise_threshold = settings.NOISE_AMPLITUDE_SLEEP;
        if (current_mode == 0){
            ambient_noise_threshold = settings.NOISE_AMPLITUDE_WAKE;
        }

        if (light_value <= LIGHT_VALUE_DARK
                && ( (settings.ambientNoiseDetection == false)
                    || last_ambient_noise < ambient_noise_threshold)){
            return 0;
        } else if (light_value < LIGHT_VALUE_BRIGHT/2.f) { // night shift, desk light on
            return 1;
        } else if (light_value < LIGHT_VALUE_BRIGHT) { // night shift, desk light on
            return 2;
        }
        // day mode
        return 3;
    }

    public void switchModes(float light_value, double last_ambient_noise){
        int current_mode = mode;
        mode = determineScreenMode(current_mode, light_value, last_ambient_noise);

        if ((mode == 0) && (current_mode != 0)){
            if (settings.muteRinger) AudioManage.setRingerModeSilent();
            background_image.setImageDrawable(bgblack);
        } else
        if ((mode != 0) && (current_mode == 0)){
            if (settings.muteRinger) AudioManage.restoreRingerMode();
            background_image.setImageDrawable(bgshape);
        }

        float dim_offset = settings.dim_offset;
        if ((mode == 1) && (current_mode == 0)) {
            dim_offset += 0.1f;
        }
        dimScreen(3000, light_value, dim_offset);

        if (soundmeter != null) {
            if (mode == 0 && soundmeter.isRunning() == false) {
                soundmeter.startMeasurement(3000);
            } else if (mode == 1 && soundmeter.isRunning() == false){
                soundmeter.startMeasurement(60000);
            } else if (mode > 1) {
                soundmeter.stopMeasurement();
            }
        }
    }

    public void setAlpha(View v, float alpha, int millis){
        if (v == null) return;

        if (Build.VERSION.SDK_INT < 14) {
             final AlphaAnimation animation = new AlphaAnimation(alpha, alpha);
             animation.setDuration(millis);
             animation.setFillAfter(true);
             v.startAnimation(animation);
        } else { // should work from 12 on but had a bug report for 13 !?!
            v.animate().setDuration(millis).alpha(alpha);
        }
    }

    // only called for apilevel >= 12
    private Runnable zoomIn = new Runnable() {
        @Override
        public void run() {
            handler.post(zoomToFit);
            clockLayout.animate().setDuration(1000).scaleX(1.f).scaleY(1.f);
       }
    };

    // only called for apilevel >= 12
    private Runnable zoomToFit = new Runnable() {
        @Override
        public void run() {
            Point d = utility.getDisplaySize();
            setDesiredClockWidth((int) (0.6 * d.x));
       }
    };

    // move the clock randomly around
    private Runnable moveAround = new Runnable() {
       @Override
       public void run() {
           removeCallbacks(hideBrightnessLevel);
           updateBatteryView();
           updateClockPosition();

           handler.postDelayed(this, 60000);
       }
    };

    private Runnable hideBrightnessLevel = new Runnable() {
       @Override
       public void run() {
           setAlpha(batteryView, 0.f, 2000);
           handler.postDelayed(hideBrightnessText, 2100);
       }
    };

    private Runnable hideAlarmClock = new Runnable() {
       @Override
       public void run() {
           if ( alarmClock.isInteractive() || AlarmService.isRunning) {
               handler.postDelayed(hideAlarmClock, 20000);
               return;
           }
           setAlpha(batteryView, 0.f, 2000);
           if (! daydreamMode) setAlpha(settingsIcon, 0.f, 2000);
           alarmClock.isVisible = false;
           alarmClock.setClickable(false);
           setAlpha(alarmClock, 0.f, 2000);
       }
    };

    private Runnable hideBrightnessText = new Runnable() {
       @Override
       public void run() {
           updateBatteryView();
           setAlpha(batteryView, 1.f, 100);
       }
    };

    // in only called for api level >= 12
    private Runnable ClickOut = new Runnable() {
       @Override
       public void run() {
            if (Build.VERSION.SDK_INT >= 12){
               clockLayout.animate().setDuration(1000).scaleX(1.f).scaleY(1.f);
            }
       }
    };

    public void onClockClicked(float last_ambient) {
        if (Build.VERSION.SDK_INT >= 12){
            clockLayout.animate().setDuration(100).scaleXBy(-0.15f).scaleYBy(-0.15f);
            handler.postDelayed(ClickOut, 100);
        }
        showAlarmClock(last_ambient);
    }

    public void showAlarmClock(float last_ambient) {
        removeCallbacks(hideAlarmClock);
        handler.postDelayed(hideAlarmClock, 20000);
        alarmClock.isVisible = true;
        alarmClock.setClickable(true);
        dimScreen(0, last_ambient, settings.dim_offset);
    }

    public boolean onTouch(View view, MotionEvent e, float last_ambient) {
        if (utility == null) return false;
        boolean event_consumed = false;
        Point click = new Point((int) e.getX(),(int) e.getY());
        Point size = utility.getDisplaySize();

        // handle the visibility of the alarm clock
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                showAlarmClock(last_ambient);
                event_consumed = true;
                break;
            case MotionEvent.ACTION_UP:
                removeCallbacks(hideAlarmClock);
                handler.postDelayed(hideAlarmClock, 20000);
                event_consumed = true;
                break;
        }

        // set the screen brightness factor
        if (click.y < 0.2 * size.y) {// upper 20% of the screen
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    removeCallbacks(hideBrightnessLevel);
                    setDimOffset = true;
                    dim_offset_init_x = click.x;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (setDimOffset == false) return false;
                    dim_offset_curr_x = click.x;

                    float dx = 2.f * (dim_offset_curr_x - dim_offset_init_x) / size.x;
                    settings.dim_offset += dx;
                    settings.dim_offset = to_range(settings.dim_offset, -1.f, 1.f);

                    int c = (int) ( (settings.dim_offset + 1.f) / 2.f * 11.f);
                    String s = "";
                    for (int i = 0; i < c; i++) s +="|";

                    batteryView.setText(s);
                    setAlpha(batteryView, 1.f, 200);
                    setAlpha(notificationbar, 1.f, 200);
                    removeCallbacks(hideBrightnessLevel);

                    dim_offset_init_x = click.x;

                    dimScreen(0, last_ambient, settings.dim_offset);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (setDimOffset == true){
                        setDimOffset = false;
                        settings.setBrightnessOffset(settings.dim_offset);
                        handler.postDelayed(hideBrightnessLevel, 1000);
                        //dimScreen(10000, last_ambient, settings.dim_offset);
                        return true;
                    }
                    break;
            }
        }
        return event_consumed;
    }

    public Drawable loadBackGroundImage() {
        if (settings.bgpath != ""){
            try{
                Point display = utility.getDisplaySize();

                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(settings.bgpath, options);

                // Calculate inSampleSize
                options.inSampleSize = Utility.calculateInSampleSize(options, display.x, display.y);

                // Decode bitmap with inSampleSize set
                options.inJustDecodeBounds = false;
                Bitmap bgimage = (BitmapFactory.decodeFile(settings.bgpath, options));

                int nw = bgimage.getWidth();
                int nh = bgimage.getHeight();
                boolean scaling_needed =false;
                if ( bgimage.getHeight() > display.y ){
                    nw = (int) ((display.y /(float) bgimage.getHeight()) * bgimage.getWidth());
                    nh = display.y;
                    scaling_needed = true;
                }

                if ( nw > display.x ){
                    nh = (int) ((display.x / (float) bgimage.getWidth()) * bgimage.getHeight());
                    nw = display.x;
                    scaling_needed = true;
                }
                if (scaling_needed){
                    bgimage = Bitmap.createScaledBitmap(bgimage,nw, nh, false);
                }

                return new BitmapDrawable(mContext.getResources(), bgimage);
            } catch (OutOfMemoryError e){
                Toast.makeText(mContext, "Out of memory. Please, try to scale down your image.",
                               Toast.LENGTH_LONG).show();
                return new ColorDrawable(Color.parseColor("#000000"));
            } catch (Exception e) {
                return new ColorDrawable(Color.parseColor("#000000"));
            }
        }
        return new ColorDrawable(Color.parseColor("#000000"));
    }

    private void checkForReviewRequest() {
        if (Build.VERSION.SDK_INT < 11) return;

        // ask only once
        if (settings.lastReviewRequestTime != 0L) return;

        long firstInstallTime = utility.getFirstInstallTime(mContext);
        Log.i(TAG, "First install time: " + String.valueOf(firstInstallTime));
        Calendar install_time = Calendar.getInstance();
        install_time.setTimeInMillis(firstInstallTime);

        Calendar two_months_ago = Calendar.getInstance();
        two_months_ago.add(Calendar.DATE, -60);

        if (install_time.before(two_months_ago)) {
            sendReviewRequest();
            settings.setLastReviewRequestTime(Calendar.getInstance().getTimeInMillis());
        }
    }

    private void sendReviewRequest() {

        final Uri uri = Uri.parse("market://details?id=" + mContext.getPackageName());
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        if (mContext.getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
            PendingIntent pIntent = PendingIntent.getActivity(mContext, 0, intent, 0);

            // build notification
            Notification n = new Notification.Builder(mContext)
                .setContentTitle(mContext.getString(R.string.app_name))
                .setContentText(mContext.getString(R.string.review_request))
                .setSmallIcon(R.drawable.ic_clock)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .build();

            n.defaults |= Notification.DEFAULT_SOUND;
            n.defaults |= Notification.DEFAULT_ALL;
            NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(mContext.NOTIFICATION_SERVICE);

            notificationManager.notify(0, n);

        }
        else {
            /* handle your error case: the device has no way to handle market urls */
        }
    }
}
