package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

public class WebRadioImageView extends AppCompatImageView {
    private static String TAG ="WebRadioImageView";

    private Context context;

    public WebRadioImageView(Context context) {
        super(context);
        init(context);
    }

    public WebRadioImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        if (Build.VERSION.SDK_INT < 14) {
            setVisibility(View.GONE);
        }
    }
}
