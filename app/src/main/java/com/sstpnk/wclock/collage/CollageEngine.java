package com.sstpnk.wclock.collage;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class CollageEngine {
    private final PhotoScanner scanner = new PhotoScanner();
    private final BitmapLoader loader = new BitmapLoader();
    private final CollageLayout layout = new CollageLayout();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final List<PhotoItem> photos = new ArrayList<PhotoItem>();
    private final List<ActivePhoto> activePhotos = new ArrayList<ActivePhoto>();
    private String loadedPath = "";
    private int nextPhotoIndex;
    private long lastAddMillis;

    public void setFolder(String path) {
        if (path == null || path.equals(loadedPath)) {
            return;
        }
        recycle();
        loadedPath = path;
        photos.addAll(scanner.scan(new File(path)));
    }

    public void draw(Canvas canvas, long nowMillis, int maxVisible, int changeSeconds) {
        canvas.drawColor(Color.rgb(12, 14, 16));
        int safeMax = Math.max(1, Math.min(50, maxVisible));
        int safeIntervalMs = Math.max(5, changeSeconds) * 1000;
        addNextIfNeeded(canvas, nowMillis, safeMax, safeIntervalMs);
        removeExpired(nowMillis, safeMax, safeIntervalMs);
        if (activePhotos.size() == 0) {
            drawPlaceholder(canvas);
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        for (int i = 0; i < activePhotos.size(); i++) {
            ActivePhoto photo = activePhotos.get(i);
            RectF frame = layout.frameForIndex(photo.layoutIndex, width, height);
            float drift = (float) Math.sin((nowMillis / 120000.0) + i) * 12.0f;
            frame.offset(drift, -drift * 0.5f);
            int alpha = alphaFor(photo, nowMillis, safeMax, safeIntervalMs);
            canvas.save();
            canvas.rotate(layout.rotationForIndex(photo.layoutIndex), frame.centerX(), frame.centerY());
            paint.setAlpha(alpha);
            paint.setColor(0x99000000);
            canvas.drawRect(frame.left + 8, frame.top + 8, frame.right + 8, frame.bottom + 8, paint);
            paint.setAlpha(alpha);
            canvas.drawBitmap(photo.bitmap, null, frame, paint);
            int ageShade = Math.min(120, (int) ((nowMillis - photo.bornMillis) / Math.max(1, safeIntervalMs)) * 10);
            paint.setAlpha(ageShade);
            paint.setColor(Color.BLACK);
            canvas.drawRect(frame, paint);
            paint.setAlpha(255);
            canvas.restore();
        }
    }

    public void recycle() {
        for (ActivePhoto photo : activePhotos) {
            if (photo.bitmap != null && !photo.bitmap.isRecycled()) {
                photo.bitmap.recycle();
            }
        }
        activePhotos.clear();
        photos.clear();
        nextPhotoIndex = 0;
        lastAddMillis = 0;
    }

    private void addNextIfNeeded(Canvas canvas, long nowMillis, int maxVisible, int intervalMs) {
        if (photos.size() == 0 || activePhotos.size() >= maxVisible) {
            return;
        }
        if (lastAddMillis != 0 && nowMillis - lastAddMillis < intervalMs) {
            return;
        }
        for (int attempts = 0; attempts < photos.size(); attempts++) {
            PhotoItem item = photos.get(nextPhotoIndex % photos.size());
            int layoutIndex = nextPhotoIndex;
            nextPhotoIndex++;
            Bitmap bitmap = loader.decode(item.file, Math.max(320, canvas.getWidth() / 2), Math.max(320, canvas.getHeight() / 2));
            if (bitmap != null) {
                activePhotos.add(new ActivePhoto(bitmap, nowMillis, layoutIndex));
                lastAddMillis = nowMillis;
                return;
            }
        }
    }

    private void removeExpired(long nowMillis, int maxVisible, int intervalMs) {
        long maxAge = Math.max(intervalMs * 4L, intervalMs * (long) (maxVisible + 2));
        Iterator<ActivePhoto> iterator = activePhotos.iterator();
        while (iterator.hasNext()) {
            ActivePhoto photo = iterator.next();
            if (nowMillis - photo.bornMillis > maxAge) {
                if (photo.bitmap != null && !photo.bitmap.isRecycled()) {
                    photo.bitmap.recycle();
                }
                iterator.remove();
            }
        }
    }

    private int alphaFor(ActivePhoto photo, long nowMillis, int maxVisible, int intervalMs) {
        long age = nowMillis - photo.bornMillis;
        long fadeIn = 2500L;
        long maxAge = Math.max(intervalMs * 4L, intervalMs * (long) (maxVisible + 2));
        if (age < fadeIn) {
            return (int) Math.max(30, 255 * age / fadeIn);
        }
        long fadeStart = (long) (maxAge * 0.78f);
        if (age > fadeStart) {
            long fadeSpan = Math.max(1, maxAge - fadeStart);
            return (int) Math.max(0, 255 - 255 * (age - fadeStart) / fadeSpan);
        }
        return 255;
    }

    private void drawPlaceholder(Canvas canvas) {
        paint.setColor(Color.rgb(48, 72, 84));
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        for (int i = 0; i < 7; i++) {
            RectF frame = layout.frameForIndex(i, width, height);
            canvas.save();
            canvas.rotate(layout.rotationForIndex(i), frame.centerX(), frame.centerY());
            canvas.drawRect(frame, paint);
            canvas.restore();
        }
    }

    private static final class ActivePhoto {
        final Bitmap bitmap;
        final long bornMillis;
        final int layoutIndex;

        ActivePhoto(Bitmap bitmap, long bornMillis, int layoutIndex) {
            this.bitmap = bitmap;
            this.bornMillis = bornMillis;
            this.layoutIndex = layoutIndex;
        }
    }
}
