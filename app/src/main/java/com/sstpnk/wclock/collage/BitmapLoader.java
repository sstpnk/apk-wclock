package com.sstpnk.wclock.collage;

import android.content.ContentResolver;
import android.os.Build;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

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
            Bitmap bitmap = BitmapFactory.decodeStream(bitmapStream, null, options);
            return rotateForOrientation(bitmap, readFileOrientation(file));
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
            Bitmap bitmap = BitmapFactory.decodeStream(bitmapStream, null, options);
            return rotateForOrientation(bitmap, readUriOrientation(item, resolver));
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

    private int readFileOrientation(File file) {
        try {
            return new ExifInterface(file.getAbsolutePath()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (Exception e) {
            return ExifInterface.ORIENTATION_NORMAL;
        }
    }

    private int readUriOrientation(PhotoItem item, ContentResolver resolver) {
        if (Build.VERSION.SDK_INT < 24 || item == null || item.uri == null) {
            return ExifInterface.ORIENTATION_NORMAL;
        }
        InputStream stream = null;
        try {
            stream = resolver.openInputStream(item.uri);
            return new ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (Exception e) {
            return ExifInterface.ORIENTATION_NORMAL;
        } finally {
            closeQuietly(stream);
        }
    }

    Bitmap rotateForOrientation(Bitmap bitmap, int orientation) {
        if (bitmap == null || orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
            matrix.postRotate(90.0f);
        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
            matrix.postRotate(180.0f);
        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            matrix.postRotate(270.0f);
        } else {
            return bitmap;
        }
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (rotated != bitmap) {
            bitmap.recycle();
        }
        return rotated;
    }

    int sampleSize(int sourceWidth, int sourceHeight, int maxWidth, int maxHeight) {
        int sample = 1;
        while ((sourceWidth / sample) > maxWidth * 2 || (sourceHeight / sample) > maxHeight * 2) {
            sample *= 2;
        }
        return sample;
    }
}
