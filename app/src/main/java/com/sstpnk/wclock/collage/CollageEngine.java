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
import java.util.Random;

public final class CollageEngine {
    public static final String MODE_FRAME = "frame";
    public static final String MODE_PHOTOWALL = "photowall";
    public static final String ORDER_RANDOM = "random";
    public static final String ORDER_SEQUENTIAL = "sequential";

    private final PhotoScanner scanner = new PhotoScanner();
    private final BitmapDecoder loader;
    private final CollageLayout layout = new CollageLayout();
    private final ContentResolver resolver;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Random random = new Random();
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
        draw(canvas, nowMillis, enabled, mode, ORDER_RANDOM, maxVisible, changeSeconds);
    }

    public void draw(Canvas canvas, long nowMillis, boolean enabled, String mode, String orderMode, int maxVisible, int changeSeconds) {
        canvas.drawColor(Color.rgb(12, 14, 16));
        if (!enabled || photos.size() == 0) {
            return;
        }
        if (MODE_FRAME.equals(mode)) {
            drawFrameMode(canvas, nowMillis, orderMode, changeSeconds);
            return;
        }
        drawPhotoWall(canvas, nowMillis, orderMode, maxVisible, changeSeconds);
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

    private void drawPhotoWall(Canvas canvas, long nowMillis, String orderMode, int maxVisible, int changeSeconds) {
        int safeMax = maxVisibleForMemory(Math.max(1, Math.min(50, maxVisible)), Runtime.getRuntime().maxMemory());
        int safeIntervalMs = Math.max(1, changeSeconds) * 1000;
        addNextIfNeeded(canvas, nowMillis, orderMode, safeMax, safeIntervalMs);
        removeExpired(nowMillis, safeMax, safeIntervalMs);
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        for (int i = 0; i < activePhotos.size(); i++) {
            ActivePhoto photo = activePhotos.get(i);
            RectF frame = layout.frameForIndex(photo.layoutIndex, width, height, photo.bitmap.getWidth(), photo.bitmap.getHeight());
            applyEntrance(frame, photo, nowMillis, width);
            int alpha = alphaFor(photo, nowMillis, safeMax, safeIntervalMs);
            float border = Math.max(3.0f, Math.min(width, height) * 0.006f);
            canvas.save();
            canvas.rotate(rotationForPhoto(photo, nowMillis), frame.centerX(), frame.centerY());
            paint.setColor(0x70000000);
            paint.setAlpha(alpha);
            canvas.drawRect(frame.left + 8, frame.top + 8, frame.right + 8, frame.bottom + 8, paint);
            paint.setColor(0xFFF4F1EA);
            paint.setAlpha(alpha);
            canvas.drawRect(frame.left - border, frame.top - border, frame.right + border, frame.bottom + border, paint);
            paint.setColor(Color.WHITE);
            paint.setAlpha(alpha);
            canvas.drawBitmap(photo.bitmap, null, frame, paint);
            paint.setAlpha(255);
            canvas.restore();
        }
    }

    private void drawFrameMode(Canvas canvas, long nowMillis, String orderMode, int changeSeconds) {
        int safeIntervalMs = Math.max(1, changeSeconds) * 1000;
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        if (activePhotos.size() == 0) {
            addFramePhoto(canvas, nowMillis, orderMode);
        } else if (activePhotos.size() == 1) {
            ActivePhoto current = activePhotos.get(0);
            long displayDuration = frameDisplayDurationMillis(current.bitmap, width, height, safeIntervalMs);
            if (nowMillis - current.bornMillis >= displayDuration) {
                addFramePhoto(canvas, nowMillis, orderMode);
            }
        }
        removeFrameExtras();
        long transitionAge = activePhotos.size() > 1
                ? nowMillis - activePhotos.get(activePhotos.size() - 1).bornMillis
                : Long.MAX_VALUE;
        for (int i = 0; i < activePhotos.size(); i++) {
            ActivePhoto photo = activePhotos.get(i);
            long displayDuration = frameDisplayDurationMillis(photo.bitmap, width, height, safeIntervalMs);
            RectF frame = frameRectForBitmap(photo.bitmap, width, height, nowMillis, photo.bornMillis, displayDuration);
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

    private float rotationForPhoto(ActivePhoto photo, long nowMillis) {
        float finalRotation = layout.rotationForIndex(photo.layoutIndex);
        long age = nowMillis - photo.bornMillis;
        long duration = 1400L;
        if (age >= duration) {
            return finalRotation;
        }
        float progress = Math.max(0.0f, Math.min(1.0f, age / (float) duration));
        progress = 1.0f - (1.0f - progress) * (1.0f - progress);
        float startOffset = photo.layoutIndex % 2 == 0 ? -22.0f : 22.0f;
        return finalRotation + startOffset * (1.0f - progress);
    }

    private void addNextIfNeeded(Canvas canvas, long nowMillis, String orderMode, int maxVisible, int intervalMs) {
        if (activePhotos.size() >= maxVisible) {
            return;
        }
        if (lastAddMillis != 0 && nowMillis - lastAddMillis < intervalMs) {
            return;
        }
        ActivePhoto photo = decodeNext(canvas, nowMillis, orderMode);
        if (photo != null) {
            activePhotos.add(photo);
            lastAddMillis = nowMillis;
        }
    }

    private void addFramePhoto(Canvas canvas, long nowMillis, String orderMode) {
        ActivePhoto photo = decodeNext(canvas, nowMillis, orderMode);
        if (photo != null) {
            activePhotos.add(photo);
            lastAddMillis = nowMillis;
        }
    }

    private ActivePhoto decodeNext(Canvas canvas, long nowMillis, String orderMode) {
        for (int attempts = 0; attempts < photos.size(); attempts++) {
            int photoIndex = nextPhotoIndex(orderMode);
            PhotoItem item = photos.get(photoIndex);
            int layoutIndex = nextPhotoIndex;
            nextPhotoIndex++;
            Bitmap bitmap = null;
            try {
                bitmap = loader.decode(item, resolver, decodeMaxWidth(canvas), decodeMaxHeight(canvas));
            } catch (OutOfMemoryError ignored) {
            }
            if (bitmap != null) {
                return new ActivePhoto(bitmap, nowMillis, layoutIndex);
            }
        }
        return null;
    }

    private int decodeMaxWidth(Canvas canvas) {
        return Math.max(320, Math.min(canvas.getWidth(), 960));
    }

    private int decodeMaxHeight(Canvas canvas) {
        return Math.max(320, Math.min(canvas.getHeight(), 720));
    }

    static int maxVisibleForMemory(int requested, long maxMemoryBytes) {
        if (maxMemoryBytes <= 64L * 1024L * 1024L) {
            return Math.min(requested, 8);
        }
        if (maxMemoryBytes <= 128L * 1024L * 1024L) {
            return Math.min(requested, 12);
        }
        return requested;
    }

    private int nextPhotoIndex(String orderMode) {
        if (ORDER_SEQUENTIAL.equals(orderMode)) {
            return nextPhotoIndex % photos.size();
        }
        return random.nextInt(photos.size());
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
            return (int) Math.max(180, 255 * age / fadeIn);
        }
        long fadeOut = Math.min(1800L, Math.max(900L, intervalMs * 2L));
        long fadeStart = maxAge - fadeOut;
        if (age > fadeStart) {
            long fadeAge = Math.max(0L, age - fadeStart);
            return (int) Math.max(0, 255 - 255 * fadeAge / fadeOut);
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

    private long frameDisplayDurationMillis(Bitmap bitmap, int width, int height, int intervalMs) {
        RectF base = frameBaseRect(bitmap, width, height);
        float horizontalScreens = base.width() / Math.max(1.0f, width);
        float verticalScreens = base.height() / Math.max(1.0f, height);
        float screens = Math.max(horizontalScreens, verticalScreens);
        if (screens <= 1.01f) {
            return intervalMs;
        }
        return Math.max(intervalMs, (long) (intervalMs * screens));
    }

    private RectF frameRectForBitmap(Bitmap bitmap, int width, int height, long nowMillis, long bornMillis, long displayDurationMs) {
        RectF base = frameBaseRect(bitmap, width, height);
        float drawWidth = base.width();
        float drawHeight = base.height();
        float maxX = Math.max(0.0f, drawWidth - width);
        float maxY = Math.max(0.0f, drawHeight - height);
        float progress = displayDurationMs <= 0L ? 1.0f : (nowMillis - bornMillis) / (float) displayDurationMs;
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        float left = maxX >= maxY ? -maxX * progress : -maxX * 0.5f;
        float top = maxY > maxX ? -maxY * progress : -maxY * 0.5f;
        return new RectF(left, top, left + drawWidth, top + drawHeight);
    }

    private RectF frameBaseRect(Bitmap bitmap, int width, int height) {
        float scale = Math.max(width / (float) bitmap.getWidth(), height / (float) bitmap.getHeight());
        float drawWidth = bitmap.getWidth() * scale;
        float drawHeight = bitmap.getHeight() * scale;
        return new RectF(0.0f, 0.0f, drawWidth, drawHeight);
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
