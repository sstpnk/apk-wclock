package com.sstpnk.wclock.render;

import androidx.test.core.app.ApplicationProvider;

import com.sstpnk.wclock.settings.SettingsRepository;
import com.sstpnk.wclock.weather.ForecastDay;
import com.sstpnk.wclock.weather.WeatherData;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
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

    @Test
    public void frameUpdatesUseCachedSettingsUntilExplicitRefresh() {
        CountingSettingsSource settingsSource = new CountingSettingsSource(SettingsRepository.Settings.defaults());
        RenderController controller = new RenderController(
                new ClockWeatherCollageView(ApplicationProvider.getApplicationContext()),
                settingsSource,
                null);

        controller.refreshSettings();
        controller.updateViewStateForTest(1000L, false);
        controller.updateViewStateForTest(1033L, false);
        assertEquals("Frame updates should reuse the cached settings snapshot", 1, settingsSource.loadCount);

        settingsSource.settings = SettingsRepository.Settings.defaults();
        settingsSource.settings.showSeconds = true;
        controller.updateViewStateForTest(1066L, false);
        assertEquals("Settings changes should not be polled on every frame", 1, settingsSource.loadCount);

        controller.refreshSettings();
        assertEquals("Explicit refresh should load settings once", 2, settingsSource.loadCount);
    }

    private WeatherData weather(boolean stale) {
        return new WeatherData("test", "Moscow", 1L, stale, 1.0, 1.0, 0, "Clear", 0.0, 0.0, 0, Collections.<ForecastDay>emptyList());
    }

    private static final class CountingSettingsSource implements RenderController.SettingsSource {
        SettingsRepository.Settings settings;
        int loadCount;

        CountingSettingsSource(SettingsRepository.Settings settings) {
            this.settings = settings;
        }

        @Override
        public SettingsRepository.Settings load() {
            loadCount++;
            return settings;
        }
    }
}
