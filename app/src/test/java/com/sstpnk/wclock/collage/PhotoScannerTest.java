package com.sstpnk.wclock.collage;

import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PhotoScannerTest {
    @Test
    public void acceptsCommonImageExtensions() {
        PhotoScanner scanner = new PhotoScanner();
        assertEquals(true, scanner.isSupportedImage(new File("a.jpg")));
        assertEquals(true, scanner.isSupportedImage(new File("a.jpeg")));
        assertEquals(true, scanner.isSupportedImage(new File("a.png")));
        assertEquals(true, scanner.isSupportedImage(new File("a.webp")));
        assertEquals(false, scanner.isSupportedImage(new File("a.txt")));
    }

    @Test
    public void missingFolderReturnsEmptyList() {
        PhotoScanner scanner = new PhotoScanner();
        List<PhotoItem> items = scanner.scan(new File("Z:/path/that/does/not/exist"));
        assertEquals(0, items.size());
    }
}
