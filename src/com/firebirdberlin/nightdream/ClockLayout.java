package com.firebirdberlin.nightdream;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;


public class ClockLayout extends LinearLayout {

    private Context context = null;
    private TextView clock = null;
    private TextView date = null;
    private View divider = null;

    public ClockLayout(Context context) {
        super(context);
        this.context = context;
    }

    public ClockLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        return true;
    }

    @Override
    protected void onFinishInflate() {
        clock = (TextView) findViewById(R.id.clock);
        date = (TextView) findViewById(R.id.date);
        divider = (View) findViewById(R.id.divider);
    }

    public void setTypeface(Typeface typeface) {
        if (clock == null) return;
        clock.setTypeface(typeface);
    }

    public void setPrimaryColor(int color) {
        if (clock == null) return;
        clock.setTextColor(color);
    }

    public void setSecondaryColor(int color) {
        if (date == null) return;
        date.setTextColor(color);
    }

    public void setTertiaryColor(int color) {
        if (divider == null) return;
        divider.setBackgroundColor(color);
    }

    public void showDate() {
        date.setVisibility(View.VISIBLE);
        divider.setVisibility(View.VISIBLE);
        setBackgroundColor(Color.parseColor("#44000000"));
    }

    public void hideDate() {
        date.setVisibility(View.INVISIBLE);
        divider.setVisibility(View.INVISIBLE);
        setBackgroundColor(Color.parseColor("#00000000"));
    }

    public void setDesiredClockWidth(float desiredWidthPercent){
        setDesiredWidth(clock, 0.6f, 100f);
        setDesiredWidth(date, 0.9f, 10f);
    }

    private void setDesiredWidth(TextView view, float desiredWidthPercent, float maxSp){
        View parent = (View) getParent();
        int desiredWidth = (int) (desiredWidthPercent * parent.getWidth());

        String text = view.getText().toString();
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, 1);
        int size = 1;
        float sizeSp = pixelsToSp(size);
        do{
            float textWidth = view.getPaint().measureText(text);

            if (textWidth < desiredWidth) {
                view.setTextSize(++size);
            } else {
                view.setTextSize(--size);
                break;
            }
            sizeSp = pixelsToSp(size);
        } while(sizeSp < maxSp);
    }

    private float pixelsToSp(float px) {
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        return px/scaledDensity;
    }

}
