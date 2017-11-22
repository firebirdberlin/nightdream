package com.firebirdberlin.nightdream;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;

import java.util.Calendar;


public class CustomAnalogClock2 extends CustomAnalogClock {
    private static final String TAG = "CustomAnalogClock";

    private Point center = new Point();
    private Point tick_start = new Point();
    private Point tick_end = new Point();

    public CustomAnalogClock2(Context context) {
        super(context);
    }

    public CustomAnalogClock2(Context context, AttributeSet attrs) {
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

        paint.setAlpha(70);
        paint.setColorFilter(customColorFilter);

        drawArc(canvas, radius, min_angle);

        // ticks

        paint.setAlpha(150);
        paint.setColorFilter(secondaryColorFilter);

        paint.setStrokeWidth(Utility.dpToPx(context, 1.f));

        for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 6) {
            tick_start.x = (int) (center.x + .87 * radius * Math.cos(angle));
            tick_start.y = (int) (center.y + .87 * radius * Math.sin(angle));
            tick_end.x = (int) (center.x + .93 * radius * Math.cos(angle));
            tick_end.y = (int) (center.y + .93 * radius * Math.sin(angle));
            canvas.drawLine(tick_start.x, tick_start.y, tick_end.x, tick_end.y, paint);
        }

        paint.setStrokeWidth(Utility.dpToPx(context, 1.f));
        paint.setAlpha(255);

        // minute hand
        canvas.save();
        paint.setColorFilter(customColorFilter);
        canvas.rotate((float) radiansToDegrees(min_angle), center.x, center.y);
        drawHand(canvas, paint, center, (int) (0.9 * radius), (int) (0.04f * radius));
        canvas.restore();

        // hour hand
        canvas.save();
        paint.setColorFilter(secondaryColorFilter);
        canvas.rotate((float) radiansToDegrees(hour_angle), center.x, center.y);
        drawHand(canvas, paint, center, (int) (0.6 * radius), (int) (0.04f * radius));
        canvas.restore();

        paint.setColorFilter(secondaryColorFilter);
        paint.setAlpha(255);
        canvas.drawCircle(center.x, center.y, 0.045f * radius, paint);

        paint.setColorFilter(null);
        paint.setColor(Color.BLACK);
        canvas.drawPoint(center.x, center.y, paint);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(center.x, center.y, 0.045f * radius, paint);
    }

    private double radiansToDegrees(double rad) {
        return rad * 180. / Math.PI;
    }

    private void drawHand(Canvas canvas, Paint paint, Point base, int height, int width) {
        int halfWidth = width / 2;

        Path path = new Path();
        path.moveTo(base.x, base.y - halfWidth);
        path.lineTo(base.x + height, base.y);
        path.lineTo(base.x, base.y + halfWidth);
        path.lineTo(base.x, base.y - halfWidth);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawArc(Canvas canvas, int radius, double angle) {
        final int[] colors = {Color.TRANSPARENT, Color.WHITE};
        final float[] positions = {0.5f, 1.f};
        Shader gradient = new SweepGradient(center.x, center.y, colors, positions);
        float rotate = (float) radiansToDegrees(angle);
        Matrix gradientMatrix = new Matrix();
        gradientMatrix.preRotate(rotate, center.x, center.y);
        gradient.setLocalMatrix(gradientMatrix);

        paint.setShader(gradient);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle(center.x, center.y, 0.9f * radius, paint);
        paint.setShader(null);
    }
}
