package com.sstpnk.wclock.collage;

import android.net.Uri;

import java.io.File;

public final class PhotoItem {
    public final File file;
    public final Uri uri;
    public final String name;
    public final long modifiedAt;

    public PhotoItem(File file, long modifiedAt) {
        this.file = file;
        this.uri = null;
        this.name = file == null ? "" : file.getName();
        this.modifiedAt = modifiedAt;
    }

    public PhotoItem(Uri uri, String name, long modifiedAt) {
        this.file = null;
        this.uri = uri;
        this.name = name == null ? "" : name;
        this.modifiedAt = modifiedAt;
    }
}
