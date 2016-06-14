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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static android.text.format.DateFormat.getBestDateTimePattern;
import static android.text.format.DateFormat.is24HourFormat;

class AlarmClock extends View {
    private static String TAG ="NightDream.AlarmClock";
    final private Handler handler = new Handler();
    public boolean isVisible = false;
    private boolean FingerDown;
    private boolean FingerDownDeleteAlarm;
    private Context ctx;
    private float tX, tY;
    private int customcolor = Color.parseColor("#33B5E5");
    private int customSecondaryColor = Color.parseColor("#C2C2C2");
    private int h;
    private int hour, min;
    private int w;
    private Paint paint = new Paint();
    private static AlarmManager am = null;
    private Settings settings = null;
    private Utility utility;
    public int touch_zone_radius = 150;

    public AlarmClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.ctx = context;

        if (am == null) {
            am = (AlarmManager) (ctx.getSystemService( Context.ALARM_SERVICE ));
        }
    }

    public void setUtility(Utility u) {utility = u;}
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

        // the view should be visible before the user interacts with it
        if (! isVisible ) return false;

        boolean eventCancelAlarm = handleAlarmCancelling(e);
        if (eventCancelAlarm) return true;

        boolean eventAlarmSet = handleAlarmSetEvents(e);
        if (eventAlarmSet) return true;
        return false;
    }

    private boolean handleAlarmSetEvents(MotionEvent e) {
        tX = e.getX();
        tY = e.getY();

        Point click = getClickedPoint(e);
        Point size = utility.getDisplaySize();

        Point ll = new Point(0, size.y); // lower left corner

        // set alarm clock
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Allow start only in the top left corner
                if (distance(click, ll) < touch_zone_radius) { // left corner
                    FingerDown = true;
                    removeAlarm();
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
        Point size = utility.getDisplaySize();
        Point lr = new Point(size.x, size.y); // lower right corner

        // set alarm clock
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (distance(click, lr) < touch_zone_radius) { // right corner
                    FingerDownDeleteAlarm = true;
                    this.invalidate();
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
                if (FingerDownDeleteAlarm == true) {
                    FingerDownDeleteAlarm = false;
                    removeAlarm();
                    this.invalidate();
                    return true;
                }
                return false;
        }
        return false;
    }

    private void XYtotime(float x, float y) {
        Point size = utility.getDisplaySize();
        int w = size.x;
        int h = size.y;
        float hours = x/w * 24;
        float mins = (1.f - y/h) * 60;
        hour = (hours >= 24.f) ? 23 : (int) hours;
        min = (mins >= 60.f) ? 0 : (int) mins;
    }

    @Override
    protected void onDraw(Canvas canvas){
        if ( !isVisible ) return;
        Point size = utility.getDisplaySize();
        ColorFilter customColorFilter = new LightingColorFilter(customcolor, 1);
        ColorFilter secondaryColorFilter = new LightingColorFilter(customSecondaryColor, 1);
        paint.setColorFilter(customColorFilter);

        int w = size.x;
        int h = size.y;
        // touch zones

        // set size of the touch zone
        if (size.x < size.y) touch_zone_radius = size.x/5;
        else touch_zone_radius = size.y/5;
        touch_zone_radius = (touch_zone_radius > 180) ? 180 : touch_zone_radius;

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

            Calendar calendar = new SimpleTime(hour, min).getCalendar();
            String l = getTimeFormatted(calendar);

            paint.setTextSize(touch_zone_radius * .6f);
            float lw = paint.measureText(l);
            float cw = touch_zone_radius-60;
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
                localPattern = getBestDateTimePattern(Locale.getDefault(), "HH:mm");
            } else {
                localPattern = getBestDateTimePattern(Locale.getDefault(), "hh:mm a");
            }
        } else {
            DateFormat formatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
            localPattern  = ((SimpleDateFormat)formatter).toLocalizedPattern();
        }

        SimpleDateFormat hourDateFormat = new SimpleDateFormat(localPattern, Locale.getDefault());
        return hourDateFormat.format(calendar.getTime());
    }

    public boolean isAlarmSet(){return (settings.nextAlarmTime > 0L);}

    public void startAlarm(){
        if ( isAlarmSet() ) {
            try {
                //utility.AlarmPlay();
                Intent i = new Intent(ctx, AlarmService.class);
                i.putExtra("start alarm", true);
                ctx.startService(i);
            } catch (Exception e) {}
            handler.postDelayed(stopRunningAlarm, 120000); // stop it after 2 mins
        }
    }

    public void stopAlarm(){
        handler.post(stopRunningAlarm);
    }

    private Runnable stopRunningAlarm = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(stopRunningAlarm);
            //utility.AlarmStop();
            Intent i = new Intent(ctx, AlarmService.class);
            i.putExtra("stop alarm", true);
            ctx.startService(i);

            removeAlarm();
            invalidate();
        }
    };

    private void setAlarm() {
        removeAlarm();
        SimpleTime alarmTime = new SimpleTime(hour, min);
        PendingIntent pI = getPendingAlarmIntent(ctx);
        if (Build.VERSION.SDK_INT >= 19){
            am.setExact(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), pI );
        } else {
            am.set(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), pI );
        }
        settings.setAlarmTime(alarmTime.getMillis());
    }

    public void removeAlarm(){
        settings.setAlarmTime(0L);
        PendingIntent pI = getPendingAlarmIntent(ctx);
        am.cancel(pI);
    }

    public static void schedule(Context context) {
        Settings settings = new Settings(context);
        if (settings.nextAlarmTime == 0L) return;
        PendingIntent pI = getPendingAlarmIntent(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= 19){
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, settings.nextAlarmTime, pI );
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, settings.nextAlarmTime, pI );
        }
    }

    private static PendingIntent getPendingAlarmIntent(Context context) {
        Intent intent = new Intent("com.firebirdberlin.nightdream.WAKEUP");
        intent.putExtra("cmd", "start alarm");
        return PendingIntent.getBroadcast( context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }
}
