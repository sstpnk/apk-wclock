package com.sstpnk.wclock.render;

import com.sstpnk.wclock.weather.ForecastDay;
import com.sstpnk.wclock.weather.WeatherData;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class RenderControllerTest {
    @Test
    public void frameDelayTargetsAtLeastThirtyFramesPerSecond() {
        assertEquals(33L, RenderController.FRAME_DELAY_MS);
    }

    @Test
    public void weatherStatusCompletionIsDelayedLongEnoughToBeVisible() {
        assertEquals(1500L, RenderController.weatherCompletionDelayMillis(1000L, 1000L));
        assertEquals(500L, RenderController.weatherCompletionDelayMillis(1000L, 2000L));
        assertEquals(0L, RenderController.weatherCompletionDelayMillis(1000L, 2600L));
    }

    @Test
    public void staleWeatherDataKeepsVisibleErrorStatus() {
        WeatherData fresh = weather(false);
        WeatherData stale = weather(true);

        assertEquals("", RenderController.weatherStatusAfterRefresh(fresh, "Weather failed"));
        assertEquals("Weather failed", RenderController.weatherStatusAfterRefresh(stale, "Weather failed"));
        assertEquals("Weather failed", RenderController.weatherStatusAfterRefresh(null, "Weather failed"));
    }

    private WeatherData weather(boolean stale) {
        return new WeatherData("test", "Moscow", 1L, stale, 1.0, 1.0, 0, "Clear", 0.0, 0.0, 0, Collections.<ForecastDay>emptyList());
    }
}
