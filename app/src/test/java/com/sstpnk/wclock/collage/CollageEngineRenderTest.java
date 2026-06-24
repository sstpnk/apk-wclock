package com.sstpnk.wclock.collage;

import android.content.Context;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.FileOutputStream;

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
        assertTrue("Photowall draw must activate a decoded photo", engine.activePhotoCountForTest() > 0);

        int redPixels = countDominantRedPixels(target);
        assertTrue("Photowall must render visible source image pixels, redPixels=" + redPixels + ", maxRed=" + maxRed(target) + ", nonBackground=" + countNonBackgroundPixels(target), redPixels > 1200);
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

    private File createImageFolder(String name, int color) throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        File folder = new File(context.getCacheDir(), name + "-" + System.nanoTime());
        assertTrue(folder.mkdirs());
        File image = new File(folder, "source.png");
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
        return folder;
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
}
