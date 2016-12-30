package com.firebirdberlin.nightdream;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import java.lang.Math;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.services.AlarmService;
import com.firebirdberlin.nightdream.services.RadioStreamService;

import static android.text.format.DateFormat.getBestDateTimePattern;
import static android.text.format.DateFormat.is24HourFormat;


public class AlarmClock extends View {
    private static String TAG ="NightDream.AlarmClock";
    final private Handler handler = new Handler();
    public boolean isVisible = false;
    private boolean FingerDown;
    private boolean FingerDownDeleteAlarm;
    private Context ctx;
    private int customcolor = Color.parseColor("#33B5E5");
    private int customSecondaryColor = Color.parseColor("#C2C2C2");
    private int display_height;
    private int hour, min;
    private int w;
    private Paint paint = new Paint();
    private static AlarmManager am = null;
    private Settings settings = null;
    public int touch_zone_radius = 150;
    public int quiet_zone_size = 60;

    public AlarmClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.ctx = context;

        if (am == null) {
            am = (AlarmManager) (ctx.getSystemService( Context.ALARM_SERVICE ));
        }
    }

    public void setSettings(Settings s) {
        settings = s;
        SimpleTime time = new SimpleTime(s.nextAlarmTime);
        hour = time.hour;
        min = time.min;
    }

    public void setCustomColor(int primary, int secondary) {
        customcolor = primary;
        customSecondaryColor = secondary;
    }

    public boolean isInteractive() {
        return (FingerDown || FingerDownDeleteAlarm);
    }

    private float distance(Point a, Point b){
        return (float) Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }

    private Point getClickedPoint(MotionEvent e) {
        return new Point((int) e.getX(), (int) e.getY());
    }

    public boolean onTouchEvent(MotionEvent e) {
        if (AlarmService.isRunning) stopAlarm();
        if (RadioStreamService.isRunning) stopRadioStream();

        // the view should be visible before the user interacts with it
        if (! isVisible ) return false;

        boolean eventCancelAlarm = handleAlarmCancelling(e);
        if (eventCancelAlarm) return true;

        boolean eventAlarmSet = handleAlarmSetEvents(e);
        if (eventAlarmSet) return true;
        return false;
    }

    private boolean handleAlarmSetEvents(MotionEvent e) {
        float tX = e.getX();
        float tY = e.getY();


        // set alarm clock
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Allow start only in the lower left corner
                Point click = getClickedPoint(e);
                Point ll = new Point(0, getHeight()); // lower left corner
                float dist = distance(click, ll);
                if (dist > quiet_zone_size && dist < touch_zone_radius) { // left corner
                    FingerDown = true;
                    cancelAlarm();
                    XYtotime(tX,tY);
                    this.invalidate();
                    return true;
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                if (FingerDown == false) return false;
                XYtotime(tX,tY);
                this.invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (FingerDown == false) return false;
                FingerDown = false;

                XYtotime(tX,tY);
                setAlarm();

                this.invalidate();
                break;
        }
        return false;
    }

    private boolean handleAlarmCancelling(MotionEvent e){
        Point click = getClickedPoint(e);
        Point lr = new Point(getWidth(), getHeight()); // lower right corner

        // set alarm clock
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                float dist = distance(click, lr);
                if (dist > quiet_zone_size && dist < touch_zone_radius) { // right corner
                    FingerDownDeleteAlarm = true;
                    this.invalidate();
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
                if (FingerDownDeleteAlarm == true) {
                    FingerDownDeleteAlarm = false;
                    cancelAlarm();
                    this.invalidate();
                    return true;
                }
                return false;
        }
        return false;
    }

    private void XYtotime(float x, float y) {
        int w = getWidth() - 2 * touch_zone_radius;
        int h = new Utility(ctx).getDisplaySize().y - 2 * touch_zone_radius;

        x -= touch_zone_radius;
        x = (x < 0) ? 0 : x;

        // the coordinate is negative outside the view
        y *= -1.f;
        y += getHeight();

        int hours = (int) (x/w * 24);
        int mins = (int) ((y/h * 60)) / 5 * 5;
        hour = (hours >= 24) ? 23 : hours;
        min = (mins >= 60) ? 55 : mins;
    }

    @Override
    protected void onDraw(Canvas canvas){
        if ( !isVisible ) return;
        ColorFilter customColorFilter = new LightingColorFilter(customcolor, 1);
        ColorFilter secondaryColorFilter = new LightingColorFilter(customSecondaryColor, 1);
        paint.setColorFilter(customColorFilter);

        int w = getWidth();
        int h = getHeight();
        // touch zones

        // set size of the touch zone
        touch_zone_radius = (w < h) ? w : h;
        quiet_zone_size = touch_zone_radius/4;

        int tzr2 = (int) (0.93 * touch_zone_radius);
        int tzr3 = (int) (0.86 * touch_zone_radius);
        int tzr4 = (int) (0.6  * touch_zone_radius);

        Resources res = getResources();
        // left corner
        {
            paint.setColor(Color.WHITE);
            if (FingerDown == true) paint.setAlpha(255);
            else paint.setAlpha(153);

            canvas.drawCircle(0, h, touch_zone_radius, paint);

            paint.setColor(Color.BLACK);
            canvas.drawCircle(0, h, tzr2, paint);

            paint.setColor(Color.WHITE);
            if (FingerDown == true) paint.setAlpha(153);
            else paint.setAlpha(102);

            canvas.drawCircle(0, h, tzr3, paint);

            Bitmap ic_audio = BitmapFactory.decodeResource(res, R.drawable.ic_audio);
            Bitmap resizedIcon = Bitmap.createScaledBitmap(ic_audio, tzr4, tzr4, false);
            canvas.drawBitmap(resizedIcon, 5, h - tzr4 - 5, paint);
        }

        // right corner
        if (isAlarmSet() || FingerDown){
            Bitmap ic_alarmclock = BitmapFactory.decodeResource(res, R.drawable.ic_alarmclock);
            Bitmap ic_no_audio = BitmapFactory.decodeResource(res, R.drawable.ic_no_audio);

            paint.setColor(Color.WHITE);
            if (FingerDownDeleteAlarm == true) paint.setAlpha(255);
            else paint.setAlpha(153);
            canvas.drawCircle(w, h, touch_zone_radius, paint);

            paint.setColor(Color.BLACK);
            canvas.drawCircle(w, h, tzr2, paint);

            paint.setColor(Color.WHITE);
            if (FingerDownDeleteAlarm == true) paint.setAlpha(153);
            else paint.setAlpha(102);

            canvas.drawCircle(w, h, tzr3, paint);

            Bitmap resizedIcon = Bitmap.createScaledBitmap(ic_no_audio, tzr4, tzr4, false);
            canvas.drawBitmap(resizedIcon, w - tzr4 - 5, h - tzr4 - 5, paint);

            paint.setColorFilter(secondaryColorFilter);

            String l = "";
            if ( FingerDown ) {
                l = getTimeFormatted(new SimpleTime(hour, min).getCalendar());
            } else
            if ( isAlarmSet() ) {
                l = getTimeFormatted(settings.getAlarmTime());
            }

            paint.setTextSize(touch_zone_radius * .5f);
            float lw = paint.measureText(l);
            float cw = touch_zone_radius - 60;
            if ((touch_zone_radius) <= 100)  cw = 0;
            if (FingerDown || isAlarmSet()){
                paint.setColor(Color.WHITE);
                canvas.drawText(l, w/2-(lw+cw)/2 + cw, h-touch_zone_radius/3, paint );
            }

            if ((touch_zone_radius) > 100){ // no image on on small screens
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(ic_alarmclock, touch_zone_radius-60, touch_zone_radius-60, false);
                canvas.drawBitmap(resizedBitmap, w/2 - (lw+cw)/2 - cw/2, h-touch_zone_radius+30, paint);
            }
        }
    }

    private String getTimeFormatted(Calendar calendar) {
        String localPattern  = "";
        if (Build.VERSION.SDK_INT >= 18){
            if (is24HourFormat(ctx)) {
                localPattern = getBestDateTimePattern(Locale.getDefault(), "EE HH:mm");
            } else {
                localPattern = getBestDateTimePattern(Locale.getDefault(), "EE hh:mm a");
            }
        } else {
            DateFormat formatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
            localPattern  = ((SimpleDateFormat)formatter).toLocalizedPattern();
        }

        SimpleDateFormat hourDateFormat = new SimpleDateFormat(localPattern, Locale.getDefault());
        return hourDateFormat.format(calendar.getTime());
    }

    public boolean isAlarmSet(){
        return (settings.nextAlarmTime > 0L);
    }

    public void startAlarm(){
        Log.i(TAG, "startAlarm()");
        handler.postDelayed(stopRunningAlarm, 120000); // stop it after 2 mins
    }

    public void stopAlarm(){
        handler.post(stopRunningAlarm);
    }

    public void stopRadioStream(){
        RadioStreamService.stop(ctx);
        cancelAlarm();
        invalidate();
    }

    private Runnable stopRunningAlarm = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(stopRunningAlarm);

            AlarmService.stop(ctx);
            cancelAlarm();
            invalidate();
        }
    };

    private void setAlarm() {
        cancelAlarm();
        SimpleTime alarmTime = new SimpleTime(hour, min);
        settings.setAlarmTime(alarmTime.getMillis());
        AlarmClock.schedule(ctx);
    }

    public void cancelAlarm(){
        settings.setAlarmTime(0L);
        PendingIntent pI = getPendingAlarmIntent(ctx);
        am.cancel(pI);
    }

    public String getNextSystemAlarmTime() {
        if ( Build.VERSION.SDK_INT < 21 ) {
            return deprecatedGetNextSystemAlarmTime();
        }
        AlarmManager.AlarmClockInfo info = am.getNextAlarmClock();
        if (info != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(info.getTriggerTime());
            return getTimeFormatted(cal);
        }
        return "";
    }

    @SuppressWarnings("deprecation")
    private String deprecatedGetNextSystemAlarmTime() {
         return android.provider.Settings.System.getString(
                 ctx.getContentResolver(),
                 android.provider.Settings.System.NEXT_ALARM_FORMATTED);
    }


    public static void schedule(Context context) {
        Settings settings = new Settings(context);
        if (settings.nextAlarmTime == 0L) return;
        PendingIntent pI = getPendingAlarmIntent(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pI);
        if (Build.VERSION.SDK_INT >= 21) {
            AlarmManager.AlarmClockInfo info =
                new AlarmManager.AlarmClockInfo(settings.nextAlarmTime, pI);
            alarmManager.setAlarmClock(info, pI);
        } else
        if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, settings.nextAlarmTime, pI );
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, settings.nextAlarmTime, pI );
        }
    }

    private static PendingIntent getPendingAlarmIntent(Context context) {
        Intent intent = new Intent("com.firebirdberlin.nightdream.WAKEUP");
        intent.putExtra("action", "start alarm");
        //return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        // PendingIntent.FLAG_CANCEL_CURRENT seems to confuse AlarmManager.cancel() on certain
        // Android devices, e.g. HTC One m7, i.e. AlarmManager.getNextAlarmClock() still returns
        // already cancelled alarm times afterwards.
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }
}
