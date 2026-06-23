package com.sstpnk.wclock.render;

import android.graphics.RectF;

public final class OverlayLayoutEngine {
    private final BurnInController burnInController = new BurnInController(6);

    public RectF primaryPanel(int zoneIndex, int width, int height) {
        int panelWidth = Math.max(360, (int) (width * 0.42f));
        int panelHeight = Math.max(180, (int) (height * 0.22f));
        BurnInController.Zone zone = burnInController.zoneForIndex(zoneIndex, width, height, panelWidth, panelHeight);
        return new RectF(zone.left, zone.top, zone.left + panelWidth, zone.top + panelHeight);
    }
}
