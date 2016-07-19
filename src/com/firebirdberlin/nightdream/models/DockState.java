package com.firebirdberlin.nightdream.models;

public class DockState {
    public boolean isUndocked = false;
    public boolean isDockedCar = false;
    public boolean isDockedDesk = false;

    public DockState(boolean undocked, boolean car, boolean desk) {
        this.isUndocked = undocked;
        this.isDockedCar = car;
        this.isDockedDesk = desk;
    }
}
