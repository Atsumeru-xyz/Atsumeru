package com.atsumeru.web.util;

import com.atsumeru.web.helper.Constants;
import com.atsumeru.web.util.comparator.AlphanumComparator;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FileUtils {
    public static final String[] ALLOWED_IMAGE_EXTENSIONS = new String[]{
            Constants.BMP,
            Constants.GIF,
            Constants.JPEG,
            Constants.JPG,
            Constants.PNG,
            Constants.WEBP
    };

    /**
     * Write given {@link String} into {@link File}
     *
     * @param filePath destination {@link File} path
     * @param content  input {@link String}
     * @return true if write was success
     */
    public static boolean writeStringToFile(String filePath, String content) {
        return writeStringToFile(new File(filePath), content);
    }

    /**
     * Write given {@link String} into {@link File}
     *
     * @param file    destination {@link File}
     * @param content input {@link String}
     * @return true if write was success
     */
    public static boolean writeStringToFile(File file, String content) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(content);
            printWriter.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(fileWriter);
        }
    }

    /**
     * Get list of all {@link File} from given directory with filtering by extension
     *
     * @param directoryPath     directory path
     * @param allowedExtensions array of extensions for filtering. May be null. In that case, {@link FileUtils#ALLOWED_IMAGE_EXTENSIONS}
     *                          will be used
     * @param recursive         set recursive mode
     * @return {@link List<File>} with files
     */
    public static List<File> getAllFilesFromDirectory(String directoryPath, String[] allowedExtensions, boolean recursive) {
        allowedExtensions = Optional.ofNullable(allowedExtensions).orElse(ALLOWED_IMAGE_EXTENSIONS);

        File file = new File(directoryPath);
        if (file.isDirectory()) {
            List<File> images = (List<File>) org.apache.commons.io.FileUtils.listFiles(file, allowedExtensions, recursive);
            if (ArrayUtils.isNotEmpty(images)) {
                images.sort((file1, file2) -> AlphanumComparator.compareStrings(file1.getName(), file2.getName()));
            }
            return images;
        }

        return new ArrayList<>();
    }

    /**
     * Add path slash {@link File#separator} into end of path if needed
     *
     * @param str input path
     * @return path with slash in end
     */
    public static String addPathSlash(String str) {
        if (StringUtils.isNotEmpty(str) && !str.endsWith("/") && !str.endsWith(File.separator)) {
            return str + File.separator;
        }
        return str;
    }

    /**
     * Remove path slash {@link File#separator} from end of path if needed
     *
     * @param str input path
     * @return path without slash in end
     */
    public static String removeLastPathSlash(String str) {
        if (StringUtils.isNotEmpty(str) && (str.endsWith("/") || str.endsWith(File.separator))) {
            return str.substring(0, str.length() - 1);
        }
        return str;
    }

    /**
     * Closes {@link Closeable} quietly
     *
     * @param closeable {@link Closeable} for close
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Check if {@link File} directory is empty
     *
     * @param file input {@link File}
     * @return true if empty
     */
    public static boolean isDirectoryEmpty(File file) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(file.toPath())) {
            return !dirStream.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Check is {@link File} exists
     *
     * @param file {@link File} for checking
     * @return true if exists
     */
    public static boolean isFileExist(File file) {
        return file != null && file.exists();
    }

    /**
     * Check is {@link File} is Directory
     *
     * @param file {@link File} for checking
     * @return true if file is directory
     */
    public static boolean isDirectory(File file) {
        return isFileExist(file) && file.isDirectory();
    }

    /**
     * Check is {@link File} is actual File
     *
     * @param file {@link File} for checking
     * @return true if file is file
     */
    public static boolean isFile(File file) {
        return isFileExist(file) && file.isFile();
    }

    /**
     * Deletes directory with files
     *
     * @param directory {@link File} directory file
     * @return true if success
     */
    public static boolean deleteDirectory(File directory) {
        boolean result = false;
        if (directory != null && directory.exists()) {
            result = true;
            File[] listFiles = directory.listFiles();
            if (ArrayUtils.isNotEmpty(listFiles)) {
                int i = 0;
                while (i < listFiles.length) {
                    File file = listFiles[i];
                    result = file.isDirectory() ? result && FileUtils.deleteDirectory(file) : result && file.delete();
                    i = i + 1;
                }
            }
            result = result && directory.delete();
        }
        return result;
    }

    /**
     * Get directory name from {@link String} path
     *
     * @param path directory path
     * @return {@link String} directory name
     */
    public static String getCurrentDirName(String path) {
        path = removeLastPathSlash(path);
        int indexSeparator = path.lastIndexOf(File.separator);
        int indexSlash = path.lastIndexOf("/");

        if (indexSeparator < 0 && indexSlash < 0) {
            return null;
        }

        if (indexSeparator != path.length() - 1 && indexSlash != path.length() - 1) {
            return path.substring(Math.max(indexSeparator, indexSlash) + 1);
        }
        return path;
    }

    /**
     * Returns directory name from provided {@link String} path
     *
     * @param path provided {@link String} path
     * @return directory name. Example: /some_dir/my-dir -> my-dir
     */
    public static String getDirName(String path) {
        int indexSeparator = path.lastIndexOf(File.separator);
        int indexSlash = path.lastIndexOf("/");

        if (indexSeparator < 0 && indexSlash < 0) {
            return null;
        }

        return path.substring(0, Math.max(indexSeparator, indexSlash));
    }

    /**
     * Get path from {@link String} path without slash in the end
     *
     * @param path path
     * @return {@link String} path without slash in the end or empty {@link String}
     */
    public static String getPath(String path) {
        int indexSlash = path.lastIndexOf("\\");
        int indexBackSlash = path.lastIndexOf("/");

        if (indexSlash >= 0) {
            return path.substring(0, indexSlash);
        } else if (indexBackSlash >= 0) {
            return path.substring(0, indexBackSlash);
        }

        return "";
    }

    /**
     * Returns {@link String} name without extension from provided {@link String} path
     *
     * @param path provided {@link String} path
     * @return {@link String} name without extension. Example: /some_dir/some_file.jpg -> some_file
     */
    public static String getFileName(String path) {
        int indexSlash = Math.max(path.lastIndexOf("/"), path.lastIndexOf(File.separator));
        int indexDot = path.lastIndexOf(".");

        if (indexSlash >= 0 && indexDot > indexSlash) {
            return path.substring(indexSlash + 1, indexDot);
        }
        if (indexSlash >= 0) {
            return path.substring(indexSlash + 1);
        }
        if (indexDot >= 0) {
            return path.substring(0, indexDot);
        }

        return path;
    }

    /**
     * Returns {@link String} name with extension from provided {@link String} path
     *
     * @param path provided {@link String} path
     * @return {@link String} name without extension. Example: /some_dir/some_file.jpg -> some_file
     */
    public static String getFileNameWithExt(String path) {
        return getFileNameWithExt(path, false);
    }

    /**
     * Returns {@link String} name with extension from provided {@link String} path
     *
     * @param path        provided {@link String} path
     * @param isLocalFile indicates that path if from FS
     * @return {@link String} name with extension. Example: /some_dir/some_file.jpg -> some_file.jpg
     */
    public static String getFileNameWithExt(String path, boolean isLocalFile) {
        int indexSlash = path.lastIndexOf(!isLocalFile ? "/" : File.separator);
        if (indexSlash >= 0) {
            return path.substring(indexSlash + 1);
        } else {
            return path;
        }
    }

    /**
     * Returns {@link File} extension from {@link String} path
     *
     * @param path provided {@link String} path
     * @return {@link File} extension. Example: logo.jpg -> jpg
     */
    public static String getFileExt(String path) {
        int indexSlash = Math.max(path.lastIndexOf("/"), path.lastIndexOf(File.separator));
        int indexDot = path.lastIndexOf(".");

        if (indexSlash >= 0 && indexDot >= 0 && indexSlash < indexDot || indexSlash < 0 && indexDot >= 0) {
            return path.substring(indexDot + 1);
        } else {
            return "";
        }
    }
}