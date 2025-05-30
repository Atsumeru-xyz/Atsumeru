package xyz.atsumeru.web.controller.rest.book;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import xyz.atsumeru.web.Beans;
import xyz.atsumeru.web.enums.LibraryPresentation;
import xyz.atsumeru.web.enums.ServiceType;
import xyz.atsumeru.web.model.book.BookSerie;
import xyz.atsumeru.web.model.book.DownloadedLinks;
import xyz.atsumeru.web.util.ArrayUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RestController
@RequestMapping("/api/v1/books/")
@Tag(name = "Filtering by Bound Service", description = "API for Books requesting by Bound Service connection")
public class FilteredByBoundServiceApiController {

    @Operation(summary = "Books by Bound Service", description = "Get Books list by Bound Service Name and ID")
    @Cacheable(value = "books_by_bound_service", key = "#request.userPrincipal.name.concat('-')" +
            ".concat(\"\" + #boundServiceName).concat('-')" +
            ".concat(#boundServiceId).concat('-')")
    @GetMapping("{bound_service_name}/{bound_service_id}")
    public List<BookSerie> getBooksByBoundService(HttpServletRequest request,
                                                  @PathVariable("bound_service_name") String boundServiceName,
                                                  @PathVariable("bound_service_id") String boundServiceId) {
        return Optional.ofNullable(ServiceType.getDbFieldNameForSimpleName(boundServiceName))
                .map(dbFieldName -> Beans.getBooksDaoManager().query(dbFieldName, boundServiceId, BookSerie.class)
                        .stream()
                        .map(BookSerie.class::cast)
                        .peek(BookSerie::prepareBoundServices)
                        .collect(Collectors.toList())
                ).orElse(null);
    }

    @Operation(summary = "Check Book present", description = "Check if Book is present in database by Download Link")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/check_downloaded")
    public DownloadedLinks checkLinksDownloaded(@RequestBody MultiValueMap<String, String> formData) {
        List<String> links = ArrayUtils.splitString(formData.getFirst("links"), ",");

        Set<String> downloadedLinks = Beans.getBooksDaoManager()
                .queryAll(BookSerie.class, LibraryPresentation.SERIES_AND_SINGLES)
                .stream()
                .map(BookSerie.class::cast)
                .flatMap(serie -> ArrayUtils.splitString(serie.getSerieLinks()).stream())
                .collect(Collectors.toSet());

        Map<Boolean, List<String>> collected = links.stream().collect(Collectors.groupingBy(downloadedLinks::contains));
        return new DownloadedLinks(collected.get(true), collected.get(false));
    }
}
