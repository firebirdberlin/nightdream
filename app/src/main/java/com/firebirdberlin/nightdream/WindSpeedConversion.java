/*
 * NightDream
 * Copyright (C) 2025 Stefan Fruhner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

    public static double metersPerSecondToKnot(double mps) {
        return mps * 1.9438446;
    }
}
