package com.firebirdberlin.nightdream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.firebirdberlin.nightdream.models.AnalogClockConfig;
import com.firebirdberlin.nightdream.models.FontCache;

import java.util.Calendar;


public class CustomAnalogClock extends View {
    private static final String TAG = "CustomAnalogClock";
    final String[] ROMAN_DIGITS = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};
    protected Paint paint = new Paint();
    Context context;
    TimeReceiver timeReceiver;
    int customColor = Color.BLUE;
    int customSecondaryColor = Color.WHITE;
    ColorFilter customColorFilter;
    ColorFilter secondaryColorFilter;
    Typeface typeface = Typeface.DEFAULT;
    Typeface boldTypeface = Typeface.DEFAULT;
    float centerX = 0.f;
    float centerY = 0.f;
    int radius = 0;

    AnalogClockConfig config;

    public CustomAnalogClock(Context context) {
        super(context);
        init(context);
    }

    public CustomAnalogClock(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    protected static float fontSizeForWidth(String dummyText, float destWidth, Paint paint) {
        final float dummyFontSize = 48f;
        paint.setTextSize(dummyFontSize);
        Rect bounds = new Rect();
        paint.getTextBounds(dummyText, 0, dummyText.length(), bounds);
        return dummyFontSize * destWidth / bounds.width();
    }

    private void init(Context context) {
        this.context = context;
        customColorFilter = new LightingColorFilter(Color.BLUE, 1);
        secondaryColorFilter = new LightingColorFilter(Color.WHITE, 1);

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
        customColorFilter = new LightingColorFilter(color, 1);
        invalidate();
    }

    public void setSecondaryColor(int color) {
        customSecondaryColor = color;
        secondaryColorFilter = new LightingColorFilter(color, 1);
        invalidate();
    }

    public void setStyle(AnalogClockConfig.Style style) {
        config = new AnalogClockConfig(context, style);

        this.typeface = FontCache.get(context, config.fontUri);
        this.boldTypeface = Typeface.create(typeface, Typeface.BOLD);
    }

    public void onDraw(Canvas canvas) {
        centerX = getWidth() / 2;
        centerY = getHeight() / 2;
        radius = getWidth() / 2 - 20;

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
        paint.setColor(Color.WHITE);

        drawBackgroundArc(canvas, centerX, centerY, radius, min_angle);

        applyShader(paint, centerX, centerY, radius);
        drawOuterCircle(canvas);
        drawTicks(canvas, centerX, centerY, radius);
        drawHourDigits(canvas, centerX, centerY, radius);
        drawHands(canvas, centerX, centerY, radius, hour_angle, min_angle);
    }

    private void drawHands(Canvas canvas, float centerX, float centerY, int radius,
                           double hour_angle, double min_angle) {
        paint.setColorFilter(customColorFilter);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        // minute hand
        canvas.save();
        paint.setColorFilter(customColorFilter);
        canvas.rotate((float) radiansToDegrees(min_angle), centerX, centerY);
        drawHand(canvas, paint, centerX, centerY, (int) (config.handLengthMinutes * radius),
                (int) (config.handWidthMinutes * radius));
        canvas.restore();

        // hour hand
        canvas.save();
        paint.setColorFilter(secondaryColorFilter);
        canvas.rotate((float) radiansToDegrees(hour_angle), centerX, centerY);
        drawHand(canvas, paint, centerX, centerY, (int) (config.handLengthHours * radius),
                (int) (config.handWidthHours * radius));
        canvas.restore();

        drawInnerCircle(canvas);
    }

    private void drawHand(Canvas canvas, Paint paint, float baseX, float baseY, int height, int width) {
        switch (config.handShape) {
            case ARC:
                drawHandArc(canvas, height, width);
                break;
            case BAR:
                drawHandBar(canvas, paint, baseX, baseY, height, width);
                break;
            case TRIANGLE:
            default:
                drawHandTriangle(canvas, paint, baseX, baseY, height, width);
                break;
        }
    }

    private void drawHandTriangle(Canvas canvas, Paint paint, float centerX, float centerY, int length, int width) {
        int halfWidth = width / 2;

        Path path = new Path();
        path.moveTo(centerX, centerY - halfWidth);
        path.lineTo(centerX + length, centerY);
        path.lineTo(centerX, centerY + halfWidth);
        path.lineTo(centerX, centerY - halfWidth);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawHandBar(Canvas canvas, Paint paint, float centerX, float centerY, int length, int width) {
        paint.setStrokeWidth(width);
        canvas.drawLine(centerX, centerY, centerX + length, centerY, paint);
    }

    private void drawHandArc(Canvas canvas, int length, int width) {
        canvas.save();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        final int[] colors = {Color.TRANSPARENT, Color.WHITE};
        final float[] positions = {0.2f, 1.f};
        Shader gradient = new SweepGradient(centerX, centerY, colors, positions);
        paint.setShader(gradient);
        canvas.drawCircle(centerX, centerY, length, paint);
        paint.setShader(null);
        canvas.restore();
    }

    private void drawInnerCircle(Canvas canvas) {
        if (config.handShape == AnalogClockConfig.HandShape.ARC) return;

        paint.setColorFilter(secondaryColorFilter);
        paint.setAlpha(255);
        canvas.drawCircle(centerX, centerY, config.innerCircleRadius * radius, paint);
        paint.setColorFilter(null);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2);
        canvas.drawPoint(centerX, centerY, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(centerX, centerY, config.innerCircleRadius * radius, paint);
    }

    private void drawTriangle(Canvas canvas, Paint paint, float baseX, float baseY, float width, float height) {
        float halfWidth = width / 2;

        Path path = new Path();
        path.moveTo(baseX - halfWidth, baseY);
        path.lineTo(baseX + halfWidth, baseY);
        path.lineTo(baseX, baseY + height);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawBackgroundArc(Canvas canvas, float centerX, float centerY, int radius, double angle) {
        if (config.decoration != AnalogClockConfig.Decoration.MINUTE_HAND) return;
        canvas.save();
        paint.setAlpha(70);
        paint.setColorFilter(customColorFilter);
        final int[] colors = {Color.TRANSPARENT, Color.WHITE};
        final float[] positions = {0.5f, 1.f};
        Shader gradient = new SweepGradient(centerX, centerY, colors, positions);
        float rotate = (float) radiansToDegrees(angle);
        Matrix gradientMatrix = new Matrix();
        gradientMatrix.preRotate(rotate, centerX, centerY);
        gradient.setLocalMatrix(gradientMatrix);

        paint.setShader(gradient);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle(centerX, centerY, config.handLengthMinutes * radius, paint);
        paint.setShader(null);
        canvas.restore();
    }

    private void applyShader(Paint paint, float centerX, float centerY, int radius) {
        if (config.decoration != AnalogClockConfig.Decoration.LABELS) return;

        int x1 = (int) (centerX - radius), y1 = (int) (centerY - radius);

        float[] hsv = new float[3];
        Color.colorToHSV(customColor, hsv);
        hsv[2] *= 1.2f;
        hsv[1] *= .90f;
        int accent = Color.HSVToColor(hsv);

        int colors[] = {Color.WHITE, accent};
        float positions[] = {0.4f, 1.f};
        Shader shader = new LinearGradient(x1, y1, centerX, centerY, colors, positions, Shader.TileMode.MIRROR);
        paint.setShader(shader);
    }

    private void drawOuterCircle(Canvas canvas) {
        if (config.outerCircleWidth == 0.00f) return;
        paint.setAlpha(255);
        paint.setColor(Color.WHITE);
        paint.setColorFilter(secondaryColorFilter);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(config.outerCircleWidth * radius);

        canvas.drawCircle(centerX, centerY, config.outerCircleRadius * radius, paint);
    }

    private void drawTicks(Canvas canvas, float centerX, float centerY, int radius) {
        // ticks
        paint.setAlpha(255);
        paint.setColorFilter(secondaryColorFilter);

        paint.setStyle(Paint.Style.FILL); //filled circle for every hour
        int minuteCounter = 0;

        float angleDelta = (float) Math.PI / 30f;
        float angleMax = (float) (2f * Math.PI);
        for (double angle = 0; angle < angleMax; angle += angleDelta) {

            boolean isHourTick = (minuteCounter % 5 == 0);
            AnalogClockConfig.TickStyle tickStyle = (isHourTick)
                    ? config.tickStyleHours : config.tickStyleMinutes;
            float tickStart = (isHourTick) ? config.tickStartHours : config.tickStartMinutes;
            float tickLength = (isHourTick) ? config.tickLengthHours : config.tickLengthMinutes;
            int width = (int) ((isHourTick)
                    ? config.tickWidthHours * radius : config.tickWidthMinutes * radius);
            paint.setStrokeWidth(width);
            float tickStartX = (float) (centerX + tickStart * radius * Math.cos(angle));
            float tickStartY = (float) (centerY + tickStart * radius * Math.sin(angle));
            float tickEndX = (float) (centerX + (tickStart + tickLength) * radius * Math.cos(angle));
            float tickEndY = (float) (centerY + (tickStart + tickLength) * radius * Math.sin(angle));
            switch (tickStyle) {
                case NONE:
                    break;
                case CIRCLE:
                    if (isHourTick && config.emphasizeHour12 && minuteCounter == 45) {
                        // for "12" digit draw a special marker
                        float triangleHeight = tickLength * radius * 1.2f;
                        float triangleWidth = triangleHeight * 1.2f;
                        drawTriangle(canvas, paint, tickEndX, tickEndY - triangleHeight * .1f, triangleWidth, triangleHeight);
                    } else {
                        float roundTickRadius = tickLength * .5f * radius;
                        float roundTickCenterX = (centerX + (tickStart + tickLength * .5f) * (float) radius * (float) Math.cos(angle));
                        float roundTickCenterY = (centerY + (tickStart + tickLength * .5f) * (float) radius * (float) Math.sin(angle));
                        canvas.drawCircle(roundTickCenterX, roundTickCenterY, roundTickRadius, paint);
                    }
                    break;
                default:
                    canvas.drawLine(tickStartX, tickStartY, tickEndX, tickEndY, paint);
                    break;
            }
            minuteCounter++;
        }
    }

    private void drawHourDigits(Canvas canvas, float centerX, float centerY, int radius) {
        if (config.digitStyle == AnalogClockConfig.DigitStyle.NONE) return;
        // calculate font-size for desired text width, so digits have equal size on any device

        // init typeface
        paint.setTypeface(typeface);

        /*
        ToDo: until now digits have fixed width of 0.8% of the radius -> should become configurable as well!
               Then digitPosition should also be configurable.
        */
        final float digitFontSizeBig = fontSizeForWidth("5", 0.08f * radius, paint);
        final float digitFontSizeSmall = fontSizeForWidth("5", 0.06f * radius, paint);

        paint.setTextSize(digitFontSizeBig);

        float minTickStart = Math.min(config.tickStartMinutes, config.tickStartHours);
        float minTickLength = Math.min(config.tickLengthMinutes, config.tickLengthHours);

        paint.setStrokeWidth(0);

        int digitCounter = 0;

        final float defaultDigitPosition = config.digitPosition * radius;
        final float ticksDistancePosition = (minTickStart * radius)  // abs start of tick
                - (minTickLength * 0.5f * radius);  // leave distance of half the tick length between digit and tick
        for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 6) {

            int currentHour = (digitCounter + 2) % 12 + 1;

            if (config.highlightQuarterOfHour) {
                if (currentHour % 3 == 0) {
                    // 3,6,9,12
                    paint.setColorFilter(customColorFilter);
                    paint.setTextSize(digitFontSizeBig);
                    paint.setTypeface(boldTypeface);
                } else {
                    paint.setColorFilter(secondaryColorFilter);
                    paint.setTextSize(digitFontSizeSmall);
                    paint.setTypeface(typeface);
                }
            } else {
                paint.setColorFilter(secondaryColorFilter);
                paint.setTextSize(digitFontSizeSmall);
                paint.setTypeface(typeface);
            }

            Rect bounds = new Rect();
            final String currentHourText = getHourTextOfDigitStyle(currentHour);

            paint.getTextBounds(currentHourText, 0, currentHourText.length(), bounds);

            // for width measureText returns more exact result than textbounds
            // for height textbounds is ok
            final float textWidth = paint.measureText(currentHourText, 0, currentHourText.length());
            final float textHeight = bounds.height();

            // ToDo: leave here only for roman digits, otherwise before the loop via dummy text bounds?
            final float distanceDigitCenterToBorder = distanceHourTextBoundsCenterToBorder(currentHour, angle, textWidth, textHeight);

            // use digitPosition, of the corrected position if digitPosition would overlap with ticks
            final float correctedAbsoluteDigitPosition = Math.min(defaultDigitPosition, ticksDistancePosition - distanceDigitCenterToBorder);

            float x = (float) (centerX + correctedAbsoluteDigitPosition * Math.cos(angle));
            float y = (float) (centerY + correctedAbsoluteDigitPosition * Math.sin(angle));

            // move center of text bounding box to x/y
            x -= textWidth / 2.;
            y -= textHeight / 2f + 1f;

            canvas.drawText(currentHourText, x, y + textHeight, paint);

            // debug: show text bounds
            /*
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(x, y, x + textWidth, y + textHeight, paint);
            */

            digitCounter++;
        }
    }

    private float distanceHourTextBoundsCenterToBorder(int currentHour, double angle, float textWidth, float textHeight) {
        float distanceDigitCenterToBorder;
        if (currentHour == 6 || currentHour == 12) {
            // hour arm orientation is vertically: use half height as distance
            distanceDigitCenterToBorder = (float) textHeight / 2f;
        } else if (currentHour == 3 || currentHour == 9) {
            // hour arm orientation is horizontally: use half width as distance
            distanceDigitCenterToBorder = textWidth / 2f;
        } else {
            // hour arm orientation is diagonally: calculate distance from center to the intersection point of rectangles border
            distanceDigitCenterToBorder = (float) distanceOfRectangleCentreToIntersectionPoint(angle, textWidth, textHeight);
        }
        return distanceDigitCenterToBorder;
    }

    private double distanceOfRectangleCentreToIntersectionPoint(double angle, float textWidth, float textHeight) {

        double degree = angle / (2 * Math.PI) * 360.0;

        double sharpAngle = angle;
        double triangleAdjacentLength;
        if ((degree >= 315 || degree < 45)) {
            // intersects right edge
            if (degree >= 45) {
                sharpAngle = 2 * Math.PI - angle;
            }
            triangleAdjacentLength = (double)textWidth / 2f;
         } else if (degree >= 45 && degree < 135) {
            // intersects bottom edge
            sharpAngle = Math.abs(Math.PI / 2f - angle);
            triangleAdjacentLength = (double)textHeight / 2f;
        } else if (degree >= 135 && degree < 225) {
            // intersects left edge
            sharpAngle = Math.abs(Math.PI - angle);
            triangleAdjacentLength = (double)textWidth / 2f;
        } else {
            // 225 to 315: intersects top edge
            sharpAngle = Math.abs(Math.PI * 1.5 - angle);
            triangleAdjacentLength = (double)textHeight / 2f;
        }

        double result = Math.abs(triangleAdjacentLength / Math.cos(sharpAngle));
        //Log.i(TAG, "angle=" + angle + " degree=" + degree + " sharpAngle=" + sharpAngle + ", triangleAdjacentLength=" + triangleAdjacentLength + ", dist=" + result);
        return result;
    }

    private String getHourTextOfDigitStyle(int currentHour) {
        String currentHourText = (config.digitStyle == AnalogClockConfig.DigitStyle.ARABIC)
                ? String.valueOf(currentHour)
                : ROMAN_DIGITS[currentHour - 1];
        return currentHourText;
    }

    private double radiansToDegrees(double rad) {
        return rad * 180. / Math.PI;
    }

    void setTimeTick() {
        timeReceiver = new TimeReceiver();
        context.registerReceiver(timeReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    class TimeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent arg1) {
            invalidate();
        }
    }

}
