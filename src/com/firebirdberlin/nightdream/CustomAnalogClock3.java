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

import java.util.Calendar;


public class CustomAnalogClock3 extends CustomAnalogClock {
    private static final String TAG = "CustomAnalogClock";

    Point center = new Point();
    private Point tick_start = new Point();
    private Point tick_end = new Point();

    public CustomAnalogClock3(Context context) {
        super(context);
    }

    public CustomAnalogClock3(Context context, AttributeSet attrs) {
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
        double hour_angle = (double) hour / 6. * Math.PI - Math.PI / 2.
                + (double) min / 60. * Math.PI / 6.;
        double min_angle = (double) min / 30. * Math.PI - Math.PI / 2.;

        paint.setAlpha(255);
        paint.setColorFilter(customColorFilter);
        paint.setStyle(Paint.Style.STROKE);

        float px = (float) (.93 - .87) * radius;
        paint.setStrokeWidth(px);
        drawArc(canvas, radius, min_angle);

        paint.setColorFilter(secondaryColorFilter);
        drawArc(canvas, radius - (int) (2 * px), hour_angle);

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
