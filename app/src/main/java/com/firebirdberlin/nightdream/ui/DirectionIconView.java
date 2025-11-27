package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;


public class DirectionIconView extends View {
    public static final float INVALID = -1.f;
    private static final String TAG = "DirectionIconView";
    Context context;
    Paint paint = new Paint();
    int customcolor = Color.WHITE;
    int width;
    int height;
    float direction;
    ColorFilter colorFilter;

    public DirectionIconView(Context context) {
        super(context);
        init(context);
    }

    public DirectionIconView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    private void init(Context context) {
        this.context = context;
        colorFilter = new LightingColorFilter(customcolor, 1);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public void setColor(int color) {
        customcolor = color;
        colorFilter = new LightingColorFilter(customcolor, 1);
        invalidate();
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int heigthWithoutPadding = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();

        height = heigthWithoutPadding;
        setMeasuredDimension(height + getPaddingLeft() + getPaddingRight(),
                height + getPaddingTop() + getPaddingBottom());
    }

    public void setDirection(float direction) {
        this.direction = direction;
    }

    public void onDraw(Canvas canvas) {
        if (this.direction == INVALID) return;

        int half = height / 2;
        canvas.translate((float) getPaddingLeft(), (float) getPaddingBottom());
        canvas.rotate(this.direction + 90.f, half, half);
        paint.setAntiAlias(true);
        paint.setColorFilter(colorFilter);
        paint.setColor(Color.WHITE);
        paint.setAlpha(255);


        paint.setColor(android.graphics.Color.WHITE);

        paint.setStrokeWidth(1);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        Point a = new Point(5, (int) (0.7f * height));
        Point b = new Point((int) (.33f * height), half);
        Point c = new Point(height, half);

        Path path = prepTriangle(a, b, c);
        canvas.drawPath(path, paint);

        paint.setStrokeWidth(3);
        paint.setStyle(Paint.Style.STROKE);
        a = new Point((int) (.33f * height), half);
        b = new Point(5, (int) (0.3f * height));
        c = new Point(height, half);

        path = prepTriangle(a, b, c);
        canvas.drawPath(path, paint);
    }

    private Path prepTriangle(Point a, Point b, Point c) {
        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(a.x, a.y);
        path.lineTo(b.x, b.y);
        path.lineTo(c.x, c.y);
        path.lineTo(a.x, a.y);
        path.close();
        return path;
    }
}
