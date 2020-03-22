package com.firebirdberlin.nightdream.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.models.AnalogClockConfig;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class CustomAnalogClockPreferencesLayout extends LinearLayout {

    static int active_layout = 0;
    private OnConfigChangedListener mListener = null;
    private boolean isPurchased = false;
    AppCompatActivity activity;

    public CustomAnalogClockPreferencesLayout(
            Context context, AnalogClockConfig.Style preset, AppCompatActivity activity
    ) {
        super(context);
        this.activity = activity;
        init(context, preset);
    }

    private static void activateLayout(LinearLayout[] layouts, TextView toggleText,
                                       int active_layout) {
        for (int i = 0; i < layouts.length; i++) {
            if (i == active_layout) continue;
            layouts[i].setVisibility(GONE);
        }
        LinearLayout layout = layouts[active_layout];
        toggleText.setText((String) layout.getTag());
        layout.setVisibility(VISIBLE);
        layout.invalidate();
    }

    private void init(Context context, AnalogClockConfig.Style preset) {
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View child = inflater.inflate(R.layout.custom_analog_clock_preferences_layout, null);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(child, lp);

        final AnalogClockConfig config = new AnalogClockConfig(getContext(), preset);

        View toggleNext = child.findViewById(R.id.toggle_preference_next);
        View togglePrev = child.findViewById(R.id.toggle_preference_prev);
        final TextView toggleText = child.findViewById(R.id.toggle_preference);
        final LinearLayout labelsLayout = child.findViewById(R.id.labels_preference_layout);
        final LinearLayout handsLayout = child.findViewById(R.id.hands_preference_layout);
        final LinearLayout ticksLayout = child.findViewById(R.id.ticks_preference_layout);
        final LinearLayout decorationLayout = child.findViewById(R.id.decoration_preference_layout);

        final LinearLayout[] layouts = {labelsLayout, handsLayout, ticksLayout, decorationLayout};

        activateLayout(layouts, toggleText, active_layout);
        toggleNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                active_layout += 1;
                if (active_layout == layouts.length) active_layout = 0;

                activateLayout(layouts, toggleText, active_layout);
            }
        });
        togglePrev.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                active_layout--;
                if (active_layout == -1) active_layout = layouts.length - 1;

                activateLayout(layouts, toggleText, active_layout);
            }
        });

        setupLayoutForLabels(child, config);
        setupLayoutForHands(child, config);
        setupLayoutForTicks(child, config);
        setupLayoutForDecoration(child, config);
    }

    private void setupLayoutForLabels(View child, final AnalogClockConfig config) {
        final TextView info = child.findViewById(R.id.info_text_labels);
        info.setVisibility(INVISIBLE);

        final TextView digitStylePreference = child.findViewById(R.id.digit_style_preference);
        setChoice(digitStylePreference, R.array.numberStyles, config.digitStyle.getValue());
        digitStylePreference.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.style)
                        .setItems(R.array.numberStyles, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                config.digitStyle = AnalogClockConfig.DigitStyle.fromValue(which);
                                setChoice(digitStylePreference, R.array.numberStyles, config.digitStyle.getValue());
                                configHasChanged(config);
                            }
                        });
                builder.show();
            }
        });

        final TextView fontButton = child.findViewById(R.id.typeface_preference);
        setFontButtonText(fontButton, config);
        fontButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (activity == null) {
                    return;
                }
                FragmentManager fm = activity.getSupportFragmentManager();
                ManageFontsDialogFragment dialog = new ManageFontsDialogFragment();
                dialog.setIsPurchased(isPurchased);
                dialog.setSelectedUri(config.fontUri);
                dialog.setDefaultFonts(
                        "roboto_regular.ttf", "roboto_light.ttf", "roboto_thin.ttf",
                        "7_segment_digital.ttf", "dseg14classic.ttf", "dancingscript_regular.ttf"
                        );
                dialog.setOnFontSelectedListener(new ManageFontsDialogFragment.ManageFontsDialogListener() {
                    @Override
                    public void onFontSelected(Uri uri, String name) {
                        config.fontUri = uri.toString();
                        setFontButtonText(fontButton, config);
                        configHasChanged(config);
                    }

                    @Override
                    public void onPurchaseRequested() {
                        if (mListener != null) {
                            mListener.onPurchaseRequested();
                        }
                    }
                });
                dialog.show(fm, "custom fonts");
            }
        });

        SeekBar digitPositionSeekBar = child.findViewById(R.id.digit_position_preference);
        digitPositionSeekBar.setProgress((int) (config.digitPosition * 100));
        digitPositionSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                config.digitPosition = progress / 100f;
                configHasChanged(config);
                info.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                info.setVisibility(VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                info.setVisibility(INVISIBLE);
            }
        });

        SeekBar digitSizeSeekBar = child.findViewById(R.id.digit_size_preference);
        digitSizeSeekBar.setProgress((int) (config.fontSize * 100 - 5));
        digitSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                config.fontSize = (5 + progress) / 100f;
                configHasChanged(config);
                info.setText(String.valueOf(5 + progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                info.setVisibility(VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                info.setVisibility(INVISIBLE);
            }
        });

        Switch emphasizeQuarters = child.findViewById(R.id.switch_emphasize_quarters);
        emphasizeQuarters.setChecked(config.highlightQuarterOfHour);
        emphasizeQuarters.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                config.highlightQuarterOfHour = checked;
                configHasChanged(config);
            }
        });
    }

    private void setupLayoutForHands(View child, final AnalogClockConfig config) {
        final TextView info = child.findViewById(R.id.info_text_hands);
        info.setVisibility(INVISIBLE);

        final TextView handShapePreference = child.findViewById(R.id.hand_shape_preference);
        setChoice(handShapePreference, R.array.handShapes, config.handShape.getValue());
        handShapePreference.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.style)
                        .setItems(R.array.handShapes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                config.handShape = AnalogClockConfig.HandShape.fromValue(which);
                                setChoice(handShapePreference, R.array.handShapes, config.handShape.getValue());
                                configHasChanged(config);
                            }
                        });
                builder.show();
            }
        });

        SeekBar hourHandLength = child.findViewById(R.id.hour_hand_length);
        hourHandLength.setProgress((int) (config.handLengthHours * 100));
        hourHandLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                config.handLengthHours = progress / 100f;
                configHasChanged(config);
                info.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                info.setVisibility(VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                info.setVisibility(INVISIBLE);
            }
        });
        SeekBar minuteHandLength = child.findViewById(R.id.minute_hand_length);
        minuteHandLength.setProgress((int) (config.handLengthMinutes * 100));
        minuteHandLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                config.handLengthMinutes = progress / 100f;
                configHasChanged(config);
                info.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                info.setVisibility(VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                info.setVisibility(INVISIBLE);
            }
        });

        SeekBar hourHandWidth = child.findViewById(R.id.hour_hand_width);
        hourHandWidth.setProgress((int) (config.handWidthHours * 100));
        hourHandWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                config.handWidthHours = progress / 100f;
                configHasChanged(config);
                info.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                info.setVisibility(VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                info.setVisibility(INVISIBLE);
            }
        });
        SeekBar minuteHandWidth = child.findViewById(R.id.minute_hand_width);
        minuteHandWidth.setProgress((int) (config.handWidthMinutes * 100));
        minuteHandWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                config.handWidthMinutes = progress / 100f;
                configHasChanged(config);
                info.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                info.setVisibility(VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                info.setVisibility(INVISIBLE);
            }
        });

    }

    private void setupLayoutForTicks(View child, final AnalogClockConfig config) {

        final TextView info = child.findViewById(R.id.info_text_ticks);
        info.setVisibility(INVISIBLE);

        final TextView tickStyleHoursPreference = child.findViewById(R.id.tick_style_hours_preference);
        setChoice(tickStyleHoursPreference, R.array.tickStyles, config.tickStyleHours.getValue());
        tickStyleHoursPreference.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.style)
                        .setItems(R.array.tickStyles, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                config.tickStyleHours = AnalogClockConfig.TickStyle.fromValue(which);
                                setChoice(tickStyleHoursPreference, R.array.tickStyles, config.tickStyleHours.getValue());
                                configHasChanged(config);
                            }
                        });
                builder.show();
            }
        });
        final TextView tickStyleMinutesPreference = child.findViewById(R.id.tick_style_minutes_preference);
        setChoice(tickStyleMinutesPreference, R.array.tickStyles, config.tickStyleMinutes.getValue());
        tickStyleMinutesPreference.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.style)
                        .setItems(R.array.tickStyles, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                config.tickStyleMinutes = AnalogClockConfig.TickStyle.fromValue(which);
                                setChoice(tickStyleMinutesPreference, R.array.tickStyles, config.tickStyleMinutes.getValue());
                                configHasChanged(config);
                            }
                        });
                builder.show();
            }
        });

        SeekBar minuteTickStart = child.findViewById(R.id.minute_tick_start);
        minuteTickStart.setProgress((int) (config.tickStartMinutes * 100));
        minuteTickStart.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                config.tickStartMinutes = progress / 100f;
                configHasChanged(config);
                info.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                info.setVisibility(VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                info.setVisibility(INVISIBLE);
            }
        });
        SeekBar minuteTickLength = child.findViewById(R.id.minute_tick_length);
        minuteTickLength.setProgress((int) (config.tickLengthMinutes * 100));
        minuteTickLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                config.tickLengthMinutes = progress / 100f;
                configHasChanged(config);
                info.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                info.setVisibility(VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                info.setVisibility(INVISIBLE);
            }
        });
        SeekBar hourTickStart = child.findViewById(R.id.hour_tick_start);
        hourTickStart.setProgress((int) (config.tickStartHours * 100));
        hourTickStart.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                config.tickStartHours = progress / 100f;
                configHasChanged(config);
                info.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                info.setVisibility(VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                info.setVisibility(INVISIBLE);
            }
        });
        SeekBar hourTickLength = child.findViewById(R.id.hour_tick_length);
        hourTickLength.setProgress((int) (config.tickLengthHours * 100));
        hourTickLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                config.tickLengthHours = progress / 100f;
                configHasChanged(config);
                info.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                info.setVisibility(VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                info.setVisibility(INVISIBLE);
            }
        });
    }

    private void setupLayoutForDecoration(View child, final AnalogClockConfig config) {

        final TextView info = child.findViewById(R.id.info_text_decoration);
        info.setVisibility(INVISIBLE);

        final TextView decorationStylePreference = child.findViewById(R.id.decoration_preference);
        setChoice(decorationStylePreference, R.array.decorationStyles, config.decoration.getValue());
        decorationStylePreference.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.style)
                        .setItems(R.array.decorationStyles, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                config.decoration = AnalogClockConfig.Decoration.fromValue(which);
                                setChoice(decorationStylePreference, R.array.decorationStyles, config.decoration.getValue());
                                configHasChanged(config);
                            }
                        });
                builder.show();
            }
        });

        SeekBar outerCirclePosition = child.findViewById(R.id.outer_circle_position);
        outerCirclePosition.setProgress((int) (config.outerCircleRadius * 100));
        outerCirclePosition.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                config.outerCircleRadius = progress / 100f;
                configHasChanged(config);
                info.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                info.setVisibility(VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                info.setVisibility(INVISIBLE);
            }
        });
        SeekBar outerCircleWidth = child.findViewById(R.id.outer_circle_width);
        outerCircleWidth.setProgress((int) (config.outerCircleWidth * 100));
        outerCircleWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                config.outerCircleWidth = progress / 100f;
                configHasChanged(config);
                info.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                info.setVisibility(VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                info.setVisibility(INVISIBLE);
            }
        });
        SeekBar innerCircleRadius = child.findViewById(R.id.inner_circle_radius);
        innerCircleRadius.setProgress((int) (config.innerCircleRadius * 100));
        innerCircleRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                config.innerCircleRadius = progress / 100f;
                configHasChanged(config);
                info.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                info.setVisibility(VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                info.setVisibility(INVISIBLE);
            }
        });
    }

    private void setChoice(TextView pref, int resID, int intValue) {
        final String[] values = getResources().getStringArray(resID);
        String text = (String) pref.getTag();
        text = String.format("%s: %s", text, values[intValue]);
        pref.setText(text);
    }

    private void setFontButtonText(TextView fontButton, AnalogClockConfig config) {
        String fontButtonText = fontButton.getTag().toString();
        String[] segments = config.fontUri.split("/");
        if (segments.length > 0) {
            String name =
                    ManageFontsDialogFragment.getUserFriendlyFileName(segments[segments.length - 1]);

            fontButtonText = String.format("%s: %s", fontButton.getTag(), name);
        }
        fontButton.setText(fontButtonText);
    }

    private void configHasChanged(AnalogClockConfig config) {
        config.save();
        if (mListener != null) {
            mListener.onConfigChanged();
        }
    }

    public void setIsPurchased(boolean isPurchased) {
        this.isPurchased = isPurchased;
    }

    public void setOnConfigChangedListener(OnConfigChangedListener listener) {
        this.mListener = listener;
    }

    public interface OnConfigChangedListener {
        void onConfigChanged();

        void onPurchaseRequested();
    }
}
