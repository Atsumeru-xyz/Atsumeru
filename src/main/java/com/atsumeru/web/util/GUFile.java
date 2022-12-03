package com.atsumeru.web.util;

import java.io.*;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class GUFile {
    final private static String ILLEGAL_CHARACTERS = "/\n\r\t\0\f`?*\\<>|\"”:.# ";
    public static int MAX_FILE_LENGTH = 20 + (Integer.MIN_VALUE + "").length();
    private static final int BUFFER_SIZE = 1024;
    private static final int BATCH_SIZE = BUFFER_SIZE * 100;

    public GUFile() {
        super();
    }

    public static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    /**
     * Add path slash {@link File#separator} into end of path if needed
     * @param str input path
     * @return path with slash in end
     */
    public static String addPathSlash(String str) {
        if (GUString.isNotEmpty(str) && !str.endsWith("/") && !str.endsWith(File.separator)) {
            return str + File.separator;
        }
        return str;
    }

    public static String removeLastPathSlash(String str) {
        if (GUString.isNotEmpty(str) && (str.endsWith("/") || str.endsWith(File.separator))) {
            return str.substring(0, str.length() - 1);
        }
        return str;
    }

    /**
     * Add path slash {@link File#separator} into start of path if needed
     * @param str input path
     * @return path with slash in start
     */
    public static String addPathSlashToBegin(String str) {
        if (GUString.isNotEmpty(str) && !str.startsWith("/") && !str.startsWith(File.separator)) {
            return File.separator + str;
        }
        return str;
    }

    /**
     * Closes {@link Closeable} quietly
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

    public static boolean isDirectoryEmpty(File file) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(file.toPath())) {
            return !dirStream.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Check is {@link File} exists
     * @param file {@link File} for checking
     * @return true if exists
     */
    public static boolean isFileExist(File file) {
        return file != null && file.exists();
    }

    /**
     * Check is {@link File} is Directory
     * @param file {@link File} for checking
     * @return true if file is Directory
     */
    public static boolean isDirectory(File file) {
        return isFileExist(file) && file.isDirectory();
    }

    public static boolean isFile(File file) {
        return isFileExist(file) && file.isFile();
    }

    /**
     * Copies content of {@link InputStream} into {@link OutputStream}
     * @param in {@link InputStream} from which need copy content
     * @param out {@link OutputStream} into which need write content
     * @return length of copied content
     * @throws IOException
     */
    public static long copy(InputStream in, OutputStream out) throws IOException {
        return copy(in, out, BUFFER_SIZE);
    }

    /**
     * Copies content of {@link InputStream} into {@link OutputStream}
     * @param in {@link InputStream} from which need copy content
     * @param out {@link OutputStream} into which need write content
     * @param bufferLength {@link Integer} buffer size
     * @return length of copied content
     * @throws IOException
     */
    public static long copy(InputStream in, OutputStream out, int bufferLength) throws IOException {
        byte[] buffer = new byte[bufferLength];
        long readed = 0L;
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            readed = readed + (long) read;
        }
        out.flush();
        return readed;
    }

    public static long copy(InputStream in, OutputStream out, OnBatchCopyListener listener) throws IOException {
        if (listener == null)
            return copy(in, out);

        long result = 0;
        long next = BATCH_SIZE;
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            result += read;
            if (result >= next) {
                listener.onCopy(result);
                next += BATCH_SIZE;
            }
        }
        return result;
    }

    /**
     * Writes content of {@link InputStream} into {@link File} by path
     * @param in {@link InputStream} from which need copy content
     * @param toPath {@link String} path to destination file
     * @param isNeedReplace {@link Boolean} indicates that need to do file rewrite if it exists
     * @return true if success
     */
    private static boolean copy(InputStream in, String toPath, boolean isNeedReplace) {
        try {
            if (in == null || in.available() < 1) {
                return false;
            }
            File file = new File(toPath);
            if (file.exists() && !isNeedReplace) {
                return true;
            }
            if (!file.exists() && !file.createNewFile()) {
                return false;
            }
            OutputStream out = new FileOutputStream(toPath);
            copy(in, out);
            in.close();
            out.flush();
            out.close();
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Writes content of {@link InputStream} into {@link OutputStream}
     * @param in {@link InputStream} from which need copy content
     * @param out {@link OutputStream} into which need write content
     * @param listener {@link OnCopyProgressListener} listener for copying progress
     * @return length of copied content
     */
    public static long copy(InputStream in, OutputStream out, OnCopyProgressListener listener) throws IOException {
        return copy(in, out, listener, -1L);
    }

    /**
     * Writes content of {@link InputStream} into {@link OutputStream}
     * @param in {@link InputStream} from which need copy content
     * @param out {@link OutputStream} into which need write content
     * @param listener {@link OnCopyProgressListener} listener for copying progress
     * @param totalSize {@link Long} size of allowable total readed content
     * @return length of copied content
     */
    public static long copy(InputStream in, OutputStream out, OnCopyProgressListener listener, long totalSize) throws IOException {
        long totalReaded;
        boolean isCancelled = false;
        if (listener != null) {
            long maxBatchSize = BATCH_SIZE;
            byte[] buffer = new byte[BUFFER_SIZE];
            totalReaded = 0L;
            while (true || !isCancelled) {
                int i = in.read(buffer);
                if (i == -1) {
                    break;
                }
                out.write(buffer, 0, i);
                totalReaded = totalReaded + (long) i;
                if (totalReaded >= maxBatchSize) {
                    isCancelled = listener.onProgress(totalReaded, totalSize);
                    maxBatchSize = maxBatchSize + (long) BATCH_SIZE;
                }
            }
            out.flush();
        } else {
            totalReaded = GUFile.copy(in, out);
        }
        return isCancelled ? 0L : totalReaded;
    }

    /**
     * Copies {@link File} from source path {@link String} into destination path {@link String}
     * @param source {@link String} source path
     * @param destination {@link String} destination path
     * @param rewriteIfExist {@link Boolean} indicates that need to do file rewrite if it exists
     * @param listener {@link OnCopyProgressListener} listener for copying progress
     * @return true if copying success
     */
    public static boolean copyFile(String source, String destination, boolean rewriteIfExist, OnCopyProgressListener listener) {
        File fileSrc = new File(source);
        File fileDest = new File(destination);
        File dirDest = fileDest.getParentFile();
        if (!fileSrc.exists() || dirDest == null || (!rewriteIfExist && fileDest.exists())) {
            return false;
        }
        if (!dirDest.exists()) {
            dirDest.mkdirs();
        }
        if (listener != null) {
            listener.onStartFile(fileSrc.getName());
        }
        InputStream in = null;
        OutputStream out = null;
        try {
            long max = fileSrc.length();
            in = new FileInputStream(fileSrc);
            out = new FileOutputStream(fileDest);
            if (listener == null || max < 100000L) {
                copy(in, out);
            }
            else {
                byte[] buffer = new byte[BUFFER_SIZE];
                long n = 0L;
                long thresh;
                long chunk = thresh = max / 100L;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    n += read;
                    if (n >= thresh) {
                        listener.onProgress(n, max);
                        thresh += chunk;
                    }
                }
            }
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            GUFile.closeQuietly(in);
            GUFile.closeQuietly(out);
        }
        return true;
    }

    /**
     * Creates valid file name without illegal character from {@link GUFile#ILLEGAL_CHARACTERS} string
     * @param value input filename or path
     * @return valid filename or path
     */
    public static String createValidFileName(String value) {
        int i = 0;
        String converted = GUString.decodeUrl(value);
        while (i < ILLEGAL_CHARACTERS.length()) {
            if (converted.indexOf(ILLEGAL_CHARACTERS.charAt(i)) >= 0) {
                converted = converted.replace(ILLEGAL_CHARACTERS.charAt(i), '_');
            }
            i = i + 1;
        }
        return converted;
    }

    public static String createValidFileName(String s, boolean isSmartTrim) {
        int i = 0;
        String converted = GUString.decodeUrl(s.replace("%", ""));
        while (i < ILLEGAL_CHARACTERS.length()) {
            if (converted.indexOf(ILLEGAL_CHARACTERS.charAt(i)) >= 0) {
                converted = converted.replace(ILLEGAL_CHARACTERS.charAt(i), '_');
            }
            i = i + 1;
        }
        return isSmartTrim ? smartTrim(converted) : converted;
    }

    public static String smartTrim(String str) {
        if (GUString.isEmpty(str)) {
            return str;
        }
        return (str.length() > MAX_FILE_LENGTH
                ? str.substring(0, MAX_FILE_LENGTH - 1) + str.hashCode()
                : str);
    }

    /**
     * Checks if {@link String} file/folder name is valid
     * @param name {@link String} name for check
     * @return true if name if valid
     */
    public static boolean isValidName(String name) {
        int i = 0;
        while (i < ILLEGAL_CHARACTERS.length()) {
            if (name.indexOf(ILLEGAL_CHARACTERS.charAt(i)) >= 0) {
                return false;
            }
            i++;
        }
        return true;
    }

    /**
     * Escapes folder name so it become valid for file system
     * @param string {@link String} folder name
     * @return escaped folder name
     */
    public static String escapeFolderName(String string) {
        return string.replaceAll("[/\\n\\r\\t`?*<>|\"”:.]+", "_");
    }

    /**
     * Deletes directory with files
     * @param directory {@link File} directory file
     * @return true if success
     */
    public static boolean deleteDirectory(File directory) {
        boolean result = false;
        if (directory != null && directory.exists()) {
            result = true;
            File[] listFiles = directory.listFiles();
            if (GUArray.isNotEmpty(listFiles)) {
                int i = 0;
                while (i < listFiles.length) {
                    File file = listFiles[i];
                    result = file.isDirectory() ? result && GUFile.deleteDirectory(file) : result && file.delete();
                    i = i + 1;
                }
            }
            result = result && directory.delete();
        }
        return result;
    }

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

    public static String getRootDirName(String path) {
        if (path.startsWith("/") || path.startsWith("\\")) {
            path = path.substring(1); // remove slash/backslash
        }

        int indexSeparator = path.indexOf(File.separator);
        int indexSlash = path.indexOf("/");

        if (indexSeparator < 0 && indexSlash < 0) {
            return null;
        }

        if (indexSeparator != path.length() - 1 && indexSlash != path.length() - 1) {
            return path.substring(0, Math.max(indexSeparator, indexSlash) + 1);
        }
        return path;
    }

    /**
     * Returns directory name from provided {@link String} path
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
     * @param path provided {@link String} path
     * @return {@link String} name without extension. Example: /some_dir/some_file.jpg -> some_file
     */
    public static String getFileNameWithExt(String path) {
        return getFileNameWithExt(path, false);
    }

    /**
     * Returns {@link String} name with extension from provided {@link String} path
     * @param path provided {@link String} path
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

    /**
     * Returns {@link File} extension from {@link String} url
     * @param url provided {@link String} url
     * @return {@link File} extension. Example: example.com/logo.jpg -> jpg
     */
    public static String getFileExtFromUrl(String url) {
        if (url == null) {
            return null;
        }
        int specialStart = url.lastIndexOf("#");
        int queryStart = url.lastIndexOf("?");

        if (specialStart >= 0 && queryStart >= 0) {
            queryStart = Math.min(specialStart, queryStart);
        } else if (queryStart < 0) {
            queryStart = (specialStart >= 0) ? specialStart : url.length();
        }

        int index = url.lastIndexOf(".", queryStart);
        if (index <= 0) {
            return null;
        }
        return url.substring(index + 1, queryStart);
    }

    /**
     * Creates {@link InputStream} from {@link File}
     * @param file input {@link File}
     * @return created {@link InputStream} or null
     */
    public static InputStream getInputStream(File file) {
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(file.getPath());
        } catch (FileNotFoundException e) {
            inputStream = null;
            e.printStackTrace();
        }
        return inputStream;
    }

    /**
     * Reads {@link File} contents into {@link String}
     * @param file input {@link File} for read
     * @return {@link String} readed contents of file
     */
    public static String getStringFromFile(File file) {
        InputStream is = null;
        try {
            if (file.exists() && !file.isDirectory()) {
                return GUFile.getStringFromInputStream(is = getInputStream(file), "UTF-8");
            }
        } catch (Exception ex) {
            return file.getAbsolutePath();
        } finally {
            GUFile.closeQuietly(is);
        }
        return null;
    }

    /**
     * Reads {@link InputStream} contents into {@link String}
     * @param inputStream input {@link InputStream} for read
     * @return {@link String} readed contents of stream
     * @throws IOException exception if reading unsuccessful
     */
    public static String getStringFromInputStream(InputStream inputStream) throws IOException {
        return getStringFromInputStream(inputStream, null);
    }

    /**
     * Reads {@link InputStream} contents into {@link String}
     * @param inputStream input {@link InputStream} for read
     * @param encoding {@link String} encoding. UTF_8 or WINDOWS_1251
     * @return {@link String} readed contents of stream
     * @throws IOException exception if reading unsuccessful
     */
    public static String getStringFromInputStream(InputStream inputStream, String encoding) throws IOException {
        if (inputStream == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        InputStreamReader reader = !GUString.isEmpty(encoding) ? new InputStreamReader(inputStream, encoding) : new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(reader);
        while (true) {
            String str = bufferedReader.readLine();
            if (str == null) {
                GUFile.closeQuietly(bufferedReader);
                return stringBuilder.toString();
            }
            stringBuilder.append(str).append("\n");
        }
    }

    /**
     * Checks is {@link String} filename contains any extension from provided {@link String[]} array
     * @param filename {@link String} input filename
     * @param extList {@link String[]} extensions array
     * @return true if contains at least one of provided extensions
     */
    public static boolean hasAnyFileExt(String filename, String... extList) {
        String ext = GUFile.getFileExtFromUrl(filename);
        if (ext == null) {
            ext = GUFile.getFileExt(filename);
        }
        if (!ext.isEmpty()) {
            ext = ext.toLowerCase();
            int i = 0;
            while (i < extList.length) {
                if (ext.equals(extList[i].toLowerCase())) {
                    return true;
                }
                i++;
            }
        }
        return false;
    }

    /**
     * Finds {@link String} filename last index of any available extension from provided {@link String[]} array
     * @param filename {@link String} input filename
     * @param extList {@link String[]} extensions array
     * @return {@link String} filename last index of any available provided extension. -1 if filename doesn't contain
     * any of provided extensions
     */
    public static int lastIndexOfAnyFileExt(String filename, String... extList) {
        if (hasAnyFileExt(filename, extList)) {
            int pos;
            for (String ext : extList) {
                pos = filename.lastIndexOf(ext);
                if (pos > -1)
                    return pos;
            }
        }
        return -1;
    }

    public interface OnCopyProgressListener {
        void onStartFile(String arg);
        boolean onProgress(long readedBytes, long totalBytes);
    }

    public interface OnBatchCopyListener {
        void onCopy(long n);
    }

    public static InputStream getInputEncoding(URLConnection connection) throws IOException {
        InputStream in;
        String encoding = connection.getContentEncoding();
        if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
            in = new GZIPInputStream(connection.getInputStream());
        } else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
            in = new InflaterInputStream(connection.getInputStream(), new Inflater(true));
        } else {
            in = connection.getInputStream();
        }
        return in;
    }

    public static boolean writeStringToFile(String filePath, String content) {
        return writeStringToFile(new File(filePath), content);
    }

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
}