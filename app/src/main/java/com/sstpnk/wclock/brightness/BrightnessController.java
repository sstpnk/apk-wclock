package com.sstpnk.wclock.brightness;

import android.view.Window;
import android.view.WindowManager;

public final class BrightnessController {
    public void apply(Window window, float brightness) {
        WindowManager.LayoutParams params = window.getAttributes();
        params.screenBrightness = clamp(brightness);
        window.setAttributes(params);
    }

    private float clamp(float value) {
        return Math.max(0.02f, Math.min(1.0f, value));
    }
}
