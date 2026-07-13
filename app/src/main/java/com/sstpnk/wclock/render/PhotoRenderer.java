package com.sstpnk.wclock.render;

public interface PhotoRenderer {
    void setPhotoSource(String path, String uri);

    void setDisplaySettings(boolean collageEnabled, String photoDisplayMode, String photoOrderMode, int maxVisiblePhotos, int photoChangeSeconds, int framePanSpeedPxPerSecond);

    void renderFrame();

    void recycle();
}
