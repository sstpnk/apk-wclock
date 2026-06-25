package com.sstpnk.wclock.collage;

import android.graphics.Bitmap;
import android.media.ExifInterface;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class BitmapLoaderTest {
    @Test
    public void rotateForOrientationSwapsDimensionsForExifRotate90() {
        Bitmap source = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888);
        Bitmap rotated = new BitmapLoader().rotateForOrientation(source, ExifInterface.ORIENTATION_ROTATE_90);

        assertEquals(80, rotated.getWidth());
        assertEquals(120, rotated.getHeight());
    }
}
