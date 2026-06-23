package com.sstpnk.wclock.render;

import android.graphics.RectF;

public final class OverlayLayoutEngine {
    private final BurnInController burnInController = new BurnInController(6);

    public RectF primaryPanel(int zoneIndex, int width, int height, float density) {
        int panelWidth = (int) clamp(width * 0.40f, 360.0f * density, 760.0f * density);
        int panelHeight = (int) clamp(height * 0.20f, 150.0f * density, 230.0f * density);
        BurnInController.Zone zone = burnInController.zoneForIndex(zoneIndex, width, height, panelWidth, panelHeight);
        return new RectF(zone.left, zone.top, zone.left + panelWidth, zone.top + panelHeight);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
