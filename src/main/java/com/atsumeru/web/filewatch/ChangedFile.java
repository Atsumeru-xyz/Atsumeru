package com.atsumeru.web.filewatch;

import java.io.File;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A single file that has changed.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see ChangedFiles
 */
public final class ChangedFile {

    private final File sourceFolder;

    private final File file;

    private final Type type;

    /**
     * Create a new {@link ChangedFile} instance.
     * @param sourceFolder the source folder
     * @param file the file
     * @param type the type of change
     */
    public ChangedFile(File sourceFolder, File file, Type type) {
        Assert.notNull(sourceFolder, "SourceFolder must not be null");
        Assert.notNull(file, "File must not be null");
        Assert.notNull(type, "Type must not be null");
        this.sourceFolder = sourceFolder;
        this.file = file;
        this.type = type;
    }

    /**
     * Return the file that was changed.
     * @return the file
     */
    public File getFile() {
        return this.file;
    }

    /**
     * Return the type of change.
     * @return the type of change
     */
    public Type getType() {
        return this.type;
    }

    /**
     * Return the name of the file relative to the source folder.
     * @return the relative name
     */
    public String getRelativeName() {
        File folder = this.sourceFolder.getAbsoluteFile();
        File file = this.file.getAbsoluteFile();
        String folderName = StringUtils.cleanPath(folder.getPath());
        String fileName = StringUtils.cleanPath(file.getPath());
        Assert.state(fileName.startsWith(folderName),
                () -> "The file " + fileName + " is not contained in the source folder " + folderName);
        return fileName.substring(folderName.length() + 1);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof ChangedFile) {
            ChangedFile other = (ChangedFile) obj;
            return this.file.equals(other.file) && this.type.equals(other.type);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.file.hashCode() * 31 + this.type.hashCode();
    }

    @Override
    public String toString() {
        return this.file + " (" + this.type + ")";
    }

    /**
     * Change types.
     */
    public enum Type {

        /**
         * A new file has been added.
         */
        ADD,

        /**
         * An existing file has been modified.
         */
        MODIFY,

        /**
         * An existing file has been deleted.
         */
        DELETE

    }

}
