package com.firebirdberlin.nightdream;

public class WindSpeedConversion {


    public static int metersPerSecondToBeaufort(double mps) {
        if (mps < 0.3) {
            return 0;
        } else if (mps < 1.6) {
            return 1;
        } else if (mps < 3.4) {
            return 2;
        } else if (mps < 5.5) {
            return 3;
        } else if (mps < 8) {
            return 4;
        } else if (mps < 10.8) {
            return 5;
        } else if (mps < 13.9) {
            return 6;
        } else if (mps < 17.2) {
            return 7;
        } else if (mps < 20.8) {
            return 8;
        } else if (mps < 24.5) {
            return 9;
        } else if (mps < 28.5) {
            return 10;
        } else if (mps < 32.7) {
            return 11;
        } else if (mps >= 32.7) {
            return 12;
        } else {
            return -1;
        }
    }

    public static double metersPerSecondToMilesPerHour(double mps) {
        return mps  * 3600. / 1609.344;
    }

    public static double metersPerSecondToKilometersPerHour(double mps) {
        return mps  * 3.6;
    }
}
