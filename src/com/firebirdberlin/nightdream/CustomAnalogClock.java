package com.firebirdberlin.nightdream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;


public class CustomAnalogClock extends View {
    private static final String TAG = "CustomAnalogClock";
    protected Paint paint = new Paint();
    Context context;
    TimeReceiver timeReceiver;
    int customColor = Color.GREEN;
    int customSecondaryColor = Color.parseColor("#C2C2C2");
    ColorFilter customColorFilter;
    ColorFilter secondaryColorFilter;
    Typeface typeface = Typeface.DEFAULT;
    public CustomAnalogClock(Context context) {
        super(context);
        init(context);
    }

    public CustomAnalogClock(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public void setStyle(Style style) {

    }

    private void init(Context context) {
        this.context = context;
        customColorFilter = new LightingColorFilter(customColor, 1);
        secondaryColorFilter = new LightingColorFilter(customSecondaryColor, 1);

    }

    public void setTypeface(Typeface typeface) {
        this.typeface = typeface;
    }

    @Override
    public void onAttachedToWindow(){
        super.onAttachedToWindow();
        setTimeTick();
    }

    @Override
    public void onDetachedFromWindow(){
        super.onDetachedFromWindow();
        if (timeReceiver != null) {
            try {
                context.unregisterReceiver(timeReceiver);
            } catch (IllegalArgumentException e) {
                // receiver was not registered,
            }
            timeReceiver = null;
        }
    }

    public void setPrimaryColor(int color) {
        customColor = color;
        customColorFilter = new LightingColorFilter(customColor, 1);
        invalidate();
    }

    public void setSecondaryColor(int color) {
        customSecondaryColor = color;
        secondaryColorFilter = new LightingColorFilter(customSecondaryColor, 1);
        invalidate();
    }

    public void onDraw(Canvas canvas) {
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setColorFilter(secondaryColorFilter);
        paint.setColor(Color.WHITE);
        paint.setAlpha(255);
        int w = getWidth() - 20;
        int h = getHeight() - 20;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.f);
        int radius = w/2 - 10;
        canvas.drawCircle(w/2 + 10, h/2 + 10, radius, paint);
        paint.setStrokeWidth(5.f);
        Point center = new Point(w/2 + 10, h/2 + 10);
        for (double angle=0; angle < 2 * Math.PI; angle += Math.PI/6 ) {
            Point start = new Point((int) (center.x + .85 * radius * Math.cos(angle)),
                                  (int) (center.y + .85 * radius * Math.sin(angle)));
            Point end = new Point((int) (center.x + .95 * radius * Math.cos(angle)),
                                  (int) (center.y + .95 * radius * Math.sin(angle)));
            canvas.drawLine(start.x, start.y, end.x, end.y, paint);
        }

        paint.setColorFilter(customColorFilter);
        int hour = Calendar.getInstance().get(Calendar.HOUR);
        int min = Calendar.getInstance().get(Calendar.MINUTE);
        double hour_angle = (double) hour/6. * Math.PI - Math.PI / 2.
                            + (double) min/60. * Math.PI / 6.;
        double min_angle = (double) min/30. * Math.PI - Math.PI / 2.;

        // hour
        Point hour_end = new Point((int) (center.x + .5 * radius * Math.cos(hour_angle)),
                              (int) (center.y + .5 * radius * Math.sin(hour_angle)));
        paint.setStrokeWidth(5.f);
        canvas.drawLine(center.x, center.y, hour_end.x, hour_end.y, paint);

        // minute
        Point min_end = new Point((int) (center.x + .8 * radius * Math.cos(min_angle)),
                              (int) (center.y + .8 * radius * Math.sin(min_angle)));
        paint.setStrokeWidth(5.f);
        canvas.drawLine(center.x, center.y, min_end.x, min_end.y, paint);
    }

    void setTimeTick() {
        timeReceiver = new TimeReceiver();
        context.registerReceiver(timeReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    public enum Style {DEFAULT, SIMPLE, ARC}

    class TimeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent arg1) {
            invalidate();
        }
    }

}
