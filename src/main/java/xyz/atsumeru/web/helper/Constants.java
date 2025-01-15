package xyz.atsumeru.web.helper;

public final class Constants {

    public static String[] SUPPORTED_SINGLE_FILES = {
            // Archive extensions
            Formats.SEVEN_ZIP,
            Formats.RAR,
            Formats.ZIP,
            Formats.CB7,
            Formats.CBR,
            Formats.CBZ,

            // Book extensions
            Formats.EPUB,
            Formats.FB2,
            Formats.PDF,
            Formats.DJVU
    };

    public static class Hashes {
        public static final String ARCHIVE_HASH_TAG = "atsumeru";
        public static final String SERIE_HASH_TAG = "atsumeru-serie";

        public static final String ATTRIBUTE_HASH = "user:atsumeru_hash";
        public static final String ATTRIBUTE_SERIE_HASH = "user:atsumeru_serie_hash";
    }

    public static class Formats {
        // Images
        public static final String BMP = "bmp";
        public static final String JPG = "jpg";
        public static final String JPEG = "jpeg";
        public static final String PNG = "png";
        public static final String GIF = "gif";
        public static final String WEBP = "webp";
        public static final String AVIF = "avif";
        public static final String HEIC = "heic";
        public static final String HEIF = "heif";

        // Archives
        public static final String SEVEN_ZIP = "7z";
        public static final String RAR = "rar";
        public static final String ZIP = "zip";
        public static final String CB7 = "cb7";
        public static final String CBR = "cbr";
        public static final String CBZ = "cbz";

        // Books
        public static final String EPUB = "epub";
        public static final String FB2 = "fb2";
        public static final String PDF = "pdf";
        public static final String DJVU = "djvu";

        // Json
        public static final String JSON = "json";
    }

    public static class MimeTypes {
        public static final String IMAGE_JPG = "image/jpeg";
        public static final String IMAGE_JPEG = "image/jpeg";
        public static final String IMAGE_PNG = "image/png";
        public static final String IMAGE_GIF = "image/gif";
        public static final String IMAGE_WEBP = "image/webp";
        public static final String IMAGE_AVIF = "image/avif";
        public static final String IMAGE_HEIC = "image/heic";
        public static final String IMAGE_HEIF = "image/heif";
    }
}
