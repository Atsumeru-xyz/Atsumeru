package com.atsumeru.web.archive;

import com.atsumeru.web.util.GUString;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.zip.Deflater.DEFAULT_COMPRESSION;

public class CBZPacker {
    private String inputDir;
    private String outputDir;
    private int compressionLevel = DEFAULT_COMPRESSION;
    private PackVariant packVariant = PackVariant.CONTENT_OF_CURRENT_FOLDER;
    private CBZNameVariant nameVariant = CBZNameVariant.CURRENT_FOLDER_NAME;
    private ArchiveType archiveType = ArchiveType.CBZ;

    private final List<String> DISALLOWED_FOLDERS_TO_PACK = new ArrayList<>();

    private BiConsumer<Float, Float> packingCBZProgress;
    private BiConsumer<Long, Long> packingZipProgress;
    private BiConsumer<String, String> packingName;
    private Consumer<String> packingFileName;

    private int currentFolderIndex = 0;
    private int foldersCount = 0;

    public CBZPacker(String inputDir) {
        setInputDir(inputDir);
        this.outputDir = fixPath(inputDir);
    }

    public CBZPacker setInputDir(String inputDir) {
        this.inputDir = fixPath(inputDir);
        return this;
    }

    public CBZPacker setOutputDir(String outpudDir) {
        this.outputDir = fixPath(outpudDir);
        return this;
    }

