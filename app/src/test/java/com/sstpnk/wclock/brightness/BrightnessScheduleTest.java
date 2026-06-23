package com.sstpnk.wclock.brightness;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BrightnessScheduleTest {
    @Test
    public void choosesNightBrightnessDuringNightHours() {
        BrightnessSchedule schedule = new BrightnessSchedule(7, 19, 23, 0.8f, 0.45f, 0.12f);
        assertEquals(0.12f, schedule.brightnessForHour(2), 0.001f);
        assertEquals(0.8f, schedule.brightnessForHour(12), 0.001f);
        assertEquals(0.45f, schedule.brightnessForHour(20), 0.001f);
    }
}
