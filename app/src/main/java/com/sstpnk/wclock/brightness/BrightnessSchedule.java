package com.sstpnk.wclock.brightness;

public final class BrightnessSchedule {
    private final int dayStartHour;
    private final int eveningStartHour;
    private final int nightStartHour;
    private final float dayBrightness;
    private final float eveningBrightness;
    private final float nightBrightness;

    public BrightnessSchedule(int dayStartHour, int eveningStartHour, int nightStartHour, float dayBrightness, float eveningBrightness, float nightBrightness) {
        this.dayStartHour = dayStartHour;
        this.eveningStartHour = eveningStartHour;
        this.nightStartHour = nightStartHour;
        this.dayBrightness = dayBrightness;
        this.eveningBrightness = eveningBrightness;
        this.nightBrightness = nightBrightness;
    }

    public float brightnessForHour(int hour) {
        if (hour >= nightStartHour || hour < dayStartHour) {
            return nightBrightness;
        }
        if (hour >= eveningStartHour) {
            return eveningBrightness;
        }
        return dayBrightness;
    }
}
