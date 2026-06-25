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
        assertEquals("photowall", settings.photoDisplayMode);
        assertEquals("random", settings.photoOrderMode);
        assertEquals(18, settings.maxVisiblePhotos);
        assertEquals(5, settings.photoChangeSeconds);
        assertEquals("coordinates", settings.locationMode);
        assertEquals("open-meteo", settings.weatherProvider);
        assertEquals("outline", settings.weatherIconStyle);
        assertTrue(settings.burnInMinMinutes >= 5);
        assertTrue(settings.burnInMaxMinutes <= 15);
        assertTrue(settings.nightOverlayAlpha >= 0.0f);
        assertTrue(settings.nightOverlayAlpha <= 1.0f);
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
}
