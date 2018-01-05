package com.firebirdberlin.nightdream.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.receivers.RadioStreamSleepTimeReceiver;
import com.firebirdberlin.nightdream.services.RadioStreamService;
import com.firebirdberlin.radiostreamapi.models.RadioStation;

public class WebRadioLayout extends RelativeLayout {
    public boolean locked = false;
    private Context context;
    private TextView textView;
    private ImageView buttonSleepTimer;
    private boolean showConnectingHint = false;
    private ProgressBar spinner;

    public WebRadioLayout(Context context) {
        super(context);
        this.context = context;
    }

    public WebRadioLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        setBackgroundResource(R.drawable.webradiopanelborder);

        textView = new TextView(context);
        textView.setId(R.id.web_radio_text_view); // id for placing spinner LEFT_OF this view
        textView.setEllipsize(TextUtils.TruncateAt.END);
        int padding = Utility.dpToPx(context, 6.f);
        textView.setPadding(padding, padding, padding, padding);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

        buttonSleepTimer = new ImageView(context);
        buttonSleepTimer.setImageResource(R.drawable.ic_nightmode);

        padding = Utility.dpToPx(context, 6.f);
        buttonSleepTimer.setPadding(padding, padding, padding, padding);
        buttonSleepTimer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fm = ((Activity) getContext()).getFragmentManager();
                SleepTimerDialogFragment dialog = new SleepTimerDialogFragment();
                dialog.show(fm, "sleep_timer");
            }
        });
        RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        lp2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        //lp2.addRule(RelativeLayout.CENTER_IN_PARENT);

        spinner = new ProgressBar(context, null, android.R.attr.progressBarStyleSmall);
        spinner.setPadding(0, padding, 0, padding);
        spinner.setVisibility(View.VISIBLE);

        RelativeLayout.LayoutParams lp3 = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp3.addRule(RelativeLayout.LEFT_OF, textView.getId());
        lp3.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        addView(textView, lp);
        addView(buttonSleepTimer, lp2);
        addView(spinner, lp3);
    }

    public void setCustomColor(int accentColor, int textColor) {
        Drawable bg = getBackground();
        bg.setColorFilter( accentColor, PorterDuff.Mode.MULTIPLY );

        buttonSleepTimer.setColorFilter(
                RadioStreamSleepTimeReceiver.isSleepTimeSet() ? accentColor : textColor,
                PorterDuff.Mode.SRC_ATOP
        );
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
        if (spinner != null) {
            spinner.setVisibility(showConnectingHint ? View.VISIBLE : View.GONE);
        }
    }

    protected void setShowConnectingHint(boolean showConnectingHint) {
        this.showConnectingHint = showConnectingHint;
    }

    @Override
    public void setClickable(boolean clickable) {
        super.setClickable(clickable);
        buttonSleepTimer.setClickable(clickable);
    }
}
