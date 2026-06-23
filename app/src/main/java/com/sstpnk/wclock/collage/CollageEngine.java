package com.sstpnk.wclock.collage;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class CollageEngine {
    private final PhotoScanner scanner = new PhotoScanner();
    private final BitmapLoader loader = new BitmapLoader();
    private final CollageLayout layout = new CollageLayout();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final List<PhotoItem> photos = new ArrayList<PhotoItem>();
    private final List<Bitmap> bitmaps = new ArrayList<Bitmap>();
    private String loadedPath = "";

    public void setFolder(String path, int maxVisible, int targetWidth, int targetHeight) {
        if (path == null || path.equals(loadedPath)) {
            return;
        }
        recycle();
        loadedPath = path;
        photos.addAll(scanner.scan(new File(path)));
        int count = Math.min(maxVisible, photos.size());
        for (int i = 0; i < count; i++) {
            Bitmap bitmap = loader.decode(photos.get(i).file, Math.max(320, targetWidth / 2), Math.max(320, targetHeight / 2));
            if (bitmap != null) {
                bitmaps.add(bitmap);
            }
        }
    }

    public void draw(Canvas canvas, long nowMillis) {
        canvas.drawColor(Color.rgb(12, 14, 16));
        if (bitmaps.size() == 0) {
            drawPlaceholder(canvas);
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        for (int i = 0; i < bitmaps.size(); i++) {
            Bitmap bitmap = bitmaps.get(i);
            RectF frame = layout.frameForIndex(i, width, height);
            float drift = (float) Math.sin((nowMillis / 120000.0) + i) * 12.0f;
            frame.offset(drift, -drift * 0.5f);
            canvas.save();
            canvas.rotate(layout.rotationForIndex(i), frame.centerX(), frame.centerY());
            paint.setColor(0x66000000);
            canvas.drawRect(frame.left + 8, frame.top + 8, frame.right + 8, frame.bottom + 8, paint);
            canvas.drawBitmap(bitmap, null, frame, paint);
            canvas.restore();
        }
    }

    public void recycle() {
        for (Bitmap bitmap : bitmaps) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        bitmaps.clear();
        photos.clear();
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
}
