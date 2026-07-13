package com.sstpnk.wclock.render;

import android.graphics.Bitmap;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

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
}
