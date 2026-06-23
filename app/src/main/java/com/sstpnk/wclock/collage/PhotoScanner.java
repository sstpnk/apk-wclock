package com.sstpnk.wclock.collage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PhotoScanner {
    public List<PhotoItem> scan(File folder) {
        List<PhotoItem> result = new ArrayList<PhotoItem>();
        if (folder == null || !folder.isDirectory()) {
            return result;
        }
        File[] files = folder.listFiles();
        if (files == null) {
            return result;
        }
        List<File> sorted = Arrays.asList(files);
        Collections.sort(sorted);
        for (File file : sorted) {
            if (file.isFile() && isSupportedImage(file)) {
                result.add(new PhotoItem(file, file.lastModified()));
            }
        }
        return result;
    }

    public boolean isSupportedImage(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp");
    }
}
