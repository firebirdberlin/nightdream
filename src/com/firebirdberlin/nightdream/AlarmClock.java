package com.firebirdberlin.nightdream;

import android.app.AlarmManager;
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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
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

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.services.AlarmHandlerService;

import static android.text.format.DateFormat.getBestDateTimePattern;
import static android.text.format.DateFormat.is24HourFormat;


public class AlarmClock extends View {
    private static String TAG ="NightDream.AlarmClock";
    private static enum Position { LEFT, RIGHT }

    final private Handler handler = new Handler();
    public boolean isVisible = false;
    private boolean FingerDown;
    private boolean userChangesAlarmTime = false;
    private boolean FingerDownDeleteAlarm;
    private Context ctx;
    private int customcolor = Color.parseColor("#33B5E5");
    private int customSecondaryColor = Color.parseColor("#C2C2C2");
    private int display_height;
    SimpleTime time;
    private int w;
    private Paint paint = new Paint();
    private Settings settings = null;
    public int touch_zone_radius = 150;
    public int quiet_zone_size = 60;
    private ColorFilter customColorFilter;
    private ColorFilter customColorFilterImage;
    private ColorFilter secondaryColorFilter;
    private HotCorner cornerLeft;
    private HotCorner cornerRight;
    private boolean blinkStateOn = false;

