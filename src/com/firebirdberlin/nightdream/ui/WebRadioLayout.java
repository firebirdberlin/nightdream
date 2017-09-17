package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.services.RadioStreamService;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

public class WebRadioLayout extends RelativeLayout {
    public boolean locked = false;
    private Context context;
    private TextView textView;
    private ImageView buttonSleepTimer;

    public WebRadioLayout(Context context) {
        super(context);
        this.context = context;
    }

    public WebRadioLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        setBackgroundResource(R.drawable.webradiopanelborder);

        textView = new TextView(context);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        //lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

        buttonSleepTimer = new ImageView(context);
        buttonSleepTimer.setImageResource(R.drawable.ic_nightmode);
        int padding = Utility.dpToPx(context, 10.f);
        buttonSleepTimer.setPadding(padding, padding, padding, padding);
        RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        lp2.addRule(RelativeLayout.CENTER_IN_PARENT);

        addView(textView, lp);
        addView(buttonSleepTimer, lp2);

    }

    public void setCustomColor(int accentColor, int textColor) {
        Drawable bg = getBackground();
        bg.setColorFilter( accentColor, PorterDuff.Mode.MULTIPLY );
        buttonSleepTimer.setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP);
        textView.setTextColor(textColor);
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    protected void setText() {
        if (textView == null) return;
        if (RadioStreamService.streamingMode == RadioStreamService.StreamingMode.RADIO) {
            RadioStation station = RadioStreamService.getCurrentRadioStation(context);
            textView.setText(station.name);
        } else {
            textView.setText("");
        }
    }

    @Override
    public void setClickable(boolean clickable) {
        super.setClickable(clickable);
    }

}
