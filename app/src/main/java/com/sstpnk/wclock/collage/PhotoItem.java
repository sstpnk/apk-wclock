package com.sstpnk.wclock.collage;

import java.io.File;

public final class PhotoItem {
    public final File file;
    public final long modifiedAt;

    public PhotoItem(File file, long modifiedAt) {
        this.file = file;
        this.modifiedAt = modifiedAt;
    }
}
