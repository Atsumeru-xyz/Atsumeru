package com.atsumeru.web.model.importer;

import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.model.book.BookSerie;
import com.atsumeru.web.model.book.IBaseBookItem;
import com.atsumeru.web.repository.BooksDatabaseRepository;
import com.atsumeru.web.util.GUString;
import com.atsumeru.web.importer.Importer;
import com.atsumeru.web.util.GUFile;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.io.File;
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

    public boolean isAsSingles(boolean isRootFolder) {
        boolean asSingles = isSingles();
        if (isSinglesInRoot() && isRootFolder) {
            asSingles = true;
        } else if (isSinglesInRoot()) {
            asSingles = false;
        }

        return asSingles;
    }

    public List<File> getArchivesInFolder() {
        return Importer.listArchives(GUFile.removeLastPathSlash(getPath()), isRecursiveImport());
    }

    public void loadInLibraryBooks() {
        inLibrarySeries = getInLibraryBooks(getPath(), BookSerie.class);
        inLibraryArchives = getInLibraryBooks(getPath(), BookArchive.class);
    }

    private static List<IBaseBookItem> getInLibraryBooks(String folderPath, Class<? extends IBaseBookItem> clazz) {
        return BooksDatabaseRepository.getInstance().getDaoManager().queryLike("FOLDER", folderPath + "%", clazz);
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
