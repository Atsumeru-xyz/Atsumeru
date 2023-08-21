package com.atsumeru.web.helper;

import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.util.StringUtils;
import com.atsumeru.web.model.book.chapter.BookChapter;
import com.atsumeru.web.util.TypeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChapterRecognition {
    public static final float UNKNOWN = -1f;
    public static final float SINGLE_OR_EXTRA = -2f;

    private static final Pattern basicVolume = Pattern.compile("(?<![a-z])(v|ver|vol|vo|version|volume|season|s|episode|ep|том|выпуск)(.?|.\\s?)([0-9]+\\.\\d+)|([0-9]+)");
    private static final Pattern yearMonthVolume = Pattern.compile("(((19\\d\\d)( - |-)(\\d+))|((20\\d\\d)( - |-)(\\d+))|((19\\d\\d)\\.(\\d+))|((20\\d\\d)\\.(\\d+)))");
    private static final Pattern dashedVolume = Pattern.compile("(\\d+)( - |-)(\\d+)");
    private static final Pattern basicIssue = Pattern.compile("#(\\d+)");

    private static final Pattern shortVolWithNumber = Pattern.compile("v(\\d+)");

    /**
     * All cases with Ch.xx
     * Mokushiroku Alice Vol.1 Ch. 4: Misrepresentation -R> 4
     */
    private static final Pattern basic = Pattern.compile("(?<=ch\\.) *([0-9]+)(\\.[0-9]+)?(\\.?[a-z]+)?");

    /**
     * Regex used when only one number occurrence
     * Example: Bleach 567: Down With Snowwhite -R> 567
     */
    private static final Pattern occurrence = Pattern.compile("([0-9]+)(\\.[0-9]+)?(\\.?[a-z]+)?");

    /**
     * Regex used when manga title removed
     * Example: Solanin 028 Vol. 2 -> 028 Vol.2 -> 028Vol.2 -R> 028
     */
    private static final Pattern withoutManga = Pattern.compile("^([0-9]+)(\\.[0-9]+)?(\\.?[a-z]+)?");

    /**
     * Regex used to detect that we need skip number parsing if chapter is Single
     * Example: Naruto v1 - 11 -R> OK!, Naruto Single -R> SKIP!
     */
    private static final Pattern skipIfSingleOccur = Pattern.compile("(\\bsingle\\b|\\bсингл\\b)");

    /**
     * Regex used to detect that we need skip number parsing if chapter is Extra/Special
     * Example: Naruto v1 - 11 -R> OK!, Naruto 3 Extra -R> SKIP!, Naruto 11 Special -R> SKIP!
     */
    private static final Pattern skipIfExtraOrSpecialOccur = Pattern.compile("(\\bextra\\b|\\bspecial\\b|\\bomake\\b|\\bэкстра\\b|\\bекстра\\b|\\bспешл\\b|\\bспэшл\\b)");

    /**
     * Regex for fixing volume definition
     * Example: Naruto 3 - 65 -R> Naruto v3 - 65
     */
    private static final Pattern volumeNumberFix = Pattern.compile("(\\d+ - \\d+)|(\\d+-\\d+)");

    private static final Pattern unwantedChapter = Pattern.compile("(?<![a-z])(ch|chapter).?[0-9]+");

    /**
     * Regex used to remove unwanted tags
     * Example Prison School 12 v.1 vol004 version1243 volume64 -R> Prison School 12
     */
    private static final Pattern unwanted = Pattern.compile("(?<![a-z])(v|ver|vol|version|volume|season|s|episode|ep|том|выпуск).?[0-9]+");

    /**
     * Regex used to remove unwanted whitespace
     * Example One Piece 12 special -R> One Piece 12special
     */
    private static final Pattern unwantedWhiteSpace = Pattern.compile("(\\s)(extra|special|omake|экстра|екстра|спешл|спэшл)");

    public static void parseNumbers(BookArchive bookArchive) {
        parseVolumeNumber(bookArchive, new File(bookArchive.getFolder()).getName(), false, false);
        if (bookArchive.getVolume() == UNKNOWN) {
            parseVolumeNumber(bookArchive, bookArchive.getTitle(), false, false);
        }
    }

    public static void parseNumbers(BookArchive bookArchive, BookChapter bookChapter) {
        parseChapterNumber(bookArchive, bookChapter, true, true);
    }

    public static void parseVolumeNumber(BookArchive bookArchive, String name, boolean isSkipSingles, boolean isSkipExtras) {
        // If volume number is known return
        if (bookArchive.getVolume().equals(SINGLE_OR_EXTRA) || bookArchive.getVolume() > UNKNOWN) {
            return;
        }

        // Set name lower case
        name = name.toLowerCase();

        // Remove comma's from volume
        name = name.replace(',', '.')
                // Remove Comiket numbers from name
                .replaceAll("^\\(c\\d+\\)", "");

        Matcher shortVolWithNumberMatcher = shortVolWithNumber.matcher(name);
        if (shortVolWithNumberMatcher.find()) {
            name = name.replace(shortVolWithNumberMatcher.group(), "vol." + shortVolWithNumberMatcher.group(1));
        }

        // If name contains defined values - skip parsing
        if (isSkipSingles && skipIfSingleOccur.matcher(name).find() || isSkipExtras && skipIfExtraOrSpecialOccur.matcher(name).find()) {
            bookArchive.setVolume(SINGLE_OR_EXTRA);
            return;
        }

        // Remove unwanted white spaces
        Matcher unwantedWhiteSpaceMatcher = unwantedWhiteSpace.matcher(name);
        while (unwantedWhiteSpaceMatcher.find()) {
            String occurence = unwantedWhiteSpaceMatcher.group();
            name = name.replace(occurence, occurence.trim());
        }

        // Remove unwanted tags
        Matcher unwantedMatcher = unwantedChapter.matcher(name);
        while (unwantedMatcher.find()) {
            name = name.replace(unwantedMatcher.group(), "");
        }

        Matcher basicIssueMatcher = basicIssue.matcher(name);
        if (updateVolume(basicIssueMatcher, bookArchive, 1)) {
            return;
        }

        // Check year with month
        Matcher yearMonthMatcher = yearMonthVolume.matcher(name);
        if (updateYearMonthVolume(yearMonthMatcher, bookArchive)) {
            return;
        }

        // Check volume
        Matcher basicMatcher = basicVolume.matcher(name);
        if (updateVolume(basicMatcher, bookArchive, 3, 4)) {
            return;
        }

        // Check volume dashed
        Matcher dashedMatcher = dashedVolume.matcher(name);
        if (updateVolume(dashedMatcher, bookArchive, 1)) {
            return;
        }

        // If name contains defined values - skip parsing
        if (skipIfSingleOccur.matcher(name).find() || skipIfExtraOrSpecialOccur.matcher(name).find()) {
            bookArchive.setVolume(SINGLE_OR_EXTRA);
        }
    }

    public static void parseChapterNumber(BookArchive bookArchive, BookChapter bookChapter, boolean isSkipSingles, boolean isSkipExtras) {
        // If chapter number is known return
        if (bookChapter.getChapter().equals(SINGLE_OR_EXTRA) || bookChapter.getChapter() > UNKNOWN) {
            return;
        }

        // Get chapter title with lower case
        String name = bookChapter.getTitle().toLowerCase();

        // Remove comma's from chapter
        name = name.replace(',', '.');

        // If name contains defined values - skip parsing
        if (isSkipSingles && skipIfSingleOccur.matcher(name).find() || isSkipExtras && skipIfExtraOrSpecialOccur.matcher(name).find()) {
            bookChapter.setChapter(SINGLE_OR_EXTRA);
            return;
        }

        // Fixing volume definition
        Matcher volumeNumberFixMatcher = volumeNumberFix.matcher(name);
        while (volumeNumberFixMatcher.find()) {
            String occurrence = volumeNumberFixMatcher.group();
            name = name.replace(occurrence, "v" + occurrence.trim());
        }

        // Remove unwanted white spaces
        Matcher unwantedWhiteSpaceMatcher = unwantedWhiteSpace.matcher(name);
        while (unwantedWhiteSpaceMatcher.find()) {
            String occurence = unwantedWhiteSpaceMatcher.group();
            name = name.replace(occurence, occurence.trim());
        }

        // Remove unwanted tags
        Matcher unwantedMatcher = unwanted.matcher(name);
        while (unwantedMatcher.find()) {
            name = name.replace(unwantedMatcher.group(), "");
        }

        // Check base case ch.xx
        Matcher basicMatcher = basic.matcher(name);
        if (updateChapter(basicMatcher, bookChapter)) {
            return;
        }

        // Check one number occurrence
        List<Matcher> occurrences = new ArrayList<>();
        Matcher occurrenceMatcher = occurrence.matcher(name);
        while (occurrenceMatcher.find()) {
            occurrences.add(occurrenceMatcher);
        }

        if (occurrences.size() == 1) {
            if (updateChapter(occurrences.get(0), bookChapter)) {
                return;
            }
        }

        // Remove manga title from chapter title
        String nameWithoutManga = getNameWithoutArchiveName(bookArchive, name);

        // Check if first value is number after title remove
        Matcher withoutMangaMatcher = withoutManga.matcher(nameWithoutManga);
        if (updateChapter(withoutMangaMatcher, bookChapter)) {
            return;
        }

        // Take the first number encountered
        Matcher occurrence2Matcher = occurrence.matcher(nameWithoutManga);
        updateChapter(occurrence2Matcher, bookChapter);
    }

    private static String getNameWithoutArchiveName(BookArchive bookArchive, String name) {
        if (StringUtils.isNotEmpty(bookArchive.getTitle()) && StringUtils.equalsIgnoreCase(bookArchive.getTitle(), name)) {
            return name.replace(bookArchive.getTitle().toLowerCase(), "").trim();
        }
        if (StringUtils.isNotEmpty(bookArchive.getAltTitle()) && StringUtils.equalsIgnoreCase(bookArchive.getAltTitle(), name)) {
            return name.replace(bookArchive.getAltTitle().toLowerCase(), "").trim();
        }
        return name;
    }

    public static String removeChapterNumbers(String title) {
        String newTitle = title.replace(".", "").replace(",", "");
        Pattern pattern = Pattern.compile("^(Том|Vol|Volume)(.|\\s)(\\d+)([^a-zA-Z0-9]+)(\\d+)|(\\d+)([^a-zA-Z0-9]+)(\\d+)");
        Matcher matcher = pattern.matcher(newTitle);

        if (matcher.find()) {
            String fullMatch = matcher.group(0);
            newTitle = newTitle.substring(newTitle.indexOf(fullMatch) + fullMatch.length());
        } else {
            newTitle = title;
        }

        return newTitle.trim()
                .replaceAll("^-", "")
                .replaceAll("^\\.", "")
                .replaceAll("^,", "")
                .replaceAll("^:", "")
                .trim();
    }

    private static boolean updateYearMonthVolume(Matcher match, BookArchive bookArchive) {
        if (match != null && match.find()) {
            String initialGroup = match.group(1).replace(" - ", ".").replace("-", ".");
            float initial = TypeUtils.getFloatDef(initialGroup, UNKNOWN);
            if (initial >= 0f) {
                bookArchive.setVolume(initial);
                return true;
            }
        }
        return false;
    }

    private static boolean updateVolume(Matcher match, BookArchive bookArchive, int...groups) {
        if (match != null && match.find()) {
            for (int group : groups) {
                String initialGroup = match.group(group);
                float initial = TypeUtils.getFloatDef(initialGroup, UNKNOWN);
                if (initial >= 0f) {
                    bookArchive.setVolume(initial);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if volume is found and update chapter
     * @param match result of regex
     * @param bookChapter chapter object
     * @return true if volume is found
     */
    private static boolean updateChapter(Matcher match, BookChapter bookChapter) {
        if (match != null && match.find()) {
            String initialGroup = match.group(1);
            String subChapterDecimal = match.group(2);
            String subChapterAlpha = match.group(3);

            float initial = initialGroup != null ? Float.parseFloat(initialGroup) : UNKNOWN;

            float addition = checkForDecimal(subChapterDecimal, subChapterAlpha);
            bookChapter.setChapter(initial + addition);
            return true;
        }
        return false;
    }

    /**
     * Check for decimal in received strings
     * @param decimal decimal value of regex
     * @param alpha alpha value of regex
     * @return decimal/alpha float value
     */
    private static float checkForDecimal(String decimal, String alpha) {
        if (StringUtils.isNotEmpty(decimal)) {
            return Float.parseFloat(decimal);
        }

        if (StringUtils.isNotEmpty(alpha)) {
            if (alpha.contains("extra"))
                return .99f;

            if (alpha.contains("omake"))
                return .98f;

            if (alpha.contains("special"))
                return .97f;

            if (alpha.charAt(0) == '.') {
                // Take value after (.)
                return parseAlphaPostFix(alpha.charAt(1));
            } else {
                return parseAlphaPostFix(alpha.charAt(0));
            }
        }

        return .0f;
    }

    /**
     * x.a -> x.1, x.b -> x.2, etc
     */
    private static float parseAlphaPostFix(char alpha) {
        return Float.parseFloat("0." + (alpha - 96));
    }
}
