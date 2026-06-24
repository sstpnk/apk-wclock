package com.sstpnk.wclock.collage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PhotoScanner {
    private static final int MAX_SCAN_DEPTH = 4;
    private static final int MAX_IMAGES = 500;

    public List<PhotoItem> scan(File folder) {
        List<PhotoItem> result = new ArrayList<PhotoItem>();
        if (folder == null || !folder.isDirectory()) {
            return result;
        }
        scanInto(folder, 0, result);
        return result;
    }

    private void scanInto(File folder, int depth, List<PhotoItem> result) {
        if (result.size() >= MAX_IMAGES || depth > MAX_SCAN_DEPTH) {
            return;
        }
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
        List<File> sorted = Arrays.asList(files);
        Collections.sort(sorted);
        for (File file : sorted) {
            if (result.size() >= MAX_IMAGES) {
                return;
            }
            if (file.isFile() && isSupportedImage(file)) {
                result.add(new PhotoItem(file, file.lastModified()));
            } else if (file.isDirectory()) {
                scanInto(file, depth + 1, result);
            }
        }
    }

    public boolean isSupportedImage(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp");
    }
}
