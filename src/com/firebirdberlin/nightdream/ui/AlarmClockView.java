package com.firebirdberlin.nightdream.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.SetAlarmClockActivity;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.services.AlarmHandlerService;
import com.firebirdberlin.nightdream.services.SqliteIntentService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static android.text.format.DateFormat.getBestDateTimePattern;
import static android.text.format.DateFormat.is24HourFormat;


public class AlarmClockView extends View {
    private static final String TAG ="AlarmClockView";
    final private Handler handler = new Handler();
    public int touch_zone_radius = 150;
    public int quiet_zone_size = 60;
    SimpleTime time = null;
    GestureDetector mGestureDetector;
    GestureDetector.SimpleOnGestureListener mSimpleOnGestureListener = new LocalSimpleOnGestureListener();
    private boolean locked = false;
    private boolean FingerDown;
    private boolean userChangesAlarmTime = false;
    private boolean FingerDownDeleteAlarm = false;
    private boolean useAlarmSwipeGesture = false;
    private boolean useSingleTap = true;
    private boolean useLongPress = false;
    private final Context ctx;
    private int customColor = Color.parseColor("#33B5E5");
    private final Paint paint = new Paint();
    private final Rect alarmTimeRect = new Rect(0, 0, 0, 0);
    private ColorFilter customColorFilter;
    private final HotCorner cornerLeft;
    private final HotCorner cornerRight;
    private boolean blinkStateOn = false;
    Runnable blink = new Runnable() {
        public void run() {
            handler.removeCallbacks(blink);
            if (alarmIsRunning()) {
                blinkStateOn = !blinkStateOn;
                invalidate();
                handler.postDelayed(blink, 1000);
            } else {
                blinkStateOn = false;
                invalidate();
            }
        }
    };
    private Float lastMoveEventY = null;
    private int lastMinSinceDragStart = 0;
    private NightDreamBroadcastReceiver broadcastReceiver = null;
    private onAlarmChangeListener listener;

    public AlarmClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.ctx = context;