    public AlarmClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.ctx = context;
        cornerLeft = new HotCorner(Position.LEFT);
        cornerLeft.setIconResource(getResources(), R.drawable.ic_audio);
        cornerRight = new HotCorner(Position.RIGHT);
        cornerRight.setIconResource(getResources(), R.drawable.ic_no_audio);
        initColorFilters();
    }

    public void setSettings(Settings s) {
        settings = s;
        time = new SimpleTime(s.nextAlarmTime);
    }

    public void setCustomColor(int primary, int secondary) {
        customcolor = primary;
        customSecondaryColor = secondary;
        initColorFilters();
        this.invalidate();
    }

    private void initColorFilters() {
        customColorFilter = new LightingColorFilter(customcolor, 1);
        customColorFilterImage = new PorterDuffColorFilter(customSecondaryColor, PorterDuff.Mode.SRC_ATOP);
        secondaryColorFilter = new LightingColorFilter(customSecondaryColor, 1);
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

        Point click = getClickedPoint(e);
        Point ll = new Point(0, getHeight()); // lower left corner
        float dist = distance(click, ll);

        // set alarm clock
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Allow start only in the lower left corner
                if (dist > quiet_zone_size && dist < touch_zone_radius) { // left corner
                    FingerDown = true;
                    this.invalidate();
                    return true;
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                if (FingerDown == false) return false;
                if ( dist > touch_zone_radius ) {
                    userChangesAlarmTime = true;
                    cancelAlarm();
                    XYtotime(tX,tY);
                }
                else {
                    userChangesAlarmTime = false;
                }
                this.invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (FingerDown == false) return false;
                if ( dist > touch_zone_radius ) {
                    XYtotime(tX,tY);
                    setAlarm();
                }
                FingerDown = false;
                userChangesAlarmTime = false;
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
                    stopAlarm();
                    ctx.sendBroadcast( new Intent(Config.ACTION_ALARM_DELETED) );
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

        int hours = (int) (x/w * 24);
        int mins = (int) ((y/h * 60)) / 5 * 5;
        time.hour = (hours >= 24) ? 23 : hours;
        time.min = (mins >= 60) ? 0 : mins;
    }

    @Override
    protected void onDraw(Canvas canvas){
        if ( !isVisible ) return;
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
            cornerLeft.setPosition(0, h);
            cornerLeft.setRadius(touch_zone_radius);
            cornerLeft.setActive(FingerDown);
            cornerLeft.draw(canvas, paint);
        }

        // right corner
        if (isAlarmSet() || userChangesAlarmTime){
            cornerRight.setPosition(w, h);
            cornerRight.setRadius(touch_zone_radius);
            cornerRight.setActive(FingerDownDeleteAlarm || blinkStateOn);
            cornerRight.draw(canvas, paint);

            paint.setColorFilter(secondaryColorFilter);

            String l = "";
            if ( userChangesAlarmTime ) {
                l = getTimeFormatted(time.getCalendar());
            } else
            if ( isAlarmSet() ) {
                l = getTimeFormatted(settings.getAlarmTime());
            }

            paint.setTextSize(touch_zone_radius * .5f);
            float lw = paint.measureText(l);
            float cw = touch_zone_radius - 60;
            if ((touch_zone_radius) <= 100)  cw = 0;
            if (userChangesAlarmTime || isAlarmSet()){
                paint.setColor(Color.WHITE);
                canvas.drawText(l, w/2-(lw+cw)/2 + cw, h-touch_zone_radius/3, paint );
            }

            if ((touch_zone_radius) > 100){ // no image on on small screens
                paint.setColorFilter(customColorFilterImage);
                Bitmap ic_alarmclock = BitmapFactory.decodeResource(res, R.drawable.ic_alarm_clock);
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(ic_alarmclock, touch_zone_radius-60, touch_zone_radius-60, false);
                canvas.drawBitmap(resizedBitmap, w/2 - (lw+cw)/2 - cw/2, h-touch_zone_radius+30, paint);
            }
        }
    }

    public void activateAlarmUI() {
        handler.removeCallbacks(blink);
        if (AlarmHandlerService.alarmIsRunning()) {
            handler.postDelayed(blink, 1000);
        }
    }

    Runnable blink = new Runnable() {
        public void run() {
            handler.removeCallbacks(blink);
            blinkStateOn = !blinkStateOn;
            invalidate();
            if (AlarmHandlerService.alarmIsRunning()) {
                handler.postDelayed(blink, 1000);
            }
        }
    };

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

    public boolean isAlarmSet() {
        return (settings.nextAlarmTime > 0L);
    }

    public void stopAlarm(){
        handler.removeCallbacks(blink);
        this.blinkStateOn = false;
        AlarmHandlerService.stop(ctx);
    }

    public void snooze() {
        handler.removeCallbacks(blink);
        this.blinkStateOn = false;
        AlarmHandlerService.snooze(ctx);
    }

    private void setAlarm() {
        setAlarm(time.getMillis());
    }

    private void setAlarm(long alarmTimeInMillis) {
        handler.removeCallbacks(blink);
        this.blinkStateOn = false;
        settings.nextAlarmTime = alarmTimeInMillis;
        AlarmHandlerService.set(ctx, alarmTimeInMillis);
    }

    public void cancelAlarm(){
        if (settings.nextAlarmTime > 0L) {
            AlarmHandlerService.cancel(ctx);
            settings.nextAlarmTime = 0L;
        }
    }

    public String getNextSystemAlarmTime() {
        if ( Build.VERSION.SDK_INT < 21 ) {
            return deprecatedGetNextSystemAlarmTime();
        }
        AlarmManager am = (AlarmManager) (ctx.getSystemService( Context.ALARM_SERVICE ));
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

    class HotCorner {
        Point center;
        int radius;
        int radius2;
        int radius3;
        int radius4;
        boolean activated = false;
        Bitmap icon;
        Position position = Position.LEFT;

        public HotCorner(Position position) {
            this.center = new Point();
            this.setRadius(100);
            this.position = position;
        }

        public void setActive(boolean activated) {
            this.activated = activated;
        }

        public void setPosition(Point center) {
            this.center = center;
        }

        public void setPosition(int x, int y) {
            this.center.x = x;
            this.center.y = y;
        }


        public void setRadius(int radius) {
            this.radius = radius;
            this.radius2 = (int) (0.93 * radius);
            this.radius3 = (int) (0.86 * radius);
            this.radius4 = (int) (0.6  * radius);
        }

        public void setIconResource(Resources res, int iconID) {
            this.icon = BitmapFactory.decodeResource(res, iconID);
        }

        public void draw(Canvas canvas, Paint paint) {
            paint.setColor(Color.WHITE);
            paint.setAlpha( ( activated ) ? 255 : 153 );
            canvas.drawCircle(center.x, center.y, radius, paint);

            paint.setColor(Color.BLACK);
            canvas.drawCircle(center.x, center.y, radius2, paint);

            paint.setColor(Color.WHITE);
            paint.setAlpha( ( activated ) ? 153 : 102 );
            canvas.drawCircle(center.x, center.y, radius3, paint);

            Bitmap resizedIcon = Bitmap.createScaledBitmap(icon, radius4, radius4, false);
            if (position == Position.LEFT) {
                canvas.drawBitmap(resizedIcon, 5, center.y - radius4 - 5, paint);
            } else {
                canvas.drawBitmap(resizedIcon, center.x - radius4 - 5, center.y - radius4 - 5, paint);
            }
        }
    }
}
