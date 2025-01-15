package xyz.atsumeru.web.archive;

import xyz.atsumeru.web.archive.iterator.IArchiveIterator;
import xyz.atsumeru.web.archive.iterator.SevenZipIterator;
import xyz.atsumeru.web.archive.iterator.ZipIterator;
import xyz.atsumeru.web.exception.MediaUnsupportedException;
import xyz.atsumeru.web.util.ContentDetector;

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
