package com.atsumeru.web.exception;

import com.atsumeru.web.archive.exception.MediaUnsupportedException;
import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.FileNotFoundException;

@ControllerAdvice
public class AtsumeruExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ImportActiveException.class)
    protected ResponseEntity<AtsumeruException> handleImportActiveException() {
        return new ResponseEntity<>(new AtsumeruException("Import is active. Content unavailable"), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MetadataUpdateActiveException.class)
    protected ResponseEntity<AtsumeruException> handleMetadataUpdateActiveException() {
        return new ResponseEntity<>(new AtsumeruException("Metadata update is active. Content unavailable"), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UserNotFoundException.class)
    protected ResponseEntity<AtsumeruException> handleUserNotFoundException() {
        return new ResponseEntity<>(new AtsumeruException("User not found"), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(NoReadableFoundException.class)
    protected ResponseEntity<AtsumeruException> handleNoReadableFoundException() {
        return new ResponseEntity<>(new AtsumeruException("Readable not found"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ChapterNotFoundException.class)
    protected ResponseEntity<AtsumeruException> handleChapterNotFoundException() {
        return new ResponseEntity<>(new AtsumeruException("Chapter not found"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PageNotFoundException.class)
    protected ResponseEntity<AtsumeruException> handlePageNotFoundException() {
        return new ResponseEntity<>(new AtsumeruException("Page not found"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NotAcceptableForOnlineReadingException.class)
    protected ResponseEntity<AtsumeruException> handleNotAcceptableFroOnlineReadableException() {
        return new ResponseEntity<>(new AtsumeruException("Readable is not acceptable for online reading"), HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(DownloadsNotAllowedException.class)
    protected ResponseEntity<AtsumeruException> handleDownloadsNotAllowedException() {
        return new ResponseEntity<>(new AtsumeruException("Downloads is not allowed"), HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(NoCoverFoundException.class)
    protected ResponseEntity<AtsumeruException> handleNoCoverFoundException() {
        return new ResponseEntity<>(new AtsumeruException("Cover not found"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(FileNotFoundException.class)
    protected ResponseEntity<AtsumeruException> handleFileNotFoundException() {
        return new ResponseEntity<>(new AtsumeruException("File not found"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ArchiveReadingException.class)
    protected ResponseEntity<AtsumeruException> handleArchiveReadingException() {
        return new ResponseEntity<>(new AtsumeruException("Error reading archive"), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(PDFReadingException.class)
    protected ResponseEntity<AtsumeruException> handlePDFReadingException() {
        return new ResponseEntity<>(new AtsumeruException("Unable to read PDF file"), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(DjVuReadingException.class)
    protected ResponseEntity<AtsumeruException> handleDjVuReadingException() {
        return new ResponseEntity<>(new AtsumeruException("Unable to read DjVu file"), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(RendererNotImplementedException.class)
    protected ResponseEntity<AtsumeruException> handleRendererNotImplementedException(RendererNotImplementedException exception) {
        return new ResponseEntity<>(new AtsumeruException(exception.getMessage()), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(MediaUnsupportedException.class)
    protected ResponseEntity<AtsumeruException> handleMediaUnsupportedException(Exception exception) {
        return new ResponseEntity<>(new AtsumeruException(exception.getMessage()), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Data
    @AllArgsConstructor
    private static class AtsumeruException {
        @Expose
        private String message;
    }
}