        mGestureDetector = new GestureDetector(context, mSimpleOnGestureListener);
        cornerLeft = new HotCorner(Position.LEFT);
        cornerLeft.setIconResource(getResources(), R.drawable.ic_alarm_clock);
        cornerRight = new HotCorner(Position.RIGHT);
        cornerRight.setIconResource(getResources(), R.drawable.ic_no_alarm_clock);
        initColorFilters();
    }

    public void setUseAlarmSwipeGesture(boolean enabled) {
        useAlarmSwipeGesture = enabled;
    }

    public void setUseSingleTap(boolean useSingleTap) {
        this.useSingleTap = useSingleTap;
    }

    public void setUseLongPress(boolean useLongPress) {
        this.useLongPress = useLongPress;
    }

    public void setOnAlarmChangedListener(onAlarmChangeListener listener) {
        this.listener = listener;
        postAlarmTime();
    }

    private void postAlarmTime() {
        if (listener != null) {
            listener.onAlarmChanged(getAlarmTimeFormatted());
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        broadcastReceiver = registerBroadcastReceiver();
        SqliteIntentService.broadcastAlarm(this.ctx);
    }

    private NightDreamBroadcastReceiver registerBroadcastReceiver() {
        NightDreamBroadcastReceiver receiver = new NightDreamBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.ACTION_ALARM_SET);
        filter.addAction(Config.ACTION_ALARM_STOPPED);
        filter.addAction(Config.ACTION_ALARM_DELETED);
        LocalBroadcastManager.getInstance(ctx).registerReceiver(receiver, filter);
        return receiver;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unregister(broadcastReceiver);
    }

    private void unregister(BroadcastReceiver receiver) {
        try {
            LocalBroadcastManager.getInstance(ctx).unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void setLocked(boolean on) {
        locked = on;
    }

    public void setCustomColor(int primary) {
        customColor = primary;
        initColorFilters();
        invalidate();
    }

    private void initColorFilters() {
        customColorFilter = new LightingColorFilter(customColor, 1);
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
        Log.d(TAG, String.format("onTouchEvent: %d", e.getAction()));

        // the view should be visible before the user interacts with it
        if (!isClickable() || locked ) return false;

        if (showRightCorner()) {
            boolean success =  mGestureDetector.onTouchEvent(e) || handleAlarmCancelling(e);
            if (success) return true;
        }
        if (showLeftCorner()) {
            return mGestureDetector.onTouchEvent(e) || handleAlarmSetEvents(e);
        }
        return false;
    }

    private boolean handleAlarmSetEvents(MotionEvent e) {
        if ( alarmIsRunning() ) return false;
        float tX = e.getX();
        float tY = e.getY();

        Point click = getClickedPoint(e);
        Point ll = new Point(0, getHeight()); // lower left corner
        float dist = distance(click, ll);

        // set alarm clock
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Allow start only in the lower left corner
                if (dist > quiet_zone_size && cornerLeft.isInside(click)) { // left corner
                    FingerDown = true;
                    this.invalidate();
                    this.lastMoveEventY = tY;
                    this.lastMinSinceDragStart = 0;
                    return true;
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                if (!FingerDown) return false;
                if ( dist > touch_zone_radius ) {
                    userChangesAlarmTime = true;
                    cancelAlarm();
                    XYtotime(tX,tY);
                }
                else {
                    userChangesAlarmTime = false;
                }
                this.invalidate();
                this.lastMoveEventY = tY;
                break;
            case MotionEvent.ACTION_UP:
                if (!FingerDown) return false;
                if ( dist > touch_zone_radius ) {
                    XYtotime(tX,tY);
                    setAlarm();
                }
                FingerDown = false;
                userChangesAlarmTime = false;
                this.invalidate();
                this.lastMoveEventY = null;
                break;
        }
        return false;
    }

    private boolean handleAlarmCancelling(MotionEvent e){
        Point click = getClickedPoint(e);
        Point lr = new Point(getWidth(), getHeight()); // lower right corner
        float dist = distance(click, lr);
        boolean isInside = dist > quiet_zone_size && cornerRight.isInside(click);

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isInside) {
                    FingerDownDeleteAlarm = true;
                    invalidate();
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
                FingerDownDeleteAlarm = false;
                invalidate();
                return false;
        }
        return false;
    }

    private void XYtotime(float x, float y) {
        int w = getWidth() - 2 * touch_zone_radius;
        int h = Utility.getDisplaySize(ctx).y - 2 * touch_zone_radius;

        final boolean movingDown = (lastMoveEventY != null && y > lastMoveEventY);

        x -= touch_zone_radius;
        x = (x < 0) ? 0 : x;

        // the coordinate is negative outside the view
        y *= -1.f;

        // adjust time in 5-minute intervals when dragging upwards, and 1-minute interval when dragging downwards.
        int roundTo = (movingDown) ? 1 : 5;
        int hours = (int) (x/w * 24);
        int mins = (int) ((y/h * 60)) / roundTo * roundTo;
        if (movingDown) {
           // make sure time never increases while dragging downwards
           mins = Math.min(mins, lastMinSinceDragStart);
        } else {
           // make sure time never decreases while dragging upwards
           mins = Math.max(mins, lastMinSinceDragStart);
        }
        lastMinSinceDragStart = mins; //save mins, but without going back from value 60 to 0

        setAlarmTime(( hours >= 24 ) ? 23 : hours,
                     ( mins >= 60 || mins < 0 ) ? 0 : mins);
    }

    private void setAlarmTime(int hour, int min) {
        if (time == null) {
            time = new SimpleTime();
        }
        time.hour = hour;
        time.min = min;
        postAlarmTime();
    }

    @Override
    protected void onDraw(Canvas canvas){
        Log.w(TAG, "onDraw()");
        if ( !isClickable() ) return;

        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setColorFilter(customColorFilter);

        int w = getWidth();
        int h = getHeight();

        // touch zones
        touch_zone_radius = Math.min(w, h);
        quiet_zone_size = touch_zone_radius/4;

        if (showLeftCorner()) {
            cornerLeft.setCenter(0, h);
            cornerLeft.setRadius(touch_zone_radius);
            cornerLeft.setActive(FingerDown);
            cornerLeft.draw(canvas, paint);

        }
        if (showRightCorner()) {
            cornerRight.setCenter(w, h);
            cornerRight.setRadius(touch_zone_radius);
            cornerRight.setIconResource(getResources(),
                    alarmIsRunning() ? R.drawable.ic_no_audio : R.drawable.ic_no_alarm_clock);
            cornerRight.setActive(FingerDownDeleteAlarm || blinkStateOn);
            cornerRight.draw(canvas, paint);
        }
    }

    private boolean showAlarmTime() {
        return (isAlarmSet() || userChangesAlarmTime);
    }

    private boolean showLeftCorner() {
        return FingerDown || (!locked && useAlarmSwipeGesture && !isAlarmSet());

    }

    private boolean showRightCorner() {
        if (locked) return false;
        if (alarmIsRunning()) return true;
        return (isAlarmSet() && !time.isRecurring()) || userChangesAlarmTime;

    }

    private String getAlarmTimeFormatted() {
        if (alarmIsRunning()) {
            SimpleTime current = AlarmHandlerService.getCurrentlyActiveAlarm();
            if (current != null) {
                Calendar cal = current.getTodaysAlarmTIme();
                if (cal != null) {
                    return getTimeFormatted(cal);
                }
            }
        }
        if ((userChangesAlarmTime || isAlarmSet()) && time != null) {
            return getTimeFormatted(time.getCalendar());
        }
        return "";
    }

    private boolean alarmIsRunning() {
        return AlarmHandlerService.alarmIsRunning();
    }

    public void activateAlarmUI() {
        if (locked) return;
        handler.removeCallbacks(blink);
        if (alarmIsRunning()) {
            handler.postDelayed(blink, 1000);
        }
    }

    private String getTimeFormatted(Calendar calendar) {
        Calendar now_in_one_week = Calendar.getInstance();
        now_in_one_week.add(Calendar.DAY_OF_MONTH, 7);
        if (calendar.after(now_in_one_week)) {
            return "";
        }
        String localPattern;
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
        return (this.time != null);
    }

    public void stopAlarm(){
        Log.d(TAG, "stopAlarm()");
        handler.removeCallbacks(blink);
        this.blinkStateOn = false;
        if (!alarmIsRunning()) {
            SqliteIntentService.deleteAlarm(ctx, time);
        }
        time = null;
        AlarmHandlerService.stop(ctx);
    }

    public void snooze() {
        handler.removeCallbacks(blink);
        this.blinkStateOn = false;
        AlarmHandlerService.snooze(ctx);
        toast("S N O O Z E");
    }

    private void setAlarm() {
        handler.removeCallbacks(blink);
        this.blinkStateOn = false;
        time.isActive = true;
        AlarmHandlerService.set(ctx, time);
    }

    public void cancelAlarm(){
        Log.d(TAG, "cancelAlarm()");
        if (isAlarmSet() ) {
            AlarmHandlerService.cancel(ctx);
        }

        this.time = null;
    }

    private void updateTime(SimpleTime time) {
        this.time = time;
        postAlarmTime();
        invalidate();
        if (time == null) {
            Log.w(TAG, "no next alarm");
        } else {
            Log.w(TAG, String.format("next Alarm %02d:%02d", time.hour, time.min));
        }
    }

    private enum Position {LEFT, RIGHT}

    public interface onAlarmChangeListener {
        void onAlarmChanged(String alarmString);
    }

    class LocalSimpleOnGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            super.onSingleTapConfirmed(e);
            Point click = getClickedPoint(e);
            if ((showAlarmTime() && alarmTimeRect.contains(click.x, click.y)) ||
                    (showLeftCorner() && cornerLeft.isInside(click))) {
                SetAlarmClockActivity.start(getContext());
                return true;
            }

            if (showRightCorner() && cornerRight.isInside(click)) {
                if (useSingleTap) {
                    stopAlarm();
                    postAlarmTime();
                }
                else if (useLongPress) {
                    String msg = getResources().getString(R.string.message_stop_alarm_long_press);
                    toast(msg);
                }
                return true;
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            Point click = getClickedPoint(e);
            if (useLongPress && showRightCorner() && cornerRight.isInside(click) ) {
                stopAlarm();
                postAlarmTime();
            }
        }
    }

    class HotCorner {
        Point center;
        int radius;
        int radius2;
        int radius3;
        int radius4;
        boolean activated = false;
        Bitmap icon;
        Bitmap scaledIcon;
        Position position;
        ColorFilter colorFilter = new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);

        HotCorner(Position position) {
            this.center = new Point();
            this.setRadius(100);
            this.position = position;
        }

        void setActive(boolean activated) {
            this.activated = activated;
        }

        public void setCenter(Point center) {
            this.center = center;
        }

        void setCenter(int x, int y) {
            this.center.x = x;
            this.center.y = y;
        }


        void setRadius(int radius) {
            if (radius == this.radius) return;
            this.radius = radius;
            this.radius2 = (int) (0.93 * radius);
            this.radius3 = (int) (0.86 * radius);
            this.radius4 = (int) (0.6  * radius);
            if (this.icon != null) {
                this.scaledIcon = Bitmap.createScaledBitmap(this.icon, radius4, radius4, false);
            }
        }

        boolean isInside(Point p) {
            int dist = (int) distance(p, center);
            return (dist < this.radius);
        }

        void setIconResource(Resources res, int iconID) {
            this.icon = BitmapFactory.decodeResource(res, iconID);
            if (this.icon != null) {
                this.scaledIcon = Bitmap.createScaledBitmap(this.icon, radius4, radius4, false);
            }
        }

        void draw(Canvas canvas, Paint paint) {
            paint.setColor(Color.WHITE);
            paint.setAlpha( ( activated ) ? 255 : 153 );
            canvas.drawCircle(center.x, center.y, radius, paint);

            paint.setColor(Color.BLACK);
            canvas.drawCircle(center.x, center.y, radius2, paint);

            paint.setColor(Color.WHITE);
            paint.setAlpha( ( activated ) ? 153 : 102 );
            canvas.drawCircle(center.x, center.y, radius3, paint);

            ColorFilter filter = paint.getColorFilter();
            paint.setColorFilter(colorFilter);
            if (position == Position.LEFT) {
                canvas.drawBitmap(scaledIcon, 5, center.y - radius4 - 5, paint);
            } else {
                canvas.drawBitmap(scaledIcon, center.x - radius4 - 5, center.y - radius4 - 5, paint);
            }
            paint.setColorFilter(filter);
        }
    }

    class NightDreamBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, action + " received.");
            if (Config.ACTION_ALARM_SET.equals(action) ||
                    Config.ACTION_ALARM_STOPPED.equals(action) ||
                    Config.ACTION_ALARM_DELETED.equals(action)) {
                Bundle extras = intent.getExtras();
                SimpleTime time = null;
                if (extras != null) {
                    time = new SimpleTime(extras);
                }
                updateTime(time);
            }
        }
    }
    final Handler mHandler = new Handler();

    void toast(final CharSequence text) {
        mHandler.post(new Runnable() {
            @Override public void run() {
                PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP, "nightdream:toastTag");
                wl.acquire(15000);
                Toast.makeText(ctx, text, Toast.LENGTH_LONG).show();
            }
        });
    }
}
