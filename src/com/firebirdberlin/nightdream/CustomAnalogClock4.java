package com.firebirdberlin.nightdream;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.Log;

import java.util.Calendar;


public class CustomAnalogClock4 extends CustomAnalogClock {
    private static final String TAG = "CustomAnalogClock";

    Point center = new Point();
    private Point tick_start = new Point();
    private Point tick_end = new Point();

    public CustomAnalogClock4(Context context) {
        super(context);
    }

    public CustomAnalogClock4(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void onDraw(Canvas canvas) {
        center.x = getWidth() / 2;
        center.y = getHeight() / 2;
        int radius = getWidth() / 2 - 20;

        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);

        int hour = Calendar.getInstance().get(Calendar.HOUR);
        int min = Calendar.getInstance().get(Calendar.MINUTE);

        // radian unit (rad), 2PI = 360 degree = 12h -> 3:00/15:00 = 0 rad
        // 12h/6=2PI
        // added minute offset: 60min = 2PI/12h -> 60min = PI/6h
        double hour_angle = (double) hour / 6. * Math.PI - Math.PI / 2.
                + (double) min / 60. * Math.PI / 6.;
        // 60min = 2PI -> min / 60 * 2PI
        double min_angle = (double) min / 30. * Math.PI - Math.PI / 2.;

        paint.setAlpha(255);
        paint.setColorFilter(customColorFilter);
        paint.setStyle(Paint.Style.STROKE);

        float strokeWidth = (float) (.93 - .87) * radius;
        paint.setStrokeWidth(strokeWidth);
        drawArc(canvas, radius, min_angle);

        paint.setColorFilter(secondaryColorFilter);
        drawArc(canvas, radius - (int) (2 * strokeWidth), hour_angle);

        // ticks
        paint.setShader(null);
        paint.setAlpha(150);
        paint.setColorFilter(secondaryColorFilter);
        int width = Utility.dpToPx(context, 1.f);
        paint.setStrokeWidth(width);

        for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 6) {
            tick_start.x = (int) (center.x + .87 * radius * Math.cos(angle));
            tick_start.y = (int) (center.y + .87 * radius * Math.sin(angle));
            tick_end.x = (int) (center.x + .93 * radius * Math.cos(angle));
            tick_end.y = (int) (center.y + .93 * radius * Math.sin(angle));
            canvas.drawLine(tick_start.x, tick_start.y, tick_end.x, tick_end.y, paint);
        }

    }

    private void drawArc(Canvas canvas, int radius, double angle) {
        final int[] colors = {Color.TRANSPARENT, Color.WHITE};
        final float[] positions = {0.2f, 1.f};
        Shader gradient = new SweepGradient(center.x, center.y, colors, positions);
        float rotate = (float) radiansToDegrees(angle);
        Matrix gradientMatrix = new Matrix();
        gradientMatrix.preRotate(rotate, center.x, center.y);
        gradient.setLocalMatrix(gradientMatrix);


        paint.setShader(gradient);
        canvas.drawCircle(center.x, center.y, 0.9f * radius, paint);
        paint.setShader(null);
    }

    private double radiansToDegrees(double rad) {
        return rad * 180. / Math.PI;
    }
}
