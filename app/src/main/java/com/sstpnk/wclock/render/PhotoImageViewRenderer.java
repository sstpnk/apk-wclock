package com.sstpnk.wclock.render;

import android.content.Context;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
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
    private final List<ImageView> retiringViews = new ArrayList<ImageView>();
    private final List<Integer> activeSourceIndexes = new ArrayList<Integer>();
    private final Random random = new Random();
    private String loadedPath = "";
    private String loadedUri = "";
    private boolean collageEnabled = true;
    private String photoDisplayMode = CollageEngine.MODE_PHOTOWALL;
    private String photoOrderMode = CollageEngine.ORDER_RANDOM;
    private int maxVisiblePhotos = 18;
    private int photoChangeSeconds = 5;
    private int focusedPhotoTimeoutSeconds = 60;
    private int framePanSpeedPxPerSecond = 20;
    private boolean loading;
    private int generation;
    private int nextPhotoIndex;
    private int nextLayoutIndex;
    private long lastAddMillis;
    private long currentFrameDisplayMillis;
    private FocusState focusState;
    private ValueAnimator focusAnimator;
    private boolean focusTransitionRunning;
    private final Runnable focusedPhotoTimeout = new Runnable() {
        @Override
        public void run() {
            if (focusState != null && !focusTransitionRunning) {
                collapseFocusedPhoto();
            }
        }
    };

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
    public void setDisplaySettings(boolean collageEnabled, String photoDisplayMode, String photoOrderMode, int maxVisiblePhotos, int photoChangeSeconds, int focusedPhotoTimeoutSeconds, int framePanSpeedPxPerSecond) {
        int safeFocusedPhotoTimeoutSeconds = Math.max(0, Math.min(3600, focusedPhotoTimeoutSeconds));
        boolean focusedPhotoTimeoutChanged = this.focusedPhotoTimeoutSeconds != safeFocusedPhotoTimeoutSeconds;
        this.collageEnabled = collageEnabled;
        this.photoDisplayMode = CollageEngine.MODE_FRAME.equals(photoDisplayMode) ? CollageEngine.MODE_FRAME : CollageEngine.MODE_PHOTOWALL;
        this.photoOrderMode = CollageEngine.ORDER_SEQUENTIAL.equals(photoOrderMode) ? CollageEngine.ORDER_SEQUENTIAL : CollageEngine.ORDER_RANDOM;
        this.maxVisiblePhotos = Math.max(1, Math.min(50, maxVisiblePhotos));
        this.photoChangeSeconds = Math.max(1, photoChangeSeconds);
        this.focusedPhotoTimeoutSeconds = safeFocusedPhotoTimeoutSeconds;
        this.framePanSpeedPxPerSecond = Math.max(4, Math.min(48, framePanSpeedPxPerSecond));
        if (focusState != null && focusedPhotoTimeoutChanged) {
            scheduleFocusedPhotoTimeout();
        }
    }

    @Override
    public void renderFrame() {
        if (!collageEnabled || photos.size() == 0 || getWidth() <= 0 || getHeight() <= 0 || loading || focusState != null || focusTransitionRunning) {
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
        final int requestGeneration = generation;
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
                        if (requestGeneration != generation) {
                            recycleBitmap(bitmap);
                            return;
                        }
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

    public boolean handlePhotoWallTap(float x, float y) {
        if (focusState != null) {
            collapseFocusedPhoto();
            return true;
        }
        if (focusTransitionRunning) {
            return true;
        }
        if (!collageEnabled || !CollageEngine.MODE_PHOTOWALL.equals(photoDisplayMode)) {
            return false;
        }
        ImageView tapped = findTopmostPhotoAt(x, y);
        if (tapped == null) {
            return false;
        }
        expandPhoto(tapped);
        return true;
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
        int index = activeViews.indexOf(old);
        if (index >= 0) {
            activeViews.remove(index);
            activeSourceIndexes.remove(index);
        }
        if (!retiringViews.contains(old)) {
            retiringViews.add(old);
        }
        old.animate().alpha(0.0f).setDuration(WALL_ENTRANCE_MS).withEndAction(new Runnable() {
            @Override
            public void run() {
                removeViewAndRecycle(old);
            }
        }).start();
    }

    private void removeViewAndRecycle(ImageView view) {
        view.animate().cancel();
        boolean removingFocusedView = focusState != null && focusState.view == view;
        if (removingFocusedView) {
            cancelFocusAnimator();
            cancelFocusedPhotoTimeout();
            focusState = null;
            focusTransitionRunning = false;
        }
        int index = activeViews.indexOf(view);
        if (index >= 0) {
            activeViews.remove(index);
            activeSourceIndexes.remove(index);
        }
        retiringViews.remove(view);
        Bitmap bitmap = null;
        if (view.getDrawable() instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) view.getDrawable()).getBitmap();
        }
        view.setImageDrawable(null);
        removeView(view);
        recycleBitmap(bitmap);
    }

    private void clearPhotos() {
        generation++;
        loading = false;
        loadedPath = "";
        loadedUri = "";
        handler.removeCallbacksAndMessages(null);
        cancelFocusedPhotoTimeout();
        if (focusAnimator != null) {
            focusAnimator.cancel();
            focusAnimator = null;
        }
        focusState = null;
        focusTransitionRunning = false;
        for (int i = activeViews.size() - 1; i >= 0; i--) {
            removeViewAndRecycle(activeViews.get(i));
        }
        for (int i = retiringViews.size() - 1; i >= 0; i--) {
            removeViewAndRecycle(retiringViews.get(i));
        }
        activeViews.clear();
        retiringViews.clear();
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

    private ImageView findTopmostPhotoAt(float x, float y) {
        for (int i = activeViews.size() - 1; i >= 0; i--) {
            ImageView view = activeViews.get(i);
            if (view.getAlpha() > 0.05f && x >= view.getX() && x <= view.getX() + view.getWidth()
                    && y >= view.getY() && y <= view.getY() + view.getHeight()) {
                return view;
            }
        }
        return null;
    }

    private void expandPhoto(final ImageView view) {
        final Bitmap bitmap = bitmapForView(view);
        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }
        cancelFocusAnimator();
        cancelFocusedPhotoTimeout();
        view.animate().cancel();
        view.bringToFront();
        final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        final FocusState state = new FocusState(view, params.leftMargin, params.topMargin, params.width, params.height, view.getRotation());
        focusState = state;
        view.setAlpha(1.0f);
        view.setBackgroundColor(Color.TRANSPARENT);
        view.setPadding(0, 0, 0, 0);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        RectF target = expandedFrame(bitmap);
        animatePhotoFrame(state, target, 260L, new Runnable() {
            @Override
            public void run() {
                focusTransitionRunning = false;
                scheduleFocusedPhotoTimeout();
            }
        });
    }

    private void collapseFocusedPhoto() {
        final FocusState state = focusState;
        if (state == null) {
            return;
        }
        focusState = null;
        cancelFocusedPhotoTimeout();
        cancelFocusAnimator();
        RectF target = new RectF(state.leftMargin, state.topMargin, state.leftMargin + state.width, state.topMargin + state.height);
        animatePhotoFrame(state, target, 220L, new Runnable() {
            @Override
            public void run() {
                state.view.setBackgroundColor(0xFFF4F1EA);
                int border = borderSize();
                state.view.setPadding(border, border, border, border);
                state.view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                focusTransitionRunning = false;
                lastAddMillis = System.currentTimeMillis();
            }
        });
    }

    private void animatePhotoFrame(final FocusState state, final RectF target, long duration, final Runnable endAction) {
        final ImageView view = state.view;
        final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        final float startLeft = params.leftMargin;
        final float startTop = params.topMargin;
        final float startWidth = params.width;
        final float startHeight = params.height;
        final float startRotation = view.getRotation();
        final float targetRotation = focusState == state ? 0.0f : state.rotation;
        focusTransitionRunning = true;
        focusAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        focusAnimator.setDuration(duration);
        focusAnimator.setInterpolator(new DecelerateInterpolator());
        focusAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float progress = ((Float) animation.getAnimatedValue()).floatValue();
                params.leftMargin = Math.round(lerp(startLeft, target.left, progress));
                params.topMargin = Math.round(lerp(startTop, target.top, progress));
                params.width = Math.max(1, Math.round(lerp(startWidth, target.width(), progress)));
                params.height = Math.max(1, Math.round(lerp(startHeight, target.height(), progress)));
                view.setLayoutParams(params);
                view.setRotation(lerp(startRotation, targetRotation, progress));
            }
        });
        focusAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean canceled;

            @Override
            public void onAnimationCancel(Animator animation) {
                canceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (focusAnimator == animation) {
                    focusAnimator = null;
                }
                if (!canceled && endAction != null) {
                    endAction.run();
                }
            }
        });
        focusAnimator.start();
    }

    private RectF expandedFrame(Bitmap bitmap) {
        float scale = Math.min(getWidth() / (float) Math.max(1, bitmap.getWidth()), getHeight() / (float) Math.max(1, bitmap.getHeight()));
        int width = Math.max(1, Math.round(bitmap.getWidth() * scale));
        int height = Math.max(1, Math.round(bitmap.getHeight() * scale));
        float left = (getWidth() - width) * 0.5f;
        float top = (getHeight() - height) * 0.5f;
        return new RectF(left, top, left + width, top + height);
    }

    private Bitmap bitmapForView(ImageView view) {
        return view.getDrawable() instanceof BitmapDrawable ? ((BitmapDrawable) view.getDrawable()).getBitmap() : null;
    }

    private void cancelFocusAnimator() {
        if (focusAnimator != null) {
            focusAnimator.cancel();
            focusAnimator = null;
        }
    }

    private void scheduleFocusedPhotoTimeout() {
        cancelFocusedPhotoTimeout();
        if (focusedPhotoTimeoutSeconds <= 0 || focusState == null || focusTransitionRunning) {
            return;
        }
        handler.postDelayed(focusedPhotoTimeout, focusedPhotoTimeoutSeconds * 1000L);
    }

    private void cancelFocusedPhotoTimeout() {
        handler.removeCallbacks(focusedPhotoTimeout);
    }

    private void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    private float lerp(float start, float end, float progress) {
        return start + (end - start) * progress;
    }

    void showWallBitmapForTest(Bitmap bitmap, int layoutIndex, int sourceIndex) {
        showWallBitmap(bitmap, layoutIndex, sourceIndex);
    }

    void fadeOutOldestViewForTest() {
        fadeOutOldestView();
    }

    int activePhotoCountForTest() {
        return activeViews.size();
    }

    int retiringPhotoCountForTest() {
        return retiringViews.size();
    }

    int sourcePhotoCountForTest() {
        return photos.size();
    }

    boolean focusedForTest() {
        return focusState != null;
    }

    RectF expandedFrameForTest(Bitmap bitmap) {
        return expandedFrame(bitmap);
    }

    private static final class FocusState {
        final ImageView view;
        final int leftMargin;
        final int topMargin;
        final int width;
        final int height;
        final float rotation;

        FocusState(ImageView view, int leftMargin, int topMargin, int width, int height, float rotation) {
            this.view = view;
            this.leftMargin = leftMargin;
            this.topMargin = topMargin;
            this.width = width;
            this.height = height;
            this.rotation = rotation;
        }
    }
}
