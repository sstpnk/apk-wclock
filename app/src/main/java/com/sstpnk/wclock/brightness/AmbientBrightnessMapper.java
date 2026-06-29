package com.sstpnk.wclock.brightness;

public final class AmbientBrightnessMapper {
    private final float minBrightness;
    private final float maxBrightness;

    public AmbientBrightnessMapper(float firstBrightness, float secondBrightness) {
        float first = clamp(firstBrightness);
        float second = clamp(secondBrightness);
        this.minBrightness = Math.min(first, second);
        this.maxBrightness = Math.max(first, second);
    }

    public float brightnessForLux(float lux) {
        float safeLux = Math.max(0.0f, lux);
        if (safeLux <= 3.0f) {
            return minBrightness;
        }
        if (safeLux >= 1000.0f) {
            return maxBrightness;
        }
        float progress = (float) ((Math.log10(safeLux) - Math.log10(3.0)) / (Math.log10(1000.0) - Math.log10(3.0)));
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        return minBrightness + (maxBrightness - minBrightness) * progress;
    }

    public float minBrightness() {
        return minBrightness;
    }

    public float maxBrightness() {
        return maxBrightness;
    }

    public static boolean dreamScreenBrightForLux(float lux) {
        return lux >= 50.0f;
    }

    private static float clamp(float value) {
        return Math.max(0.05f, Math.min(1.0f, value));
    }
}
