package com.atsumeru.web.model.importer;

import com.atsumeru.web.importer.Importer;
import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.model.book.BookSerie;
import com.atsumeru.web.model.book.IBaseBookItem;
import com.atsumeru.web.repository.BooksDatabaseRepository;
import com.atsumeru.web.service.ImportService;
import com.atsumeru.web.util.GUFile;
import com.atsumeru.web.util.GUString;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class FolderProperty {
    @Expose
    private String hash;

    @Expose
    private String path;

    @Expose
    @SerializedName("singles")
    private boolean isSingles;

    @Expose
    @SerializedName("singles_in_root")
    private boolean isSinglesInRoot;

    @Expose
    @SerializedName("singles_if_in_root_with_folders")
    private boolean isSinglesIfInRootWithFolders;

    @Expose
    @SerializedName("ignore_volume_numbers_detection")
    private boolean isIgnoreVolumeNumbersDetection;

    @Expose
    @SerializedName("series_count")
    private long seriesCount;

    @Expose
    @SerializedName("singles_count")
    private long singlesCount;

    @Expose
    @SerializedName("archives_count")
    private long archivesCount;

    @Expose
    @SerializedName("chapters_count")
    private long chaptersCount;

    private transient List<IBaseBookItem> inLibrarySeries;
    private transient List<IBaseBookItem> inLibraryArchives;

    private transient Set<String> addedSerieFolders;
    private transient Set<String> addedArchiveFolders;

    private transient String singleArchivePath;

    private transient Boolean recursiveImport;
    private transient boolean reImportIfExist;
    private transient boolean forceUpdateCovers;

    public FolderProperty(String path) {
        this.path = path;
        createHash();
    }

    public void createHash() {
        this.hash = GUString.md5Hex(path);
    }

    public String getHash() {
        if (GUString.isEmpty(hash)) {
            createHash();
        }
        return hash;
    }

    public boolean isRecursiveImport() {
        return recursiveImport != null ? recursiveImport : true;
    }

    @Override
    public boolean equals(Object object) {
        boolean result = false;
        if (object != null && object.getClass() == this.getClass()) {
            result = path.equalsIgnoreCase(((FolderProperty) object).getPath());
        }
        return result;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public boolean isAsSingles(File file, boolean isRootFolder) {
        if (isSinglesInRoot() && isRootFolder) {
            return true;
        } else if (isSinglesInRoot()) {
            return false;
        } else if (isSinglesIfInRootWithFolders()) {
            try {
                return Files.list(file.getParentFile().toPath())
                        .map(Path::toFile)
                        .filter(file1 -> !GUString.equalsIgnoreCase(file1.getName(), ReadableContent.EXTERNAL_INFO_DIRECTORY_NAME))
                        .anyMatch(GUFile::isDirectory);
            } catch (IOException e) {
                return false;
            }
        } else {
            return isSingles();
        }
    }

    public List<File> getArchivesInFolder() {
        return Importer.listArchives(GUFile.removeLastPathSlash(getPath()), isRecursiveImport());
    }

    public void loadInLibraryBooks() {
        inLibrarySeries = getInLibraryBooks(getPath(), BookSerie.class);
        inLibraryArchives = getInLibraryBooks(getPath(), BookArchive.class);
    }

    private static List<IBaseBookItem> getInLibraryBooks(String folderPath, Class<? extends IBaseBookItem> clazz) {
        return BooksDatabaseRepository.getInstance().getDaoManager().queryLike(ImportService.FOLDER_FIELD_NAME, folderPath + "%", clazz);
    }

    public void addSerieFolder(String folder) {
        addedSerieFolders.add(folder);
    }

    public void addArchiveFolder(String folder) {
        addedArchiveFolders.add(folder);
    }

    public void clearAddedFolders() {
        addedSerieFolders = new HashSet<>();
        addedArchiveFolders = new HashSet<>();
    }
}
