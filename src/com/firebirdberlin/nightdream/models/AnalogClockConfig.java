package com.firebirdberlin.nightdream.models;

import android.content.Context;
import android.content.SharedPreferences;

import com.firebirdberlin.nightdream.ui.ClockLayout;

public class AnalogClockConfig {
    public Decoration decoration = Decoration.NONE;
    public float digitPosition = 0.85f;
    public DigitStyle digitStyle = DigitStyle.ARABIC;
    public boolean emphasizeHour12 = true;
    public boolean showSecondHand = false;
    public HandShape handShape = HandShape.TRIANGLE;
    public float handLengthHours = 0.8f;
    public float handLengthMinutes = 0.95f;
    public float handWidthHours = 0.04f;
    public float handWidthMinutes = 0.04f;
    public boolean highlightQuarterOfHour = true;
    public float innerCircleRadius = 0.045f;
    public float tickStartMinutes = 0.95f;
    public TickStyle tickStyleMinutes = TickStyle.DASH;
    public float tickLengthMinutes = 0.04f;
    public float tickStartHours = 0.95f;
    public float tickWidthHours = 0.01f;
    public float tickWidthMinutes = 0.01f;
    public TickStyle tickStyleHours = TickStyle.CIRCLE;
    public float tickLengthHours = 0.04f;
    public float outerCircleRadius = 1.f;
    public float outerCircleWidth = 0f;
    public float fontSize = 0.08f;
    public String fontUri = "file:///android_asset/fonts/dancingscript_regular.ttf";
    Context context;
    Style style;
    public AnalogClockConfig(Context context, Style style) {
        this.context = context;
        this.style = style;
        if (!stored_preferences_exists()) {
            reset();
        } else {
            load();
        }
    }

    public static Style toClockStyle(int layoutId) {
        switch (layoutId) {
            case ClockLayout.LAYOUT_ID_ANALOG:
                return Style.MINIMALISTIC;
            case ClockLayout.LAYOUT_ID_ANALOG2:
                return Style.SIMPLE;
            case ClockLayout.LAYOUT_ID_ANALOG3:
                return Style.ARC;
            case ClockLayout.LAYOUT_ID_ANALOG4:
                return Style.DEFAULT;
            default:
                return Style.MINIMALISTIC;
        }
    }

    public void reset() {
        initStyle(this.style);
        save();
    }

    public boolean stored_preferences_exists() {
        SharedPreferences settings = context.getSharedPreferences(style.name(), 0);
        return (settings.contains("digitStyle"));
    }

    public void load() {
        SharedPreferences settings = context.getSharedPreferences(style.name(), 0);
        String digitStyleString = settings.getString("digitStyle", DigitStyle.NONE.name());
        digitStyle = DigitStyle.valueOf(digitStyleString);

        digitPosition = settings.getFloat("digitPosition", 0.85f);

        String decorationString = settings.getString("decoration", Decoration.NONE.name());
        decoration = Decoration.valueOf(decorationString);

        emphasizeHour12 = settings.getBoolean("emphasizeHour12", true);
        showSecondHand = settings.getBoolean("showSecondHand", false);

        String handShapeString = settings.getString("handShape", HandShape.TRIANGLE.name());
        handShape = HandShape.valueOf(handShapeString);

        handLengthHours = settings.getFloat("handLengthHours", 0.8f);
        handLengthMinutes = settings.getFloat("handLengthMinutes", 0.95f);
        handWidthHours = settings.getFloat("handWidthHours", 0.04f);
        handWidthMinutes = settings.getFloat("handWidthMinutes", 0.04f);
        highlightQuarterOfHour = settings.getBoolean("highlightQuarterOfHour", true);
        innerCircleRadius = settings.getFloat("innerCircleRadius", 0.045f);
        tickLengthMinutes = settings.getFloat("tickLengthMinutes", 0.04f);
        tickLengthHours = settings.getFloat("tickLengthHours", 0.04f);
        tickStartMinutes = settings.getFloat("tickStartMinutes", 0.95f);
        tickStartHours = settings.getFloat("tickStartHours", 0.95f);

        String tickStyleMinutesString = settings.getString("tickStyleMinutes", TickStyle.DASH.name());
        tickStyleMinutes = TickStyle.valueOf(tickStyleMinutesString);

        String tickStyleHoursString = settings.getString("tickStyleHours", TickStyle.CIRCLE.name());
        tickStyleHours = TickStyle.valueOf(tickStyleHoursString);

        tickWidthHours = settings.getFloat("tickWidthHours", 0.01f);
        tickWidthMinutes = settings.getFloat("tickWidthMinutes", 0.01f);
        outerCircleRadius = settings.getFloat("outerCircleRadius", 1.f);
        outerCircleWidth = settings.getFloat("outerCircleWidth", 0.f);


        fontSize = settings.getFloat("fontSize", 0.08f);

        fontUri = settings.getString("fontUri", "file:///android_asset/fonts/dancingscript_regular.ttf");
    }

