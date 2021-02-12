package com.firebirdberlin.nightdream.ui;


import android.animation.LayoutTransition;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.FragmentManager;

import com.firebirdberlin.nightdream.BillingHelperActivity;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.SetAlarmClockActivity;
import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.repositories.VibrationHandler;
import com.firebirdberlin.radiostreamapi.models.FavoriteRadioStations;
import com.firebirdberlin.radiostreamapi.models.RadioStation;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;

public class AlarmClockLayout extends LinearLayout {

    private static final String TAG = "AlarmClockLayout";
    private final Context context;
    private final ToggleButton[] dayButtons = new ToggleButton[7];
    private final int firstDayOfWeek = Utility.getFirstDayOfWeek();
    private String timeFormat = "h:mm";
    private String dateFormat;
    private ConstraintLayout mainLayout = null;
    private SimpleTime alarmClockEntry = null;
    private final ToggleButton.OnClickListener dayButtonOnclickListener =
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
                    ((SetAlarmClockActivity) context).onEntryStateChanged(alarmClockEntry);
                }
            };
    private TextView timeView = null;
    private TextView textViewSound = null;
    private TextView textViewRadio = null;
    private TextView textViewVibrate = null;
    private TextView textViewWhen = null;
    private ImageView buttonDown = null;
    private ConstraintLayout layoutDays = null;
    private final CheckBox.OnCheckedChangeListener checkboxOnCheckedChangeListener =
            new CheckBox.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    layoutDays.setVisibility((checked) ? View.VISIBLE : View.GONE);
                    if (!checked) {
                        for (int d : SimpleTime.DAYS) {
                            alarmClockEntry.removeRecurringDay(d);
                            dayButtons[d - 1].setChecked(false);
                        }
                        ((SetAlarmClockActivity) context).onEntryStateChanged(alarmClockEntry);
                    }
                }
            };
    private ConstraintLayout secondaryLayout = null;
    private ImageView imageViewDelete = null;
    private Button butondelete = null;
    private ToggleButton toggleActive = null;
    private SwitchCompat switchActive = null;
    private CheckBox checkBoxIsRepeating = null;
    private final ImageView.OnClickListener buttonDownOnClickListener = new ImageView.OnClickListener() {
        @Override
        public void onClick(View view) {
            boolean gone = secondaryLayout.getVisibility() == View.GONE;
            showSecondaryLayout(gone);
        }
    };
    private FavoriteRadioStations radioStations;

    public AlarmClockLayout(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public AlarmClockLayout(
            Context context, SimpleTime entry, String timeFormat, String dateFormat,
            FavoriteRadioStations radioStations
    ) {
        super(context);
        this.context = context;
        this.alarmClockEntry = entry;
        this.timeFormat = timeFormat;
        this.dateFormat = dateFormat;
        this.radioStations = radioStations;
        init();
        imageViewDelete.setTag(entry);
        timeView.setTag(entry);
        textViewWhen.setTag(entry);
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

    public void showSecondaryLayout(boolean on) {
        secondaryLayout.setVisibility(on ? View.VISIBLE : View.GONE);
        buttonDown.setImageResource(on ? R.drawable.ic_collapse : R.drawable.ic_expand);
        layoutDays.setVisibility(
                (on && checkBoxIsRepeating.isChecked()) ? View.VISIBLE : View.GONE
        );

        mainLayout.setBackgroundColor(on ? getResources().getColor(R.color.grey) : Color.TRANSPARENT);
    }

    private void init() {

        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View child = inflater.inflate(R.layout.alarm_clock_layout, null);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(child, lp);
        mainLayout = findViewById(R.id.mainLayout);
        timeView = findViewById(R.id.timeView);
        textViewSound = findViewById(R.id.textViewSound);
        textViewRadio = findViewById(R.id.textViewRadio);
        textViewVibrate = findViewById(R.id.textViewVibrate);
        textViewWhen = findViewById(R.id.textViewWhen);
        layoutDays = findViewById(R.id.layoutDays);
        buttonDown = findViewById(R.id.button_down);
        imageViewDelete = findViewById(R.id.imageViewDelete);
        secondaryLayout = findViewById(R.id.secondaryLayout);
        toggleActive = findViewById(R.id.enabled);
        switchActive = findViewById(R.id.enabledswitch);
        checkBoxIsRepeating = findViewById(R.id.checkBoxIsRepeating);

        ConstraintLayout middle = findViewById(R.id.middle);
        LayoutTransition layoutTransition = middle.getLayoutTransition();
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING);

        dayButtons[0] = findViewById(R.id.dayButton1);
        dayButtons[1] = findViewById(R.id.dayButton2);
        dayButtons[2] = findViewById(R.id.dayButton3);
        dayButtons[3] = findViewById(R.id.dayButton4);
        dayButtons[4] = findViewById(R.id.dayButton5);
        dayButtons[5] = findViewById(R.id.dayButton6);
        dayButtons[6] = findViewById(R.id.dayButton7);

        String[] weekdayStrings = Utility.getWeekdayStrings();
        for (int i : SimpleTime.DAYS) {
            int idx = (i - 1 + firstDayOfWeek - 1) % 7 + 1;
            ToggleButton button = dayButtons[i - 1];
            button.setTag(idx);
            button.setTextOn(weekdayStrings[idx]);
            button.setTextOff(weekdayStrings[idx]);
            button.setText(weekdayStrings[idx]);
            button.setOnClickListener(dayButtonOnclickListener);
        }

        buttonDown.setImageResource(R.drawable.ic_expand);
        buttonDown.setSoundEffectsEnabled(false);
        buttonDown.setOnClickListener(buttonDownOnClickListener);

        checkBoxIsRepeating.setOnCheckedChangeListener(checkboxOnCheckedChangeListener);
        update();
        SwitchCompat.OnCheckedChangeListener checkedChangeListener = new SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                alarmClockEntry.isActive = isChecked;
                if (isChecked) {
                    switchActive.setChecked(true);
                    toggleActive.setChecked(true);
                    if (Utility.languageIs("de", "en")) {
                        Snackbar snackbar = Snackbar.make(
                                child,
                                alarmClockEntry.getRemainingTimeString(context),
                                Snackbar.LENGTH_LONG
                        );
                        snackbar.setBackgroundTint(getResources().getColor(R.color.material_grey));
                        snackbar.show();
                    }
                } else {
                    switchActive.setChecked(false);
                    toggleActive.setChecked(false);
                }
                ((SetAlarmClockActivity) context).onEntryStateChanged(alarmClockEntry);
                child.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
        };
        toggleActive.setOnCheckedChangeListener(checkedChangeListener);
        switchActive.setOnCheckedChangeListener(checkedChangeListener);
        timeView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ((SetAlarmClockActivity) context).onTimeClicked(view);
            }
        });
        textViewWhen.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ((SetAlarmClockActivity) context).onDateClicked(view);
            }
        });

        textViewSound.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (alarmClockEntry == null) return;

                FragmentManager fm = ((AppCompatActivity) getContext()).getSupportFragmentManager();
                ManageAlarmSoundsDialogFragment dialog = new ManageAlarmSoundsDialogFragment();
                dialog.setIsPurchased(
                        ((BillingHelperActivity) context).isPurchased(BillingHelperActivity.ITEM_WEB_RADIO)
                );
                dialog.setContext(getContext());
                dialog.setSelectedUri(alarmClockEntry.soundUri);
                dialog.setOnAlarmToneSelectedListener(new ManageAlarmSoundsDialogFragment.ManageAlarmSoundsDialogListener() {
                    @Override
                    public void onAlarmToneSelected(Uri uri, String name) {
                        Log.i(TAG, "onAlarmToneSelected: " + uri + ", " + name);
                        if (alarmClockEntry == null) {
                            return;
                        }
                        alarmClockEntry.soundUri = uri.toString();
                        ((SetAlarmClockActivity) context).onEntryStateChanged(alarmClockEntry);
                    }

                    @Override
                    public void onPurchaseRequested() {
                        Log.w(TAG, "purchase requested");
                        ((BillingHelperActivity) context).showPurchaseDialog();
                    }

                });
                dialog.show(fm, "custom sounds");
            }
        });

        String stationName = getResources().getString(R.string.radio_station_none);
        if (alarmClockEntry.radioStationIndex > -1) {

            stationName = getResources().getString(R.string.radio_station) + " #" + (alarmClockEntry.radioStationIndex + 1);
            if (radioStations != null) {
                RadioStation station = radioStations.get(alarmClockEntry.radioStationIndex);
                if (station != null) {
                    stationName = station.name;
                }
            }

        }

        textViewRadio.setText(stationName);
        textViewRadio.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (alarmClockEntry == null) return;

                BillingHelperActivity billingHelperActivity = (BillingHelperActivity) context;
                if (!billingHelperActivity.isPurchased(BillingHelperActivity.ITEM_WEB_RADIO)) {
                    billingHelperActivity.showPurchaseDialog();
                } else {
                    FragmentManager fm = ((AppCompatActivity) getContext()).getSupportFragmentManager();
                    SelectRadioStationSlotDialogFragment dialog = new SelectRadioStationSlotDialogFragment();
                    dialog.setRadioStations(radioStations);
                    dialog.setOnStationSlotSelectedListener(
                            new SelectRadioStationSlotDialogFragment.SelectRadioStationSlotDialogListener() {
                                @Override
                                public void onStationSlotSelected(int index, String name) {
                                    Log.i(TAG, "onStationSlotSelected: " + index + ", " + name);
                                    if (alarmClockEntry == null) {
                                        return;
                                    }
                                    textViewRadio.setText(name);
                                    alarmClockEntry.radioStationIndex = index - 1;
                                    ((SetAlarmClockActivity) context).onEntryStateChanged(alarmClockEntry);
                                }

                            }
                    );
                    dialog.show(fm, "radio station");
                }
            }
        });

        textViewVibrate.setVisibility(VibrationHandler.hasVibrator(context) ? VISIBLE : GONE);
        textViewVibrate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                alarmClockEntry.vibrate = !alarmClockEntry.vibrate;
                ((SetAlarmClockActivity) context).onEntryStateChanged(alarmClockEntry);
                setupVibrationIcon();
            }
        });

    }

    public void updateAlarmClockEntry(SimpleTime entry) {
        this.alarmClockEntry = entry;
        update();
    }

    public void update() {
        if (alarmClockEntry != null) {
            long now = System.currentTimeMillis();
            Calendar time = alarmClockEntry.getCalendar();
            String text = Utility.formatTime(timeFormat, time);
            timeView.setText(text);
            toggleActive.setChecked(alarmClockEntry.isActive);
            switchActive.setChecked(alarmClockEntry.isActive);

            String textWhen = "";
            if (alarmClockEntry.isRecurring()) {
                textWhen = alarmClockEntry.getWeekDaysAsString();

                if (alarmClockEntry.nextEventAfter != null &&
                        alarmClockEntry.nextEventAfter > now) {
                    // if the alarm is postponed by the user show the date of the next event
                    textWhen += String.format(
                            "\n%s %s",
                            context.getString(R.string.alarmStartsFrom),
                            Utility.formatTime(dateFormat, time)
                    );
                }
            } else if (alarmClockEntry.nextEventAfter != null && alarmClockEntry.nextEventAfter > now) {
                // if the alarm is postponed by the user show the date of the next event
                textWhen = String.format("%s", Utility.formatTime(dateFormat, time));
            } else if (isToday(time)) {
                textWhen = context.getString(R.string.today);
            } else if (isTomorrow(time)) {
                textWhen = context.getString(R.string.tomorrow);
            }

            textViewWhen.setText(textWhen);

            checkBoxIsRepeating.setOnCheckedChangeListener(null);
            checkBoxIsRepeating.setChecked(alarmClockEntry.isRecurring());
            checkBoxIsRepeating.setOnCheckedChangeListener(checkboxOnCheckedChangeListener);


            for (ToggleButton dayButton : dayButtons) {
                int day = (int) dayButton.getTag();
                dayButton.setChecked(alarmClockEntry.hasDay(day));
            }

            String displayName;
            if (alarmClockEntry.soundUri == null || alarmClockEntry.soundUri.isEmpty()) {
                Uri soundUri = Utility.getDefaultAlarmToneUri();
                displayName = Utility.getSoundFileTitleFromUri(context, soundUri);
            } else {
                displayName = Utility.getSoundFileTitleFromUri(context, alarmClockEntry.soundUri);
            }
            textViewSound.setText(displayName);
            setupVibrationIcon();
            setupDeleteIcon();
        }
        invalidate();
    }

    void setupVibrationIcon() {
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_vibration, null);
        if (drawable != null) {
            drawable = DrawableCompat.wrap(drawable);
            int color = (alarmClockEntry.vibrate)
                    ? ContextCompat.getColor(context, R.color.blue)
                    : ContextCompat.getColor(context, R.color.material_grey);
            DrawableCompat.setTint(drawable, color);
            DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
            textViewVibrate.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        }
    }

    void setupDeleteIcon() {
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_delete, null);
        if (drawable != null) {
            drawable = DrawableCompat.wrap(drawable);
            int color = ContextCompat.getColor(context, R.color.blue);
            DrawableCompat.setTint(drawable, color);
            DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
            imageViewDelete.setImageDrawable(drawable);
        }
    }

}
