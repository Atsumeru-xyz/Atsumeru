package com.atsumeru.web.archive.iterator;

import com.atsumeru.web.util.GUFile;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipIterator implements IArchiveIterator {
    private static final Logger logger = LoggerFactory.getLogger(ZipIterator.class.getSimpleName());

    @Getter private String archivePath;
    private ZipFile zipFile;
    private ListIterator<? extends ZipEntry> iterator;
    private ZipEntry entry;

    public static ZipIterator create() {
        return new ZipIterator();
    }

    private ZipIterator() {
    }

    @Override
    public List<String> getMediaTypes() {
        return Arrays.asList("application/zip", "application/epub+zip");
    }

    @Override
    public IArchiveIterator createInstance() {
        return create();
    }

    @Override
    public void open(String archivePath) throws IOException {
        this.archivePath = archivePath;

        // TODO: log into file
//        logger.info("Reading archive: " + archivePath);

        // Открытие архива для чтения
        openWithCharset(Charset.forName("CP866"));
        reset();
    }

    private void openWithCharset(Charset charset) throws IOException {
        zipFile = new ZipFile(archivePath, charset);
    }

    @Override
    public void reset() {
        List<? extends ZipEntry> entries = Collections.list(zipFile.entries());

        iterator = entries.stream()
                .filter(it -> !it.isDirectory())
                .sorted((entry1, entry2) -> natSortComparator.compare(entry1.getName().toLowerCase(), entry2.getName().toLowerCase()))
                .collect(Collectors.toList())
                .listIterator();
    }

    @Override
    public boolean next() {
        if (iterator.hasNext()) {
            entry = iterator.next();
            return true;
        }
        return false;
    }

    @Override
    public long getEntrySize() {
        return entry.getSize();
    }

    @Override
    public String getEntryName() {
        return entry.getName();
    }

    @Override
    public InputStream getEntryInputStream() throws IOException {
        return zipFile.getInputStream(entry);
    }

    @Override
    public InputStream getEntryInputStreamByName(String entryName) throws IOException {
        try {
            entry = zipFile.getEntry(entryName);
        } catch (IllegalArgumentException ignored) {
        }
        // Maybe archive packed with in UTF-8 charset and entry has Cyrillic symbols?
        if (entry == null) {
            close();
            openWithCharset(StandardCharsets.UTF_8);
            entry = zipFile.getEntry(entryName);
        }
        if (entry == null) {
            throw new IOException();
        }
        return zipFile.getInputStream(entry);
    }

    @Override
    public boolean saveIntoArchive(String filePath, String fileName, String fileContent) {
        close();

        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        URI uri = URI.create("jar:" + Paths.get(filePath).toUri());
        FileSystem fs = null;
        try {
            fs = FileSystems.newFileSystem(uri, env);
            Path nf = fs.getPath(fileName);
            try (Writer writer = Files.newBufferedWriter(nf, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
                writer.write(fileContent);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            GUFile.closeQuietly(fs);
        }
    }

    @Override
    public boolean saveIntoArchive(String filePath, Map<String, String> fileNameWithContentMap) {
        close();

        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        URI uri = URI.create("jar:" + Paths.get(filePath).toUri());
        FileSystem fileSystem = null;
        try {
            fileSystem = FileSystems.newFileSystem(uri, env);
            for (Map.Entry<String, String> entry : fileNameWithContentMap.entrySet()) {
                Path nf = fileSystem.getPath(entry.getKey());
                try (Writer writer = Files.newBufferedWriter(nf, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
                    writer.write(entry.getValue());
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            GUFile.closeQuietly(fileSystem);
        }
    }

    @Override
    public void close() {
        iterator = null;
        entry = null;
        GUFile.closeQuietly(zipFile);
    }
}
