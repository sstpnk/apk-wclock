package com.sstpnk.wclock.render;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.widget.ImageView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class PhotoImageViewRendererTest {
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
}
