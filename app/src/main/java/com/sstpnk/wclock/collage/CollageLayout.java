package com.sstpnk.wclock.collage;

import android.graphics.RectF;

import java.util.Random;

public final class CollageLayout {
    public RectF frameForIndex(int index, int width, int height) {
        float shortSide = Math.max(160.0f, Math.min(width, height) * 0.32f);
        float longSide = shortSide * 1.35f;
        Random random = new Random(index * 1103515245L + 12345L);
        float minX = -shortSide * 0.28f;
        float maxX = width - shortSide * 0.72f;
        float minY = -longSide * 0.28f;
        float maxY = height - longSide * 0.72f;
        float x = minX + random.nextFloat() * Math.max(1.0f, maxX - minX);
        float y = minY + random.nextFloat() * Math.max(1.0f, maxY - minY);
        return new RectF(x, y, x + shortSide, y + longSide);
    }

    public float rotationForIndex(int index) {
        Random random = new Random(index * 1664525L + 1013904223L);
        return -8.0f + random.nextFloat() * 16.0f;
    }
}
