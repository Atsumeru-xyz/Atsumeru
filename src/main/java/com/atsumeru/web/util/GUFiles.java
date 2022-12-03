package com.atsumeru.web.util;

import java.io.File;

public class GUFiles {

    /**
     * Deletes provided directory
     * @param dir directory for deletion
     */
    public static void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            } else {
                file.delete();
            }
        }
        dir.delete();
    }
}
