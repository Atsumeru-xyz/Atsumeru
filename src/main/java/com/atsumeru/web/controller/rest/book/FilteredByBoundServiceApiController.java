package com.atsumeru.web.controller.rest.book;

import com.atsumeru.web.model.book.BookSerie;
import com.atsumeru.web.repository.BooksDatabaseRepository;
import com.atsumeru.web.enums.LibraryPresentation;
import com.atsumeru.web.enums.ServiceType;
import com.atsumeru.web.model.book.DownloadedLinks;
import com.atsumeru.web.util.ArrayUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RestController
@RequestMapping("/api/v1/books/")
public class FilteredByBoundServiceApiController {

    @Cacheable(value = "books_by_bound_service", key = "#request.userPrincipal.name.concat('-')" +
            ".concat(\"\" + #boundServiceName).concat('-')" +
            ".concat(#boundServiceId).concat('-')")
    @GetMapping("{bound_service_name}/{bound_service_id}")
    public List<BookSerie> getBooksByBoundService(HttpServletRequest request,
                                                  @PathVariable("bound_service_name") String boundServiceName,
                                                  @PathVariable("bound_service_id") String boundServiceId) {
        return Optional.ofNullable(ServiceType.getDbFieldNameForSimpleName(boundServiceName))
                .map(dbFieldName -> BooksDatabaseRepository.getInstance().getDaoManager().query(dbFieldName, boundServiceId, BookSerie.class)
                        .stream()
                        .map(BookSerie.class::cast)
                        .peek(BookSerie::prepareBoundServices)
                        .collect(Collectors.toList())
                ).orElse(null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/check_downloaded")
    public DownloadedLinks checkLinksDownloaded(@RequestBody MultiValueMap<String, String> formData) {
        List<String> links = ArrayUtils.splitString(formData.getFirst("links"), ",");

        Set<String> downloadedLinks = BooksDatabaseRepository.getInstance().getDaoManager().queryAll(BookSerie.class, LibraryPresentation.SERIES_AND_SINGLES)
                .stream()
                .map(BookSerie.class::cast)
                .flatMap(serie -> ArrayUtils.splitString(serie.getSerieLinks()).stream())
                .collect(Collectors.toSet());

        Map<Boolean, List<String>> collected = links.stream().collect(Collectors.groupingBy(downloadedLinks::contains));
        return new DownloadedLinks(collected.get(true), collected.get(false));
    }
}
