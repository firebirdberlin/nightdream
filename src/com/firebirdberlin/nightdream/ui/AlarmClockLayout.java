package com.firebirdberlin.nightdream.ui;


import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.firebirdberlin.nightdream.BillingHelper;
import com.firebirdberlin.nightdream.BillingHelperActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.SetAlarmClockActivity;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.SimpleTime;

import java.util.Calendar;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class AlarmClockLayout extends LinearLayout {

    private static final String TAG = "AlarmClockLayout";
    private Context context;
    private String timeFormat = "h:mm";
    private View mainLayout = null;
    private SimpleTime alarmClockEntry = null;
    private TextView timeView = null;
    private TextView textViewSound = null;
    private TextView textViewWhen = null;
    private ImageView buttonDown = null;
    private LinearLayout layoutDays = null;
    private LinearLayout secondaryLayout = null;
    private Button buttonDelete = null;
    private Switch switchActive = null;
    private CheckBox checkBoxIsRepeating = null;
    private ToggleButton[] dayButtons = new ToggleButton[7];
    private int firstdayOfWeek = Utility.getFirstDayOfWeek();
    private CheckBox.OnCheckedChangeListener checkboxOnCheckedChangeListener =
            new CheckBox.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    layoutDays.setVisibility((checked) ? View.VISIBLE : View.GONE);
                    if (!checked) {
                        for (int d : SimpleTime.DAYS) {
                            alarmClockEntry.removeRecurringDay(d);
                            dayButtons[d - 1].setChecked(false);
                        }
                        update();
                        ((SetAlarmClockActivity) context).onEntryStateChanged(alarmClockEntry);
                    }
                }
            };
    private ToggleButton.OnClickListener dayButtonOnclickListener =
            new ToggleButton.OnClickListener() {
                @Override
                public void onClick(View view) {

                    int day = (int) view.getTag();
                    ToggleButton button = (ToggleButton) view;

                    if (button.isChecked()) {
                        alarmClockEntry.addRecurringDay(day);
                    } else {
                        alarmClockEntry.removeRecurringDay(day);
                    }
                    update();
                    ((SetAlarmClockActivity) context).onEntryStateChanged(alarmClockEntry);
                }
            };
    private ImageView.OnClickListener buttonDownOnClickListener = new ImageView.OnClickListener() {
        @Override
        public void onClick(View view) {
            boolean gone = secondaryLayout.getVisibility() == View.GONE;
            secondaryLayout.setVisibility(gone ? View.VISIBLE : View.GONE);
            buttonDown.setImageResource(gone ? R.drawable.ic_collapse : R.drawable.ic_expand);
            layoutDays.setVisibility(
                    (gone && checkBoxIsRepeating.isChecked()) ? View.VISIBLE : View.GONE
            );
            mainLayout.setBackgroundColor(gone ? Color.DKGRAY : Color.TRANSPARENT);
        }
    };

    public AlarmClockLayout(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public AlarmClockLayout(Context context, SimpleTime entry, String timeFormat) {
        super(context);
        this.context = context;
        this.alarmClockEntry = entry;
        this.timeFormat = timeFormat;
        init();
        buttonDelete.setTag(entry);
        timeView.setTag(entry);
    }

    public AlarmClockLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public static boolean isTomorrow(Calendar d) {
        return DateUtils.isToday(d.getTimeInMillis() - DateUtils.DAY_IN_MILLIS);
    }

    public static boolean isToday(Calendar d) {
        return DateUtils.isToday(d.getTimeInMillis());
    }

    private void init() {

        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View child = inflater.inflate(R.layout.alarm_clock_layout, null);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(child, lp);
        mainLayout = findViewById(R.id.mainLayout);
        timeView = (TextView) findViewById(R.id.timeView);
        textViewSound = (TextView) findViewById(R.id.textViewSound);
        textViewWhen = (TextView) findViewById(R.id.textViewWhen);
        layoutDays = (LinearLayout) findViewById(R.id.layoutDays);
        buttonDown = (ImageView) findViewById(R.id.button_down);
        buttonDelete = (Button) findViewById(R.id.button_delete);
        secondaryLayout = (LinearLayout) findViewById(R.id.secondaryLayout);
        switchActive = (Switch) findViewById(R.id.enabled);
        checkBoxIsRepeating = (CheckBox) findViewById(R.id.checkBoxIsRepeating);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            RelativeLayout middle = (RelativeLayout) findViewById(R.id.middle);
            LayoutTransition layoutTransition = middle.getLayoutTransition();
            layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
        }

        dayButtons[0] = (ToggleButton) findViewById(R.id.dayButton1);
        dayButtons[1] = (ToggleButton) findViewById(R.id.dayButton2);
        dayButtons[2] = (ToggleButton) findViewById(R.id.dayButton3);
        dayButtons[3] = (ToggleButton) findViewById(R.id.dayButton4);
        dayButtons[4] = (ToggleButton) findViewById(R.id.dayButton5);
        dayButtons[5] = (ToggleButton) findViewById(R.id.dayButton6);
        dayButtons[6] = (ToggleButton) findViewById(R.id.dayButton7);

        String[] weekdayStrings = Utility.getWeekdayStrings();
        for (int i : SimpleTime.DAYS) {
            int idx = (i - 1 + firstdayOfWeek - 1) % 7 + 1;
            ToggleButton button = dayButtons[i - 1];
            button.setTag(idx);
            button.setTextOn(weekdayStrings[idx]);
            button.setTextOff(weekdayStrings[idx]);
            button.setText(weekdayStrings[idx]);
            button.setOnClickListener(dayButtonOnclickListener);
        }

        Drawable icon = getResources().getDrawable(R.drawable.ic_delete);
        Bitmap bitmap = ((BitmapDrawable) icon).getBitmap();
        Drawable scaled = new BitmapDrawable(
                getResources(),
                Bitmap.createScaledBitmap(
                        bitmap,
                        (int) (0.6f * bitmap.getWidth()),
                        (int) (0.6f * bitmap.getHeight()),
                        true
                )
        );

        buttonDelete.setCompoundDrawablesWithIntrinsicBounds(scaled, null, null, null);

        buttonDown.setImageResource(R.drawable.ic_expand);
        buttonDown.setSoundEffectsEnabled(false);
        buttonDown.setOnClickListener(buttonDownOnClickListener);

        checkBoxIsRepeating.setOnCheckedChangeListener(checkboxOnCheckedChangeListener);
        update();
        switchActive.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                alarmClockEntry.isActive = isChecked;
                ((SetAlarmClockActivity) context).onEntryStateChanged(alarmClockEntry);
            }
        });
        timeView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ((SetAlarmClockActivity) context).onTimeClicked(view);
            }
        });
        textViewSound.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fm = ((Activity) getContext()).getFragmentManager();
                ManageAlarmSoundsDialogFragment dialog = new ManageAlarmSoundsDialogFragment();
                dialog.setIsPurchased(
                        ((BillingHelperActivity) context).isPurchased(BillingHelper.ITEM_WEB_RADIO)
                );
