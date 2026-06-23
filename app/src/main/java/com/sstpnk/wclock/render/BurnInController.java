package com.sstpnk.wclock.render;

public final class BurnInController {
    private final int zoneCount;

    public BurnInController(int zoneCount) {
        this.zoneCount = Math.max(4, zoneCount);
    }

    public Zone zoneForIndex(int index, int screenWidth, int screenHeight, int panelWidth, int panelHeight) {
        int safeWidth = Math.max(1, screenWidth - panelWidth);
        int safeHeight = Math.max(1, screenHeight - panelHeight);
        int normalized = Math.abs(index % zoneCount);
        float xRatio;
        float yRatio;
        switch (normalized % 6) {
            case 0:
                xRatio = 0.04f;
                yRatio = 0.04f;
                break;
            case 1:
                xRatio = 0.54f;
                yRatio = 0.06f;
                break;
            case 2:
                xRatio = 0.08f;
                yRatio = 0.58f;
                break;
            case 3:
                xRatio = 0.50f;
                yRatio = 0.56f;
                break;
            case 4:
                xRatio = 0.28f;
                yRatio = 0.28f;
                break;
            default:
                xRatio = 0.64f;
                yRatio = 0.34f;
                break;
        }
        return new Zone((int) (safeWidth * xRatio), (int) (safeHeight * yRatio));
    }

    public static final class Zone {
        public final int left;
        public final int top;

        public Zone(int left, int top) {
            this.left = left;
            this.top = top;
        }
    }
}
