package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

public class ColorPrefWidgetView extends View {
    Paint paint;
    float rectSize;
    float strokeWidth;
    int color = 0xffffffff;

    public ColorPrefWidgetView(Context context, AttributeSet attrs) {
        super(context, attrs);

        float density = context.getResources().getDisplayMetrics().density;
        rectSize = (float) Math.floor(24. * density + 0.5);
        strokeWidth = (float) Math.floor(1. * density + 0.5);

        paint = new Paint();
        paint.setColor(0xffffffff);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setColor(0xffffffff);
        paint.setStyle(Style.STROKE);
        canvas.drawRect(0, 0, rectSize, rectSize, paint);
        paint.setColor(color);
        paint.setStyle(Style.FILL);
        canvas.drawRect(strokeWidth, strokeWidth, rectSize - strokeWidth, rectSize - strokeWidth, paint);
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
