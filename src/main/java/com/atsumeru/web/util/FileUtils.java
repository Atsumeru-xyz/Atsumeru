package com.atsumeru.web.util;

import com.atsumeru.web.helper.Constants;
import com.atsumeru.web.util.comparator.AlphanumComparator;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class FileUtils {
    public static final String[] ALLOWED_IMAGE_EXTENSIONS = new String[]{
            Constants.BMP,
            Constants.GIF,
            Constants.JPEG,
            Constants.JPG,
            Constants.PNG,
            Constants.WEBP
    };

    public static List<File> getAllFilesFromDirectory(String directoryPath, String[] allowedExtensions, boolean recursive) {
        List<File> imagesPathList = null;
        if (allowedExtensions == null) {
            allowedExtensions = ALLOWED_IMAGE_EXTENSIONS;
        }
        File file = new File(directoryPath);
        if (file.isDirectory()) {
            imagesPathList = (List<File>) org.apache.commons.io.FileUtils.listFiles(file, allowedExtensions, recursive);
        }

        if (GUArray.isNotEmpty(imagesPathList)) {
            imagesPathList.sort((file1, file2) -> AlphanumComparator.compareStrings(file1.getName(), file2.getName()));
        }
        return imagesPathList;
    }

    public static int countFiles(String directoryPath, String[] allowedExtensions, boolean recursive) {
        return getAllFilesFromDirectory(directoryPath, allowedExtensions,recursive).size();
    }

    public static String getFileSize(long size) {
        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.GERMAN);
        otherSymbols.setDecimalSeparator('.');
        otherSymbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("0.00", otherSymbols);
        float sizeKb = 1024.0f;
        float sizeMb = sizeKb * sizeKb;
        float sizeGb = sizeMb * sizeKb;
        float sizeTerra = sizeGb * sizeKb;
        if(size < sizeMb)
            return df.format(size / sizeKb)+ " Kb";
        else if(size < sizeGb)
            return df.format(size / sizeMb) + " Mb";
        else if(size < sizeTerra)
            return df.format(size / sizeGb) + " Gb";

        return "";
    }

    public static void copyFile(File src, File dest) {
        try {
            org.apache.commons.io.FileUtils.copyFile(src, dest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
