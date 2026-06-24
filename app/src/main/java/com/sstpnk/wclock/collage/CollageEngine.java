package com.sstpnk.wclock.collage;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class CollageEngine {
    public static final String MODE_FRAME = "frame";
    public static final String MODE_PHOTOWALL = "photowall";

    private final PhotoScanner scanner = new PhotoScanner();
    private final BitmapDecoder loader;
    private final CollageLayout layout = new CollageLayout();
    private final ContentResolver resolver;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final List<PhotoItem> photos = new ArrayList<PhotoItem>();
    private final List<ActivePhoto> activePhotos = new ArrayList<ActivePhoto>();
    private String loadedPath = "";
    private String loadedUri = "";
    private boolean sourceLoaded;
    private int nextPhotoIndex;
    private long lastAddMillis;

    public CollageEngine(ContentResolver resolver) {
        this(resolver, new BitmapLoader());
    }

    CollageEngine(ContentResolver resolver, BitmapDecoder loader) {
        this.resolver = resolver;
        this.loader = loader;
    }

    public void setSource(String path, String uriString) {
        String safePath = path == null ? "" : path;
        String safeUri = uriString == null ? "" : uriString;
        if (safePath.equals(loadedPath) && safeUri.equals(loadedUri) && sourceLoaded) {
            return;
        }
        recycle();
        loadedPath = safePath;
        loadedUri = safeUri;
        if (safeUri.length() > 0) {
            photos.addAll(scanner.scanTree(resolver, Uri.parse(safeUri)));
        } else if (safePath.length() > 0) {
            photos.addAll(scanner.scan(new File(safePath)));
        }
        sourceLoaded = true;
    }

    public void draw(Canvas canvas, long nowMillis, boolean enabled, String mode, int maxVisible, int changeSeconds) {
        canvas.drawColor(Color.rgb(12, 14, 16));
        if (!enabled || photos.size() == 0) {
            return;
        }
        if (MODE_FRAME.equals(mode)) {
            drawFrameMode(canvas, nowMillis, changeSeconds);
            return;
        }
        drawPhotoWall(canvas, nowMillis, maxVisible, changeSeconds);
    }

    public void recycle() {
        for (ActivePhoto photo : activePhotos) {
            if (photo.bitmap != null && !photo.bitmap.isRecycled()) {
                photo.bitmap.recycle();
            }
        }
        activePhotos.clear();
        photos.clear();
        loadedPath = "";
        loadedUri = "";
        sourceLoaded = false;
        nextPhotoIndex = 0;
        lastAddMillis = 0;
    }

    int photoCountForTest() {
        return photos.size();
    }

    int activePhotoCountForTest() {
        return activePhotos.size();
    }

    private void drawPhotoWall(Canvas canvas, long nowMillis, int maxVisible, int changeSeconds) {
        int safeMax = Math.max(1, Math.min(50, maxVisible));
        int safeIntervalMs = Math.max(1, changeSeconds) * 1000;
        addNextIfNeeded(canvas, nowMillis, safeMax, safeIntervalMs);
        removeExpired(nowMillis, safeMax, safeIntervalMs);
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        for (int i = 0; i < activePhotos.size(); i++) {
            ActivePhoto photo = activePhotos.get(i);
            RectF frame = layout.frameForIndex(photo.layoutIndex, width, height, photo.bitmap.getWidth(), photo.bitmap.getHeight());
            applyEntrance(frame, photo, nowMillis, width);
            float drift = (float) Math.sin((nowMillis / 120000.0) + i) * 12.0f;
            frame.offset(drift, -drift * 0.5f);
            int alpha = alphaFor(photo, nowMillis, safeMax, safeIntervalMs);
            float border = Math.max(3.0f, Math.min(width, height) * 0.006f);
            paint.setColor(0x70000000);
            paint.setAlpha(alpha);
            canvas.drawRect(frame.left + 8, frame.top + 8, frame.right + 8, frame.bottom + 8, paint);
            paint.setColor(0xF0F4F1EA);
            paint.setAlpha(alpha);
            canvas.drawRect(frame.left - border, frame.top - border, frame.right + border, frame.bottom + border, paint);
            paint.setColor(Color.WHITE);
            paint.setAlpha(alpha);
            canvas.drawBitmap(photo.bitmap, null, frame, paint);
            int ageShade = Math.min(105, (int) ((nowMillis - photo.bornMillis) / Math.max(1, safeIntervalMs)) * 8);
            paint.setColor(Color.BLACK);
            paint.setAlpha(ageShade);
            canvas.drawRect(frame, paint);
            paint.setAlpha(255);
        }
    }

    private void drawFrameMode(Canvas canvas, long nowMillis, int changeSeconds) {
        int safeIntervalMs = Math.max(1, changeSeconds) * 1000;
        if (activePhotos.size() == 0 || nowMillis - lastAddMillis >= safeIntervalMs) {
            addFramePhoto(canvas, nowMillis);
        }
        removeFrameExtras();
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        long transitionAge = activePhotos.size() > 1
                ? nowMillis - activePhotos.get(activePhotos.size() - 1).bornMillis
                : Long.MAX_VALUE;
        for (int i = 0; i < activePhotos.size(); i++) {
            ActivePhoto photo = activePhotos.get(i);
            RectF frame = frameRectForBitmap(photo.bitmap, width, height, nowMillis, photo.layoutIndex);
            paint.setAlpha(activePhotos.size() == 1 ? 255 : alphaForFrameTransition(transitionAge, i == activePhotos.size() - 1));
            canvas.drawBitmap(photo.bitmap, null, frame, paint);
        }
        paint.setAlpha(255);
        removeFadedFrameExtras(transitionAge);
    }

    private void applyEntrance(RectF frame, ActivePhoto photo, long nowMillis, int screenWidth) {
        long age = nowMillis - photo.bornMillis;
        long duration = 1400L;
        if (age >= duration) {
            return;
        }
        float progress = Math.max(0.0f, Math.min(1.0f, age / (float) duration));
        progress = 1.0f - (1.0f - progress) * (1.0f - progress);
        boolean fromLeft = photo.layoutIndex % 2 == 0;
        float startLeft = fromLeft ? -frame.width() - 20.0f : screenWidth + 20.0f;
        float dx = (startLeft - frame.left) * (1.0f - progress);
        frame.offset(dx, 0.0f);
    }

    private void addNextIfNeeded(Canvas canvas, long nowMillis, int maxVisible, int intervalMs) {
        if (activePhotos.size() >= maxVisible) {
            return;
        }
        if (lastAddMillis != 0 && nowMillis - lastAddMillis < intervalMs) {
            return;
        }
        ActivePhoto photo = decodeNext(canvas, nowMillis);
        if (photo != null) {
            activePhotos.add(photo);
            lastAddMillis = nowMillis;
        }
    }

    private void addFramePhoto(Canvas canvas, long nowMillis) {
        ActivePhoto photo = decodeNext(canvas, nowMillis);
        if (photo != null) {
            activePhotos.add(photo);
            lastAddMillis = nowMillis;
        }
    }

    private ActivePhoto decodeNext(Canvas canvas, long nowMillis) {
        for (int attempts = 0; attempts < photos.size(); attempts++) {
            PhotoItem item = photos.get(nextPhotoIndex % photos.size());
            int layoutIndex = nextPhotoIndex;
            nextPhotoIndex++;
            Bitmap bitmap = loader.decode(item, resolver, Math.max(320, canvas.getWidth()), Math.max(320, canvas.getHeight()));
            if (bitmap != null) {
                return new ActivePhoto(bitmap, nowMillis, layoutIndex);
            }
        }
        return null;
    }

    private void removeExpired(long nowMillis, int maxVisible, int intervalMs) {
        long maxAge = Math.max(intervalMs * 4L, intervalMs * (long) (maxVisible + 2));
        Iterator<ActivePhoto> iterator = activePhotos.iterator();
        while (iterator.hasNext()) {
            ActivePhoto photo = iterator.next();
            if (nowMillis - photo.bornMillis > maxAge) {
                recycle(photo);
                iterator.remove();
            }
        }
    }

    private void removeFrameExtras() {
        while (activePhotos.size() > 2) {
            ActivePhoto first = activePhotos.remove(0);
            recycle(first);
        }
    }

    private void removeFadedFrameExtras(long transitionAge) {
        if (activePhotos.size() > 1 && transitionAge > 1400L) {
            ActivePhoto first = activePhotos.remove(0);
            recycle(first);
        }
    }

    private void recycle(ActivePhoto photo) {
        if (photo.bitmap != null && !photo.bitmap.isRecycled()) {
            photo.bitmap.recycle();
        }
    }

    private int alphaFor(ActivePhoto photo, long nowMillis, int maxVisible, int intervalMs) {
        long age = nowMillis - photo.bornMillis;
        long fadeIn = 900L;
        long maxAge = Math.max(intervalMs * 4L, intervalMs * (long) (maxVisible + 2));
        if (age < fadeIn) {
            return (int) Math.max(220, 255 * age / fadeIn);
        }
        long fadeStart = (long) (maxAge * 0.78f);
        if (age > fadeStart) {
            long fadeSpan = Math.max(1, maxAge - fadeStart);
            return (int) Math.max(0, 255 - 255 * (age - fadeStart) / fadeSpan);
        }
        return 255;
    }

    private int alphaForFrameTransition(long transitionAge, boolean newest) {
        long fade = 1400L;
        long age = Math.max(0L, Math.min(fade, transitionAge));
        if (newest) {
            return (int) Math.min(255, 255 * age / fade);
        }
        return (int) Math.max(0, 255 - 255 * age / fade);
    }

    private RectF frameRectForBitmap(Bitmap bitmap, int width, int height, long nowMillis, int index) {
        float scale = Math.max(width / (float) bitmap.getWidth(), height / (float) bitmap.getHeight()) * 1.08f;
        float drawWidth = bitmap.getWidth() * scale;
        float drawHeight = bitmap.getHeight() * scale;
        float maxX = Math.max(0.0f, drawWidth - width);
        float maxY = Math.max(0.0f, drawHeight - height);
        float progress = ((nowMillis / 18000.0f) + (index * 0.37f)) % 1.0f;
        float pingPong = progress < 0.5f ? progress * 2.0f : (1.0f - progress) * 2.0f;
        float left = maxX > maxY ? -maxX * pingPong : -maxX * 0.5f;
        float top = maxY >= maxX ? -maxY * pingPong : -maxY * 0.5f;
        return new RectF(left, top, left + drawWidth, top + drawHeight);
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
