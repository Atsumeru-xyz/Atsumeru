package com.atsumeru.web.controller.rest.filesystem;

import com.google.gson.annotations.Expose;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/files/explore")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('IMPORTER')")
public class FileSystemApiController {
    private static final String RECYCLE_BIN_FOLDER = "$recycle.bin";
    private static final String SVI_FOLDER = "system volume information";

    private final FileSystem fs = FileSystems.getDefault();

    @PostMapping
    public DirectoryListingDto getDirectoryListing(@RequestBody(required = false) DirectoryRequestDto request) {
        if (request == null || request.path.isEmpty()) {
            List<PathDto> paths = new ArrayList<>();
            fs.getRootDirectories().forEach(it -> paths.add(pathToDto(it)));
            return new DirectoryListingDto(null, paths);
        } else {
            Path path = fs.getPath(request.path);
            if (!path.isAbsolute()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path must be absolute");
            }
            try {
                String parent = path.getParent() != null ? path.getParent().toString() : "";
                List<PathDto> list = Files.list(path)
                        .sequential()
                        .filter(Files::isDirectory)
                        .filter(it -> {
                            try {
                                return !Files.isHidden(it);
                            } catch (IOException exception) {
                                exception.printStackTrace();
                            }
                            return true;
                        })
                        .filter(it -> {
                            String fileName = it.getFileName().toString();
                            return !RECYCLE_BIN_FOLDER.equalsIgnoreCase(fileName) && !SVI_FOLDER.equalsIgnoreCase(fileName);
                        })
                        .sorted(Comparator.comparing(Path::toString, String.CASE_INSENSITIVE_ORDER))
                        .map(this::pathToDto)
                        .collect(Collectors.toList());

                return new DirectoryListingDto(parent, list);
            } catch (IOException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path does not exist");
            }
        }
    }

    private PathDto pathToDto(Path path) {
        return new PathDto(
                Files.isDirectory(path) ? "directory" : "file",
                path.getFileName() != null ? path.getFileName().toString() : path.toString(),
                path.toString()
        );
    }

    static class DirectoryRequestDto {
        @Expose String path;

        DirectoryRequestDto(String path) {
            this.path = path;
        }
    }

    static class DirectoryListingDto {
        @Expose String parent;
        @Expose List<PathDto> directories;

        DirectoryListingDto(String parent, List<PathDto> directories) {
            this.parent = parent;
            this.directories = directories;
        }
    }

    static class PathDto {
        @Expose String type;
        @Expose String name;
        @Expose String path;

        PathDto(String type, String name, String path) {
            this.type = type;
            this.name = name;
            this.path = path;
        }
    }
}
