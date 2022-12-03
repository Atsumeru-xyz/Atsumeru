package com.atsumeru.web.archive.iterator;

import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public interface IArchiveIterator extends Closeable {
    Comparator<String> natSortComparator = CaseInsensitiveSimpleNaturalComparator.getInstance();

    List<String> getMediaTypes();

    IArchiveIterator createInstance();
    void open(String archivePath) throws IOException;
    void reset() throws IOException;
    boolean next();
    long getEntrySize() throws IOException;
    String getEntryName() throws IOException;
    InputStream getEntryInputStream() throws IOException;
    InputStream getEntryInputStreamByName(String entryName) throws IOException;
    boolean saveIntoArchive(String filePath, String fileName, String fileContent);
    boolean saveIntoArchive(String filePath, Map<String, String> fileNameWithContentMap);
    String getArchivePath();
    void close();
}
