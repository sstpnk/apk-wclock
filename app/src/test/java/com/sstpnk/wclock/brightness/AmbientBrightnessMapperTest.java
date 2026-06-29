package com.sstpnk.wclock.brightness;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AmbientBrightnessMapperTest {
    @Test
    public void mapsLuxToConfiguredBrightnessRange() {
        AmbientBrightnessMapper mapper = new AmbientBrightnessMapper(0.10f, 0.80f);

        assertEquals(0.10f, mapper.brightnessForLux(0.0f), 0.001f);
        assertEquals(0.10f, mapper.brightnessForLux(3.0f), 0.001f);
        assertEquals(0.45f, mapper.brightnessForLux(80.0f), 0.05f);
        assertEquals(0.80f, mapper.brightnessForLux(1500.0f), 0.001f);
    }

    @Test
    public void normalizesInvalidRange() {
        AmbientBrightnessMapper mapper = new AmbientBrightnessMapper(0.95f, 0.20f);

        assertEquals(0.20f, mapper.minBrightness(), 0.001f);
        assertEquals(0.95f, mapper.maxBrightness(), 0.001f);
    }

    @Test
    public void dreamScreenBrightUsesLuxThreshold() {
        assertEquals(false, AmbientBrightnessMapper.dreamScreenBrightForLux(3.0f));
        assertEquals(false, AmbientBrightnessMapper.dreamScreenBrightForLux(20.0f));
        assertEquals(true, AmbientBrightnessMapper.dreamScreenBrightForLux(80.0f));
    }
}
