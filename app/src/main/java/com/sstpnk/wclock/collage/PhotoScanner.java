package com.sstpnk.wclock.collage;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

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

    public List<PhotoItem> scanTree(ContentResolver resolver, Uri treeUri) {
        List<PhotoItem> result = new ArrayList<PhotoItem>();
        if (resolver == null || treeUri == null) {
            return result;
        }
        try {
            String rootId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri rootDocument = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId);
            scanDocumentChildren(resolver, treeUri, rootDocument, 0, result);
        } catch (Exception ignored) {
        }
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

    private void scanDocumentChildren(ContentResolver resolver, Uri treeUri, Uri documentUri, int depth, List<PhotoItem> result) {
        if (result.size() >= MAX_IMAGES || depth > MAX_SCAN_DEPTH) {
            return;
        }
        String documentId = DocumentsContract.getDocumentId(documentUri);
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId);
        Cursor cursor = null;
        try {
            cursor = resolver.query(childrenUri,
                    new String[]{
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                            DocumentsContract.Document.COLUMN_MIME_TYPE,
                            DocumentsContract.Document.COLUMN_LAST_MODIFIED
                    },
                    null,
                    null,
                    null);
            if (cursor == null) {
                return;
            }
            while (cursor.moveToNext() && result.size() < MAX_IMAGES) {
                String childId = cursor.getString(0);
                String name = cursor.getString(1);
                String mime = cursor.getString(2);
                long modified = cursor.getLong(3);
                Uri childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId);
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                    scanDocumentChildren(resolver, treeUri, childUri, depth + 1, result);
                } else if (isSupportedDocumentImage(name, mime)) {
                    result.add(new PhotoItem(childUri, name, modified));
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean isSupportedDocumentImage(String name, String mime) {
        if (mime != null && mime.toLowerCase().startsWith("image/")) {
            return true;
        }
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp");
    }
}
