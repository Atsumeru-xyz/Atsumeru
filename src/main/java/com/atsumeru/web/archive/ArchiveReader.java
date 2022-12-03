package com.atsumeru.web.archive;

import com.atsumeru.web.archive.exception.MediaUnsupportedException;
import com.atsumeru.web.archive.iterator.IArchiveIterator;
import com.atsumeru.web.archive.iterator.SevenZipIterator;
import com.atsumeru.web.archive.iterator.ZipIterator;
import com.atsumeru.web.util.ContentDetector;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ArchiveReader {
    static Map<String, IArchiveIterator> archiveIteratorMap = new HashMap<>();

    static {
        addArchiveIterator(ZipIterator.create());
        addArchiveIterator(SevenZipIterator.create());
    }

    private static void addArchiveIterator(IArchiveIterator archiveIterator) {
        archiveIterator.getMediaTypes().forEach(it -> archiveIteratorMap.putIfAbsent(it, archiveIterator));
    }

    public static IArchiveIterator getArchiveIterator(String path) throws IOException {
        String mediaType = ContentDetector.detectMediaType(Paths.get(path));
        if (archiveIteratorMap.containsKey(mediaType)) {
            IArchiveIterator archiveIterator = archiveIteratorMap.get(mediaType).createInstance();
            archiveIterator.open(path);
            return archiveIterator;
        }

        throw new MediaUnsupportedException("Unsupported archive format: " + mediaType);
    }
}
