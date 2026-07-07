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
import java.util.concurrent.Executor;

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
    private final Random random;
    private final Executor decodeExecutor;
    private final Object decodeLock = new Object();
    private final List<PhotoItem> photos = new ArrayList<PhotoItem>();
    private final List<ActivePhoto> activePhotos = new ArrayList<ActivePhoto>();
    private final List<RetiredPhoto> retiredPhotos = new ArrayList<RetiredPhoto>();
    private String loadedPath = "";
    private String loadedUri = "";
    private boolean sourceLoaded;
    private boolean decodeInFlight;
    private int decodeGeneration;
    private PreparedPhoto preparedPhoto;
    private int nextPhotoIndex;
    private long lastAddMillis;
    private static final long RETIRED_BITMAP_GRACE_MS = 2000L;

    public CollageEngine(ContentResolver resolver) {
        this(resolver, new BitmapLoader(), new Random(), backgroundDecodeExecutor());
    }

    CollageEngine(ContentResolver resolver, BitmapDecoder loader) {
        this(resolver, loader, new Random(), directDecodeExecutor());
    }

    CollageEngine(ContentResolver resolver, BitmapDecoder loader, Random random) {
        this(resolver, loader, random, directDecodeExecutor());
    }

    CollageEngine(ContentResolver resolver, BitmapDecoder loader, Random random, Executor decodeExecutor) {
        this.resolver = resolver;
        this.loader = loader;
        this.random = random == null ? new Random() : random;
        this.decodeExecutor = decodeExecutor == null ? backgroundDecodeExecutor() : decodeExecutor;
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
        draw(canvas, nowMillis, enabled, mode, ORDER_RANDOM, maxVisible, changeSeconds, 20);
    }

    public void draw(Canvas canvas, long nowMillis, boolean enabled, String mode, String orderMode, int maxVisible, int changeSeconds) {
        draw(canvas, nowMillis, enabled, mode, orderMode, maxVisible, changeSeconds, 20);
    }

    public void draw(Canvas canvas, long nowMillis, boolean enabled, String mode, String orderMode, int maxVisible, int changeSeconds, int framePanSpeedPxPerSecond) {
        canvas.drawColor(Color.rgb(12, 14, 16));
        if (!enabled || photos.size() == 0) {
            return;
        }
        if (MODE_FRAME.equals(mode)) {
            drawFrameMode(canvas, nowMillis, orderMode, changeSeconds, framePanSpeedPxPerSecond);
            return;
        }
        drawPhotoWall(canvas, nowMillis, orderMode, maxVisible, changeSeconds);
    }

    public void recycle() {
        PreparedPhoto queued;
        synchronized (decodeLock) {
            decodeGeneration++;
            decodeInFlight = false;
            queued = preparedPhoto;
            preparedPhoto = null;
        }
        recycle(queued);
        for (ActivePhoto photo : activePhotos) {
            recycle(photo);
        }
        for (RetiredPhoto photo : retiredPhotos) {
            recycle(photo);
        }
        activePhotos.clear();
        retiredPhotos.clear();
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
        recycleRetired(nowMillis);
        int safeMax = maxVisibleForMemory(Math.max(1, Math.min(50, maxVisible)), Runtime.getRuntime().maxMemory());
        int safeIntervalMs = Math.max(1, changeSeconds) * 1000;
        removeExpired(nowMillis, safeMax, safeIntervalMs);
        activatePreparedIfNeeded(nowMillis, safeMax, safeIntervalMs);
        requestNextIfNeeded(canvas, orderMode, safeMax);
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

    private void drawFrameMode(Canvas canvas, long nowMillis, String orderMode, int changeSeconds, int framePanSpeedPxPerSecond) {
        recycleRetired(nowMillis);
        int safeIntervalMs = Math.max(1, changeSeconds) * 1000;
        int safePanSpeed = Math.max(4, Math.min(48, framePanSpeedPxPerSecond));
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        if (activePhotos.size() == 0) {
            requestNextIfNeeded(canvas, orderMode, 2);
            activatePreparedFrameIfAvailable(nowMillis);
            if (activePhotos.size() == 0) {
                return;
            }
        } else if (activePhotos.size() == 1) {
            ActivePhoto current = activePhotos.get(0);
            long displayDuration = frameDisplayDurationMillis(current.bitmap, width, height, safeIntervalMs, safePanSpeed);
            requestNextIfNeeded(canvas, orderMode, 2);
            if (nowMillis - current.bornMillis >= displayDuration) {
                activatePreparedFrameIfAvailable(nowMillis);
            }
        }
        removeFrameExtras(nowMillis);
        long transitionAge = activePhotos.size() > 1
                ? nowMillis - activePhotos.get(activePhotos.size() - 1).bornMillis
                : Long.MAX_VALUE;
        for (int i = 0; i < activePhotos.size(); i++) {
            ActivePhoto photo = activePhotos.get(i);
            long displayDuration = frameDisplayDurationMillis(photo.bitmap, width, height, safeIntervalMs, safePanSpeed);
            RectF frame = frameRectForBitmap(photo.bitmap, width, height, nowMillis, photo.bornMillis, displayDuration);
            paint.setAlpha(activePhotos.size() == 1 ? 255 : alphaForFrameTransition(transitionAge, i == activePhotos.size() - 1));
            canvas.drawBitmap(photo.bitmap, null, frame, paint);
        }
        paint.setAlpha(255);
        removeFadedFrameExtras(nowMillis, transitionAge);
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

    private void activatePreparedIfNeeded(long nowMillis, int maxVisible, int intervalMs) {
        if (activePhotos.size() >= maxVisible) {
            return;
        }
        if (lastAddMillis != 0 && nowMillis - lastAddMillis < intervalMs) {
            return;
        }
        PreparedPhoto photo;
        synchronized (decodeLock) {
            photo = preparedPhoto;
            preparedPhoto = null;
        }
        if (photo == null) {
            return;
        }
        activePhotos.add(new ActivePhoto(photo.bitmap, nowMillis, photo.layoutIndex, photo.sourceIndex));
        lastAddMillis = nowMillis;
    }

    private void requestNextIfNeeded(Canvas canvas, String orderMode, int maxVisible) {
        synchronized (decodeLock) {
            if (decodeInFlight || preparedPhoto != null) {
                return;
            }
        }
        final DecodeRequest request = createDecodeRequest(orderMode, decodeMaxWidth(canvas), decodeMaxHeight(canvas));
        if (request.candidates.size() == 0) {
            return;
        }
        synchronized (decodeLock) {
            if (decodeInFlight || preparedPhoto != null) {
                return;
            }
            decodeInFlight = true;
        }
        decodeExecutor.execute(new Runnable() {
            @Override
            public void run() {
                decodePreparedPhoto(request);
            }
        });
    }

    private DecodeRequest createDecodeRequest(String orderMode, int maxWidth, int maxHeight) {
        int generation;
        synchronized (decodeLock) {
            generation = decodeGeneration;
        }
        List<DecodeCandidate> candidates = new ArrayList<DecodeCandidate>();
        int photoIndex = nextPhotoIndex(orderMode);
        PhotoItem item = photos.get(photoIndex);
        int layoutIndex = nextPhotoIndex;
        nextPhotoIndex++;
        candidates.add(new DecodeCandidate(item, layoutIndex, photoIndex));
        return new DecodeRequest(candidates, maxWidth, maxHeight, generation);
    }

    private void decodePreparedPhoto(DecodeRequest request) {
        PreparedPhoto decoded = null;
        try {
            for (DecodeCandidate candidate : request.candidates) {
                Bitmap bitmap = null;
                try {
                    bitmap = loader.decode(candidate.item, resolver, request.maxWidth, request.maxHeight);
                } catch (OutOfMemoryError ignored) {
                }
                if (bitmap != null) {
                    decoded = new PreparedPhoto(bitmap, candidate.layoutIndex, candidate.sourceIndex);
                    break;
                }
            }
        } finally {
            PreparedPhoto stale = null;
            synchronized (decodeLock) {
                boolean currentGeneration = request.generation == decodeGeneration;
                if (currentGeneration && preparedPhoto == null) {
                    preparedPhoto = decoded;
                    decoded = null;
                }
                if (currentGeneration) {
                    decodeInFlight = false;
                }
                stale = decoded;
            }
            recycle(stale);
        }
    }

    private void activatePreparedFrameIfAvailable(long nowMillis) {
        if (activePhotos.size() >= 2) {
            return;
        }
        PreparedPhoto photo;
        synchronized (decodeLock) {
            photo = preparedPhoto;
            preparedPhoto = null;
        }
        if (photo == null) {
            return;
        }
        activePhotos.add(new ActivePhoto(photo.bitmap, nowMillis, photo.layoutIndex, photo.sourceIndex));
        lastAddMillis = nowMillis;
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
        int startIndex = random.nextInt(photos.size());
        for (int offset = 0; offset < photos.size(); offset++) {
            int candidate = (startIndex + offset) % photos.size();
            if (!isActiveSourceIndex(candidate)) {
                return candidate;
            }
        }
        return startIndex;
    }

    private boolean isActiveSourceIndex(int sourceIndex) {
        for (int i = 0; i < activePhotos.size(); i++) {
            if (activePhotos.get(i).sourceIndex == sourceIndex) {
                return true;
            }
        }
        return false;
    }

    private void removeExpired(long nowMillis, int maxVisible, int intervalMs) {
        long maxAge = Math.max(intervalMs * 4L, intervalMs * (long) (maxVisible + 2));
        Iterator<ActivePhoto> iterator = activePhotos.iterator();
        while (iterator.hasNext()) {
            ActivePhoto photo = iterator.next();
            if (nowMillis - photo.bornMillis > maxAge) {
                retire(photo, nowMillis);
                iterator.remove();
            }
        }
    }

    private void removeFrameExtras(long nowMillis) {
        while (activePhotos.size() > 2) {
            ActivePhoto first = activePhotos.remove(0);
            retire(first, nowMillis);
        }
    }

    private void removeFadedFrameExtras(long nowMillis, long transitionAge) {
        if (activePhotos.size() > 1 && transitionAge > 1800L) {
            ActivePhoto first = activePhotos.remove(0);
            retire(first, nowMillis);
        }
    }

    private void retire(ActivePhoto photo, long nowMillis) {
        retiredPhotos.add(new RetiredPhoto(photo.bitmap, nowMillis + RETIRED_BITMAP_GRACE_MS));
    }

    private void recycleRetired(long nowMillis) {
        Iterator<RetiredPhoto> iterator = retiredPhotos.iterator();
        while (iterator.hasNext()) {
            RetiredPhoto photo = iterator.next();
            if (nowMillis >= photo.recycleAfterMillis) {
                recycle(photo);
                iterator.remove();
            }
        }
    }

    private void recycle(ActivePhoto photo) {
        if (photo.bitmap != null && !photo.bitmap.isRecycled()) {
            photo.bitmap.recycle();
        }
    }

    private void recycle(PreparedPhoto photo) {
        if (photo != null && photo.bitmap != null && !photo.bitmap.isRecycled()) {
            photo.bitmap.recycle();
        }
    }

    private void recycle(RetiredPhoto photo) {
        if (photo != null && photo.bitmap != null && !photo.bitmap.isRecycled()) {
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
        long fade = 1800L;
        long age = Math.max(0L, Math.min(fade, transitionAge));
        if (newest) {
            return (int) Math.min(255, 255 * age / fade);
        }
        return (int) Math.max(0, 255 - 255 * age / fade);
    }

    private long frameDisplayDurationMillis(Bitmap bitmap, int width, int height, int intervalMs) {
        return frameDisplayDurationMillis(bitmap, width, height, intervalMs, 20);
    }

    private long frameDisplayDurationMillis(Bitmap bitmap, int width, int height, int intervalMs, int framePanSpeedPxPerSecond) {
        RectF base = frameBaseRect(bitmap, width, height);
        float horizontalScreens = base.width() / Math.max(1.0f, width);
        float verticalScreens = base.height() / Math.max(1.0f, height);
        float screens = Math.max(horizontalScreens, verticalScreens);
        if (screens <= 1.01f) {
            return intervalMs;
        }
        float maxPanPx = Math.max(0.0f, Math.max(base.width() - width, base.height() - height));
        float safePanSpeed = Math.max(4.0f, Math.min(48.0f, framePanSpeedPxPerSecond));
        long durationForSmoothPan = (long) Math.ceil(maxPanPx / safePanSpeed * 1000.0f);
        return Math.max(Math.max(intervalMs, (long) (intervalMs * screens * 1.5f)), durationForSmoothPan);
    }

    private RectF frameRectForBitmap(Bitmap bitmap, int width, int height, long nowMillis, long bornMillis, long displayDurationMs) {
        RectF base = frameBaseRect(bitmap, width, height);
        float drawWidth = base.width();
        float drawHeight = base.height();
        float maxX = Math.max(0.0f, drawWidth - width);
        float maxY = Math.max(0.0f, drawHeight - height);
        float progress = frameProgress(nowMillis, bornMillis, displayDurationMs);
        float left = maxX >= maxY ? -maxX * progress : -maxX * 0.5f;
        float top = maxY > maxX ? -maxY * progress : -maxY * 0.5f;
        return new RectF(left, top, left + drawWidth, top + drawHeight);
    }

    private float frameProgress(long nowMillis, long bornMillis, long displayDurationMs) {
        float progress = displayDurationMs <= 0L ? 1.0f : (nowMillis - bornMillis) / (float) displayDurationMs;
        return Math.max(0.0f, Math.min(1.0f, progress));
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
        final int sourceIndex;

        ActivePhoto(Bitmap bitmap, long bornMillis, int layoutIndex, int sourceIndex) {
            this.bitmap = bitmap;
            this.bornMillis = bornMillis;
            this.layoutIndex = layoutIndex;
            this.sourceIndex = sourceIndex;
        }
    }

    private static final class PreparedPhoto {
        final Bitmap bitmap;
        final int layoutIndex;
        final int sourceIndex;

        PreparedPhoto(Bitmap bitmap, int layoutIndex, int sourceIndex) {
            this.bitmap = bitmap;
            this.layoutIndex = layoutIndex;
            this.sourceIndex = sourceIndex;
        }
    }

    private static final class RetiredPhoto {
        final Bitmap bitmap;
        final long recycleAfterMillis;

        RetiredPhoto(Bitmap bitmap, long recycleAfterMillis) {
            this.bitmap = bitmap;
            this.recycleAfterMillis = recycleAfterMillis;
        }
    }

    private static final class DecodeCandidate {
        final PhotoItem item;
        final int layoutIndex;
        final int sourceIndex;

        DecodeCandidate(PhotoItem item, int layoutIndex, int sourceIndex) {
            this.item = item;
            this.layoutIndex = layoutIndex;
            this.sourceIndex = sourceIndex;
        }
    }

    private static final class DecodeRequest {
        final List<DecodeCandidate> candidates;
        final int maxWidth;
        final int maxHeight;
        final int generation;

        DecodeRequest(List<DecodeCandidate> candidates, int maxWidth, int maxHeight, int generation) {
            this.candidates = candidates;
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
            this.generation = generation;
        }
    }

    private static Executor backgroundDecodeExecutor() {
        return new Executor() {
            @Override
            public void execute(Runnable command) {
                Thread thread = new Thread(command, "wclock-photo-decode");
                thread.setDaemon(true);
                thread.start();
            }
        };
    }

    private static Executor directDecodeExecutor() {
        return new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
    }
}
