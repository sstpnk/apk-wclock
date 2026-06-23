package com.sstpnk.wclock.collage;

import android.graphics.RectF;

public final class CollageLayout {
    public RectF frameForIndex(int index, int width, int height) {
        float shortSide = Math.max(160.0f, Math.min(width, height) * 0.32f);
        float longSide = shortSide * 1.35f;
        float x = ((index * 173) % Math.max(1, width));
        float y = ((index * 97) % Math.max(1, height));
        x = Math.max(-shortSide * 0.25f, Math.min(width - shortSide * 0.75f, x - shortSide * 0.5f));
        y = Math.max(-longSide * 0.25f, Math.min(height - longSide * 0.75f, y - longSide * 0.5f));
        return new RectF(x, y, x + shortSide, y + longSide);
    }

    public float rotationForIndex(int index) {
        return ((index * 13) % 17) - 8.0f;
    }
}