//                dialog.setSelectedUri(settings.AlarmToneUri);
                dialog.setOnAlarmToneSelectedListener(new ManageAlarmSoundsDialogFragment.ManageAlarmSoundsDialogListener() {
                    @Override
                    public void onAlarmToneSelected(Uri uri, String name) {
                        Log.i(TAG, "onAlarmToneSelected: " + uri + ", " + name);
//                        String summary;
//                        if (purchased_web_radio || !uri.getScheme().equals("file")) {
//                            settings.setAlarmToneUri(uri != null ? uri.toString() : null, name);
//                            summary = name;
//                        } else {
//                            summary = String.format("%s (%s)", name,
//                                    mContext.getString(R.string.product_name_webradio));
//                        }
//                        customAlarmToneURI.setSummary(summary);
                    }

                    @Override
                    public void onPurchaseRequested() {
                        Log.w(TAG, "purchase requested");
//                        showPurchaseDialog();
                    }

                });
                dialog.show(fm, "custom sounds");
            }
        });
    }

    private void update() {
        if (alarmClockEntry != null) {
            Calendar time = alarmClockEntry.getCalendar();
            String text = Utility.formatTime(timeFormat, time);
            timeView.setText(text);
            switchActive.setChecked(alarmClockEntry.isActive);

            if (alarmClockEntry.isRecurring()) {
                textViewWhen.setText(alarmClockEntry.getWeekDaysAsString());
            } else if (isToday(time)) {
                textViewWhen.setText(R.string.today);
            } else if (isTomorrow(time)) {
                textViewWhen.setText(R.string.tomorrow);
            }

            checkBoxIsRepeating.setOnCheckedChangeListener(null);
            checkBoxIsRepeating.setChecked(alarmClockEntry.isRecurring());
            checkBoxIsRepeating.setOnCheckedChangeListener(checkboxOnCheckedChangeListener);


            for (int i = 0; i < dayButtons.length; i++) {
                int day = (int) dayButtons[i].getTag();
                dayButtons[i].setChecked(alarmClockEntry.hasDay(day));
            }
        }

        invalidate();
    }

}