    public CBZPacker setCompressionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
        return this;
    }

    public CBZPacker setPackVariant(PackVariant packVariant) {
        this.packVariant = packVariant;
        return this;
    }

    public CBZPacker setOutputFileNameVariant(CBZNameVariant nameVariant) {
        this.nameVariant = nameVariant;
        return this;
    }

    public CBZPacker setArchiveType(ArchiveType archiveType) {
        this.archiveType = archiveType;
        return this;
    }

    public CBZPacker setDisallowedFoldersToPack(String... folders) {
        DISALLOWED_FOLDERS_TO_PACK.addAll(Arrays.asList(folders));
        return this;
    }

    public void pack() throws InterruptedException {
        String inputDirName = Paths.get(inputDir).getFileName().toString();
        String outputFilePath;
        System.out.println("Packing: " + inputDirName);

        File[] folders = populateFoldersToPack();

        this.foldersCount = folders.length;
        for (int i = 0; i < folders.length; i++) {
            this.currentFolderIndex = i;
            File folder = folders[i];

            String extension = "." + archiveType.toString().toLowerCase();
            if (this.nameVariant == CBZNameVariant.CURRENT_FOLDER_NAME_DELIMITER_PACKING_FOLDER_NAME) {
                outputFilePath = outputDir + inputDirName + " - " + folder.toPath().getFileName().toString() + extension;
            } else {
                outputFilePath = outputDir + folder.toPath().getFileName().toString() + extension;
            }
            System.out.println("Packing subdir: " + folder.toPath().getFileName().toString());
            pack(inputDirName, folder, outputFilePath);
        }
        if (packingName != null) {
            packingName.accept("Done!", "Done!");
        }
        if (packingFileName != null) {
            packingFileName.accept("All files packed successfully.");
        }
        System.out.println("Packing completed successfully!");
    }

    private void pack(String inputDirName, File folder, String outputFilePath) throws InterruptedException {
        if (!DISALLOWED_FOLDERS_TO_PACK.contains(folder.toPath().getFileName().toString())) {
            ZipArchive zu = new ZipArchive();
            zu.setFilePackingProgress(packingZipProgress);
            zu.setFolderPackingProgress(packingCBZProgress);
            zu.setConsumerFileName(packingFileName);
            zu.setFoldersCount(currentFolderIndex, foldersCount);
            if (packingName != null) {
                packingName.accept(inputDirName, folder.toPath().getFileName().toString());
            }
            zu.packDirectory(folder, outputFilePath, compressionLevel);
        }
    }

    private File[] populateFoldersToPack() {
        File[] folders = new File(inputDir).listFiles(File::isDirectory);
        if (folders.length == 0 || packVariant.equals(PackVariant.CONTENT_OF_CURRENT_FOLDER)) {
            ArrayList<File> file = new ArrayList<>();
            file.add(new File(inputDir));
            folders = file.toArray(new File[file.size()]);
        }
        return folders;
    }

    private String fixPath(String path) {
        if (GUString.isEmpty(path)) {
            return path;
        }
        if (path.endsWith("\"")) {
            path = path.substring(0, path.length() - 1);
        }
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        return path;
    }

    public void setZipProgressUpdate(BiConsumer<Long, Long> packingZipProgress) {
        this.packingZipProgress = packingZipProgress;
    }

    public void setCBZProgressUpdate(BiConsumer<Float, Float> packingCBZProgress) {
        this.packingCBZProgress = packingCBZProgress;
    }

    public void setNameUpdate(BiConsumer<String, String> packingName) {
        this.packingName = packingName;
    }

    public void setFileNameUpdate(Consumer<String> packingFileName) {
        this.packingFileName = packingFileName;
    }

    public enum PackVariant {CONTENT_OF_CURRENT_FOLDER, CONTENT_OF_SUBFOLDERS}

    public enum CBZNameVariant {CURRENT_FOLDER_NAME, CURRENT_FOLDER_NAME_DELIMITER_PACKING_FOLDER_NAME}

    public enum ArchiveType {CBZ, ZIP}

    private static class ZipArchive {
        private final List<String> filesInDir = new ArrayList<>();

        private BiConsumer<Long, Long> filePackingProgress;
        private BiConsumer<Float, Float> folderPackingProgress;
        private Consumer<String> fileName;

        private int currentFolderIndex = 0;
        private int foldersCount = 0;

        public void packDirectory(File dir, String zipDirName, int compression) throws InterruptedException {
            try {
                populateFilesList(dir);
                FileOutputStream fos = new FileOutputStream(zipDirName);
                ZipArchiveOutputStream zos = new ZipArchiveOutputStream(fos);

                zos.setFallbackToUTF8(true);
                zos.setUseLanguageEncodingFlag(true);
                zos.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS);

                zos.setLevel(compression);

                packDirectory(dir, zos);

                zos.close();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void packDirectory(File dir, ZipArchiveOutputStream zos) {
            try {
                long packedBytesSize = 0;
                long totalFilesSize = getTotalFilesSize(dir);
                for (String filePath : filesInDir) {
                    if (this.fileName != null) {
                        this.fileName.accept(Paths.get(filePath).getFileName().toString());
                    }
                    ZipArchiveEntry ze = new ZipArchiveEntry(filePath.substring(dir.getAbsolutePath().length() + 1));
                    zos.putArchiveEntry(ze);
                    FileInputStream fis = new FileInputStream(filePath);
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        packedBytesSize += len;
                        countProgress(packedBytesSize, totalFilesSize);
                        zos.write(buffer, 0, len);
                    }
                    zos.closeArchiveEntry();
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void countProgress(long packedBytesSize, long totalFilesSize) {
            if (filePackingProgress != null && folderPackingProgress != null) {
                filePackingProgress.accept(packedBytesSize, totalFilesSize);
                folderPackingProgress.accept((((float) packedBytesSize / totalFilesSize) * (1.0f / foldersCount) + (currentFolderIndex) / (float) foldersCount), 1.0f);
            }
        }

        private void populateFilesList(File dir) throws IOException {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.isFile()) filesInDir.add(file.getAbsolutePath());
                else populateFilesList(file);
            }
        }

        private long getTotalFilesSize(File dir) {
            return FileUtils.sizeOfDirectory(dir);
        }

        public void setFilePackingProgress(BiConsumer<Long, Long> progress) {
            this.filePackingProgress = progress;
        }

        public void setFolderPackingProgress(BiConsumer<Float, Float> progress) {
            this.folderPackingProgress = progress;
        }

        public void setConsumerFileName(Consumer<String> fileName) {
            this.fileName = fileName;
        }

        public void setFoldersCount(int currentFolderIndex, int foldersCount) {
            this.currentFolderIndex = currentFolderIndex;
            this.foldersCount = foldersCount;
        }
    }

}
