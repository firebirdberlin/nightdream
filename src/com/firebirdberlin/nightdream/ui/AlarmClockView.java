package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.SetAlarmClockActivity;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.services.AlarmHandlerService;
import com.firebirdberlin.nightdream.services.SqliteIntentService;

import java.util.Calendar;


public class AlarmClockView extends View {
    private static final String TAG = "AlarmClockView";
    final Handler mHandler = new Handler();
    final private Handler handler = new Handler();
    private final Context ctx;
    private final Paint paint = new Paint();
    private final Rect alarmTimeRect = new Rect(0, 0, 0, 0);
    private final HotCorner cornerLeft;
    private final HotCorner cornerRight;
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
    private int customColor = Color.parseColor("#33B5E5");
    private int paddingHorizontal = 0;
    private ColorFilter customColorFilter;
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
    private onAlarmChangeListener listener;
    private float lastHourX = -1;
    private int lastHour = -1;

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

    public void setPaddingHorizontal(int paddingHorizontal) {
        this.paddingHorizontal = paddingHorizontal;
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

    private float distance(Point a, Point b) {
        return (float) Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }

    private Point getClickedPoint(MotionEvent e) {
        return new Point((int) e.getX(), (int) e.getY());
    }

    public boolean onTouchEvent(MotionEvent e) {
        Log.d(TAG, String.format("onTouchEvent: %d", e.getAction()));

        // the view should be visible before the user interacts with it
        if (!isClickable() || locked) return false;

        if (showRightCorner()) {
            boolean success = mGestureDetector.onTouchEvent(e) || handleAlarmCancelling(e);
            if (success) return true;
        }
        if (showLeftCorner()) {
            return mGestureDetector.onTouchEvent(e) || handleAlarmSetEvents(e);
        }
        return false;
    }

    private boolean handleAlarmSetEvents(MotionEvent e) {
        if (alarmIsRunning()) return false;
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
                if (dist > touch_zone_radius) {
                    userChangesAlarmTime = true;
                    cancelAlarm();
                    XYtotime(tX, tY);
                } else {
                    userChangesAlarmTime = false;
                }
                this.invalidate();
                this.lastMoveEventY = tY;
                break;
            case MotionEvent.ACTION_UP:
                if (!FingerDown) return false;
                if (dist > touch_zone_radius) {
                    XYtotime(tX, tY);
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

    private boolean handleAlarmCancelling(MotionEvent e) {
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

        // separate hours from minutes
        int separator = 1 * touch_zone_radius;

        // adjust time in 5-minute intervals when dragging upwards, and 1-minute interval when dragging downwards.
        int roundTo = (movingDown) ? 1 : 5;
        if (y > separator) {
            lastHour = (int) (lastHourX / w * 24);
            int diff = (int) ((x - lastHourX) / w * 8);
            diff = to_range(diff, -1, 1);
            lastHour = lastHour + diff;
            lastHour = (lastHour >= 24) ? 23 : lastHour;
        } else {
            lastHourX = x;
            int hours = (int) (x / w * 24);
            lastHour = (hours >= 24) ? 23 : hours;
        }
        int mins = (int) (((y - separator) / (h - separator) * 60)) / roundTo * roundTo;
        if (movingDown) {
            // make sure time never increases while dragging downwards
            mins = Math.min(mins, lastMinSinceDragStart);
        } else {
            // make sure time never decreases while dragging upwards
            mins = Math.max(mins, lastMinSinceDragStart);
        }
        lastMinSinceDragStart = mins; //save mins, but without going back from value 60 to 0
        mins = to_range(mins, 0, 59);
        setAlarmTime(lastHour, mins);
    }

    private int to_range(int value, int min, int max) {
        return Math.max(Math.min(value, max), min);
    }

    private void setAlarmTime(int hour, int min) {
        if (time == null) {
            time = new SimpleTime();
        }
        time.hour = hour;
        time.min = min;
        time.soundUri = Settings.getDefaultAlarmTone(ctx);
        time.radioStationIndex = Settings.getDefaultRadioStation(ctx);
        postAlarmTime();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isClickable()) return;

        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setColorFilter(customColorFilter);

        int w = getWidth();
        int maxHeight = Utility.dpToPx(getContext(), 60);
        int h = Math.min(getHeight(), maxHeight);

        // touch zones
        touch_zone_radius = Math.min(w, h);
        quiet_zone_size = touch_zone_radius / 4;

        if (showLeftCorner()) {
            cornerLeft.setCenter(paddingHorizontal, h);
            cornerLeft.setRadius(touch_zone_radius);
            cornerLeft.setActive(FingerDown);
            cornerLeft.draw(canvas, paint);

        }
        if (showRightCorner()) {
            cornerRight.setCenter(w - paddingHorizontal, h);
            cornerRight.setRadius(touch_zone_radius);
            cornerRight.setIconResource(
                    getResources(),
                    alarmIsRunning() ? R.drawable.ic_no_audio : R.drawable.ic_no_alarm_clock
            );
            cornerRight.setActive(FingerDownDeleteAlarm || blinkStateOn);
            cornerRight.draw(canvas, paint);
        }

        if (userChangesAlarmTime) {
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(6.f);
            paint.setStrokeMiter(10.f);
            paint.setPathEffect(new DashPathEffect(new float[]{20f, 40f}, 0f));

            canvas.drawLine(
                    0.f,
                    -1.f * maxHeight,
                    (float) w,
                    -1.f * maxHeight,
                    paint
            );
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
                if (Utility.isEmpty(current.name)) {
                    Calendar cal = current.getTodaysAlarmTIme();
                    if (cal != null) {
                        return Utility.getTimeFormatted(ctx, cal);
                    }
                } else {
                    return current.name;
                }
            }
        }
        if ((userChangesAlarmTime || isAlarmSet()) && time != null) {
            return Utility.getTimeFormatted(ctx, time.getCalendar());
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

    public boolean isAlarmSet() {
        return (this.time != null);
    }

    public void stopAlarm() {
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
        time.name = getResources().getString(R.string.alarm);
        AlarmHandlerService.set(ctx, time);
    }

    public void cancelAlarm() {
        Log.d(TAG, "cancelAlarm()");
        if (isAlarmSet()) {
            AlarmHandlerService.cancel(ctx);
        }

        this.time = null;
    }

    protected void updateTime(SimpleTime time) {
        this.time = time;
        post(() -> {
            postAlarmTime();
            invalidate();
            if (time == null) {
                Log.w(TAG, "no next alarm");
            } else {
                Log.w(TAG, String.format("next Alarm %02d:%02d", time.hour, time.min));
            }
            requestLayout();
        });
    }

    void toast(final CharSequence text) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP, "nightdream:toastTag");
                wl.acquire(15000);
                Toast.makeText(ctx, text, Toast.LENGTH_LONG).show();
            }
        });
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
                } else if (useLongPress) {
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
            if (useLongPress && showRightCorner() && cornerRight.isInside(click)) {
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
            if (radius < 100) radius = 100;

            this.radius = radius;
            this.radius2 = (int) (0.93 * radius);
            this.radius3 = (int) (0.86 * radius);
            this.radius4 = (int) (0.6 * radius);
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
            if (position == Position.LEFT) {
                paint.setColor(Color.WHITE);
                paint.setAlpha((activated) ? 255 : 153);
                RectF oval = new RectF(
                        center.x - radius, center.y - radius,
                        center.x + radius, center.y + radius
                );
                canvas.drawArc(oval, 270, 90, true, paint);
                canvas.drawRect(center.x, center.y, center.x + radius, center.y + radius, paint);
                canvas.drawRect(0, 0, center.x, center.y, paint);

                paint.setColor(Color.BLACK);
                int diff = radius - radius2;
                oval = new RectF(
                        center.x - radius2, center.y - radius2,
                        center.x + radius2, center.y + radius2 - diff
                );
                canvas.drawArc(oval, 270, 90, true, paint);
                canvas.drawRect(center.x, center.y - diff, center.x + radius2, center.y + radius2, paint);
                canvas.drawRect(0, diff, center.x, center.y, paint);

                paint.setColor(Color.WHITE);
                paint.setAlpha((activated) ? 153 : 102);
                diff = radius - radius3;
                oval = new RectF(
                        center.x - radius3, center.y - radius3,
                        center.x + radius3, center.y + radius3 - diff
                );
                canvas.drawArc(oval, 270, 90, true, paint);
                canvas.drawRect(center.x, center.y - (radius2 - radius3), center.x + radius3, center.y + radius3, paint);
                canvas.drawRect(0, diff, center.x, center.y, paint);
            } else if (position == Position.RIGHT) {
                paint.setColor(Color.WHITE);
                paint.setAlpha((activated) ? 255 : 153);
                RectF oval = new RectF(
                        center.x - radius, center.y - radius,
                        center.x + radius, center.y + radius
                );
                canvas.drawArc(oval, 180, 90, true, paint);
                canvas.drawRect(center.x, center.y, center.x - radius, center.y + radius, paint);
                canvas.drawRect(center.x, 0, center.x + radius, center.y, paint);

                paint.setColor(Color.BLACK);
                int diff = radius - radius2;
                oval = new RectF(
                        center.x - radius2, center.y - radius2,
                        center.x + radius2, center.y + radius2 - diff
                );
                canvas.drawArc(oval, 180, 90, true, paint);
                canvas.drawRect(center.x, center.y - diff, center.x - radius2, center.y + radius2, paint);
                canvas.drawRect(center.x, diff, center.x + radius, center.y, paint);

                paint.setColor(Color.WHITE);
                paint.setAlpha((activated) ? 153 : 102);
                diff = radius - radius3;
                oval = new RectF(
                        center.x - radius3, center.y - radius3,
                        center.x + radius3, center.y + radius3 - diff
                );
                canvas.drawArc(oval, 180, 90, true, paint);
                canvas.drawRect(center.x, center.y - (radius2 - radius3), center.x - radius3, center.y + radius3, paint);
                canvas.drawRect(center.x, diff, center.x + radius, center.y, paint);
            }

            ColorFilter filter = paint.getColorFilter();
            paint.setColorFilter(colorFilter);
            if (position == Position.LEFT) {
                canvas.drawBitmap(scaledIcon, center.x + 5, center.y - radius4 - 5, paint);
            } else {
                canvas.drawBitmap(scaledIcon, center.x - radius4 - 5, center.y - radius4 - 5, paint);
            }
            paint.setColorFilter(filter);
        }
    }
}
