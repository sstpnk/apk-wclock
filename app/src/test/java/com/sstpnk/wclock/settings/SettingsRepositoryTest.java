package com.sstpnk.wclock.settings;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SettingsRepositoryTest {
    @Test
    public void defaultsAreValidForFirstLaunch() {
        SettingsRepository.Settings settings = SettingsRepository.Settings.defaults();

        assertEquals("Москва", settings.cityName);
        assertEquals(55.7558, settings.latitude, 0.0001);
        assertEquals(37.6173, settings.longitude, 0.0001);
        assertEquals(30, settings.weatherRefreshMinutes);
        assertTrue(settings.collageEnabled);
        assertTrue(settings.showClock);
        assertTrue(settings.showWeather);
        assertTrue(settings.showForecast);
        assertEquals("photowall", settings.photoDisplayMode);
        assertEquals("random", settings.photoOrderMode);
        assertEquals(18, settings.maxVisiblePhotos);
        assertEquals(5, settings.photoChangeSeconds);
        assertEquals(20, settings.framePanSpeedPxPerSecond);
        assertEquals("coordinates", settings.locationMode);
        assertEquals("open-meteo", settings.weatherProvider);
        assertEquals("outline", settings.weatherIconStyle);
        assertTrue(settings.autoBrightnessEnabled);
        assertEquals(0.08f, settings.autoBrightnessMin, 0.001f);
        assertEquals(0.90f, settings.autoBrightnessMax, 0.001f);
        assertTrue(settings.burnInMinMinutes >= 5);
        assertEquals(0.56f, settings.clockPanelBackgroundAlpha, 0.001f);
        assertEquals(0.56f, settings.weatherPanelBackgroundAlpha, 0.001f);
    }

    @Test
    public void coordinateValidationRejectsInvalidValues() {
        assertTrue(SettingsRepository.isValidLatitude(55.75));
        assertTrue(SettingsRepository.isValidLongitude(37.61));
        assertTrue(!SettingsRepository.isValidLatitude(120.0));
        assertTrue(!SettingsRepository.isValidLongitude(220.0));
    }

    @Test
    public void photoOrderModeAcceptsOnlySequentialOtherwiseRandom() {
        SettingsRepository.Settings settings = SettingsRepository.Settings.defaults();

        settings.photoOrderMode = "sequential";
        assertEquals("sequential", settings.normalized().photoOrderMode);

        settings.photoOrderMode = "unknown";
        assertEquals("random", settings.normalized().photoOrderMode);

        settings.photoOrderMode = null;
        assertEquals("random", settings.normalized().photoOrderMode);
    }

    @Test
    public void weatherProviderGroupsFreeFallbackProvidersAsAutomatic() {
        SettingsRepository.Settings settings = SettingsRepository.Settings.defaults();

        settings.weatherProvider = "wttr-in";
        assertEquals("open-meteo", settings.normalized().weatherProvider);

        settings.weatherProvider = "met-norway";
        assertEquals("open-meteo", settings.normalized().weatherProvider);

        settings.weatherProvider = "open-meteo";
        assertEquals("open-meteo", settings.normalized().weatherProvider);
    }

    @Test
    public void framePanSpeedIsNormalized() {
        SettingsRepository.Settings settings = SettingsRepository.Settings.defaults();

        settings.framePanSpeedPxPerSecond = 2;
        assertEquals(4, settings.normalized().framePanSpeedPxPerSecond);

        settings.framePanSpeedPxPerSecond = 96;
        assertEquals(48, settings.normalized().framePanSpeedPxPerSecond);

        settings.framePanSpeedPxPerSecond = 28;
        assertEquals(28, settings.normalized().framePanSpeedPxPerSecond);
    }

    @Test
    public void autoBrightnessRangeIsNormalized() {
        SettingsRepository.Settings settings = SettingsRepository.Settings.defaults();

        settings.autoBrightnessMin = 0.95f;
        settings.autoBrightnessMax = 0.02f;

        SettingsRepository.Settings safe = settings.normalized();
        assertEquals(0.05f, safe.autoBrightnessMin, 0.001f);
        assertEquals(0.95f, safe.autoBrightnessMax, 0.001f);
    }

    @Test
    public void panelBackgroundAlphaIsNormalized() {
        SettingsRepository.Settings settings = SettingsRepository.Settings.defaults();

        settings.clockPanelBackgroundAlpha = -0.3f;
        assertEquals(0.0f, settings.normalized().clockPanelBackgroundAlpha, 0.001f);

        settings.weatherPanelBackgroundAlpha = 1.2f;
        assertEquals(0.85f, settings.normalized().weatherPanelBackgroundAlpha, 0.001f);

        settings.clockPanelBackgroundAlpha = 0.62f;
        settings.weatherPanelBackgroundAlpha = 0.64f;
        assertEquals(0.62f, settings.normalized().clockPanelBackgroundAlpha, 0.001f);
        assertEquals(0.64f, settings.normalized().weatherPanelBackgroundAlpha, 0.001f);
    }

    @Test
    public void recommendedPhotoLimitFollowsAvailableHeap() {
        assertEquals(8, SettingsRepository.recommendedMaxVisiblePhotos(48L * 1024L * 1024L));
        assertEquals(12, SettingsRepository.recommendedMaxVisiblePhotos(96L * 1024L * 1024L));
        assertEquals(18, SettingsRepository.recommendedMaxVisiblePhotos(160L * 1024L * 1024L));
        assertEquals(50, SettingsRepository.recommendedMaxVisiblePhotos(256L * 1024L * 1024L));
    }
}
