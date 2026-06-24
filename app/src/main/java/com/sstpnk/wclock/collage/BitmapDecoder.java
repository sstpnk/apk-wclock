package com.sstpnk.wclock.collage;

import android.content.ContentResolver;
import android.graphics.Bitmap;

public interface BitmapDecoder {
    Bitmap decode(PhotoItem item, ContentResolver resolver, int maxWidth, int maxHeight);
}
