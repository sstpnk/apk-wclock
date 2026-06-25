package com.sstpnk.wclock.collage;

import android.content.Context;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CollageEngineRenderTest {
    @Test
    public void robolectricCanvasDrawsBitmapPixels() {
        Bitmap source = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888);
        Canvas sourceCanvas = new Canvas(source);
        sourceCanvas.drawColor(Color.rgb(230, 40, 40));
        Bitmap target = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);

        canvas.drawBitmap(source, 20.0f, 20.0f, null);

        assertTrue("Canvas sanity check must see drawn bitmap pixels", countDominantRedPixels(target) > 1000);
    }

    @Test
    public void photoWallDrawsImagePixelsNotOnlyBackground() throws Exception {
        File folder = createImageFolder("photowall", Color.rgb(230, 40, 40));
        Bitmap target = Bitmap.createBitmap(420, 640, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);

        CollageEngine engine = new CollageEngine(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                new SolidBitmapDecoder(Color.rgb(230, 40, 40)));
        engine.setSource(folder.getAbsolutePath(), "");
        assertTrue("Photowall source folder must produce photos", engine.photoCountForTest() > 0);
        engine.draw(canvas, 1000L, true, CollageEngine.MODE_PHOTOWALL, 10, 1);
        engine.draw(canvas, 2500L, true, CollageEngine.MODE_PHOTOWALL, 10, 1);
        assertTrue("Photowall draw must activate a decoded photo", engine.activePhotoCountForTest() > 0);

        int redPixels = countDominantRedPixels(target);
        assertTrue("Photowall must render visible source image pixels, redPixels=" + redPixels + ", maxRed=" + maxRed(target) + ", nonBackground=" + countNonBackgroundPixels(target), redPixels > 1200);
        assertTrue("Photowall must draw a light photo border", countLightBorderPixels(target) > 80);
    }

    @Test
    public void frameModeDrawsImagePixelsNotOnlyBackground() throws Exception {
        File folder = createImageFolder("frame", Color.rgb(40, 220, 70));
        Bitmap target = Bitmap.createBitmap(420, 640, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);

        CollageEngine engine = new CollageEngine(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                new SolidBitmapDecoder(Color.rgb(40, 220, 70)));
        engine.setSource(folder.getAbsolutePath(), "");
        assertTrue("Frame source folder must produce photos", engine.photoCountForTest() > 0);
        engine.draw(canvas, 1600L, true, CollageEngine.MODE_FRAME, 1, 1);

        int greenPixels = countDominantGreenPixels(target);
        assertTrue("Frame mode must render visible source image pixels, greenPixels=" + greenPixels, greenPixels > 20000);
    }

    @Test
    public void frameModeCrossfadesOldAndNewPhotos() throws Exception {
        File folder = createImageFolder("frame-crossfade", Color.rgb(230, 40, 40));
        Bitmap target = Bitmap.createBitmap(420, 640, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);

        CollageEngine engine = new CollageEngine(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                new SequenceBitmapDecoder());
        engine.setSource(folder.getAbsolutePath(), "");
        engine.draw(canvas, 1000L, true, CollageEngine.MODE_FRAME, 1, 1);
        engine.draw(canvas, 2500L, true, CollageEngine.MODE_FRAME, 1, 1);
        assertTrue("Crossfade must keep old image visible", countDominantRedPixels(target) > 5000);

        engine.draw(canvas, 3400L, true, CollageEngine.MODE_FRAME, 1, 1);
        assertTrue("Crossfade must fade in new image", countDominantGreenPixels(target) > 5000);
    }

    @Test
    public void frameModeExtendsDisplayUntilWidePhotoHasBeenPannedThrough() throws Exception {
        CollageEngine engine = new CollageEngine(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                new WideBitmapDecoder());
        Bitmap bitmap = Bitmap.createBitmap(1200, 600, Bitmap.Config.ARGB_8888);
        Method duration = CollageEngine.class.getDeclaredMethod("frameDisplayDurationMillis", Bitmap.class, int.class, int.class, int.class);
        Method rect = CollageEngine.class.getDeclaredMethod("frameRectForBitmap", Bitmap.class, int.class, int.class, long.class, long.class, long.class);
        duration.setAccessible(true);
        rect.setAccessible(true);

        long displayMs = (Long) duration.invoke(engine, bitmap, 600, 600, 5000);
        RectF start = (RectF) rect.invoke(engine, bitmap, 600, 600, 1000L, 1000L, displayMs);
        RectF end = (RectF) rect.invoke(engine, bitmap, 600, 600, 1000L + displayMs, 1000L, displayMs);

        assertTrue("Wide photo needs more time than the configured interval to show every horizontal segment", displayMs > 5000L);
        assertEquals("Pan starts at the left edge", 0.0f, start.left, 0.01f);
        assertTrue("Pan ends after moving to the right edge of the oversized bitmap", end.left < -590.0f);
    }

    @Test
    public void sequentialOrderDecodesPhotosByScannedOrder() throws Exception {
        File folder = createImageFolder("sequential-order", Color.rgb(230, 40, 40));
        createImageFile(folder, "a-second.png", Color.rgb(40, 220, 70));
        RecordingBitmapDecoder decoder = new RecordingBitmapDecoder();
        CollageEngine engine = new CollageEngine(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                decoder);
        Bitmap target = Bitmap.createBitmap(420, 640, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);

        engine.setSource(folder.getAbsolutePath(), "");
        engine.draw(canvas, 1000L, true, CollageEngine.MODE_PHOTOWALL, "sequential", 10, 1);
        engine.draw(canvas, 2500L, true, CollageEngine.MODE_PHOTOWALL, "sequential", 10, 1);

        assertEquals("a-second.png", decoder.names.get(0));
        assertEquals("source.png", decoder.names.get(1));
    }

    @Test
    public void decodeFailureDoesNotCrashPhotoWall() throws Exception {
        File folder = createImageFolder("oom-safe", Color.rgb(230, 40, 40));
        CollageEngine engine = new CollageEngine(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                new FailingBitmapDecoder());
        Bitmap target = Bitmap.createBitmap(420, 640, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);

        engine.setSource(folder.getAbsolutePath(), "");
        engine.draw(canvas, 1000L, true, CollageEngine.MODE_PHOTOWALL, 10, 1);

        assertEquals(0, engine.activePhotoCountForTest());
    }

    @Test
    public void lowMemoryDevicesUseSmallerPhotoWallLimit() {
        assertEquals(8, CollageEngine.maxVisibleForMemory(18, 48L * 1024L * 1024L));
        assertEquals(12, CollageEngine.maxVisibleForMemory(18, 96L * 1024L * 1024L));
        assertEquals(18, CollageEngine.maxVisibleForMemory(18, 192L * 1024L * 1024L));
    }

    private File createImageFolder(String name, int color) throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        File folder = new File(context.getCacheDir(), name + "-" + System.nanoTime());
        assertTrue(folder.mkdirs());
        File image = new File(folder, "source.png");
        createImageFile(folder, image.getName(), color);
        return folder;
    }

    private void createImageFile(File folder, String name, int color) throws Exception {
        File image = new File(folder, name);
        Bitmap bitmap = Bitmap.createBitmap(120, 180, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(color);
        FileOutputStream output = new FileOutputStream(image);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        } finally {
            output.close();
            bitmap.recycle();
        }
    }

    private static final class SolidBitmapDecoder implements BitmapDecoder {
        private final int color;

        SolidBitmapDecoder(int color) {
            this.color = color;
        }

        @Override
        public Bitmap decode(PhotoItem item, ContentResolver resolver, int maxWidth, int maxHeight) {
            Bitmap bitmap = Bitmap.createBitmap(120, 180, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(color);
            return bitmap;
        }
    }

    private static final class SequenceBitmapDecoder implements BitmapDecoder {
        private int index;

        @Override
        public Bitmap decode(PhotoItem item, ContentResolver resolver, int maxWidth, int maxHeight) {
            int color = index++ == 0 ? Color.rgb(230, 40, 40) : Color.rgb(40, 220, 70);
            Bitmap bitmap = Bitmap.createBitmap(120, 180, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(color);
            return bitmap;
        }
    }

    private static final class WideBitmapDecoder implements BitmapDecoder {
        @Override
        public Bitmap decode(PhotoItem item, ContentResolver resolver, int maxWidth, int maxHeight) {
            Bitmap bitmap = Bitmap.createBitmap(1200, 600, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.rgb(40, 120, 220));
            return bitmap;
        }
    }

    private static final class RecordingBitmapDecoder implements BitmapDecoder {
        final List<String> names = new ArrayList<String>();

        @Override
        public Bitmap decode(PhotoItem item, ContentResolver resolver, int maxWidth, int maxHeight) {
            names.add(item.name);
            Bitmap bitmap = Bitmap.createBitmap(120, 180, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.rgb(230, 40, 40));
            return bitmap;
        }
    }

    private static final class FailingBitmapDecoder implements BitmapDecoder {
        @Override
        public Bitmap decode(PhotoItem item, ContentResolver resolver, int maxWidth, int maxHeight) {
            throw new OutOfMemoryError("test bitmap pressure");
        }
    }

    private int countDominantRedPixels(Bitmap bitmap) {
        int count = 0;
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                int pixel = bitmap.getPixel(x, y);
                if (Color.red(pixel) > 140 && Color.green(pixel) < 120 && Color.blue(pixel) < 120) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countDominantGreenPixels(Bitmap bitmap) {
        int count = 0;
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                int pixel = bitmap.getPixel(x, y);
                if (Color.green(pixel) > 140 && Color.red(pixel) < 120 && Color.blue(pixel) < 120) {
                    count++;
                }
            }
        }
        return count;
    }

    private int maxRed(Bitmap bitmap) {
        int max = 0;
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                max = Math.max(max, Color.red(bitmap.getPixel(x, y)));
            }
        }
        return max;
    }

    private int countNonBackgroundPixels(Bitmap bitmap) {
        int background = Color.rgb(12, 14, 16);
        int count = 0;
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                if (bitmap.getPixel(x, y) != background) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countLightBorderPixels(Bitmap bitmap) {
        int count = 0;
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                int pixel = bitmap.getPixel(x, y);
                if (Color.red(pixel) > 190 && Color.green(pixel) > 185 && Color.blue(pixel) > 170) {
                    count++;
                }
            }
        }
        return count;
    }
}