    public void save(){
        SharedPreferences settings = context.getSharedPreferences(style.name(), 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("emphasizeHour12", emphasizeHour12);
        editor.putBoolean("highlightQuarterOfHour", highlightQuarterOfHour);
        editor.putBoolean("showSecondHand", showSecondHand);
        editor.putFloat("digitPosition", digitPosition);
        editor.putFloat("fontSize", fontSize);
        editor.putFloat("handLengthHours", handLengthHours);
        editor.putFloat("handLengthMinutes", handLengthMinutes);
        editor.putFloat("handWidthHours", handWidthHours);
        editor.putFloat("handWidthMinutes", handWidthMinutes);
        editor.putFloat("innerCircleRadius", innerCircleRadius);
        editor.putFloat("outerCircleRadius", outerCircleRadius);
        editor.putFloat("outerCircleWidth", outerCircleWidth);
        editor.putFloat("tickLengthHours", tickLengthHours);
        editor.putFloat("tickLengthMinutes", tickLengthMinutes);
        editor.putFloat("tickStartHours", tickStartHours);
        editor.putFloat("tickStartMinutes", tickStartMinutes);
        editor.putFloat("tickWidthHours", tickWidthHours);
        editor.putFloat("tickWidthMinutes", tickWidthMinutes);
        editor.putString("decoration", decoration.name());
        editor.putString("digitStyle", digitStyle.name() );
        editor.putString("fontUri", fontUri);
        editor.putString("handShape", handShape.name());
        editor.putString("tickStyleHours", tickStyleHours.name());
        editor.putString("tickStyleMinutes", tickStyleMinutes.name());
        editor.apply();
    }

    public void initStyle(Style style) {
        switch (style) {
            case DEFAULT:
                decoration = Decoration.NONE;
                digitPosition = 0.85f;
                digitStyle = DigitStyle.ARABIC;
                emphasizeHour12 = true;
                fontSize = 0.08f;
                handLengthHours = 0.8f;
                handLengthMinutes = 0.95f;
                handShape = HandShape.TRIANGLE;
                handWidthHours = 0.04f;
                handWidthMinutes = 0.04f;
                highlightQuarterOfHour = true;
                innerCircleRadius = 0.045f;
                outerCircleRadius = 1.f;
                outerCircleWidth = 0.f;
                showSecondHand = false;
                tickLengthHours = 0.04f;
                tickLengthMinutes = 0.04f;
                tickStartHours = 0.95f;
                tickStartMinutes = 0.95f;
                tickStyleHours = TickStyle.CIRCLE;
                tickStyleMinutes = TickStyle.DASH;
                tickWidthHours = 0.01f;
                tickWidthMinutes = 0.01f;
                break;
            case SIMPLE:
                decoration = Decoration.MINUTE_HAND;
                digitPosition = 0.85f;
                digitStyle = DigitStyle.NONE;
                emphasizeHour12 = true;
                fontSize = 0.08f;
                handLengthHours = 0.6f;
                handLengthMinutes = 0.9f;
                handShape = HandShape.TRIANGLE;
                handWidthHours = 0.04f;
                handWidthMinutes = 0.04f;
                highlightQuarterOfHour = false;
                innerCircleRadius = 0.045f;
                outerCircleRadius = 1.f;
                outerCircleWidth = 0.f;
                showSecondHand = false;
                tickLengthHours = 0.06f;
                tickLengthMinutes = 0.06f;
                tickStartHours = 0.87f;
                tickStartMinutes = 0.87f;
                tickStyleHours = TickStyle.DASH;
                tickStyleMinutes = TickStyle.NONE;
                tickWidthHours = 0.01f;
                tickWidthMinutes = 0.01f;
                break;
            case ARC:
                decoration = Decoration.NONE;
                digitPosition = 0.85f;
                digitStyle = DigitStyle.NONE;
                emphasizeHour12 = true;
                fontSize = 0.08f;
                handLengthHours = 0.80f;
                handLengthMinutes = 0.90f;
                handShape = HandShape.ARC;
                handWidthHours = 0.06f;
                handWidthMinutes = 0.06f;
                highlightQuarterOfHour = false;
                innerCircleRadius = 0.045f;
                outerCircleRadius = 1.f;
                outerCircleWidth = 0.f;
                showSecondHand = false;
                tickLengthHours = 0.06f;
                tickLengthMinutes = 0.06f;
                tickStartHours = 0.87f;
                tickStartMinutes = 0.87f;
                tickStyleHours = TickStyle.DASH;
                tickStyleMinutes = TickStyle.NONE;
                tickWidthHours = 0.01f;
                tickWidthMinutes = 0.01f;
                break;
            case MINIMALISTIC:
                decoration = Decoration.NONE;
                digitPosition = 0.7f;
                digitStyle = DigitStyle.NONE;
                emphasizeHour12 = true;
                fontSize = 0.08f;
                handLengthHours = 0.6f;
                handLengthMinutes = 0.8f;
                handShape = HandShape.BAR;
                handWidthHours = 0.02f;
                handWidthMinutes = 0.02f;
                highlightQuarterOfHour = false;
                innerCircleRadius = 0.0f;
                outerCircleRadius = 1.f;
                outerCircleWidth = 0.01f;
                showSecondHand = false;
                tickLengthHours = 0.1f;
                tickLengthMinutes = 0.06f;
                tickStartHours = 0.84f;
                tickStartMinutes = 0.87f;
                tickStyleHours = TickStyle.DASH;
                tickStyleMinutes = TickStyle.NONE;
                tickWidthHours = 0.025f;
                tickWidthMinutes = 0.025f;
                break;
        }

    }

    public enum Style {DEFAULT, SIMPLE, ARC, MINIMALISTIC}

    public enum DigitStyle {
        NONE(0), ARABIC(1), ROMAN(2);

        private final int value;

        DigitStyle(int value) {
            this.value = value;
        }

        public static DigitStyle fromValue(int i) {
            for (DigitStyle style : values()) {
                if (style.value == i) return style;
            }
            return DigitStyle.NONE;
        }

        public int getValue() {
            return value;
        }
    }

    public enum TickStyle {
        NONE(0), DASH(1), CIRCLE(2);

        private final int value;

        TickStyle(int value) {
            this.value = value;
        }

        public static TickStyle fromValue(int i) {
            for (TickStyle style : values()) {
                if (style.value == i) return style;
            }
            return TickStyle.NONE;
        }

        public int getValue() {
            return value;
        }
    }
    public enum Decoration {
        NONE(0), MINUTE_HAND(1), LABELS(2), GOLD(3), COPPER(4), RUST(5);

        private final int value;

        Decoration(int value) {
            this.value = value;
        }

        public static Decoration fromValue(int i) {
            for (Decoration style : values()) {
                if (style.value == i) return style;
            }
            return Decoration.NONE;
        }

        public int getValue() {
            return value;
        }
    }

    public enum HandShape {
        BAR(0), TRIANGLE(1), ARC(2);

        private final int value;

        HandShape(int value) {
            this.value = value;
        }

        public static HandShape fromValue(int i) {
            for (HandShape style : values()) {
                if (style.value == i) return style;
            }
            return HandShape.BAR;
        }

        public int getValue() {
            return value;
        }
    }
}
