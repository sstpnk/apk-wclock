package com.sstpnk.wclock;

import org.junit.Test;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class IconAssetTest {
    @Test
    public void launcherIconsUseTransparentCorners() throws Exception {
        File resDir = findMainResDir();
        File[] densityDirs = resDir.listFiles();
        assertTrue("Main res directory must contain mipmap icon densities", densityDirs != null && densityDirs.length > 0);

        int checked = 0;
        for (File dir : densityDirs) {
            if (!dir.getName().startsWith("mipmap-")) {
                continue;
            }
            File icon = new File(dir, "ic_launcher.png");
            if (!icon.isFile()) {
                continue;
            }
            Bitmap image = BitmapFactory.decodeFile(icon.getAbsolutePath());
            assertTrue("Launcher icon must be readable: " + icon, image != null);
            assertTrue("Top-left corner must be transparent for shaped launcher icon: " + icon, alphaAt(image, 0, 0) < 12);
            assertTrue("Top-right corner must be transparent for shaped launcher icon: " + icon, alphaAt(image, image.getWidth() - 1, 0) < 12);
            assertTrue("Bottom-left corner must be transparent for shaped launcher icon: " + icon, alphaAt(image, 0, image.getHeight() - 1) < 12);
            assertTrue("Bottom-right corner must be transparent for shaped launcher icon: " + icon, alphaAt(image, image.getWidth() - 1, image.getHeight() - 1) < 12);
            image.recycle();
            checked++;
        }

        assertTrue("At least one launcher icon density must be checked", checked > 0);
    }

    private File findMainResDir() {
        File direct = new File("src/main/res");
        if (direct.isDirectory()) {
            return direct;
        }
        return new File("app/src/main/res");
    }

    private int alphaAt(Bitmap image, int x, int y) {
        return (image.getPixel(x, y) >>> 24) & 0xFF;
    }
}
