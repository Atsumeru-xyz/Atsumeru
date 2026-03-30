package xyz.atsumeru.web.model.importer;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import xyz.atsumeru.web.AtsumeruApplication;
import xyz.atsumeru.web.importer.Importer;
import xyz.atsumeru.web.model.book.BookArchive;
import xyz.atsumeru.web.model.book.BookSerie;
import xyz.atsumeru.web.model.book.IBaseBookItem;
import xyz.atsumeru.web.repository.dao.BooksDaoManager;
import xyz.atsumeru.web.service.ImportService;
import xyz.atsumeru.web.util.FileUtils;
import xyz.atsumeru.web.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class ImportFolder {
    private String hash;
    private String path;

    @Schema(name = "singles")
    @SerializedName("singles")
    private boolean isSingles;

    @Schema(name = "singles_in_root")
    @SerializedName("singles_in_root")
    private boolean isSinglesInRoot;

    @Schema(name = "singles_if_in_root_with_folders")
    @SerializedName("singles_if_in_root_with_folders")
    private boolean isSinglesIfInRootWithFolders;

    @Schema(name = "ignore_volume_numbers_detection")
    @SerializedName("ignore_volume_numbers_detection")
    private boolean isIgnoreVolumeNumbersDetection;

    @Schema(name = "series_count")
    @SerializedName("series_count")
    private long seriesCount;

    @Schema(name = "singles_count")
    @SerializedName("singles_count")
    private long singlesCount;

    @Schema(name = "archives_count")
    @SerializedName("archives_count")
    private long archivesCount;

    @Schema(name = "chapters_count")
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

    public ImportFolder(String path) {
        this.path = path;
        createHash();
    }

    public void createHash() {
        this.hash = StringUtils.md5Hex(path);
    }

    public String getHash() {
        if (StringUtils.isEmpty(hash)) {
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
            result = path.equalsIgnoreCase(((ImportFolder) object).getPath());
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
                        .filter(file1 -> !StringUtils.equalsIgnoreCase(file1.getName(), ReadableContent.EXTERNAL_INFO_DIRECTORY_NAME))
                        .anyMatch(FileUtils::isDirectory);
            } catch (IOException e) {
                return false;
            }
        } else {
            return isSingles();
        }
    }

    public List<File> getArchivesInFolder() {
        return Importer.listArchives(FileUtils.removeLastPathSlash(getPath()), isRecursiveImport());
    }

    public void loadInLibraryBooks() {
        inLibrarySeries = getInLibraryBooks(getPath(), BookSerie.class);
        inLibraryArchives = getInLibraryBooks(getPath(), BookArchive.class);
    }

    private static List<IBaseBookItem> getInLibraryBooks(String folderPath, Class<? extends IBaseBookItem> clazz) {
        return AtsumeruApplication.getContext().getBean(BooksDaoManager.class).queryLike(ImportService.FOLDER_FIELD_NAME, folderPath + "%", clazz);
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
