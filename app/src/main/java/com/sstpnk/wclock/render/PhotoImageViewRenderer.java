package com.sstpnk.wclock.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.sstpnk.wclock.collage.BitmapLoader;
import com.sstpnk.wclock.collage.CollageEngine;
import com.sstpnk.wclock.collage.CollageLayout;
import com.sstpnk.wclock.collage.PhotoItem;
import com.sstpnk.wclock.collage.PhotoScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class PhotoImageViewRenderer extends FrameLayout implements PhotoRenderer {
    private static final long FRAME_CROSSFADE_MS = 1800L;
    private static final long WALL_ENTRANCE_MS = 1400L;

    private final PhotoScanner scanner = new PhotoScanner();
    private final BitmapLoader loader = new BitmapLoader();
    private final CollageLayout layout = new CollageLayout();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<PhotoItem> photos = new ArrayList<PhotoItem>();
    private final List<ImageView> activeViews = new ArrayList<ImageView>();
    private final List<Integer> activeSourceIndexes = new ArrayList<Integer>();
    private final Random random = new Random();
    private String loadedPath = "";
    private String loadedUri = "";
    private boolean collageEnabled = true;
    private String photoDisplayMode = CollageEngine.MODE_PHOTOWALL;
    private String photoOrderMode = CollageEngine.ORDER_RANDOM;
    private int maxVisiblePhotos = 18;
    private int photoChangeSeconds = 5;
    private int framePanSpeedPxPerSecond = 20;
    private boolean loading;
    private int nextPhotoIndex;
    private int nextLayoutIndex;
    private long lastAddMillis;
    private long currentFrameDisplayMillis;

    public PhotoImageViewRenderer(Context context) {
        super(context);
        setBackgroundColor(Color.rgb(12, 14, 16));
        setClipChildren(false);
    }

    @Override
    public void setPhotoSource(String path, String uri) {
        String safePath = path == null ? "" : path;
        String safeUri = uri == null ? "" : uri;
        if (safePath.equals(loadedPath) && safeUri.equals(loadedUri)) {
            return;
        }
        clearPhotos();
        loadedPath = safePath;
        loadedUri = safeUri;
        if (safeUri.length() > 0) {
            photos.addAll(scanner.scanTree(getContext().getContentResolver(), Uri.parse(safeUri)));
        } else if (safePath.length() > 0) {
            photos.addAll(scanner.scan(new File(safePath)));
        }
    }

    @Override
    public void setDisplaySettings(boolean collageEnabled, String photoDisplayMode, String photoOrderMode, int maxVisiblePhotos, int photoChangeSeconds, int framePanSpeedPxPerSecond) {
        this.collageEnabled = collageEnabled;
        this.photoDisplayMode = CollageEngine.MODE_FRAME.equals(photoDisplayMode) ? CollageEngine.MODE_FRAME : CollageEngine.MODE_PHOTOWALL;
        this.photoOrderMode = CollageEngine.ORDER_SEQUENTIAL.equals(photoOrderMode) ? CollageEngine.ORDER_SEQUENTIAL : CollageEngine.ORDER_RANDOM;
        this.maxVisiblePhotos = Math.max(1, Math.min(50, maxVisiblePhotos));
        this.photoChangeSeconds = Math.max(1, photoChangeSeconds);
        this.framePanSpeedPxPerSecond = Math.max(4, Math.min(48, framePanSpeedPxPerSecond));
    }

    @Override
    public void renderFrame() {
        if (!collageEnabled || photos.size() == 0 || getWidth() <= 0 || getHeight() <= 0 || loading) {
            return;
        }
        if (CollageEngine.MODE_FRAME.equals(photoDisplayMode)) {
            renderFrameMode();
            return;
        }
        renderPhotoWall();
    }

    @Override
    public void recycle() {
        clearPhotos();
    }

    private void renderFrameMode() {
        if (activeViews.size() == 0) {
            requestNextBitmap(true);
            return;
        }
        long displayMillis = Math.max(Math.max(1, photoChangeSeconds) * 1000L, currentFrameDisplayMillis);
        if (System.currentTimeMillis() - lastAddMillis >= displayMillis && activeViews.size() == 1) {
            requestNextBitmap(true);
        }
    }

    private void renderPhotoWall() {
        long now = System.currentTimeMillis();
        if (lastAddMillis != 0 && now - lastAddMillis < Math.max(1, photoChangeSeconds) * 1000L) {
            return;
        }
        if (activeViews.size() >= maxVisiblePhotos) {
            fadeOutOldestView();
        }
        requestNextBitmap(false);
    }

    private void requestNextBitmap(final boolean frameMode) {
        if (loading) {
            return;
        }
        loading = true;
        final int sourceIndex = nextPhotoIndex();
        if (CollageEngine.ORDER_SEQUENTIAL.equals(photoOrderMode)) {
            nextPhotoIndex = (sourceIndex + 1) % photos.size();
        }
        final PhotoItem item = photos.get(sourceIndex);
        final int layoutIndex = nextLayoutIndex++;
        final int maxWidth = Math.max(320, Math.min(getWidth(), 960));
        final int maxHeight = Math.max(320, Math.min(getHeight(), 720));
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final Bitmap bitmap = loader.decode(item, getContext().getContentResolver(), maxWidth, maxHeight);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        loading = false;
                        if (bitmap == null) {
                            return;
                        }
                        if (frameMode) {
                            showFrameBitmap(bitmap, sourceIndex);
                        } else {
                            showWallBitmap(bitmap, layoutIndex, sourceIndex);
                        }
                    }
                });
            }
        }, "wclock-imageview-decode");
        thread.setDaemon(true);
        thread.start();
    }

    private void showFrameBitmap(Bitmap bitmap, int sourceIndex) {
        final ImageView view = imageView(bitmap, false);
        FrameLayout.LayoutParams params = frameLayoutParams(bitmap);
        view.setAlpha(0.0f);
        addView(view, params);
        activeViews.add(view);
        activeSourceIndexes.add(Integer.valueOf(sourceIndex));
        currentFrameDisplayMillis = frameDisplayDurationMillis(bitmap, Math.max(1, photoChangeSeconds) * 1000L, framePanSpeedPxPerSecond);
        animateFramePan(view, bitmap, currentFrameDisplayMillis);
        view.animate().alpha(1.0f).setDuration(FRAME_CROSSFADE_MS).start();
        while (activeViews.size() > 2) {
            removeOldestView();
        }
        if (activeViews.size() == 2) {
            final ImageView old = activeViews.get(0);
            old.animate().alpha(0.0f).setDuration(FRAME_CROSSFADE_MS).withEndAction(new Runnable() {
                @Override
                public void run() {
                    removeViewAndRecycle(old);
                }
            }).start();
        }
        lastAddMillis = System.currentTimeMillis();
    }

    private void animateFramePan(ImageView view, Bitmap bitmap, long duration) {
        float scale = Math.max(getWidth() / (float) Math.max(1, bitmap.getWidth()), getHeight() / (float) Math.max(1, bitmap.getHeight()));
        float drawWidth = bitmap.getWidth() * scale;
        float drawHeight = bitmap.getHeight() * scale;
        float maxX = Math.max(0.0f, drawWidth - getWidth());
        float maxY = Math.max(0.0f, drawHeight - getHeight());
        if (maxX >= maxY && maxX > 0.0f) {
            view.setTranslationX(0.0f);
            view.setTranslationY(-maxY * 0.5f);
            view.animate().translationX(-maxX).setDuration(duration).setInterpolator(new LinearInterpolator()).start();
        } else if (maxY > 0.0f) {
            view.setTranslationX(-maxX * 0.5f);
            view.setTranslationY(0.0f);
            view.animate().translationY(-maxY).setDuration(duration).setInterpolator(new LinearInterpolator()).start();
        } else {
            view.setTranslationX(0.0f);
            view.setTranslationY(0.0f);
        }
    }

    private void showWallBitmap(Bitmap bitmap, int layoutIndex, int sourceIndex) {
        ImageView view = imageView(bitmap, true);
        RectF frame = layout.frameForIndex(layoutIndex, getWidth(), getHeight(), bitmap.getWidth(), bitmap.getHeight());
        int border = borderSize();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(Math.max(1, Math.round(frame.width() + border * 2.0f)), Math.max(1, Math.round(frame.height() + border * 2.0f)));
        params.leftMargin = Math.round(frame.left - border);
        params.topMargin = Math.round(frame.top - border);
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        view.setRotation(layout.rotationForIndex(layoutIndex));
        view.setAlpha(0.0f);
        view.setTranslationX(layoutIndex % 2 == 0 ? -frame.width() - 20.0f : getWidth() + 20.0f);
        addView(view, params);
        activeViews.add(view);
        activeSourceIndexes.add(Integer.valueOf(sourceIndex));
        view.animate().translationX(0.0f).alpha(1.0f).setDuration(WALL_ENTRANCE_MS).setInterpolator(new DecelerateInterpolator()).start();
        lastAddMillis = System.currentTimeMillis();
    }

    private ImageView imageView(Bitmap bitmap, boolean bordered) {
        ImageView view = new ImageView(getContext());
        if (bordered) {
            int border = borderSize();
            view.setBackgroundColor(0xFFF4F1EA);
            view.setPadding(border, border, border, border);
        }
        view.setImageBitmap(bitmap);
        return view;
    }

    private int borderSize() {
        return Math.max(2, Math.round(getResources().getDisplayMetrics().density * 4.0f));
    }

    private FrameLayout.LayoutParams frameLayoutParams(Bitmap bitmap) {
        float scale = Math.max(getWidth() / (float) Math.max(1, bitmap.getWidth()), getHeight() / (float) Math.max(1, bitmap.getHeight()));
        int width = Math.max(getWidth(), Math.round(bitmap.getWidth() * scale));
        int height = Math.max(getHeight(), Math.round(bitmap.getHeight() * scale));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        params.leftMargin = 0;
        params.topMargin = 0;
        return params;
    }

    private int nextPhotoIndex() {
        if (CollageEngine.ORDER_SEQUENTIAL.equals(photoOrderMode)) {
            return nextPhotoIndex % photos.size();
        }
        int start = random.nextInt(photos.size());
        for (int offset = 0; offset < photos.size(); offset++) {
            int candidate = (start + offset) % photos.size();
            if (!activeSourceIndexes.contains(Integer.valueOf(candidate))) {
                return candidate;
            }
        }
        return start;
    }

    private void removeOldestView() {
        if (activeViews.size() == 0) {
            return;
        }
        removeViewAndRecycle(activeViews.get(0));
    }

    private void fadeOutOldestView() {
        if (activeViews.size() == 0) {
            return;
        }
        final ImageView old = activeViews.get(0);
        old.animate().alpha(0.0f).setDuration(WALL_ENTRANCE_MS).withEndAction(new Runnable() {
            @Override
            public void run() {
                removeViewAndRecycle(old);
            }
        }).start();
    }

    private void removeViewAndRecycle(ImageView view) {
        int index = activeViews.indexOf(view);
        if (index >= 0) {
            activeViews.remove(index);
            activeSourceIndexes.remove(index);
        }
        Bitmap bitmap = null;
        if (view.getDrawable() instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) view.getDrawable()).getBitmap();
        }
        view.setImageDrawable(null);
        removeView(view);
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    private void clearPhotos() {
        loading = false;
        handler.removeCallbacksAndMessages(null);
        for (int i = activeViews.size() - 1; i >= 0; i--) {
            removeViewAndRecycle(activeViews.get(i));
        }
        activeViews.clear();
        activeSourceIndexes.clear();
        photos.clear();
        nextPhotoIndex = 0;
        nextLayoutIndex = 0;
        lastAddMillis = 0L;
        currentFrameDisplayMillis = 0L;
    }

    long frameDisplayDurationMillisForTest(Bitmap bitmap, long intervalMs, int panSpeedPxPerSecond) {
        return frameDisplayDurationMillis(bitmap, intervalMs, panSpeedPxPerSecond);
    }

    private long frameDisplayDurationMillis(Bitmap bitmap, long intervalMs, int panSpeedPxPerSecond) {
        float scale = Math.max(getWidth() / (float) Math.max(1, bitmap.getWidth()), getHeight() / (float) Math.max(1, bitmap.getHeight()));
        float drawWidth = bitmap.getWidth() * scale;
        float drawHeight = bitmap.getHeight() * scale;
        float maxPanPx = Math.max(0.0f, Math.max(drawWidth - getWidth(), drawHeight - getHeight()));
        long panDuration = (long) Math.ceil(maxPanPx / Math.max(4.0f, Math.min(48.0f, panSpeedPxPerSecond)) * 1000.0f);
        return Math.max(intervalMs, panDuration);
    }
}
