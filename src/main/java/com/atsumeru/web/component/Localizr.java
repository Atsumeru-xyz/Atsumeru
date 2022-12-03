package com.atsumeru.web.component;

import com.atsumeru.web.enums.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class Localizr {
    private static ResourceBundleMessageSource messageSource;

    @Autowired
    Localizr(ResourceBundleMessageSource messageSource) {
        Localizr.messageSource = messageSource;
    }

    public static String toLocale(String msgCode) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            return messageSource.getMessage(msgCode, null, locale);
        } catch (Exception ex) {
            return "Unlocalized";
        }
    }

    public static String toLocale(String msgCode, String... formatArgs) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            return String.format(messageSource.getMessage(msgCode, null, locale), formatArgs);
        } catch (Exception ex) {
            return "Unlocalized";
        }
    }

    public static String getFormatterForVolumeOrIssue(ContentType contentType, boolean archiveMode) {
        return contentType == ContentType.COMICS
                ? toLocale(archiveMode ? "web.issue" : "web.serie_issue")
                : toLocale(archiveMode ? "web.volume" : "web.serie_volume");
    }
}