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

package com.firebirdberlin.nightdream.models;

import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class TimeRange {

    public Calendar start;
    public Calendar end;


    public TimeRange(Calendar start, Calendar end){
        this.start = (Calendar) start.clone();
        this.end = (Calendar) end.clone();
        this.start = fixDate(this.start);
        this.end = fixDate(this.end);
    }

    private Calendar fixDate(Calendar cal) {
        Calendar now = Calendar.getInstance();
        // we're only interested in the time, the date shall be today.
        cal.set(Calendar.YEAR, now.get(Calendar.YEAR));
        cal.set(Calendar.MONTH, now.get(Calendar.MONTH));
        cal.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

        if ( cal.before(now) ) {
            cal.add(Calendar.DATE, 1);
        }
        return cal;
    }

    public boolean inRange() {
        Calendar now = Calendar.getInstance();
        return inRange(now);
    }

    public boolean inRange(Calendar time) {
        time = fixDate(time);

        if (start.equals(time) ) {
            return true;
        } else if (end.before(start)){
            return ( time.after(start) || time.before(end) );
        } else if (! start.equals(end)) {
            return ( time.after(start) && time.before(end) );
        }
        return true;
    }

    public Calendar getNextEvent() {

        Calendar now = Calendar.getInstance();
        if (start.after(now) && start.before(end)) {
            return start;
        }
        return end;
    }
}

