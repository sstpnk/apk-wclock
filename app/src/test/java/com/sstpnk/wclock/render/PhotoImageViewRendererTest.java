package com.sstpnk.wclock.render;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Looper;
import android.widget.ImageView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class PhotoImageViewRendererTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void frameDisplayWaitsUntilPanCanFinish() {
        PhotoImageViewRenderer renderer = new PhotoImageViewRenderer(ApplicationProvider.getApplicationContext());
        renderer.layout(0, 0, 600, 600);
        Bitmap bitmap = Bitmap.createBitmap(1200, 600, Bitmap.Config.RGB_565);

        long duration = renderer.frameDisplayDurationMillisForTest(bitmap, 5000L, 8);

        assertTrue("Wide frame should stay long enough for full pan", duration >= 70000L);
    }

    @Test
    public void repeatedFadeOutDoesNotRetireSamePhotoTwice() {
        PhotoImageViewRenderer renderer = new PhotoImageViewRenderer(ApplicationProvider.getApplicationContext());
        renderer.layout(0, 0, 600, 600);
        renderer.showWallBitmapForTest(Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565), 0, 0);

        renderer.fadeOutOldestViewForTest();
        renderer.fadeOutOldestViewForTest();

        assertEquals(0, renderer.activePhotoCountForTest());
        assertEquals(1, renderer.retiringPhotoCountForTest());
        assertEquals(1, renderer.getChildCount());
    }

    @Test
    public void expandedPhotoFrameFitsInsideViewWithoutStretching() {
        PhotoImageViewRenderer renderer = new PhotoImageViewRenderer(ApplicationProvider.getApplicationContext());
        renderer.layout(0, 0, 600, 400);
        Bitmap bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.RGB_565);

        RectF frame = renderer.expandedFrameForTest(bitmap);

        assertEquals(200.0f, frame.left, 0.01f);
        assertEquals(0.0f, frame.top, 0.01f);
        assertEquals(200.0f, frame.width(), 0.01f);
        assertEquals(400.0f, frame.height(), 0.01f);
    }

    @Test
    public void tappingPhotoWallPhotoEntersFocusedMode() {
        PhotoImageViewRenderer renderer = new PhotoImageViewRenderer(ApplicationProvider.getApplicationContext());
        renderer.layout(0, 0, 600, 600);
        renderer.showWallBitmapForTest(Bitmap.createBitmap(120, 80, Bitmap.Config.RGB_565), 0, 0);
        ImageView view = (ImageView) renderer.getChildAt(0);
        view.setTranslationX(0.0f);
        view.setAlpha(1.0f);

        boolean handled = renderer.handlePhotoWallTap(view.getX() + view.getWidth() * 0.5f, view.getY() + view.getHeight() * 0.5f);

        assertTrue(handled);
        assertTrue(renderer.focusedForTest());
    }

    @Test
    public void focusedPhotoAutoCollapsesAfterConfiguredTimeout() {
        PhotoImageViewRenderer renderer = new PhotoImageViewRenderer(ApplicationProvider.getApplicationContext());
        renderer.layout(0, 0, 600, 600);
        renderer.setDisplaySettings(true, "photowall", "random", 18, 5, 1, 20);
        renderer.showWallBitmapForTest(Bitmap.createBitmap(120, 80, Bitmap.Config.RGB_565), 0, 0);
        ImageView view = (ImageView) renderer.getChildAt(0);
        view.setTranslationX(0.0f);
        view.setAlpha(1.0f);

        renderer.handlePhotoWallTap(view.getX() + view.getWidth() * 0.5f, view.getY() + view.getHeight() * 0.5f);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(300, TimeUnit.MILLISECONDS);

        assertTrue(renderer.focusedForTest());

        Shadows.shadowOf(Looper.getMainLooper()).idleFor(500, TimeUnit.MILLISECONDS);
        renderer.setDisplaySettings(true, "photowall", "random", 18, 5, 1, 20);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(500, TimeUnit.MILLISECONDS);

        assertFalse(renderer.focusedForTest());
    }

    @Test
    public void focusedPhotoTimeoutZeroWaitsForExplicitTap() {
        PhotoImageViewRenderer renderer = new PhotoImageViewRenderer(ApplicationProvider.getApplicationContext());
        renderer.layout(0, 0, 600, 600);
        renderer.setDisplaySettings(true, "photowall", "random", 18, 5, 0, 20);
        renderer.showWallBitmapForTest(Bitmap.createBitmap(120, 80, Bitmap.Config.RGB_565), 0, 0);
        ImageView view = (ImageView) renderer.getChildAt(0);
        view.setTranslationX(0.0f);
        view.setAlpha(1.0f);

        renderer.handlePhotoWallTap(view.getX() + view.getWidth() * 0.5f, view.getY() + view.getHeight() * 0.5f);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(2, TimeUnit.SECONDS);

        assertTrue(renderer.focusedForTest());

        renderer.handlePhotoWallTap(0.0f, 0.0f);

        assertFalse(renderer.focusedForTest());
    }

    @Test
    public void recycleAllowsSameSourceToBeLoadedAgain() throws Exception {
        File folder = temporaryFolder.newFolder("photos");
        createImageFile(folder, "source.png");
        PhotoImageViewRenderer renderer = new PhotoImageViewRenderer(ApplicationProvider.getApplicationContext());

        renderer.setPhotoSource(folder.getAbsolutePath(), "");
        assertEquals(1, renderer.sourcePhotoCountForTest());

        renderer.recycle();
        assertEquals(0, renderer.sourcePhotoCountForTest());

        renderer.setPhotoSource(folder.getAbsolutePath(), "");
        assertEquals(1, renderer.sourcePhotoCountForTest());
    }

    private void createImageFile(File folder, String name) throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.rgb(230, 40, 40));
        FileOutputStream output = new FileOutputStream(new File(folder, name));
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        } finally {
            output.close();
            bitmap.recycle();
        }
    }
}
