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
import java.util.Random;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

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
        engine.draw(canvas, 3900L, true, CollageEngine.MODE_PHOTOWALL, 10, 1);
        assertTrue("Photowall draw must activate a decoded photo", engine.activePhotoCountForTest() > 0);

        int redPixels = countDominantRedPixels(target);
        assertTrue("Photowall must render visible source image pixels, redPixels=" + redPixels + ", maxRed=" + maxRed(target) + ", nonBackground=" + countNonBackgroundPixels(target), redPixels > 1200);
        assertTrue("Photowall must draw a light photo border", countLightBorderPixels(target) > 80);
    }

    @Test
    public void photoWallDoesNotBlockDrawWhileNextBitmapIsDecoded() throws Exception {
        File folder = createImageFolder("photowall-nonblocking", Color.rgb(230, 40, 40));
        final BlockingBitmapDecoder decoder = new BlockingBitmapDecoder(Color.rgb(230, 40, 40));
        final CollageEngine engine = asyncEngine(decoder);
        final Bitmap target = Bitmap.createBitmap(420, 640, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(target);
        final CountDownLatch drawReturned = new CountDownLatch(1);

        engine.setSource(folder.getAbsolutePath(), "");
        Thread drawThread = new Thread(new Runnable() {
            @Override
            public void run() {
                engine.draw(canvas, 1000L, true, CollageEngine.MODE_PHOTOWALL, 10, 1);
                drawReturned.countDown();
            }
        }, "test-photowall-draw");
        drawThread.start();

        assertTrue("Decoder should be asked for the next bitmap", decoder.decodeStarted.await(1, TimeUnit.SECONDS));
        assertTrue("draw() must not wait for image decode to finish", drawReturned.await(150, TimeUnit.MILLISECONDS));

        decoder.releaseDecode.countDown();
        drawThread.join(1000L);
        engine.recycle();
    }

    @Test
    public void photoWallAddsPreparedBitmapOnLaterFrame() throws Exception {
        File folder = createImageFolder("photowall-prefetch", Color.rgb(230, 40, 40));
        BlockingBitmapDecoder decoder = new BlockingBitmapDecoder(Color.rgb(230, 40, 40));
        CollageEngine engine = asyncEngine(decoder);
        Bitmap target = Bitmap.createBitmap(420, 640, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);

        engine.setSource(folder.getAbsolutePath(), "");
        engine.draw(canvas, 1000L, true, CollageEngine.MODE_PHOTOWALL, 10, 1);
        assertEquals("First draw should schedule decode but keep the frame unblocked", 0, engine.activePhotoCountForTest());

        assertTrue(decoder.decodeStarted.await(1, TimeUnit.SECONDS));
        decoder.releaseDecode.countDown();
        assertTrue(decoder.decodeFinished.await(1, TimeUnit.SECONDS));

        long deadline = System.currentTimeMillis() + 1000L;
        long frameTime = 1100L;
        while (engine.activePhotoCountForTest() == 0 && System.currentTimeMillis() < deadline) {
            engine.draw(canvas, frameTime, true, CollageEngine.MODE_PHOTOWALL, 10, 1);
            frameTime += 33L;
            Thread.sleep(10L);
        }
        assertEquals("Prepared bitmap should become active on a later frame", 1, engine.activePhotoCountForTest());
        engine.recycle();
    }

    @Test
    public void photoWallPrefetchesNextBitmapBeforeDisplayIntervalElapses() throws Exception {
        File folder = createImageFolder("photowall-prefetch-early", Color.rgb(230, 40, 40));
        createImageFile(folder, "a-second.png", Color.rgb(40, 220, 70));
        RecordingBitmapDecoder decoder = new RecordingBitmapDecoder();
        CollageEngine engine = new CollageEngine(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                decoder);
        Bitmap target = Bitmap.createBitmap(420, 640, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);

        engine.setSource(folder.getAbsolutePath(), "");
        engine.draw(canvas, 1000L, true, CollageEngine.MODE_PHOTOWALL, "sequential", 10, 5);
        engine.draw(canvas, 1100L, true, CollageEngine.MODE_PHOTOWALL, "sequential", 10, 5);

        assertEquals("First draw should prepare the first bitmap and second draw should prefetch the next one", 2, decoder.names.size());
        assertEquals("a-second.png", decoder.names.get(0));
        assertEquals("source.png", decoder.names.get(1));
        assertEquals("Only the first prepared bitmap should be visible before the configured interval", 1, engine.activePhotoCountForTest());
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
    public void frameModeDoesNotBlockDrawWhileBitmapIsDecoded() throws Exception {
        File folder = createImageFolder("frame-nonblocking", Color.rgb(40, 220, 70));
        final BlockingBitmapDecoder decoder = new BlockingBitmapDecoder(Color.rgb(40, 220, 70));
        final CollageEngine engine = asyncEngine(decoder);
        final Bitmap target = Bitmap.createBitmap(420, 640, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(target);
        final CountDownLatch drawReturned = new CountDownLatch(1);

        engine.setSource(folder.getAbsolutePath(), "");
        Thread drawThread = new Thread(new Runnable() {
            @Override
            public void run() {
                engine.draw(canvas, 1000L, true, CollageEngine.MODE_FRAME, 1, 1);
                drawReturned.countDown();
            }
        }, "test-frame-draw");
        drawThread.start();

        assertTrue("Decoder should be asked for the frame bitmap", decoder.decodeStarted.await(1, TimeUnit.SECONDS));
        assertTrue("frame draw() must not wait for image decode to finish", drawReturned.await(150, TimeUnit.MILLISECONDS));

        decoder.releaseDecode.countDown();
        drawThread.join(1000L);
        engine.recycle();
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
        engine.draw(canvas, 2700L, true, CollageEngine.MODE_FRAME, 1, 1);
        assertTrue("Crossfade must keep old image visible", countDominantRedPixels(target) > 5000);

        engine.draw(canvas, 4501L, true, CollageEngine.MODE_FRAME, 1, 1);
        assertTrue("Crossfade must fade in new image", countDominantGreenPixels(target) > 5000);
    }

    @Test
    public void frameModeExtendsDisplayUntilWidePhotoHasBeenPannedThrough() throws Exception {
        CollageEngine engine = new CollageEngine(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                new WideBitmapDecoder());
        Bitmap bitmap = Bitmap.createBitmap(1200, 600, Bitmap.Config.ARGB_8888);
        Method duration = CollageEngine.class.getDeclaredMethod("frameDisplayDurationMillis", Bitmap.class, int.class, int.class, int.class, int.class);
        Method rect = CollageEngine.class.getDeclaredMethod("frameRectForBitmap", Bitmap.class, int.class, int.class, long.class, long.class, long.class);
        duration.setAccessible(true);
        rect.setAccessible(true);

        long displayMs = (Long) duration.invoke(engine, bitmap, 600, 600, 5000, 20);
        RectF start = (RectF) rect.invoke(engine, bitmap, 600, 600, 1000L, 1000L, displayMs);
        RectF end = (RectF) rect.invoke(engine, bitmap, 600, 600, 1000L + displayMs, 1000L, displayMs);

        assertTrue("Wide photo needs more time than the configured interval to show every horizontal segment", displayMs > 5000L);
        assertEquals("Pan starts at the left edge", 0.0f, start.left, 0.01f);
        assertTrue("Pan ends after moving to the right edge of the oversized bitmap", end.left < -590.0f);
    }

    @Test
    public void frameModeUsesEasedSlowPanForOversizedPhotos() throws Exception {
        CollageEngine engine = new CollageEngine(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                new WideBitmapDecoder());
        Bitmap bitmap = Bitmap.createBitmap(1200, 600, Bitmap.Config.ARGB_8888);
        Method duration = CollageEngine.class.getDeclaredMethod("frameDisplayDurationMillis", Bitmap.class, int.class, int.class, int.class, int.class);
        Method progress = CollageEngine.class.getDeclaredMethod("frameProgress", long.class, long.class, long.class);
        duration.setAccessible(true);
        progress.setAccessible(true);

        long displayMs = (Long) duration.invoke(engine, bitmap, 600, 600, 5000, 8);
        float quarter = (Float) progress.invoke(engine, 1000L + displayMs / 4L, 1000L, displayMs);
        float half = (Float) progress.invoke(engine, 1000L + displayMs / 2L, 1000L, displayMs);

        assertTrue("Wide pan should be slow enough for sub-pixel frame-to-frame movement", displayMs >= 70000L);
        assertEquals("Pan should use a stable constant speed through the frame display", 0.25f, quarter, 0.02f);
        assertEquals("Pan midpoint should still reach the center of the image", 0.50f, half, 0.03f);
    }

    @Test
    public void framePanSpeedControlsDisplayDuration() throws Exception {
        CollageEngine engine = new CollageEngine(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                new WideBitmapDecoder());
        Bitmap bitmap = Bitmap.createBitmap(1200, 600, Bitmap.Config.ARGB_8888);
        Method duration = CollageEngine.class.getDeclaredMethod("frameDisplayDurationMillis", Bitmap.class, int.class, int.class, int.class, int.class);
        duration.setAccessible(true);

        long slow = (Long) duration.invoke(engine, bitmap, 600, 600, 5000, 8);
        long fast = (Long) duration.invoke(engine, bitmap, 600, 600, 5000, 32);

        assertTrue("Faster configured pan speed should shorten the frame display duration", fast < slow / 2);
        assertTrue("Fast pan should still respect the configured minimum interval", fast >= 5000L);
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
        engine.draw(canvas, 3500L, true, CollageEngine.MODE_PHOTOWALL, "sequential", 10, 1);

        assertEquals("a-second.png", decoder.names.get(0));
        assertEquals("source.png", decoder.names.get(1));
    }

    @Test
    public void randomOrderAvoidsPhotosAlreadyOnScreenWhenAlternativesExist() throws Exception {
        File folder = createImageFolder("random-unique", Color.rgb(230, 40, 40));
        createImageFile(folder, "a-first.png", Color.rgb(40, 220, 70));
        createImageFile(folder, "b-second.png", Color.rgb(40, 120, 220));
        RecordingBitmapDecoder decoder = new RecordingBitmapDecoder();
        CollageEngine engine = new CollageEngine(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                decoder,
                new ConstantZeroRandom());
        Bitmap target = Bitmap.createBitmap(420, 640, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);

        engine.setSource(folder.getAbsolutePath(), "");
        engine.draw(canvas, 1000L, true, CollageEngine.MODE_PHOTOWALL, "random", 10, 1);
        engine.draw(canvas, 2500L, true, CollageEngine.MODE_PHOTOWALL, "random", 10, 1);
        engine.draw(canvas, 4000L, true, CollageEngine.MODE_PHOTOWALL, "random", 10, 1);
        engine.draw(canvas, 5500L, true, CollageEngine.MODE_PHOTOWALL, "random", 10, 1);
        engine.draw(canvas, 7000L, true, CollageEngine.MODE_PHOTOWALL, "random", 10, 1);

        assertEquals("a-first.png", decoder.names.get(0));
        assertEquals("b-second.png", decoder.names.get(1));
        assertEquals("source.png", decoder.names.get(2));
    }

    @Test
    public void randomOrderAllowsRepeatsWhenEveryPhotoIsAlreadyOnScreen() throws Exception {
        File folder = createImageFolder("random-repeat-small-folder", Color.rgb(230, 40, 40));
        RecordingBitmapDecoder decoder = new RecordingBitmapDecoder();
        CollageEngine engine = new CollageEngine(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                decoder,
                new ConstantZeroRandom());
        Bitmap target = Bitmap.createBitmap(420, 640, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);

        engine.setSource(folder.getAbsolutePath(), "");
        engine.draw(canvas, 1000L, true, CollageEngine.MODE_PHOTOWALL, "random", 10, 1);
        engine.draw(canvas, 2500L, true, CollageEngine.MODE_PHOTOWALL, "random", 10, 1);
        engine.draw(canvas, 3500L, true, CollageEngine.MODE_PHOTOWALL, "random", 10, 1);

        assertEquals("source.png", decoder.names.get(0));
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

    @Test
    public void expiredPhotoWallBitmapIsRetainedBrieflyBeforeRecycle() throws Exception {
        File folder = createImageFolder("photowall-retired-bitmap", Color.rgb(230, 40, 40));
        RecordingBitmapDecoder decoder = new RecordingBitmapDecoder();
        CollageEngine engine = new CollageEngine(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                decoder);
        Bitmap target = Bitmap.createBitmap(420, 640, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);

        engine.setSource(folder.getAbsolutePath(), "");
        engine.draw(canvas, 1000L, true, CollageEngine.MODE_PHOTOWALL, "sequential", 1, 1);
        engine.draw(canvas, 2000L, true, CollageEngine.MODE_PHOTOWALL, "sequential", 1, 1);
        Bitmap firstBitmap = decoder.bitmaps.get(0);

        engine.draw(canvas, 7001L, true, CollageEngine.MODE_PHOTOWALL, "sequential", 1, 1);
        assertTrue("Expired bitmap should stay valid for the hardware renderer's pending frame", !firstBitmap.isRecycled());

        engine.draw(canvas, 9102L, true, CollageEngine.MODE_PHOTOWALL, "sequential", 1, 1);
        assertTrue("Retired bitmap should be recycled after the render pipeline grace window", firstBitmap.isRecycled());
    }

    @Test
    public void photoWallActivatesPreparedReplacementWhenFullPhotoExpires() throws Exception {
        File folder = createImageFolder("photowall-full-replacement", Color.rgb(230, 40, 40));
        createImageFile(folder, "a-second.png", Color.rgb(40, 220, 70));
        RecordingBitmapDecoder decoder = new RecordingBitmapDecoder();
        CollageEngine engine = new CollageEngine(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                decoder);
        Bitmap target = Bitmap.createBitmap(420, 640, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);

        engine.setSource(folder.getAbsolutePath(), "");
        engine.draw(canvas, 1000L, true, CollageEngine.MODE_PHOTOWALL, "sequential", 1, 1);
        engine.draw(canvas, 2000L, true, CollageEngine.MODE_PHOTOWALL, "sequential", 1, 1);
        assertEquals("One photo should be visible while the wall is at capacity", 1, engine.activePhotoCountForTest());

        engine.draw(canvas, 6101L, true, CollageEngine.MODE_PHOTOWALL, "sequential", 1, 1);

        assertEquals("Expired full-wall photo should be replaced without an empty frame", 1, engine.activePhotoCountForTest());
        assertTrue("Replacement photo should have been prefetched before the old photo expired", decoder.names.size() >= 2);
    }

    private File createImageFolder(String name, int color) throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        File folder = new File(context.getCacheDir(), name + "-" + System.nanoTime());
        assertTrue(folder.mkdirs());
        File image = new File(folder, "source.png");
        createImageFile(folder, image.getName(), color);
        return folder;
    }

    private CollageEngine asyncEngine(BitmapDecoder decoder) {
        return new CollageEngine(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                decoder,
                new Random(),
                new Executor() {
                    @Override
                    public void execute(Runnable command) {
                        Thread thread = new Thread(command, "test-photo-decode");
                        thread.setDaemon(true);
                        thread.start();
                    }
                });
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

    private static final class BlockingBitmapDecoder implements BitmapDecoder {
        final CountDownLatch decodeStarted = new CountDownLatch(1);
        final CountDownLatch releaseDecode = new CountDownLatch(1);
        final CountDownLatch decodeFinished = new CountDownLatch(1);
        private final int color;

        BlockingBitmapDecoder(int color) {
            this.color = color;
        }

        @Override
        public Bitmap decode(PhotoItem item, ContentResolver resolver, int maxWidth, int maxHeight) {
            decodeStarted.countDown();
            try {
                releaseDecode.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            Bitmap bitmap = Bitmap.createBitmap(120, 180, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(color);
            decodeFinished.countDown();
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
        final List<Bitmap> bitmaps = new ArrayList<Bitmap>();

        @Override
        public Bitmap decode(PhotoItem item, ContentResolver resolver, int maxWidth, int maxHeight) {
            names.add(item.name);
            Bitmap bitmap = Bitmap.createBitmap(120, 180, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.rgb(230, 40, 40));
            bitmaps.add(bitmap);
            return bitmap;
        }
    }

    private static final class FailingBitmapDecoder implements BitmapDecoder {
        @Override
        public Bitmap decode(PhotoItem item, ContentResolver resolver, int maxWidth, int maxHeight) {
            throw new OutOfMemoryError("test bitmap pressure");
        }
    }

    private static final class ConstantZeroRandom extends Random {
        @Override
        public int nextInt(int bound) {
            return 0;
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
