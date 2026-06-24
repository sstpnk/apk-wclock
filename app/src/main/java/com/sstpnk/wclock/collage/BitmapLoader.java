package com.sstpnk.wclock.collage;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public final class BitmapLoader implements BitmapDecoder {
    @Override
    public Bitmap decode(PhotoItem item, ContentResolver resolver, int maxWidth, int maxHeight) {
        if (item == null) {
            return null;
        }
        if (item.uri != null) {
            return decodeUri(item, resolver, maxWidth, maxHeight);
        }
        return decodeFile(item.file, maxWidth, maxHeight);
    }

    private Bitmap decodeFile(File file, int maxWidth, int maxHeight) {
        if (file == null) {
            return null;
        }
        InputStream boundsStream = null;
        InputStream bitmapStream = null;
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            boundsStream = new FileInputStream(file);
            BitmapFactory.decodeStream(boundsStream, null, bounds);
            closeQuietly(boundsStream);
            boundsStream = null;
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxWidth, maxHeight);
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            bitmapStream = new FileInputStream(file);
            return BitmapFactory.decodeStream(bitmapStream, null, options);
        } catch (Exception e) {
            return null;
        } finally {
            closeQuietly(boundsStream);
            closeQuietly(bitmapStream);
        }
    }

    private Bitmap decodeUri(PhotoItem item, ContentResolver resolver, int maxWidth, int maxHeight) {
        InputStream boundsStream = null;
        InputStream bitmapStream = null;
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            boundsStream = resolver.openInputStream(item.uri);
            BitmapFactory.decodeStream(boundsStream, null, bounds);
            closeQuietly(boundsStream);
            boundsStream = null;
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxWidth, maxHeight);
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            bitmapStream = resolver.openInputStream(item.uri);
            return BitmapFactory.decodeStream(bitmapStream, null, options);
        } catch (Exception e) {
            return null;
        } finally {
            closeQuietly(boundsStream);
            closeQuietly(bitmapStream);
        }
    }

    private void closeQuietly(InputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (Exception ignored) {
        }
    }

    int sampleSize(int sourceWidth, int sourceHeight, int maxWidth, int maxHeight) {
        int sample = 1;
        while ((sourceWidth / sample) > maxWidth * 2 || (sourceHeight / sample) > maxHeight * 2) {
            sample *= 2;
        }
        return sample;
    }
}
