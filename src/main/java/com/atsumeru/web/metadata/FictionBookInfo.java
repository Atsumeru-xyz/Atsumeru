package com.atsumeru.web.metadata;

import com.atsumeru.web.component.Localizr;
import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.util.TypeUtils;
import com.kursx.parser.fb2.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

public class FictionBookInfo {

    public static boolean readInfo(BookArchive bookArchive, String filePath, FictionBook fictionBook) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            Document xml = Jsoup.parse(fis, "UTF-8", "", Parser.xmlParser());

            TitleInfo titleInfo = fictionBook.getDescription().getTitleInfo();
            PublishInfo publishInfo = fictionBook.getDescription().getPublishInfo();

            bookArchive.setTitle(Optional.ofNullable(titleInfo.getSequence())
                    .map(Sequence::getName)
                    .orElse(titleInfo.getBookTitle()));
            bookArchive.setAltTitle(fictionBook.getTitle());
            bookArchive.setAuthors(titleInfo.getAuthors()
                    .stream()
                    .map(Person::getFullName)
                    .collect(Collectors.joining(",")));
            bookArchive.setTranslators(titleInfo.getTranslators()
                    .stream()
                    .map(Person::getFullName)
                    .collect(Collectors.joining(",")));
            bookArchive.setPublisher(publishInfo.getPublisher());
            bookArchive.setYear(publishInfo.getYear());
            // TODO: Language localization
            bookArchive.setLanguage(fictionBook.getLang());
            bookArchive.setVolume(Optional.ofNullable(titleInfo.getSequence())
                    .map(Sequence::getNumber)
                    .map(number -> TypeUtils.getFloatDef(number, -1f))
                    .orElse(-1f));
            bookArchive.setTags(titleInfo.getGenres()
                    .stream()
                    .map(genre -> "fb2_" + genre)
                    .map(Localizr::toLocale)
                    .collect(Collectors.joining(",")));

            bookArchive.setDescription(Optional.ofNullable(xml.select("description > title-info > annotation"))
                    .map(Elements::text)
                    .orElse(null));

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
