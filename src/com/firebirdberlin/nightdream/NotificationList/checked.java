package com.firebirdberlin.nightdream.NotificationList;

import java.io.Serializable;

public class checked implements Serializable {

    private boolean isChecked = false;

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }
}
