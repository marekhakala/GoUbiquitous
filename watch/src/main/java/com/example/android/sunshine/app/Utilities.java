package com.example.android.sunshine.app;

import android.content.res.Resources;

import java.util.Calendar;

public class Utilities {

    public static int getIconResourceForWeatherCondition(int id) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (id >= 200 && id <= 232) {
            return R.drawable.ic_storm;
        } else if (id >= 300 && id <= 321) {
            return R.drawable.ic_light_rain;
        } else if (id >= 500 && id <= 504) {
            return R.drawable.ic_rain;
        } else if (id == 511) {
            return R.drawable.ic_snow;
        } else if (id >= 520 && id <= 531) {
            return R.drawable.ic_rain;
        } else if (id >= 600 && id <= 622) {
            return R.drawable.ic_snow;
        } else if (id >= 701 && id <= 761) {
            return R.drawable.ic_fog;
        } else if (id == 761 || id == 781) {
            return R.drawable.ic_storm;
        } else if (id == 800) {
            return R.drawable.ic_clear;
        } else if (id == 801) {
            return R.drawable.ic_light_clouds;
        } else if (id >= 802 && id <= 804) {
            return R.drawable.ic_cloudy;
        }
        return -1;
    }

    public static String getAmPm(Resources resources, int amPm) {
        if(amPm == Calendar.AM)
            return resources.getString(R.string.am_text);
        else
            return resources.getString(R.string.pm_text);
    }

    public static String getMonthOfYear(Resources resources, int monthOfYear) {
        switch(monthOfYear) {
            case Calendar.JANUARY:
                return resources.getString(R.string.january);
            case Calendar.FEBRUARY:
                return resources.getString(R.string.february);
            case Calendar.MARCH:
                return resources.getString(R.string.march);
            case Calendar.APRIL:
                return resources.getString(R.string.april);
            case Calendar.MAY:
                return resources.getString(R.string.may);
            case Calendar.JUNE:
                return resources.getString(R.string.june);
            case Calendar.JULY:
                return resources.getString(R.string.july);
            case Calendar.AUGUST:
                return resources.getString(R.string.august);
            case Calendar.SEPTEMBER:
                return resources.getString(R.string.september);
            case Calendar.OCTOBER:
                return resources.getString(R.string.october);
            case Calendar.NOVEMBER:
                return resources.getString(R.string.november);
            case Calendar.DECEMBER:
                return resources.getString(R.string.december);
            default:
                return "";
        }
    }

    public static String getDayOfWeek(Resources resources, int day) {
        switch (day) {
            case Calendar.SUNDAY:
                return resources.getString(R.string.sunday);
            case Calendar.MONDAY:
                return resources.getString(R.string.monday);
            case Calendar.TUESDAY:
                return resources.getString(R.string.tuesday);
            case Calendar.WEDNESDAY:
                return resources.getString(R.string.wednesday);
            case Calendar.THURSDAY:
                return resources.getString(R.string.thursday);
            case Calendar.FRIDAY:
                return resources.getString(R.string.friday);
            case Calendar.SATURDAY:
                return resources.getString(R.string.saturday);
            default:
                return "";
        }
    }
}