package com.sstpnk.wclock.collage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

public final class BitmapLoader {
    public Bitmap decode(File file, int maxWidth, int maxHeight) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxWidth, maxHeight);
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    int sampleSize(int sourceWidth, int sourceHeight, int maxWidth, int maxHeight) {
        int sample = 1;
        while ((sourceWidth / sample) > maxWidth * 2 || (sourceHeight / sample) > maxHeight * 2) {
            sample *= 2;
        }
        return sample;
    }
}
