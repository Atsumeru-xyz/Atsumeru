package com.atsumeru.web.controller.rest.uploader;

import com.atsumeru.web.repository.BooksRepository;
import com.atsumeru.web.util.ContentDetector;
import com.atsumeru.web.util.GUString;
import com.atsumeru.web.archive.CBZPacker;
import com.atsumeru.web.manager.AtsumeruCacheManager;
import com.atsumeru.web.archive.iterator.SevenZipIterator;
import com.atsumeru.web.util.GUEnum;
import com.atsumeru.web.util.GUFile;
import com.atsumeru.web.component.Localizr;
import com.atsumeru.web.configuration.FileWatcherConfig;
import com.atsumeru.web.helper.RestHelper;
import com.atsumeru.web.model.AtsumeruMessage;
import com.atsumeru.web.util.WorkspaceUtils;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import static java.util.zip.Deflater.NO_COMPRESSION;

@RestController
@RequestMapping(UploaderApiController.ROOT_ENDPOINT)
@PreAuthorize("hasRole('ADMIN') or hasAuthority('UPLOADER')")
public class UploaderApiController {
    private static final Logger logger = LoggerFactory.getLogger(UploaderApiController.class.getSimpleName());

    public static final String ROOT_ENDPOINT = "/api/v1/uploader";
    private static final String UPLOAD_ENDPOINT = "/upload";

    @PostMapping(UPLOAD_ENDPOINT)
    public ResponseEntity<AtsumeruMessage> uploadFile(@RequestParam("hash") String hash,
                                                      @RequestParam(value = "override", defaultValue = "false", required = false) boolean overrideFile,
                                                      @RequestParam(value = "repack", defaultValue = "false", required = false) boolean repackArchive,
                                                      @RequestParam(value = "type", defaultValue = "CBZ", required = false) String archiveType,
                                                      @RequestParam("file") MultipartFile file) {
        if (!BooksRepository.isSeriesHash(hash)) {
            return RestHelper.createResponseMessage(
                    Localizr.toLocale("error.upload.serie_not_exist"),
                    HttpStatus.NOT_FOUND.value(),
                    HttpStatus.OK
            );
        }
        if (!file.isEmpty()) {
            if (GUString.isEmpty(file.getOriginalFilename())) {
                return RestHelper.createResponseMessage(
                        Localizr.toLocale("error.upload.empty_filename"),
                        HttpStatus.NOT_ACCEPTABLE.value(),
                        HttpStatus.OK
                );
            }

            FileWatcherConfig.destroy();

            String serieFolder = BooksRepository.getBookDetails(hash).getFolder();
            File outputFile = new File(serieFolder, file.getOriginalFilename());
            ResponseEntity<AtsumeruMessage> message = writeMultipartFile(file, outputFile, overrideFile);

            if (repackArchive && ContentDetector.isRepackableArchive(outputFile.toPath())) {
                return repackArchive(outputFile, GUEnum.valueOf(CBZPacker.ArchiveType.class, archiveType));
            }

            AtsumeruCacheManager.evictAll();
            FileWatcherConfig.start();

            return message;
        } else {
            return RestHelper.createResponseMessage(
                    Localizr.toLocale("error.upload.empty_file"),
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                    HttpStatus.OK
            );
        }
    }

    private ResponseEntity<AtsumeruMessage> writeMultipartFile(MultipartFile file, File resultFile, boolean overrideFile) {
        if (!resultFile.exists() || overrideFile) {
            BufferedOutputStream bos = null;
            try {
                byte[] bytes = file.getBytes();
                bos = new BufferedOutputStream(new FileOutputStream(resultFile));
                bos.write(bytes);
                bos.close();

                return RestHelper.createResponseMessage(
                        Localizr.toLocale("success.upload.file_uploaded"),
                        HttpStatus.OK
                );
            } catch (Exception e) {
                return RestHelper.createResponseMessage(
                        Localizr.toLocale("error.upload.unable_to_load_with_exception", file.getOriginalFilename(), e.getMessage()),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        HttpStatus.OK
                );
            } finally {
                GUFile.closeQuietly(bos);
            }
        } else {
            return RestHelper.createResponseMessage(
                    Localizr.toLocale("error.upload.file_exists", file.getOriginalFilename()),
                    HttpStatus.FORBIDDEN.value(),
                    HttpStatus.OK
            );
        }
    }

    @SneakyThrows
    private ResponseEntity<AtsumeruMessage> repackArchive(File inputFile, CBZPacker.ArchiveType archiveType) {
        String absoluteFilePath = inputFile.getAbsolutePath();
        String outputArchivePath = inputFile.getParent();
        String fileName = GUFile.getFileName(absoluteFilePath);
        String fileExt = GUFile.getFileExt(absoluteFilePath);
        String outputTempDir = WorkspaceUtils.TEMP_DIR + GUFile.addPathSlash(fileName);

        if (GUString.equalsIgnoreCase(fileExt, archiveType.toString())) {
            return RestHelper.createResponseMessage(
                    Localizr.toLocale("success.upload.file_uploaded_not_repacked"),
                    HttpStatus.OK
            );
        }

        CBZPacker packer = new CBZPacker(outputTempDir);

        logger.info("Unpacking file at " + absoluteFilePath);
        boolean success = SevenZipIterator.unpack(inputFile, outputTempDir);
        if (success) {
            logger.info("Repacking file from temp dir... ");
            packer.setOutputDir(outputArchivePath)
                    .setPackVariant(CBZPacker.PackVariant.CONTENT_OF_CURRENT_FOLDER)
                    .setOutputFileNameVariant(CBZPacker.CBZNameVariant.CURRENT_FOLDER_NAME)
                    .setCompressionLevel(NO_COMPRESSION)
                    .setArchiveType(archiveType);


            packer.pack();
            inputFile.delete();
            GUFile.deleteDirectory(new File(outputTempDir));
            logger.info("Deleted file after repack: " + absoluteFilePath);

            return RestHelper.createResponseMessage(
                    Localizr.toLocale("success.upload.file_uploaded_and_repacked"),
                    HttpStatus.OK
            );
        } else {
            logger.error("Unable to unpack file");
            return RestHelper.createResponseMessage(
                    Localizr.toLocale("error.upload.unable_to_unpack_file"),
                    HttpStatus.UNPROCESSABLE_ENTITY.value(),
                    HttpStatus.OK
            );
        }
    }
}
