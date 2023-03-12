package com.atsumeru.web.model.importer;

import com.atsumeru.web.metadata.*;
import com.atsumeru.web.util.*;
import com.atsumeru.web.archive.ArchiveReader;
import com.atsumeru.web.archive.iterator.IArchiveIterator;
import com.atsumeru.web.enums.BookType;
import com.atsumeru.web.enums.ContentType;
import com.atsumeru.web.helper.ChapterRecognition;
import com.atsumeru.web.helper.Constants;
import com.atsumeru.web.helper.FilesHelper;
import com.atsumeru.web.helper.HashHelper;
import com.atsumeru.web.importer.Importer;
import com.atsumeru.web.manager.ImageCache;
import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.model.book.chapter.BookChapter;
import com.atsumeru.web.model.book.image.Images;
import com.atsumeru.web.renderer.AbstractRenderer;
import com.atsumeru.web.renderer.DjVuRenderer;
import com.atsumeru.web.renderer.PDFRenderer;
import com.kursx.parser.fb2.FictionBook;
import com.kursx.parser.fb2.Image;
import com.trickl.palette.Palette;
import kotlin.Pair;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Stream;

@Data
public class ReadableContent implements Closeable {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ReadableContent.class.getSimpleName());

    public static final String EXTERNAL_INFO_DIRECTORY_NAME = ".atsumeru";
    public static final String XML_INFO_FILENAME = "ComicInfo.xml";
    public static final String OPF_INFO_EXTENSION = ".opf";
    public static final String BOOK_JSON_INFO_FILENAME = "book_info.json";
    public static final String SERIE_JSON_INFO_FILENAME = "serie_info.json";
    public static final String CHAPTER_JSON_INFO_FILENAME = "chapter_info.json";
    public static final String[] SUPPORTED_IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"};
    public static final String ZERO_COVER_FILENAME_START = "00000.";
    public static final String COVER_FILENAME_START = "cover";

    private IArchiveIterator archiveIterator = null;

    private boolean hasMetadata;
    private InputStream xmlInfoStream; // ComicInfo.xml
    private InputStream opfInfoStream; // .*.opf
    private InputStream bookJsonInfoStream; // book_info.json
    private InputStream serieJsonInfoStream; // serie_info.json

    private Pair<Integer, PDDocumentInformation> pdDocumentInformationPair; // <Pages, PDFInfo>
    private Pair<String, FictionBook> fictionBookPair; // <Path, FictionBook>
    private int djVuBookPagesCount;

    private Map<String, InputStream> chapterJsonInfoStream = new TreeMap<>(IArchiveIterator.natSortComparator); // map of <chapter folder, chapter_info.json>
    private InputStream coverStream; // first image from archive
    private InputStream serieCoverStream; // cover image from folder
    private String coverFilePath;
    private Images images;
    private List<String> pageEntryNames;
    private List<BookChapter> chapters = new ArrayList<>();

    private String serieHash;
    private BookArchive bookArchive;
    private BookArchive serieArchive;
    private Map<String, List<String>> chapterPages;

    private Map<String, BookArchive> archivesMap;
    private String parentPath;
    private String archivePath;
    private boolean reImportIfExist;
    private boolean ignoreVolumeNumbersDetection;
    private boolean forceUpdateCovers;

    private boolean isArchiveFile;
    private boolean asSingle;
    private boolean isBookFile;

    private ReadableContent(Map<String, BookArchive> archivesMap, String parentPath, String archivePath, boolean asSingle, boolean reImportIfExist, boolean ignoreVolumeNumbersDetection, boolean forceUpdateCovers) {
        this.archivesMap = archivesMap;
        this.parentPath = parentPath;
        this.archivePath = archivePath;
        this.asSingle = asSingle;
        this.reImportIfExist = reImportIfExist;
        this.ignoreVolumeNumbersDetection = ignoreVolumeNumbersDetection;
        this.forceUpdateCovers = forceUpdateCovers;
    }

    @Nullable
    public static ReadableContent create(Map<String, BookArchive> archivesMap, String parentPath, String archivePath,
                                         boolean asSingle, boolean reImportIfExist, boolean ignoreVolumeNumbersDetection, boolean forceUpdateCovers)
            throws IOException, ParserConfigurationException, SAXException {
        return new ReadableContent(archivesMap, parentPath, archivePath, asSingle, reImportIfExist, ignoreVolumeNumbersDetection, forceUpdateCovers).createContent();
    }

    public static File getSerieExternalCover(String archivePath) {
        return FileUtils.getAllFilesFromDirectory(new File(archivePath).getParentFile().getAbsolutePath(), null, false)
                .stream()
                .filter(file -> GUString.startsWithIgnoreCase(file.getName(), "cover."))
                .findFirst()
                .orElse(null);
    }

    private boolean isSkipImport(String archiveHash, String oldArchiveHash) {
        if (GUArray.isNotEmpty(archivesMap) && (archivesMap.containsKey(archiveHash) || archivesMap.containsKey(oldArchiveHash))) {
            log(Importer.fileLogger, "Item with hash = " + archiveHash + " already exists!" + (reImportIfExist ? " Reimporting..." : ""));
            return !reImportIfExist;
        }
        return false;
    }

    @Nullable
    public ReadableContent createContent() throws IOException, ParserConfigurationException, SAXException {
        File archiveFile = new File(archivePath);
        Path path = archiveFile.toPath();

        boolean supportsFileAttributeView = Files.getFileStore(path).supportsFileAttributeView(UserDefinedFileAttributeView.class);

        // Создание хеша архива используя старый алгоритм
        String oldHash = createOldArchiveHash();

        Supplier<String> hashSupplier = () -> HashHelper.getMHash2(
                Constants.ARCHIVE_HASH_TAG, supportsFileAttributeView
                        ? UUID.randomUUID().toString()
                        : oldHash
        );

        // Создание уникального хеша архива и запись/чтение его из кастомных аттрибутов файла
        String archiveHash = NotEmptyString.ofNullable(FilesHelper.readHashFileAttribute(path,
                Constants.ATTRIBUTE_HASH,
                hashSupplier.get())
        ).orElseGet(hashSupplier);

        // Пропуск импорта архива, если он уже импортирован и пользователь не запросил реимпорт
        if (isSkipImport(archiveHash, oldHash)) {
            return null;
        }

        BookArchive bookArchive = new BookArchive();
        bookArchive.setFolder(archivePath);
        bookArchive.setFileSize(archiveFile.length());

        // Создание модели ReadableContent для заполнения
        log(Importer.fileLogger, "Reading archive: " + archivePath);
        readContent(bookArchive, Importer.fileLogger);

        // Заполнение модели BookArchive
        log(Importer.fileLogger, "Reading and filling metadata...");
        fillBookArchive(bookArchive);

        if (!GUString.equalsIgnoreCase(archiveHash, bookArchive.getContentId()) && isSkipImport(bookArchive.getContentId(), oldHash)) {
            return null;
        }

        // Установка хеша архива модели BookArchive, если хеш не был прочитан из метаданных
        if (GUString.isEmpty(bookArchive.getContentId())) {
            bookArchive.setContentId(archivesMap.containsKey(archiveHash) ? archiveHash : oldHash);
        }

        // Создание/чтение хеша Серии
        setSerieHash(Optional.ofNullable(bookArchive.getSerieHash()).orElse(createSerieHash(bookArchive.getContentId(), asSingle || bookArchive.isSingle())));
        bookArchive.setSerieHash(getSerieHash());

        // Запись хешей в аттрибуты файла
        FilesHelper.writeHashFileAttribute(path, Constants.ATTRIBUTE_HASH, bookArchive.getContentId());
        FilesHelper.writeHashFileAttribute(path, Constants.ATTRIBUTE_SERIE_HASH, getSerieHash());

        // Сохранение обложки архива в кеш (превью), если пользовател явно это запросил при импорте
        saveCoverIntoCache(bookArchive.getContentId(), bookArchive.getSerieHash());

        // Установка ссылки на локальную обложку
        bookArchive.setCover(bookArchive.getContentId());

        // Установка акцентного цвета обложки
        if (getImages() != null) {
            bookArchive.setCoverAccent(getImages().getAccent());
        }

        // Парсинг номеров томов
        if (GUString.isNotEmpty(bookArchive.getFolder())) {
            ChapterRecognition.parseNumbers(bookArchive);
        }

        if (isArchiveFile() && GUArray.isNotEmpty(getChapterPages())) {
            // Импорт глав из архива. На основе директорий или из файлов chapter_info.json
            importChapters(getChapterPages(), getArchiveIterator(), bookArchive.getContentId());
        }

        // Чтение внешних метаданных Серии
        readExternalSerieInfo();

        GUFile.closeQuietly(this);
        return this;
    }

    private void readContent(@Nullable BookArchive bookArchive, @Nullable Logger logger) throws IOException, ParserConfigurationException, SAXException {
        // Опредение является ли файл книгой (EPUB, FB2, PDF)
        BookType bookType = ContentDetector.detectBookType(Paths.get(archivePath));
        boolean isArchiveFile = bookType == BookType.ARCHIVE || bookType == BookType.EPUB;

        setArchiveFile(isArchiveFile);
        setBookArchive(bookArchive);
        setBookFile(bookType != BookType.ARCHIVE);
        findExternalSerieInfo();
        findExternalBookInfo();

        if (isArchiveFile) {
            // Открытие архива для чтения
            setArchiveIterator(ArchiveReader.getArchiveIterator(archivePath));
            create(getBookJsonInfoStream() != null, logger);
        } else if (bookType == BookType.FB2) {
            createForFictionBook();
        } else if (bookType == BookType.PDF) {
            createForPDF();
        }  else if (bookType == BookType.DJVU) {
            createForDVJU();
        }
    }

    private void readExternalSerieInfo() {
        if (getSerieJsonInfoStream() != null) {
            try {
                BookArchive archive = new BookArchive();
                BookInfo.fromJSON(archive, IOUtils.toString(getSerieJsonInfoStream(), StandardCharsets.UTF_8));

                archive.setPagesCount(bookArchive.getPagesCount());
                if (GUString.isEmpty(archive.getContentId())) {
                    archive.setContentId(bookArchive.getContentId());
                }

                serieArchive = archive;
            } catch (IOException e) {
                logW(Importer.fileLogger, "Unable to read external serie_info.json file...");
            }
        }
    }

    public static Images saveCoverImage(BookArchive bookArchive, boolean asSingle) {
        try (ReadableContent archive = new ReadableContent(null, null, bookArchive.getFolder(), asSingle, false, true, true)) {
            archive.readContent(null, null);
            archive.saveCoverIntoCache(
                    bookArchive.getContentId(),
                    Optional.ofNullable(archive.getSerieCoverStream())
                            .map(stream -> bookArchive.getSerie().getSerieId())
                            .orElse(null)
            );
            return archive.getImages();
        } catch (Exception ex) {
            return null;
        }
    }

    @SneakyThrows
    private void findExternalSerieInfo() {
        if (!asSingle) {
            File serieInfoFile = new File(new File(archivePath).getParentFile(), SERIE_JSON_INFO_FILENAME);
            if (GUFile.isFile(serieInfoFile)) {
                serieJsonInfoStream = Files.newInputStream(serieInfoFile.toPath());
            }
        }
    }

    private void findExternalBookInfo() {
        File bookInfo = Stream.of(new File(new File(archivePath).getParentFile(), EXTERNAL_INFO_DIRECTORY_NAME))
                .filter(GUFile::isDirectory)
                .peek(FilesHelper::setAttributeHidden)
                .map(atsumeruFolder -> new File(atsumeruFolder, GUFile.getFileName(archivePath)))
                .filter(GUFile::isDirectory)
                .peek(contentFolder -> FileUtils.getAllFilesFromDirectory(contentFolder.toString(), new String[]{Constants.JSON}, true)
                        .stream()
                        .filter(file -> !file.toString().contains(BOOK_JSON_INFO_FILENAME))
                        .forEach(file -> {
                            try {
                                String chapterPath = file.toString()
                                        .replace(contentFolder + File.separator, "")
                                        .replace(CHAPTER_JSON_INFO_FILENAME, "");
                                putChapterJsonInfoStream(chapterPath, Files.newInputStream(file.toPath()));
                            } catch (IOException ignored) {
                            }
                        }))
                .map(contentFolder -> new File(contentFolder, BOOK_JSON_INFO_FILENAME))
                .filter(GUFile::isFile)
                .findAny()
                .orElse(null);

        if (bookInfo != null) {
            try {
                setBookJsonInfoStream(new FileInputStream(bookInfo));
            } catch (FileNotFoundException ignored) {
            }
        }
    }

    private void create(boolean hasExternalMetadata, Logger logger) throws IOException {
        IArchiveIterator archiveIterator = getArchiveIterator();

        // <Chapter name, List<File path>>
        Map<String, List<String>> chapterPages = new TreeMap<>(IArchiveIterator.natSortComparator);

        boolean zeroCoverFound = false;

        // Поиск внешней обложки
        Optional.ofNullable(getSerieExternalCover(archivePath))
                .ifPresent(file -> {
                    try {
                        setSerieCoverStream(new FileInputStream(file));
                    } catch (FileNotFoundException ignored) {
                    }
                });

        List<String> pageEntryNames = new ArrayList<>();
        while (archiveIterator.next()) {
            String entryName = archiveIterator.getEntryName();
            String fileName = entryName.toLowerCase();

            if (!hasExternalMetadata) {
                // Поиск XML файла с метаданными и получение InputStream, если файл найден
                if (GUString.equalsIgnoreCase(fileName, ReadableContent.XML_INFO_FILENAME)) {
                    setXmlInfoStream(archiveIterator.getEntryInputStream());
                    log(logger, "Found " + ReadableContent.XML_INFO_FILENAME + " file");
                    continue;
                }

                // Поиск EPUB OPF файла с метаданными книги и получение InputStream, если файл найден
                if (GUString.endsWithIgnoreCase(fileName, ReadableContent.OPF_INFO_EXTENSION)) {
                    setOpfInfoStream(archiveIterator.getEntryInputStream());
                    log(logger, "Found EPUB .*" + ReadableContent.OPF_INFO_EXTENSION + " file");
                    continue;
                }

                // Поиск JSON файла с метаданными книги и получение InputStream, если файл найден
                if (GUString.equalsIgnoreCase(fileName, ReadableContent.BOOK_JSON_INFO_FILENAME)) {
                    setBookJsonInfoStream(archiveIterator.getEntryInputStream());
                    log(logger, "Found " + ReadableContent.BOOK_JSON_INFO_FILENAME + " file");
                    continue;
                }

                // Поиск JSON файла с метаданными главы и получение InputStream, если файл найден
                if (GUString.endsWithIgnoreCase(fileName, ReadableContent.CHAPTER_JSON_INFO_FILENAME)) {
                    putChapterJsonInfoStream(entryName, archiveIterator.getEntryInputStream());
                    log(logger, "Found " + ReadableContent.CHAPTER_JSON_INFO_FILENAME + " file in path " + entryName);
                    continue;
                }
            }

            for (String extension : ReadableContent.SUPPORTED_IMAGE_EXTENSIONS) {
                if (fileName.endsWith(extension)) {
                    // Поиск первого изображения и получение InputStream оного. В последствии данное изображение будет
                    // использовано как превью и полноформатная обложка
                    if (!zeroCoverFound && (getCoverStream() == null || isCoverFile(fileName))) {
                        if (getCoverStream() == null || !isCoverFile(getCoverFilePath())) {
                            setCoverStream(archiveIterator.getEntryInputStream());
                            setCoverFilePath(fileName);
                            log(logger, "Found cover image in path: " + fileName);
                            zeroCoverFound = GUFile.getFileNameWithExt(fileName).toLowerCase().startsWith(ZERO_COVER_FILENAME_START);
                        }
                    }

                    if (!isBookFile) {
                        // Сохранение названия Entry в список для подальшего использования
                        pageEntryNames.add(archiveIterator.getEntryName());

                        // Добавление пути к файлу к "главам"
                        separateIntoChapters(chapterPages, entryName);
                    }
                    break;
                }
            }
        }

        if (!isBookFile) {
            // Установка путей к страницам в архиве
            setPageEntryNames(pageEntryNames);
        }

        setChapterPages(chapterPages);
    }

    private void createForFictionBook() throws IOException, ParserConfigurationException, SAXException {
        // Чтение FictionBook
        FictionBook fictionBook = new FictionBook(new File(archivePath));

        // Поиск и чтение обложки
        InputStream stream = fictionBook.getDescription().getTitleInfo().getCoverPage()
                .stream()
                .filter(image -> GUString.isNotEmpty(image.getValue()))
                .limit(1)
                .map(Image::getValue)
                .map(imageId -> imageId.replace("#", ""))
                .map(imageId -> fictionBook.getBinaries().get(imageId))
                .filter(Objects::nonNull)
                .map(binary -> Base64.getDecoder().decode(binary.getBinary().replace("\n", "").getBytes(StandardCharsets.UTF_8)))
                .filter(Objects::nonNull)
                .map(ByteArrayInputStream::new)
                .findAny()
                .orElse(null);

        setCoverStream(stream);

        setFictionBookPair(new Pair<>(archivePath, fictionBook));
    }

    private void createForPDF() {
        PDFRenderer renderer = PDFRenderer.create(archivePath);
        PDDocument document = renderer.getDocumentNonCacheable();
        setPdDocumentInformationPair(new Pair<>(document.getNumberOfPages(), document.getDocumentInformation()));
        renderBookCover(renderer, 100);
        GUFile.closeQuietly(document);
    }

    private void createForDVJU() {
        DjVuRenderer renderer = DjVuRenderer.create(archivePath);
        setDjVuBookPagesCount(renderer.getBook().getTotalPages());
        renderBookCover(renderer, 0.3);
    }

    private void renderBookCover(AbstractRenderer renderer, double scaleOrDpi) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (renderer.renderPage(baos, 1, scaleOrDpi)) {
            setCoverStream(new ByteArrayInputStream(baos.toByteArray()));
        }
    }

    private void importChapters(Map<String, List<String>> chapterPages, IArchiveIterator archiveIterator, String archiveHash) {
        for (Map.Entry<String, List<String>> entry : chapterPages.entrySet()) {
            String chapterPath = entry.getKey();
            List<String> pagePaths = entry.getValue();

            pagePaths.sort((path1, path2) -> IArchiveIterator.natSortComparator.compare(path1.toLowerCase(), path2.toLowerCase()));

            BookChapter chapter = new BookChapter();
            String archivePath = archiveIterator.getArchivePath();
            InputStream stream = getChapterJsonInfoStream().get(chapterPath.replace("/", "|").replace("\\", "|"));
            if (stream != null) {
                try {
                    BookInfo.fromJSON(chapter, IOUtils.toString(stream, StandardCharsets.UTF_8), chapterPath, archivePath, archiveHash);
                    log(Importer.fileLogger, "Found chapter in archive metadata with title = '" + chapter.getTitle() + "' and id = " + chapter.getChapterId());
                } catch (IOException ex) {
                    logger.error("Unable to read chapter_info.json stream...", ex);
                    logE(Importer.fileLogger, "Unable to read chapter_info.json stream...", ex);
                    return;
                } finally {
                    GUFile.closeQuietly(stream);
                }
            } else {
                chapter = new BookChapter(getChapterTitle(chapterPath, archivePath), chapterPath, archiveHash);
                log(Importer.fileLogger, "Found chapter in archive with title = '" + chapter.getTitle() + "' and id = " + chapter.getChapterId());
            }

            fillChapterData(chapter, pagePaths);
            getChapters().add(chapter);
        }
    }

    private void fillChapterData(BookChapter chapter, List<String> pagePaths) {
        chapter.setCreatedAt(System.currentTimeMillis());
        chapter.setPageEntryNames(pagePaths);

        if (chapter.getUpdatedAt() == null || chapter.getUpdatedAt() <= 0) {
            chapter.setUpdatedAt(System.currentTimeMillis());
        }
    }

    private String getChapterTitle(String chapterPath, String archivePath) {
        if (GUString.isNotEmpty(chapterPath)) {
            String chapterTitle = GUFile.getDirName(chapterPath);
            if (GUString.isEmpty(chapterTitle)) {
                return chapterPath;
            }
            return chapterTitle;
        } else {
            return GUFile.getFileName(archivePath);
        }
    }

    private boolean isCoverFile(String fileName) {
        fileName = GUFile.getFileNameWithExt(fileName);
        return fileName.startsWith(ZERO_COVER_FILENAME_START) || fileName.contains(COVER_FILENAME_START);
    }

    private void separateIntoChapters(Map<String, List<String>> chapters, String entryName) {
        String chapterPath = GUFile.getPath(entryName);
        List<String> filePaths = chapters.get(chapterPath);
        if (GUArray.isEmpty(filePaths)) {
            filePaths = new ArrayList<>();
            chapters.put(chapterPath, filePaths);
        }

        filePaths.add(entryName);
    }

    private void fillBookArchive(BookArchive bookArchive) throws IOException {
        if (bookJsonInfoStream != null) {
            log(Importer.fileLogger, "Reading info from " + BOOK_JSON_INFO_FILENAME);
            hasMetadata = BookInfo.fromJSON(bookArchive, IOUtils.toString(bookJsonInfoStream, StandardCharsets.UTF_8));
        } else if (xmlInfoStream != null) {
            log(Importer.fileLogger, "Reading info from " + XML_INFO_FILENAME);
            hasMetadata = ComicInfo.readComicInfo(bookArchive, xmlInfoStream);
        } else if (opfInfoStream != null) {
            log(Importer.fileLogger, "Reading info from .*" + OPF_INFO_EXTENSION);
            hasMetadata = EpubOPF.readInfo(bookArchive, opfInfoStream);
        } else if (fictionBookPair != null) {
            log(Importer.fileLogger, "Reading info from FictionBook");
            hasMetadata = FictionBookInfo.readInfo(bookArchive, fictionBookPair.getFirst(), fictionBookPair.getSecond());
        } else if (pdDocumentInformationPair != null) {
            log(Importer.fileLogger, "Reading info from PDF");
            hasMetadata = PDFInfo.readInfo(bookArchive, pdDocumentInformationPair.getSecond(), pdDocumentInformationPair.getFirst());
        } else if (djVuBookPagesCount > 0) {
            log(Importer.fileLogger, "Reading info from DjVu");
            DjVuInfo.readInfo(bookArchive, djVuBookPagesCount);
        }

        if (!hasMetadata) {
            logW(Importer.fileLogger, "No supported info file found! Filling base info");
        }

        if (GUString.isEmpty(bookArchive.getTitle())) {
            bookArchive.setTitle(GUFile.getFileName(archivePath));
        }

        if (isBookFile) {
            bookArchive.setIsBook(true);
            if (bookArchive.getContentType() == ContentType.UNKNOWN) {
                bookArchive.setContentType(ContentType.LIGHT_NOVEL.toString());
            }
        }
    }

    private void saveCoverIntoCache(String itemHash, @Nullable String serieHash) {
        if (forceUpdateCovers) {
            log(Importer.fileLogger, "Saving cover image as " + itemHash + ".png file");
            if (coverStream == null) {
                logger.error("Cover image for hash: [" + itemHash + "] not found!");
                logW(Importer.fileLogger, "Cover image for hash: [" + itemHash + "] not found!");
                return;
            }

            images = ImageCache.saveToFile(
                    coverStream,
                    itemHash,
                    Constants.PNG
            );

            if (serieCoverStream != null && GUString.isNotEmpty(serieHash)) {
                ImageCache.saveToFile(
                        serieCoverStream,
                        serieHash,
                        Constants.PNG
                );
            }

            images.setAccent(calculateCoverAccent(images.getOriginalBufferedImage()));
            images.setOriginalBufferedImage(null);
        }
    }

    private void putChapterJsonInfoStream(String entryName, InputStream stream) {
        chapterJsonInfoStream.put(GUFile.getPath(entryName).replace("/", "|").replace("\\", "|"), stream);
    }

    private String createSerieHash(String itemHash, boolean asSingle) {
        return HashHelper.getMHash2(Constants.SERIE_HASH_TAG, createValidPathForHashing(!asSingle ? parentPath : itemHash));
    }

    private String createOldArchiveHash() {
        return HashHelper.getMHash2(Constants.ARCHIVE_HASH_TAG, createValidPathForHashing(archivePath));
    }

    public static String createValidPathForHashing(String path) {
        return path.replace("#", "");
    }

    private String calculateCoverAccent(BufferedImage image) {
        if (image != null) {
            return getCoverAccent(image);
        }
        return null;
    }

    private String getCoverAccent(BufferedImage bufferedImage) {
        Palette palette = Palette.from(bufferedImage).generate();
        return String.format("#%06x", 0xFFFFFF & getMutedColorFromPalette(palette).getRGB());
    }

    private static Color getMutedColorFromPalette(@Nullable Palette palette) {
        Color colorWithoutPaletteOpacity = Color.decode("#1c1d23");
        return palette != null
                ? palette.getMutedColor(colorWithoutPaletteOpacity)
                : colorWithoutPaletteOpacity;
    }

    private static void log(Logger logger, String message) {
        if (logger != null) {
            logger.info(message);
        }
    }

    private static void logW(Logger logger, String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }

    private static void logE(Logger logger, String message, Throwable throwable) {
        if (logger != null) {
            logger.warning(message + "\n" + throwable.toString());
        }
    }

    @Override
    public void close() {
        GUFile.closeQuietly(archiveIterator);
        GUFile.closeQuietly(xmlInfoStream);
        GUFile.closeQuietly(opfInfoStream);
        GUFile.closeQuietly(bookJsonInfoStream);
        GUFile.closeQuietly(serieJsonInfoStream);
        GUFile.closeQuietly(coverStream);
        GUFile.closeQuietly(serieCoverStream);
        chapterJsonInfoStream.values().forEach(GUFile::closeQuietly);
    }
}
