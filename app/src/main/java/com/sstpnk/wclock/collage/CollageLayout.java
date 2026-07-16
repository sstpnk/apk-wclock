package com.sstpnk.wclock.collage;

import android.graphics.RectF;

import java.util.Random;

public final class CollageLayout {
    private final Random random = new Random();

    public RectF frameForIndex(int index, int width, int height, int bitmapWidth, int bitmapHeight) {
        random.setSeed(index * 1103515245L + 12345L);
        float frameWidth = Math.max(1.0f, bitmapWidth);
        float frameHeight = Math.max(1.0f, bitmapHeight);
        float maxArea = width * height * 0.25f;
        float area = frameWidth * frameHeight;
        if (area > maxArea) {
            float scale = (float) Math.sqrt(maxArea / area);
            frameWidth *= scale;
            frameHeight *= scale;
        }
        float minX = -frameWidth * 0.10f;
        float maxX = width - frameWidth * 0.90f;
        float minY = -frameHeight * 0.10f;
        float maxY = height - frameHeight * 0.90f;
        float x = minX + random.nextFloat() * Math.max(1.0f, maxX - minX);
        float y = minY + random.nextFloat() * Math.max(1.0f, maxY - minY);
        return new RectF(x, y, x + frameWidth, y + frameHeight);
    }

    public float rotationForIndex(int index) {
        random.setSeed(index * 1664525L + 1013904223L);
        return -8.0f + random.nextFloat() * 16.0f;
    }
}
